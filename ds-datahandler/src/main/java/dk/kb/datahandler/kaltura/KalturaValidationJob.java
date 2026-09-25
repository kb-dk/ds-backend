package dk.kb.datahandler.kaltura;

import com.kaltura.client.enums.EntryStatus;
import com.kaltura.client.types.APIException;
import com.kaltura.client.types.MediaEntry;
import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.kaltura.client.DsKalturaAnalytics;
import dk.kb.kaltura.client.DsKalturaClient;
import dk.kb.kaltura.client.DsKalturaClientBase;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class has a single public method to validate that kaltura_id values registered on records in storage
 * still point to a valid Kaltura entry with status 'READY'.
 */
public class KalturaValidationJob {
    static DsKalturaClient kalturaClient = null;
    static DsKalturaAnalytics kalturaAnalyticsClient = null;
    private static final Logger log = LoggerFactory.getLogger(KalturaValidationJob.class);

    /**
     * Start job that validates all records with a registered kaltura_id.
     * Workflow:
     * 1) Extract records from Solr using the condition kaltura_id:* AND NOT kaltura_id:ERROR_* so upload error
     * markers such as ERROR_FILE_MISSING are never cleared. Only extract the few fields from solr that
     * are required: id,file_id,kaltura_id,internal_storage_mTime
     * This is not a delta job. All records with a kaltura_id must be (re)checked on every run, so mTimeFrom
     * always starts at 0.
     * 2) Look up the entry status in Kaltura for the whole Solr batch in a single eSearch call. Entries missing
     * from the eSearch result are looked up one at a time, before their kaltura_id is cleared.
     * 3) If no entry exists in Kaltura for the kaltura_id, or the entry exists but is not in status READY,
     * the Kaltura entry is deleted (if it exists) and the record's kaltura_id is cleared (set to null) in
     * storage. A cleared kaltura_id makes the record eligible for upload again by KalturaDeltaUploadJob.
     * 4) A summary of the kaltura_ids that were not found or not READY is logged at the end.
     *
     * @param dryRun If true, no Kaltura entries are deleted and no kaltura_ids are cleared in storage. Only the
     *               summary of what would have been done is logged.
     * @return number of records where the kaltura_id was cleared, or would have been cleared if dryRun
     * @throws InternalServiceException If any Solr call fails, or if a Kaltura/storage call for a record fails.
     *                                  Stop validating more.
     */
    public static int validateKalturaIds(boolean dryRun) throws InternalServiceException {
        ValidationSummary summary = new ValidationSummary();
        try {
            validateAllRecords(dryRun, summary);
        } finally {
            // Also log on failure, so the records handled before the error are visible
            summary.logSummary(dryRun);
        }
        return summary.size();
    }

    private static void validateAllRecords(boolean dryRun, ValidationSummary summary) throws InternalServiceException {
        boolean moreSolrRecords = true;
        long mTimeFromCurrent = 0; //Not a delta job. All records with a kaltura_id must be checked every time.
        String dsStorageUrl = ServiceConfig.getDsStorageUrl();
        DsStorageClient storageClient = new DsStorageClient(dsStorageUrl);

        while (moreSolrRecords) {
            SolrDocumentList docs;
            try {
                docs = fetchSolrRecords(mTimeFromCurrent, DsKalturaClientBase.MAX_BATCH_SIZE);
            } catch (SolrServerException | IOException e) {
                // Can not fetch more records. Stop validation
                moreSolrRecords = false;
                String errorMessage = "Could not fetch more solr records from mTime=" + mTimeFromCurrent;
                log.error(errorMessage);
                throw new InternalServiceException(errorMessage, e);
            }
            if (docs.getNumFound() == 0) {
                return;
            }

            List<String> kalturaIds = docs.stream()
                    .map(doc -> (String) doc.getFieldValue("kaltura_id"))
                    .collect(Collectors.toList());
            Map<String, EntryStatus> batchStatuses;
            try {
                batchStatuses = getEntryStatuses(kalturaIds);
            } catch (Exception e) {
                log.error("Error fetching Kaltura entry statuses for batch starting at mTime={}", mTimeFromCurrent, e);
                throw new InternalServiceException("Error fetching Kaltura entry statuses for batch starting at mTime="
                        + mTimeFromCurrent, e);
            }

            for (SolrDocument doc : docs) {
                String id = (String) doc.getFieldValue("id");
                String fileId = (String) doc.getFieldValue("file_id");
                String kalturaId = (String) doc.getFieldValue("kaltura_id");
                long recordMtime = (long) doc.getFieldValue("internal_storage_mTime");

                mTimeFromCurrent = recordMtime + 1L; //update mTime for next call

                validateRecord(storageClient, id, fileId, kalturaId, batchStatuses.get(kalturaId), dryRun, summary);
            }
        }
    }

