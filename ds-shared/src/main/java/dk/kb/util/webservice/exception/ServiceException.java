package dk.kb.util.webservice.exception;

import javax.ws.rs.core.Response;

/*
 * Superclass for Exceptions that has a specific HTTP response code.
 * </p><p>
 * Note that this class has 2 "modes": Plain text message or custom response object,
 * intended for use with OpenAPI-generated Dto response objects.
 */
public class ServiceException extends RuntimeException {
    private static final long serialVersionUID = 27182819L;
    private final Response.Status responseStatus;

    private String mimeType = "text/plain";
    private Object entity = null;

	public Response.Status getResponseStatus() {
		return responseStatus;
	}
	
	public ServiceException(Response.Status responseStatus) {
        super();
		this.responseStatus = responseStatus;
	}
    
    public ServiceException(String message, Response.Status responseStatus) {
        super(message);
		this.responseStatus = responseStatus;
	}
    
    public ServiceException(String message, Throwable cause, Response.Status responseStatus) {
        super(message, cause);
		this.responseStatus = responseStatus;
	}
    
    public ServiceException(Throwable cause, Response.Status responseStatus) {
        super(cause);
		this.responseStatus = responseStatus;
	}

	/**
	 * Custom message object.
	 * @param mimeType the MIME type used for the HTTP response headers.
	 * @param entity the entity to translate into the HTTP response body (normally an OpenAPI generated Dto).
	 * @param responseStatus HTTP response code.
	 */
	public ServiceException(String mimeType, Object entity, Response.Status responseStatus) {
        super();
		this.responseStatus = responseStatus;
		this.mimeType = mimeType;
		this.entity = entity;
	}

	/**
	 * Custom message object.
	 * @param mimeType the MIME type used for the HTTP response headers.
	 * @param entity the entity to translate into the HTTP response body (normally an OpenAPI generated Dto).
	 * @param cause the originating Exception.
	 * @param responseStatus HTTP response code.
	 */
	public ServiceException(String mimeType, Object entity, Throwable cause, Response.Status responseStatus) {
        super(cause);
		this.responseStatus = responseStatus;
		this.mimeType = mimeType;
		this.entity = entity;
	}

	/**
	 * Custom message object.
	 * @param message the message for the Exception.
	 * @param mimeType the MIME type used for the HTTP response headers.
	 * @param entity the entity to translate into the HTTP response body (normally an OpenAPI generated Dto).
	 * @param cause the originating Exception.
	 * @param responseStatus HTTP response code.
	 */
	public ServiceException(String message, String mimeType, Object entity, Throwable cause,
							Response.Status responseStatus) {
        super(message, cause);
		this.responseStatus = responseStatus;
		this.mimeType = mimeType;
		this.entity = entity;
	}

	/**
	 * Transfers {@link #mimeType}, {@link #entity} and {@link #responseStatus} from inner, wrapping inner and assigning
	 * the given message.
	 * @param inner   any Serviceexception.
	 * @param message the message for the new ServiceException.
	 * @return a ServiceException with the same {@link #mimeType}, {@link #entity} and {@link #responseStatus},
	 * but wrapping the old exception and stating the given message.
	 */
	public static ServiceException extend(ServiceException inner, String message) {
		return new ServiceException(message, inner.getMimeType(), inner.getEntity(), inner, inner.getResponseStatus());
	}

	/**
	 * Wraps the current ServiceExcetion, transferring {@link #mimeType}, {@link #entity} and {@link #responseStatus}
	 * to a new ServiceException and assigning the given message.
	 * @param message the message for the new ServiceException.
	 * @return a ServiceException with the same {@link #mimeType}, {@link #entity} and {@link #responseStatus},
	 * but wrapping the old exception and stating the given message.
	 */
	public ServiceException extend(String message) {
		return extend(this, message);
	}

	public String getMimeType() {
		return mimeType;
	}

	public Object getEntity() {
		return entity == null ? getMessage() : entity;
	}
}
