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
import dk.kb.datahandler.solr.SolrIndexResponse;
import dk.kb.datahandler.solr.SolrResponseHeader;
import dk.kb.present.model.v1.FormatDto;
import dk.kb.present.util.DsPresentClient;
import dk.kb.storage.model.v1.DsRecordMinimalDto;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.exception.InternalServiceException;
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
import java.util.ArrayList;
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
     * For each batch a list of record ids is fetched from
     * ds-storage, each id is transformed into a solr document by ds-present one record at a time, and the
     * assembled batch is POSTed to solr. Paging is driven by the mTime of the last record in the batch.
     * <p>
     * A batch either succeeds completely or fails completely: any exception aborts the whole indexing run,
     * and nothing is persisted about how far it got. The local mTime is simply discarded. A later delta index
     * derives its start time from the highest internal_storage_mTime in solr, so it resumes after the last batch solr accepted.
     * <p>
     * The per record call to ds-present is deliberately sequential for now. The calls are independent, so the
     * loop over the ids of a batch is the natural place to add a thread pool later.
     *
     * @param origin    where the records come from. Has to be registered with DS-Storage
     * @param sinceTime A long representation of time since epoch. Exclusive. {@code null} is treated as 0.
     * @return          A status on how many records have been indexed.
     */
    public static SolrIndexResponse indexOrigin(String origin, Long sinceTime) {

        final Long requestedSinceTime = sinceTime;

        DsStorageClient storageClient = new DsStorageClient(ServiceConfig.getDsStorageUrl());
        DsPresentClient presentClient = new DsPresentClient(ServiceConfig.getDsPresentUrl());

        URL solrUpdateUrl;
        try {
            solrUpdateUrl = new URIBuilder(ServiceConfig.getSolrUpdateUrl())
                    .setParameter("commit", "true")
                    .build().toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            log.warn("Update URL for solr could not be constructed. Tried to build URL from '{}'.", ServiceConfig.getSolrUpdateUrl());
            throw new InternalServiceException(e);
        }

        final int batchSize = ServiceConfig.getSolrBatchSize();

        long mTime = sinceTime == null ? 0L : sinceTime;
        long documents = 0L;
        SolrIndexResponse finalResponse = new SolrIndexResponse();
        // Must be non-null even when there is nothing to index: DsDatahandlerFacade calls intValue() on it.
        finalResponse.setAllDocumentsIndexed(0L);

        log.info("Starting indexing of records with sinceTime: '{}' from origin: '{}'", sinceTime, origin);

        while (true) {
            // Note that this endpoint does not filter on recordType, so it returns every record type in the
            // origin. Every record in the indexed origins is a DELIVERABLEUNIT today, so it makes no
            // difference. If an origin ever gets MANIFESTATION records, ds-storage needs a record type filter
            // here, otherwise ds-present will be asked to transform records its solr view was not written for.
            List<DsRecordMinimalDto> records = storageClient.getMinimalRecords(origin, batchSize, mTime);
            if (records == null || records.isEmpty()) {
                log.info("No more records in ds-storage for origin '{}' after mTime {}. Indexing is up to date.",
                        origin, mTime);
                break;
            }

            log.info("Indexing {} records from DS-storage origin '{}' after mTime {}. " +
                     "'{}' documents have been indexed through this request.",
                     records.size(), origin, mTime, documents);

            // Soft deleted records are not handled here. They are returned by ds-storage like any other record,
            // their data field is left intact when they are marked as deleted, and ds-present therefore returns
            // a perfectly normal solr document for them. They are consequently indexed as live documents, where
            // the previous implementation turned them into solr delete commands.
            //
            // This is a deliberate, known limitation. Soft deletes are extremely rare in practice. The intended
            // solution is to index the deleted flag as a field in solr and let the license module filter on it,
            // which is handled as a separate task. When that lands, the flag has to be carried from ds-storage
            // to the indexed document rather than being ignored.
            List<String> solrDocuments = new ArrayList<>(records.size());
            for (DsRecordMinimalDto record : records) {
                solrDocuments.add(presentClient.getRecord(record.getId(), FormatDto.SOLRJSON));
            }

            String solrResponse = postDocumentsToSolr(solrUpdateUrl, solrDocuments);
            documents += solrDocuments.size();
            updateFinalResponse(solrResponse, finalResponse, documents);
            log.info("indexed #records=" + solrDocuments.size());

            mTime = records.get(records.size() - 1).getmTime();
        }

        log.info("Solr index completed for origin: '{}', mTime: {}, #docs: {}", origin, mTime, documents);

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
     * POST the given solr documents to the solr update handler as a single request.
     * The update URL carries {@code commit=true}, so the documents are searchable when this method returns.
     *
     * @param solrUpdateUrl the solr update handler, including the commit parameter.
     * @param solrDocuments solr documents as delivered by ds-present.
     * @return the raw response from solr, used for the job bookkeeping.
     */
    private static String postDocumentsToSolr(URL solrUpdateUrl, List<String> solrDocuments) {
        String payload = assembleSolrUpdatePayload(solrDocuments);
        try {
            HttpURLConnection solrServerConnection = (HttpURLConnection) solrUpdateUrl.openConnection();
            String solrResponse = HttpPostUtil.callPost(solrServerConnection,
                    new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8)), "application/json");

            if (!solrResponse.contains("\"status\":0")) {
                //Example: {  "responseHeader":{    "rf":1,    "status":0,    "QTime":1348}}
                log.error("Unexpected reply from solr: '" + solrResponse + "'");
                throw new IOException("Unexpected status from solr: '" + solrResponse + "'");
            }
            return solrResponse;
        } catch (IOException e) {
            log.warn("An error occurred when posting {} documents to Solr at: '{}'", solrDocuments.size(), solrUpdateUrl);
            throw new InternalServiceException(e);
        }
    }

    /**
     * Wrap the solr documents as add-commands in the structure solr expects for JSON index updates.
     * <p>
     * Solr accepts repeated {@code add} keys inside one object. That is not valid JSON, but it is the format
     * solr prescribes for index updates, and it is the same structure the previous streaming implementation
     * produced.
     *
     * @param solrDocuments solr documents as delivered by ds-present.
     * @return a complete request body for the solr update handler.
     */
    static String assembleSolrUpdatePayload(List<String> solrDocuments) {
        StringBuilder payload = new StringBuilder("{\n");
        for (int i = 0; i < solrDocuments.size(); i++) {
            payload.append("\"add\": { \"doc\" : ").append(solrDocuments.get(i)).append(" }");
            if (i < solrDocuments.size() - 1) {
                payload.append(",");
            }
            payload.append("\n");
        }
        return payload.append("}\n").toString();
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