    /**
     * Validate a single record's kaltura_id against Kaltura. If the kaltura_id does not exist in Kaltura, or
     * exists but is not in status READY, the Kaltura entry is deleted (if it exists) and the kaltura_id is
     * cleared for the record in storage.
     *
     * @param batchStatus The status found for the kaltura_id by the batch lookup, or null if it was not found.
     * @param dryRun      If true, nothing is deleted or cleared. The record is only added to the summary.
     * @param summary     Records that are (or would be) cleared are added to this.
     * @return true if the record's kaltura_id was cleared in storage, or would have been cleared if dryRun.
     */
    static boolean validateRecord(DsStorageClient storageClient, String id, String fileId, String kalturaId,
                                  EntryStatus batchStatus, boolean dryRun, ValidationSummary summary) {
        try {
            EntryStatus status = batchStatus;
            if (status == null) {
                // The eSearch index can lag behind or omit entries, so confirm against the entry service before
                // clearing. Otherwise the record would be uploaded again, leaving a duplicate entry in Kaltura.
                status = getEntryStatus(kalturaId);
            }
            if (status == EntryStatus.READY) {
                return false; //Valid mapping. Nothing to do.
            }

            String prefix = dryRun ? "DRY RUN: " : "";
            String verb = dryRun ? "Would" : "Will";
            if (status != null) { //Entry exists in Kaltura but is not ready. Remove it.
                log.warn("{}Kaltura entry='{}' for id='{}' has status='{}', not READY. {} delete entry and clear kaltura_id.",
                        prefix, kalturaId, id, status, verb);
                if (!dryRun) {
                    deleteStream(kalturaId);
                    clearKalturaIdForRecord(storageClient, fileId);
                }
                summary.addNotReady(id, kalturaId, status);
            } else {
                log.warn("{}Kaltura entry='{}' for id='{}' does not exist in Kaltura. {} clear kaltura_id.",
                        prefix, kalturaId, id, verb);
                if (!dryRun) {
                    clearKalturaIdForRecord(storageClient, fileId);
                }
                summary.addNotFound(id, kalturaId);
            }
            return true;
        } catch (Exception e) {
            //Totally stop all validation if a single call fails. Change strategy if this does seem to happen sporadic
            //Validation job can be started again.
            log.error("Error validating kaltura_id='{}' for id='{}'", kalturaId, id, e);
            throw new InternalServiceException("Error validating kaltura_id: " + kalturaId + " for id: " + id, e);
        }
    }

    static void clearKalturaIdForRecord(DsStorageClient storageClient, String fileId) {
        storageClient.clearKalturaIdForRecord(fileId);
    }

