package dk.kb.datahandler.kaltura;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Counts the records whose kaltura_id was cleared or marked with an error by {@link KalturaValidationJob} (or would
 * be in a dry run), split by the reason. Also counts the entries that had to be confirmed with a media.get fallback
 * lookup. Each record is logged by the job when it is handled, so only the counts are kept here.
 */
class ValidationSummary {
    private static final Logger log = LoggerFactory.getLogger(ValidationSummary.class);

    private int notFound = 0;
    private int noContent = 0;
    private int markedError = 0;
    private int fallbackLookups = 0;

    void addNotFound() {
        notFound++;
    }

    void addNoContent() {
        noContent++;
    }

    void addMarkedError() {
        markedError++;
    }

    /**
     * Count an entry that was missing from the batch media.list result and had to be looked up with media.get.
     */
    void addFallbackLookup() {
        fallbackLookups++;
    }

    int size() {
        return notFound + noContent + markedError;
    }

    int getFallbackLookups() {
        return fallbackLookups;
    }

    void logSummary(boolean dryRun) {
        String action = dryRun ? "DRY RUN: would change" : "Changed";
        log.info("Kaltura validation summary. {} kaltura_id on {} records: {} not found in Kaltura, {} with no content, " +
                        "{} in Kaltura error state. Confirmed with media.get: {}",
                action, size(), notFound, noContent, markedError, fallbackLookups);
    }
}
