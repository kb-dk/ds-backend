package dk.kb.datahandler.kaltura;

import com.kaltura.client.enums.EntryStatus;
import dk.kb.storage.model.v1.StreamErrorTypeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Records whose kaltura_id was cleared or marked with an error by {@link KalturaValidationJob} (or would be in a
 * dry run), split by the reason. Also counts the entries that had to be confirmed with a media.get fallback lookup.
 */
class ValidationSummary {
    private static final Logger log = LoggerFactory.getLogger(ValidationSummary.class);

    private final List<String> notFound = new ArrayList<>();
    private final List<String> noContent = new ArrayList<>();
    private final List<String> markedError = new ArrayList<>();
    private int fallbackLookups = 0;

    void addNotFound(String id, String kalturaId) {
        notFound.add("id=" + id + " kaltura_id=" + kalturaId);
    }

    void addNoContent(String id, String kalturaId) {
        noContent.add("id=" + id + " kaltura_id=" + kalturaId);
    }

    void addMarkedError(String id, String kalturaId, EntryStatus status, StreamErrorTypeDto streamError) {
        markedError.add("id=" + id + " kaltura_id=" + kalturaId + " status=" + status + " error=" + streamError.getValue());
    }

    /**
     * Count an entry that was missing from the batch media.list result and had to be looked up with media.get.
     */
    void addFallbackLookup() {
        fallbackLookups++;
    }

    int size() {
        return notFound.size() + noContent.size() + markedError.size();
    }

    int getFallbackLookups() {
        return fallbackLookups;
    }

    void logSummary(boolean dryRun) {
        String action = dryRun ? "DRY RUN: would change" : "Changed";
        log.info("Kaltura validation summary. {} kaltura_id on {} records: {} not found in Kaltura, {} with no content, " +
                        "{} in Kaltura error state. Confirmed with media.get: {}",
                action, size(), notFound.size(), noContent.size(), markedError.size(), fallbackLookups);
        if (!notFound.isEmpty()) {
            log.info("Not found in Kaltura ({}): {}", notFound.size(), notFound);
        }
        if (!noContent.isEmpty()) {
            log.info("No content in Kaltura, entry {} ({}): {}",
                    dryRun ? "would be deleted" : "deleted", noContent.size(), noContent);
        }
        if (!markedError.isEmpty()) {
            log.info("Kaltura error state, kaltura_id {} error ({}): {}",
                    dryRun ? "would be set to" : "set to", markedError.size(), markedError);
        }
    }
}
