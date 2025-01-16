package dk.kb.present.util;

/**
 * An object containing information on transformations errors for a given {@link dk.kb.storage.model.v1.DsRecordDto}.
 * This object contains the ID of the DsRecord which failed the transformation and the error message for the failure.
 */
public class ErrorRecord {

    public final String id;

    public String errorMessage;

    public ErrorRecord(String id, String errorMessage) {
        this.id = id;
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "ErrorRecord{" +
                "id='" + id + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
