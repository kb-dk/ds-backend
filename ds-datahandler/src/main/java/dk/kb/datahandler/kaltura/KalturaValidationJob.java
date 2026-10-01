package dk.kb.datahandler.kaltura;

import com.kaltura.client.enums.EntryStatus;
import com.kaltura.client.types.APIException;
import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.kaltura.client.DsKalturaClient;
import dk.kb.kaltura.client.DsKalturaClientBase;
import dk.kb.storage.model.v1.DsRecordKalturaDto;
import dk.kb.storage.model.v1.StreamErrorTypeDto;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class has a single public method to validate that kaltura_id values registered on records in storage
 * still point to a valid Kaltura entry with status 'READY'.
 */
public class KalturaValidationJob {
    static DsKalturaClient kalturaClient = null;
    private static final Logger log = LoggerFactory.getLogger(KalturaValidationJob.class);
    // Origins with streams uploaded to Kaltura
    static final List<String> ORIGINS = List.of("ds.tv", "ds.radio");

    /**
     * Start job that validates all records with a registered kaltura_id.
     * Workflow:
     * 1) For each origin, extract the records with a kaltura_id from ds-storage in batches ordered by mTime. Storage
     * is used rather than Solr, so kaltura_ids changed since the last index are seen. Storage does not return upload
     * error markers (ERROR_*, e.g. ERROR_FILE_MISSING), so they are never cleared. Records marked for delete are
     * skipped.
     * This is not a delta job. All records with a kaltura_id must be (re)checked on every run, so mTime
     * always starts at 0.
     * 2) Look up the entry status in Kaltura for the whole batch in a single media.list call. Kaltura paging is
     * not trusted, so entries missing from the batch result are confirmed one at a time with media.get.
     * 3) If the entry is in status ERROR_CONVERTING or ERROR_IMPORTING, the entry is kept and the record's kaltura_id
     * is set to the matching error marker. Otherwise, if no entry exists in Kaltura for the kaltura_id, or the entry
     * exists but is not in status READY, the Kaltura entry is deleted (if it exists) and the record's kaltura_id is
     * cleared (set to null) in storage. A cleared kaltura_id makes the record eligible for upload again by
     * KalturaDeltaUploadJob.
     * 4) A summary of the kaltura_ids that were cleared or marked with an error is logged at the end.
     *
     * @param dryRun If true, no Kaltura entries are deleted and no kaltura_ids are cleared in storage. Only the
     *               summary of what would have been done is logged.
     * @return number of records where the kaltura_id was cleared or marked, or would have been if dryRun
     * @throws InternalServiceException If fetching records from storage fails, or if a Kaltura/storage call for a
     *                                  record fails. Stop validating more.
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
        String dsStorageUrl = ServiceConfig.getDsStorageUrl();
        DsStorageClient storageClient = new DsStorageClient(dsStorageUrl);
        for (String origin : ORIGINS) {
            validateOrigin(storageClient, origin, dryRun, summary);
        }
    }

    private static void validateOrigin(DsStorageClient storageClient, String origin, boolean dryRun,
                                       ValidationSummary summary) throws InternalServiceException {
        long mTimeFromCurrent = 0; //Not a delta job. All records with a kaltura_id must be checked every time.

        while (true) {
            List<DsRecordKalturaDto> records;
            try {
                records = fetchStorageRecords(storageClient, origin, mTimeFromCurrent, DsKalturaClientBase.MAX_BATCH_SIZE);
            } catch (Exception e) {
                // Can not fetch more records. Stop validation
                String errorMessage = "Could not fetch more storage records for origin=" + origin + " from mTime=" + mTimeFromCurrent;
                log.error(errorMessage);
                throw new InternalServiceException(errorMessage, e);
            }
            if (records.isEmpty()) {
                return;
            }
            mTimeFromCurrent = records.get(records.size() - 1).getmTime(); //Storage returns records with mTime after this

            List<DsRecordKalturaDto> recordsToValidate = records.stream()
                    .filter(record -> !Boolean.TRUE.equals(record.getDeleted()))
                    .collect(Collectors.toList());
            if (recordsToValidate.isEmpty()) {
                continue;
            }

            List<String> kalturaIds = recordsToValidate.stream()
                    .map(DsRecordKalturaDto::getKalturaId)
                    .collect(Collectors.toList());
            Map<String, EntryStatus> batchStatuses;
            try {
                batchStatuses = getEntryStatuses(kalturaIds);
            } catch (Exception e) {
                log.error("Error fetching Kaltura entry statuses for origin={} batch after mTime={}", origin, mTimeFromCurrent, e);
                throw new InternalServiceException("Error fetching Kaltura entry statuses for origin=" + origin +
                        " batch after mTime=" + mTimeFromCurrent, e);
            }

            for (DsRecordKalturaDto record : recordsToValidate) {
                validateRecord(storageClient, record.getId(), record.getReferenceId(), record.getKalturaId(),
                        batchStatuses.get(record.getKalturaId()), dryRun, summary);
            }
        }
    }

    /**
     * Validate a single record's kaltura_id against Kaltura. If the Kaltura entry is in an error state
     * (ERROR_CONVERTING or ERROR_IMPORTING), the kaltura_id is replaced with the matching error marker. Otherwise, if
     * the kaltura_id does not exist in Kaltura, or exists but is not in status READY, the Kaltura entry is deleted
     * (if it exists) and the kaltura_id is cleared for the record in storage.
     *
     * @param batchStatus The status found for the kaltura_id by the batch lookup, or null if it was not found.
     * @param dryRun      If true, nothing is deleted or cleared. The record is only added to the summary.
     * @param summary     Records that are (or would be) cleared or marked with an error are added to this.
     */
    static void validateRecord(DsStorageClient storageClient, String id, String fileId, String kalturaId,
                                  EntryStatus batchStatus, boolean dryRun, ValidationSummary summary) {
        try {
            EntryStatus status = batchStatus;
            if (status == null) {
                // Kaltura paging is not reliable, so confirm a missing entry with media.get before clearing.
                status = getEntryStatus(kalturaId);
            }
            if (status == EntryStatus.READY) {
                return; //Valid mapping. Nothing to do.
            }

            String prefix = dryRun ? "DRY RUN: " : "";
            String verb = dryRun ? "Would" : "Will";
            StreamErrorTypeDto streamError = KalturaUtil.getStreamError(status);
            if (streamError != null) { //Entry failed in Kaltura. Keep the entry and mark the record with the error.
                log.warn("{}Kaltura entry='{}' for id='{}' has status='{}'. {} set kaltura_id to '{}'.",
                        prefix, kalturaId, id, status, verb, streamError.getValue());
                if (!dryRun) {
                    updateKalturaIdForRecordWithError(storageClient, fileId, streamError);
                }
                summary.addMarkedError(id, kalturaId, status, streamError);
            } else if (status != null) { //Entry exists in Kaltura but is not ready. Remove it.
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

    static void updateKalturaIdForRecordWithError(DsStorageClient storageClient, String fileId, StreamErrorTypeDto streamErrorTypeDto) {
        storageClient.updateKalturaIdForRecord(fileId, streamErrorTypeDto.getValue());
    }

    /**
     * Fetch records with a kaltura_id (id, mTime, referenceId, kaltura_id, deleted) from storage.
     *
     * @param origin    The origin to extract records for.
     * @param mTimeFrom Only extract records with mTime higher than this value.
     * @param batchSize Maximum number of records to extract.
     * @return The records ordered by mTime. Empty when there are no more records.
     */
    static List<DsRecordKalturaDto> fetchStorageRecords(DsStorageClient storageClient, String origin, long mTimeFrom,
                                                        int batchSize) {
        List<DsRecordKalturaDto> records = storageClient.getKalturaRecords(origin, batchSize, mTimeFrom);
        log.debug("Loaded storage records for kaltura validation. origin={}, mTime={}, #records={}", origin, mTimeFrom, records.size());
        return records;
    }

    /**
     * Get the status of a batch of Kaltura entries with a single media.list call.
     *
     * @param kalturaEntryIds The internal kaltura entryIds. At most {@link DsKalturaClientBase#MAX_BATCH_SIZE}.
     * @return Map from entryId to status. Entries not found in Kaltura are absent from the map.
     * @throws APIException If API error
     */
    static Map<String, EntryStatus> getEntryStatuses(List<String> kalturaEntryIds) throws APIException {
        initKalturaClient();
        return kalturaClient.getEntryStatuses(kalturaEntryIds);
    }

    /**
     * Get the status of a single Kaltura entry with media.get.
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
}
