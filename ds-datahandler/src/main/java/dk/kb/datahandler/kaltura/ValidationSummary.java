package dk.kb.datahandler.kaltura;

import com.kaltura.client.enums.EntryStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Records whose kaltura_id was cleared by {@link KalturaValidationJob} (or would be cleared in a dry run),
 * split by the reason.
 */
class ValidationSummary {
    private static final Logger log = LoggerFactory.getLogger(ValidationSummary.class);

    private final List<String> notFound = new ArrayList<>();
    private final List<String> notReady = new ArrayList<>();

    void addNotFound(String id, String kalturaId) {
        notFound.add("id=" + id + " kaltura_id=" + kalturaId);
    }

    void addNotReady(String id, String kalturaId, EntryStatus status) {
        notReady.add("id=" + id + " kaltura_id=" + kalturaId + " status=" + status);
    }

    int size() {
        return notFound.size() + notReady.size();
    }

    void logSummary(boolean dryRun) {
        String action = dryRun ? "DRY RUN: would clear" : "Cleared";
        log.info("Kaltura validation summary. {} kaltura_id on {} records: {} not found in Kaltura, {} found but not READY",
                action, size(), notFound.size(), notReady.size());
        if (!notFound.isEmpty()) {
            log.info("Not found in Kaltura ({}): {}", notFound.size(), notFound);
        }
        if (!notReady.isEmpty()) {
            log.info("Found in Kaltura but not READY, entry {} ({}): {}",
                    dryRun ? "would be deleted" : "deleted", notReady.size(), notReady);
        }
    }
}
