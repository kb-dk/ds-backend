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
import dk.kb.present.util.DsPresentClient;
import dk.kb.storage.model.v1.DsRecordMinimalDto;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
     * No streams are involved anywhere in this flow. A batch of record ids is fetched from ds-storage, and each
     * id is then transformed by ds-present and posted to solr on its own. Paging is driven by the mTime of the
     * last record in the batch, so no continuation token or HTTP header is needed to know how far the indexing
     * has come.
     * <p>
     * Indexing a single record is self contained, see {@link #indexSingleRecord}. The records of a batch are
     * processed sequentially for now, but the per record call is written so it can be submitted to a thread
     * pool without changing anything else.
     * <p>
     * Records are posted without a commit and solr is committed once when the origin is done. Committing per
     * document would mean one hard commit per record, which solr handles very poorly.
     *
     * @param origin    where the records come from. Has to be registered with DS-Storage
     * @param sinceTime A long representation of time since epoch. Exclusive. {@code null} is treated as 0.
     * @return          A status on how many records have been indexed.
     */
    public static SolrIndexResponse indexOrigin(String origin, Long sinceTime) {
        // sinceTime is advanced below, so keep the requested start point for the full-vs-delta decision.
        final Long requestedSinceTime = sinceTime;

        DsStorageClient storageClient = new DsStorageClient(ServiceConfig.getDsStorageUrl());
        DsPresentClient presentClient = new DsPresentClient(ServiceConfig.getDsPresentUrl());

        URL solrUpdateUrl = buildSolrUpdateUrl(false);
        URL solrCommitUrl = buildSolrUpdateUrl(true);

        final int batchSize = ServiceConfig.getSolrBatchSize();

        long mTime = sinceTime == null ? 0L : sinceTime;
        long documents = 0L;
        long failedRecords = 0L;
        String lastSolrResponse = null;

        SolrIndexResponse finalResponse = new SolrIndexResponse();
        // Must be non-null even when there is nothing to index: DsDatahandlerFacade calls intValue() on it.
        finalResponse.setAllDocumentsIndexed(0L);

        log.info("Starting indexing of records with sinceTime: '{}' from origin: '{}'", sinceTime, origin);

        while (true) {
            // Minimal records carry id, mTime, referenceId and kalturaId but not the data field, so this stays
            // cheap even though records can be very large.
            //
            // Note that this endpoint does not filter on recordType, so it returns every record type in the
            // origin. Every record in the indexed origins is a DELIVERABLEUNIT today, so it makes no
            // difference. If an origin ever gets MANIFESTATION records, ds-storage needs a record type filter
            // here, otherwise ds-present will be asked to transform records its solr view was not written for.
            //
            // Soft deleted records are not handled either. They are returned like any other record, their data
            // is left intact when they are marked as deleted, and they are therefore indexed as live documents
            // where the previous implementation turned them into solr delete commands. The intended solution is
            // to index the deleted flag as a field in solr and let the license module filter on it, which is
            // handled as a separate task.
            List<DsRecordMinimalDto> records = storageClient.getMinimalRecords(origin, batchSize, mTime);
            if (records == null || records.isEmpty()) {
                log.info("No more records in ds-storage for origin '{}' after mTime {}. Indexing is up to date.",
                        origin, mTime);
                break;
            }

            log.info("Indexing {} records from DS-storage origin '{}' after mTime {}. " +
                     "'{}' documents have been indexed through this request.",
                     records.size(), origin, mTime, documents);

            for (DsRecordMinimalDto record : records) {
                RecordIndexResult result = indexSingleRecord(presentClient, solrUpdateUrl, record.getId());

                if (result.isSuccess()) {
                    documents++;
                    lastSolrResponse = result.getSolrResponse();
                    continue;
                }

                // A failing record is logged and skipped rather than aborting the origin. A transformation
                // failure is deterministic, so retrying the same record achieves nothing and one bad record
                // would otherwise block the origin permanently.
                failedRecords++;
                log.warn("Skipping record '{}' from origin '{}'. ds-present status: {}, solr status: {}. Response: {}",
                        result.getRecordId(), origin, result.getPresentStatus(), result.getSolrStatus(),
                        result.getExceptionMessage());
            }

            if (lastSolrResponse != null) {
                updateFinalResponse(lastSolrResponse, finalResponse, documents);
            }

            mTime = records.get(records.size() - 1).getmTime();
        }

        // Documents were posted without a commit, so nothing is searchable until this runs. A failed run leaves
        // the documents uncommitted, which is harmless: the next delta index resolves its start time from what
        // is actually in solr and simply indexes the window again.
        commitSolr(solrCommitUrl);

        log.info("Solr index completed for origin: '{}', mTime: {}, #docs: {}, #failed records: {}",
                origin, mTime, documents, failedRecords);

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
     * Transform a single record in ds-present and post it to solr.
     * <p>
     * Solr is only called when ds-present answered 200. Neither failure throws: the outcome of both calls is
     * reported in the returned {@link RecordIndexResult} so the caller can log it and continue.
     * <p>
     * This method is the unit of work intended for a thread pool. It holds no state beyond its arguments.
     *
     * @param presentClient client for the remote ds-present.
     * @param solrUpdateUrl the solr update handler. Must not carry a commit parameter.
     * @param recordId      the record to transform and index.
     * @return the outcome for this record.
     */
    static RecordIndexResult indexSingleRecord(DsPresentClient presentClient, URL solrUpdateUrl, String recordId) {
        String solrDocument;
        try {
            solrDocument = presentClient.getRecord(recordId, FormatDto.SOLRJSON);
        } catch (ServiceException e) {
            // kb-util maps the HTTP status to an exception type and does not keep the response body, so the
            // exception message is the best available description of the failure.
            int status = e.getResponseStatus() == null
                    ? RecordIndexResult.NOT_CALLED
                    : e.getResponseStatus().getStatusCode();
            return RecordIndexResult.presentFailed(recordId, status, e.getMessage());
        } catch (Exception e) {
            return RecordIndexResult.presentFailed(recordId, RecordIndexResult.NOT_CALLED, e.toString());
        }

        String payload = "{\n\"add\": { \"doc\" : " + solrDocument + " }\n}\n";
        try {
            HttpURLConnection solrServerConnection = (HttpURLConnection) solrUpdateUrl.openConnection();
            String solrResponse = HttpPostUtil.callPost(solrServerConnection,
                    new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8)), "application/json");
            int solrStatus = solrServerConnection.getResponseCode();

            // Solr reports failures both as an HTTP status and in the body, so check both.
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
     * A failed commit is logged but does not fail the indexing: the documents are in solr either way, and the
     * next index run will simply cover the same window again.
     *
     * @param solrCommitUrl the solr update handler with {@code commit=true}.
     */
    private static void commitSolr(URL solrCommitUrl) {
        try {
            HttpURLConnection solrServerConnection = (HttpURLConnection) solrCommitUrl.openConnection();
            String solrResponse = HttpPostUtil.callPost(solrServerConnection,
                    new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)), "application/json");
            if (!solrResponse.contains("\"status\":0")) {
                log.warn("Unexpected reply from solr when committing: '{}'", solrResponse);
            }
        } catch (IOException e) {
            log.warn("Could not commit to solr at '{}'. The documents are posted but are not searchable until " +
                     "solr commits on its own.", solrCommitUrl, e);
        }
    }

    /**
     * Build the solr update URL from configuration.
     *
     * @param commit whether the request should commit. Documents are posted without a commit and the origin is
     *               committed once at the end, since a commit per document would cripple solr.
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
