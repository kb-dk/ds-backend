package dk.kb.datahandler.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import dk.kb.datahandler.api.v1.impl.DsDatahandlerApiServiceImpl;
import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.facade.DsDatahandlerFacade;
import dk.kb.datahandler.model.v1.CategoryDto;
import dk.kb.datahandler.model.v1.JobDto;
import dk.kb.datahandler.model.v1.JobStatusDto;
import dk.kb.datahandler.model.v1.TypeDto;
import dk.kb.datahandler.solr.RecordIndexResult;
import dk.kb.datahandler.solr.SolrIndexResponse;
import dk.kb.datahandler.solr.SolrResponseHeader;
import dk.kb.present.model.v1.FormatDto;
import dk.kb.storage.model.v1.DsRecordMinimalDto;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.Service2ServiceRequest;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SolrUtils {
    
    
   /**
    * Tracks whether a suggest index build is currently running in the background.
    * Used to prevent {@link #buildSuggestIndex()} from starting a second build
    * while one is already in progress.
    * 
    * Declared {@code volatile} so that when the background build thread updates
    * this flag (e.g. sets it back to {@code false} when the build finishes),
    * that change is immediately visible to other threads. Without {@code volatile},
    * a thread could keep reading a stale, cached value and incorrectly think a
    * build is still running (or not running) after the actual state has changed.
    */
    private static volatile boolean suggestIndexBuildRunning = false;
    
    private static final Logger log = LoggerFactory.getLogger(SolrUtils.class);


    /**
     * Get the latest MTime for records in the backing storage represented in the existing solr index
     * for the requested origin.
     * @param origin to extract latest mTime for.
     * @return the latest mTime as a long representing time in epoch with three added digits.
     */
    public static Long getLatestMTimeForOrigin(String origin) throws SolrServerException, IOException {
        // Solr query client
        String solrQueryUrl = ServiceConfig.getSolrQueryUrl();
        String storageMTime;
        QueryResponse response;
        try (SolrClient solrClient = new HttpJdkSolrClient.Builder(solrQueryUrl).build()) {
            storageMTime = "internal_storage_mTime";

            // Perform a query
            SolrQuery query = new SolrQuery("origin:" + origin + " AND " + storageMTime + ":*");
            query.setFields(storageMTime);
            query.setSort(storageMTime, SolrQuery.ORDER.desc);
            query.setRows(1);
            // Have to add facet and highlights like this as the query.setFacet and query.setHighlight aren't appended
            // to the query.
            query.add("facet", "false");
            query.add("hl", "false");

            // Parse response to get last modified field
            response = solrClient.query(query);
        } catch (Exception exception) {
            log.error("Exception: ", exception);
            throw new InternalServiceException(exception.getMessage());
        }

        if (!response.getResults().isEmpty()) {
            Long lastStorageMTime = (Long) response.getResults().get(0).getFieldValue(storageMTime);
            return lastStorageMTime;
        } else {
            return 0L;
        }
    }

    /**
     * Index documents from a given origin into the configured solr index.
     * <p>
     * Record ids are fetched from ds-storage in batches. Within a batch each id is transformed by ds-present
     * and posted to solr individually, on {@code solr.indexingThreads} threads. The batch is committed when
     * all its records are done, and mTime is then advanced to the mTime of its last record.
     * <p>
     * A failing record is logged and skipped. A failing commit aborts the run.
     *
     * @param origin    where the records come from. Has to be registered with DS-Storage
     * @param sinceTime A long representation of time since epoch. Exclusive. {@code null} is treated as 0.
     * @return          A status on how many records have been indexed.
     */
    public static SolrIndexResponse indexOrigin(String origin, Long sinceTime) {
        // sinceTime is advanced below. The requested value decides full vs delta.
        final Long requestedSinceTime = sinceTime;

        DsStorageClient storageClient = new DsStorageClient(ServiceConfig.getDsStorageUrl());
        final String presentUrl = ServiceConfig.getDsPresentUrl();

        // The OAuth token is read from a CXF thread local, which only exists on the request thread. It is
        // captured here and passed to the worker threads.
        final String accessToken = Service2ServiceRequest.getOAuth2Token();

        URL solrUpdateUrl = buildSolrUpdateUrl(false);
        URL solrCommitUrl = buildSolrUpdateUrl(true);

        final int batchSize = ServiceConfig.getSolrBatchSize();

        long mTime = sinceTime == null ? 0L : sinceTime;
        long documents = 0L;
        long deletedRecords = 0L;
        long failedRecords = 0L;
        String lastSolrResponse = null;

        SolrIndexResponse finalResponse = new SolrIndexResponse();
        // DsDatahandlerFacade calls intValue() on this, so it must not be null.
        finalResponse.setAllDocumentsIndexed(0L);

        log.info("Starting indexing of records with sinceTime: '{}' from origin: '{}'", sinceTime, origin);

        ExecutorService executor = Executors.newFixedThreadPool(ServiceConfig.getSolrIndexingThreads());
        try {
            while (true) {
                // Minimal records hold id, mTime, referenceId, kalturaId and deleted, but not the data field.
                // includeDeleted is true: soft deleted records become solr delete commands below.
                // The endpoint does not filter on recordType. All records in the indexed origins are
                // DELIVERABLEUNIT; other record types would also be sent to ds-present.
                List<DsRecordMinimalDto> records = storageClient.getMinimalRecordsJson(origin, batchSize, mTime, true);
                if (records == null || records.isEmpty()) {
                    log.info("No more records in ds-storage for origin '{}' after mTime {}. Indexing is up to date.",
                            origin, mTime);
                    break;
                }

                log.info("Indexing {} records from DS-storage origin '{}' after mTime {}. " +
                         "'{}' documents have been indexed through this request.",
                         records.size(), origin, mTime, documents);

                List<Callable<RecordIndexResult>> tasks = new ArrayList<>(records.size());
                for (DsRecordMinimalDto record : records) {
                    // Soft delete only sets a flag in ds-storage and keeps the data field, so ds-present would
                    // return a normal document. ds-present is not called for these records.
                    final String recordId = record.getId();
                    tasks.add(Boolean.TRUE.equals(record.getDeleted())
                            ? () -> deleteSingleRecord(solrUpdateUrl, recordId)
                            : () -> indexSingleRecord(presentUrl, accessToken, solrUpdateUrl, recordId));
                }

                // Blocks until every record in the batch is done. The results are in the order of the tasks.
                List<Future<RecordIndexResult>> futures = invokeAll(executor, tasks);

                for (int i = 0; i < records.size(); i++) {
                    RecordIndexResult result = resultOf(futures.get(i));
                    boolean deleted = Boolean.TRUE.equals(records.get(i).getDeleted());

                    if (result.isSuccess()) {
                        if (deleted) {
                            deletedRecords++;
                        } else {
                            documents++;
                        }
                        lastSolrResponse = result.getSolrResponse();
                        continue;
                    }

                    // Logged and skipped. mTime still advances past the record.
                    failedRecords++;
                    log.warn("Skipping record '{}' from origin '{}'. ds-present status: {}, solr status: {}. Response: {}",
                            result.getRecordId(), origin, result.getPresentStatus(), result.getSolrStatus(),
                            result.getExceptionMessage());
                }

                if (lastSolrResponse != null) {
                    updateFinalResponse(lastSolrResponse, finalResponse, documents);
                }

                // autoCommit in solrconfig uses openSearcher=false and autoSoftCommit is disabled, so this is
                // what makes the batch searchable. It throws if solr rejects it, leaving mTime on the last
                // committed batch.
                commitSolr(solrCommitUrl);

                mTime = records.get(records.size() - 1).getmTime();
            }
        } finally {
            executor.shutdown();
        }

        log.info("Solr index completed for origin: '{}', mTime: {}, #docs: {}, #deleted: {}, #failed records: {}",
                origin, mTime, documents, deletedRecords, failedRecords);

        //Only build suggest for full index
        if (requestedSinceTime == null || requestedSinceTime == 0) {
            log.info("Started building solr suggest-index (full index). Added document#=:"+finalResponse.getAllDocumentsIndexed());
            buildSuggestIndex();
        }
        else {
            log.info("Skipping suggest build for delta-index");
        }
        return finalResponse;
    }

    /**
     * Transform a single record in ds-present and post it to solr. Solr is only called when ds-present
     * answered 200. Neither failure throws; both are reported in the returned {@link RecordIndexResult}.
     * The method holds no state beyond its arguments and is run on a worker thread.
     * <p>
     * ds-present is called directly rather than through DsPresentClient: the client reads the OAuth token
     * from a CXF thread local that does not exist on a worker thread, and it discards the response body.
     *
     * @param presentUrl    base URL for the remote ds-present.
     * @param accessToken   OAuth token captured on the request thread. May be null.
     * @param solrUpdateUrl the solr update handler. Must not carry a commit parameter.
     * @param recordId      the record to transform and index.
     * @return the outcome for this record.
     */
    static RecordIndexResult indexSingleRecord(
            String presentUrl, String accessToken, URL solrUpdateUrl, String recordId) {
        String solrDocument;
        try {
            URL recordUrl = new URIBuilder(presentUrl)
                    .appendPathSegments("record", recordId)
                    .addParameter("format", FormatDto.SOLRJSON.getValue())
                    .build().toURL();

            HttpURLConnection connection = (HttpURLConnection) recordUrl.openConnection();
            connection.setRequestMethod("GET");
            if (accessToken != null) {
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            }

            int presentStatus = connection.getResponseCode();
            String body = readBody(connection, presentStatus);
            if (presentStatus != 200) {
                return RecordIndexResult.presentFailed(recordId, presentStatus, body);
            }
            solrDocument = body;
        } catch (Exception e) {
            return RecordIndexResult.presentFailed(recordId, RecordIndexResult.NOT_CALLED, e.toString());
        }

        return postToSolr(solrUpdateUrl, recordId, "\"add\": { \"doc\" : " + solrDocument + " }");
    }

    /**
     * Read the body of a response, from the error stream when the status is not 200.
     *
     * @param connection a connected {@link HttpURLConnection}.
     * @param status     the HTTP status of the response.
     * @return the response body, or the empty string if there was none.
     */
    private static String readBody(HttpURLConnection connection, int status) throws IOException {
        try (InputStream in = status == 200 ? connection.getInputStream() : connection.getErrorStream()) {
            return in == null ? "" : IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }

    /**
     * Run every task and wait for all of them to finish.
     *
     * @param executor the pool to run the tasks on.
     * @param tasks    one task per record in the batch.
     * @return the results, in the order of the tasks.
     * @throws InternalServiceException if the wait was interrupted.
     */
    private static List<Future<RecordIndexResult>> invokeAll(
            ExecutorService executor, List<Callable<RecordIndexResult>> tasks) {
        try {
            return executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServiceException(e);
        }
    }

    /**
     * Read the result of a finished task.
     *
     * @param future a task from {@link #invokeAll}.
     * @return the outcome for the record the task handled.
     * @throws InternalServiceException if the task did not produce a result.
     */
    private static RecordIndexResult resultOf(Future<RecordIndexResult> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServiceException(e);
        } catch (ExecutionException e) {
            throw new InternalServiceException(e);
        }
    }

    /**
     * Remove a single record from the solr index. ds-present is not involved.
     *
     * @param solrUpdateUrl the solr update handler. Must not carry a commit parameter.
     * @param recordId      the record to remove from the index.
     * @return the outcome for this record.
     */
    static RecordIndexResult deleteSingleRecord(URL solrUpdateUrl, String recordId) {
        return postToSolr(solrUpdateUrl, recordId, "\"delete\": { \"id\": \"" + recordId + "\" }");
    }

    /**
     * POST a single solr update command, either an add or a delete.
     *
     * @param solrUpdateUrl the solr update handler. Must not carry a commit parameter.
     * @param recordId      the record the command concerns, used for reporting.
     * @param command       a solr update command without the surrounding object.
     * @return the outcome for this record.
     */
    private static RecordIndexResult postToSolr(URL solrUpdateUrl, String recordId, String command) {
        String payload = "{\n" + command + "\n}\n";
        try {
            HttpURLConnection solrServerConnection = (HttpURLConnection) solrUpdateUrl.openConnection();
            String solrResponse = HttpPostUtil.callPost(solrServerConnection,
                    new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8)), "application/json");
            int solrStatus = solrServerConnection.getResponseCode();

            // Solr reports failures both as an HTTP status and in the body.
            //Example body: {  "responseHeader":{    "rf":1,    "status":0,    "QTime":1348}}
            if (solrStatus != 200 || !solrResponse.contains("\"status\":0")) {
                return RecordIndexResult.solrFailed(recordId, solrStatus, solrResponse);
            }
            return RecordIndexResult.success(recordId, solrResponse);
        } catch (Exception e) {
            return RecordIndexResult.solrFailed(recordId, RecordIndexResult.NOT_CALLED, e.toString());
        }
    }

    /**
     * Commit the documents posted since the last commit, making them searchable.
     *
     * @param solrCommitUrl the solr update handler with {@code commit=true}.
     * @throws InternalServiceException if solr did not accept the commit.
     */
    private static void commitSolr(URL solrCommitUrl) {
        try {
            HttpURLConnection solrServerConnection = (HttpURLConnection) solrCommitUrl.openConnection();
            String solrResponse = HttpPostUtil.callPost(solrServerConnection,
                    new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)), "application/json");
            if (!solrResponse.contains("\"status\":0")) {
                throw new InternalServiceException(
                        "Unexpected reply from solr when committing: '" + solrResponse + "'");
            }
        } catch (IOException e) {
            log.error("Could not commit to solr at '{}'", solrCommitUrl, e);
            throw new InternalServiceException(e);
        }
    }

    /**
     * Build the solr update URL from configuration.
     *
     * @param commit whether the request should commit. Documents are posted without a commit; each batch is
     *               committed with a separate request.
     * @return the solr update handler URL.
     */
    private static URL buildSolrUpdateUrl(boolean commit) {
        try {
            URIBuilder uriBuilder = new URIBuilder(ServiceConfig.getSolrUpdateUrl());
            if (commit) {
                uriBuilder.setParameter("commit", "true");
            }
            return uriBuilder.build().toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            log.warn("Update URL for solr could not be constructed. Tried to build URL from '{}'.",
                    ServiceConfig.getSolrUpdateUrl());
            throw new InternalServiceException(e);
        }
    }

    /**
     * Update the final {@code SolrIndexResponse} with the content from the single {@code individualSolrResponse}.
     * The updated {@code SolrIndexResponse} is used as the response for the endpoint
     * {@link dk.kb.datahandler.api.v1.impl.DsDatahandlerApiServiceImpl#indexSolr(String, Long, TypeDto)}. This
     * updated response contains information on the amount of documents that have been indexed in total and not just
     * during the last batch of the stream.
     *
     * @param individualSolrResponse a string representation of a JSON solr response returned when indexing a batch of
     *                               documents. Eg:
     * <pre>
     * {"responseHeader": {
     *   "rf":1,
     *   "status":0,
     *   "QTime":1348}}
     *</pre>
     * @param finalResponse         containing the {@code rf} value from the latest added {@code individualSolrResponse},
     *                              the {@code status} value from the latest added {@code individualSolrResponse}
     *                              the combined {@code QTime} for all added response headers and the total amount
     *                              of documents indexed.
     * @param documents             The total amount of documents indexed.
     */
     public static void updateFinalResponse(String individualSolrResponse, SolrIndexResponse finalResponse,
                                            Long documents) {
        SolrResponseHeader currentResponseHeader = new SolrResponseHeader(individualSolrResponse);

        finalResponse.setLastSolrResponseHeader(currentResponseHeader);
        finalResponse.setAllDocumentsIndexed(documents);
        finalResponse.incrementCombinedQTime(currentResponseHeader.getqTime());
    }

    /**
     * Convert a solr index response to a string representing a JSON structure.
     *
     * @param solrIndexResponse
     * @return a string representing a JSON structure
     */
    public static String solrIndexObjectAsJSON(SolrIndexResponse solrIndexResponse) {
        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String result = "";

        try {
            result = objectWriter.writeValueAsString(solrIndexResponse);
        } catch (JsonProcessingException e) {
            log.warn("An error occurred when trying to convert a response from solr to JSON. Indexing has not been affected ");
        }

        return result;
    }


    /**
     * Tracks whether a suggest index build is currently running in the background.
     * Used to prevent {@link #buildSuggestIndex()} from starting a second build
     * while one is already in progress.
     * <p>
     * Declared {@code volatile} so that when the background build thread updates
     * this flag (e.g. sets it back to {@code false} when the build finishes),
     * that change is immediately visible to other threads. Without {@code volatile},
     * a thread could keep reading a stale, cached value and incorrectly think a
     * build is still running (or not running) after the actual state has changed.
     */
    public static synchronized void buildSuggestIndex() {
        if (suggestIndexBuildRunning) {
            log.info("Suggest index build already in progress, skipping this request.");
            return;
        }
        suggestIndexBuildRunning = true;
        JobDto jobDto = DsDatahandlerFacade.startJob(TypeDto.FULL, CategoryDto.SOLR_BUILD_SUGGEST, "", null, DsDatahandlerApiServiceImpl.getCurrentUsername());
        
        Thread thread = new Thread(() -> {
            try {
                String solrUrl = ServiceConfig.getSolrWriteCollectionUrl();
                try (SolrClient solrClient = new HttpJdkSolrClient.Builder(solrUrl)
                        .withRequestTimeout(24, TimeUnit.HOURS) // 24 hour is overkill, should be enough
                        .build()) {
                    SolrQuery query = new SolrQuery();
                    query.setRequestHandler("/suggest");
                    query.set("suggest.build", "true");
                    log.info("Starts building suggest index by querying '{}' with this request: '{}'.", solrUrl, query);

                    solrClient.query(query);
                    log.info("Suggest index build completed.");
                    DsDatahandlerFacade.updateJob(jobDto, JobStatusDto.COMPLETED, null, OffsetDateTime.now(ZoneOffset.UTC), null, null); 
                }
            } catch (Exception e) {
                log.warn("1 Hour was not enough to build suggest index. This is not expected", e);
                DsDatahandlerFacade.updateJob(jobDto, JobStatusDto.FAILED, null, OffsetDateTime.now(ZoneOffset.UTC), null, null); 
            } finally {
                suggestIndexBuildRunning = false;
            }
        }, "suggest-index-builder");
        thread.setDaemon(true);
        thread.start();
    }
}
