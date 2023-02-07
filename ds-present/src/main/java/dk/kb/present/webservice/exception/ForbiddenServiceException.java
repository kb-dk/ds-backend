package dk.kb.present.webservice.exception;

import dk.kb.util.webservice.exception.ServiceException;

import javax.ws.rs.core.Response;

/*
 * Custom web-exception class (401)
 */
public class ForbiddenServiceException extends ServiceException {

    //Constant fields for the OpenApi
    public static final String description = "ForbiddenServiceException";
    public static final String responseCode = "403";

    private static final long serialVersionUID = 27152722L;
    private static final Response.Status responseStatus = Response.Status.FORBIDDEN; // 401

    public ForbiddenServiceException() {
        super(responseStatus);
    }

    public ForbiddenServiceException(String message) {
        super(message, responseStatus);
    }

    public ForbiddenServiceException(String message, Throwable cause) {
        super(message, cause, responseStatus);
    }

    public ForbiddenServiceException(Throwable cause) {
        super(cause, responseStatus);
    }

    public ForbiddenServiceException(String mimeType, Object entity) {
        super(mimeType, entity, responseStatus);
    }

    public ForbiddenServiceException(String mimeType, Object entity, Throwable cause) {
        super(mimeType, entity, cause, responseStatus);
    }

}
