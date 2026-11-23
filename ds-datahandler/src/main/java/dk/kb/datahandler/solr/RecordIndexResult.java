package dk.kb.datahandler.solr;

/**
 * The outcome of indexing a single record: transforming it in ds-present and posting it to solr.
 * <p>
 * The two calls are reported separately. A transformation failure repeats on every run; a solr failure is
 * usually transient.
 */
public class RecordIndexResult {

    /** Used for a call that was never made, typically because the preceding call failed. */
    public static final int NOT_CALLED = 0;

    private final String recordId;
    private final int presentStatus;
    private final int solrStatus;
    private final String exceptionMessage;
    private final String solrResponse;

    private RecordIndexResult(String recordId, int presentStatus, int solrStatus,
                              String exceptionMessage, String solrResponse) {
        this.recordId = recordId;
        this.presentStatus = presentStatus;
        this.solrStatus = solrStatus;
        this.exceptionMessage = exceptionMessage;
        this.solrResponse = solrResponse;
    }

    /**
     * The record was transformed and accepted by solr.
     *
     * @param recordId     the record that was indexed.
     * @param solrResponse the raw response from solr, used for the job bookkeeping.
     */
    public static RecordIndexResult success(String recordId, String solrResponse) {
        return new RecordIndexResult(recordId, 200, 200, null, solrResponse);
    }

    /**
     * ds-present could not deliver the record. Solr was not called.
     *
     * @param presentStatus    HTTP status from ds-present, or {@link #NOT_CALLED} if no response was received.
     * @param exceptionMessage the response from ds-present, or the exception if there was no response.
     */
    public static RecordIndexResult presentFailed(String recordId, int presentStatus, String exceptionMessage) {
        return new RecordIndexResult(recordId, presentStatus, NOT_CALLED, exceptionMessage, null);
    }

    /**
     * The record was transformed, but solr rejected it.
     *
     * @param solrStatus       HTTP status from solr, or {@link #NOT_CALLED} if no response was received.
     * @param exceptionMessage the response from solr, or the exception if there was no response.
     */
    public static RecordIndexResult solrFailed(String recordId, int solrStatus, String exceptionMessage) {
        return new RecordIndexResult(recordId, 200, solrStatus, exceptionMessage, null);
    }

    /** @return true if the record was transformed and accepted by solr. */
    public boolean isSuccess() {
        return exceptionMessage == null;
    }

    /** @return true if ds-present could not deliver the record. */
    public boolean isPresentFailure() {
        return !isSuccess() && presentStatus != 200;
    }

    public String getRecordId() {
        return recordId;
    }

    public int getPresentStatus() {
        return presentStatus;
    }

    public int getSolrStatus() {
        return solrStatus;
    }

    /** @return the response behind a status that was not 200, or null when the record was indexed. */
    public String getExceptionMessage() {
        return exceptionMessage;
    }

    /** @return the raw solr response for a successful post, or null when the record never reached solr. */
    public String getSolrResponse() {
        return solrResponse;
    }

    @Override
    public String toString() {
        return "RecordIndexResult{recordId='" + recordId + "', presentStatus=" + presentStatus +
               ", solrStatus=" + solrStatus + ", exceptionMessage='" + exceptionMessage + "'}";
    }
}
