package dk.kb.datahandler.kaltura;

import com.kaltura.client.enums.EntryStatus;
import dk.kb.storage.model.v1.StreamErrorTypeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Records whose kaltura_id was cleared or marked with an error by {@link KalturaValidationJob} (or would be in a
 * dry run), split by the reason.
 */
class ValidationSummary {
    private static final Logger log = LoggerFactory.getLogger(ValidationSummary.class);

    private final List<String> notFound = new ArrayList<>();
    private final List<String> notReady = new ArrayList<>();
    private final List<String> markedError = new ArrayList<>();

    void addNotFound(String id, String kalturaId) {
        notFound.add("id=" + id + " kaltura_id=" + kalturaId);
    }

    void addNotReady(String id, String kalturaId, EntryStatus status) {
        notReady.add("id=" + id + " kaltura_id=" + kalturaId + " status=" + status);
    }

    void addMarkedError(String id, String kalturaId, EntryStatus status, StreamErrorTypeDto streamError) {
        markedError.add("id=" + id + " kaltura_id=" + kalturaId + " status=" + status + " error=" + streamError.getValue());
    }

    int size() {
        return notFound.size() + notReady.size() + markedError.size();
    }

    void logSummary(boolean dryRun) {
        String action = dryRun ? "DRY RUN: would change" : "Changed";
        log.info("Kaltura validation summary. {} kaltura_id on {} records: {} not found in Kaltura, {} found but not READY, {} in Kaltura error state",
                action, size(), notFound.size(), notReady.size(), markedError.size());
        if (!notFound.isEmpty()) {
            log.info("Not found in Kaltura ({}): {}", notFound.size(), notFound);
        }
        if (!notReady.isEmpty()) {
            log.info("Found in Kaltura but not READY, entry {} ({}): {}",
                    dryRun ? "would be deleted" : "deleted", notReady.size(), notReady);
        }
        if (!markedError.isEmpty()) {
            log.info("Kaltura error state, kaltura_id {} error ({}): {}",
                    dryRun ? "would be set to" : "set to", markedError.size(), markedError);
        }
    }
}
