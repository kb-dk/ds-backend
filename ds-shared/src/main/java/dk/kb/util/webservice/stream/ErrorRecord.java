package dk.kb.util.webservice.stream;

/**
 * An object containing information on transformations errors for a given XSLT transformation.
 * This object can contain the ID of the record which failed the transformation and the error message for the failure.
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