    /**
     * Make Solr call to fetch records with a kaltura_id registered. Upload error markers (ERROR_*) are excluded.
     *
     * @param mTimeFrom Only extract records with mTime higher that this value
     * @param batchSize solr batch size.
     * @return
     * @throws SolrServerException
     * @throws IOException
     */
    public static SolrDocumentList fetchSolrRecords(long mTimeFrom, int batchSize) throws SolrServerException, IOException {
        String solrUrl = ServiceConfig.getSolrQueryUrl();
        // KalturaDeltaUploadJob stores upload errors (StreamErrorTypeDto, e.g. ERROR_FILE_MISSING) in kaltura_id.
        // They are not Kaltura entries and must not be cleared, or the record would be uploaded again.
        String filterQuery = "kaltura_id:* AND NOT kaltura_id:ERROR_*";

        HttpJdkSolrClient client = new HttpJdkSolrClient.Builder(solrUrl).build();

        String query = "internal_storage_mTime:[" + mTimeFrom + " TO *]"; // mTimeFrom must start with this value or higher.
        String fieldList = "id,file_id,kaltura_id,internal_storage_mTime"; // only extract fields we need

        try (client) { // autoclosable
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query);
            solrQuery.setFilterQueries(filterQuery);
            solrQuery.set("facet", "false"); // very important. Must overwrite to false. Facets are very slow and expensive.
            solrQuery.set("hl", false);// no highlights
            solrQuery.set("spellcheck", false); //No spellcheck
            solrQuery.add("sort", "internal_storage_mTime ASC"); // increasing order
            solrQuery.add("fl", fieldList);
            solrQuery.setRows(batchSize);
            QueryResponse response = client.query(solrQuery);
            SolrDocumentList results = response.getResults();
            log.info("Load solr records for kaltura validation={}", results.getNumFound());
            return results;
        }
    }

    /**
     * Get the status of a batch of Kaltura entries with a single eSearch call.
     *
     * @param kalturaEntryIds The internal kaltura entryIds. At most {@link DsKalturaClientBase#MAX_BATCH_SIZE}.
     * @return Map from entryId to status. Entries not found in Kaltura are absent from the map.
     * @throws APIException If API error
     */
    static Map<String, EntryStatus> getEntryStatuses(List<String> kalturaEntryIds) throws APIException {
        initKalturaAnalyticsClient();
        Map<String, EntryStatus> statuses = new HashMap<>();
        for (MediaEntry entry : kalturaAnalyticsClient.listEntryBatch(kalturaEntryIds)) {
            statuses.put(entry.getId(), entry.getStatus());
        }
        return statuses;
    }

    /**
     * Get the status of a Kaltura entry.
     *
     * @param kalturaEntryId The internal kaltura entryId
     * @return The status of the entry, or null if the entry does not exist in Kaltura.
     * @throws APIException If API error
     */
    static EntryStatus getEntryStatus(String kalturaEntryId) throws APIException {
        initKalturaClient();
        return kalturaClient.getEntryStatus(kalturaEntryId);
    }

    /**
     * Delete a stream in Kaltura.
     *
     * @param kalturaEntryId The internal kaltura entryId
     * @throws APIException If API error
     */
    static void deleteStream(String kalturaEntryId) throws APIException {
        initKalturaClient();
        boolean deleted = kalturaClient.deleteStreamByEntryId(kalturaEntryId);
        if (deleted) {
            log.info("Deleted kaltura entryId='{}'", kalturaEntryId);
        } else {
            log.warn("Failed deleting kaltura entryId='{}'", kalturaEntryId);
        }
    }

    static void initKalturaClient() {
        if (kalturaClient != null) {
            return; // already inititalised
        }

        String kalturaUrl = ServiceConfig.getKalturaUrl();
        int partnerId = ServiceConfig.getKalturaPartnerId();
        String adminSecret = null;// We use appTokens instead
        String userId = ServiceConfig.getKalturaUserId();
        String token = ServiceConfig.getKalturaToken();
        String tokenId = ServiceConfig.getKalturaTokenId();
        int sessionDurationSeconds = ServiceConfig.getKalturaSessionDurationSeconds();
        int sessionRefreshThreshold = ServiceConfig.getKalturaSessionRefreshThreshold();
        int conversionQueueThreshold = ServiceConfig.getConversionQueueThreshold();
        int conversionQueueDelaySeconds = ServiceConfig.getConversionQueueDelaySeconds();

        try {
            kalturaClient = new DsKalturaClient(
                    kalturaUrl,
                    userId,
                    partnerId,
                    token,
                    tokenId,
                    adminSecret,
                    sessionDurationSeconds,
                    sessionRefreshThreshold,
                    conversionQueueThreshold,
                    conversionQueueDelaySeconds
            );
        } catch (Exception e) {
            log.error("Could not instantiate DsKaltura client.", e);
        }
    }

    static void initKalturaAnalyticsClient() {
        if (kalturaAnalyticsClient != null) {
            return; // already inititalised
        }

        String kalturaUrl = ServiceConfig.getKalturaUrl();
        int partnerId = ServiceConfig.getKalturaPartnerId();
        String adminSecret = null;// We use appTokens instead
        String userId = ServiceConfig.getKalturaUserId();
        String token = ServiceConfig.getKalturaToken();
        String tokenId = ServiceConfig.getKalturaTokenId();
        int sessionDurationSeconds = ServiceConfig.getKalturaSessionDurationSeconds();
        int sessionRefreshThreshold = ServiceConfig.getKalturaSessionRefreshThreshold();

        try {
            kalturaAnalyticsClient = new DsKalturaAnalytics(
                    kalturaUrl,
                    userId,
                    partnerId,
                    token,
                    tokenId,
                    adminSecret,
                    sessionDurationSeconds,
                    sessionRefreshThreshold
            );
        } catch (Exception e) {
            log.error("Could not instantiate DsKalturaAnalytics client.", e);
        }
    }
}
