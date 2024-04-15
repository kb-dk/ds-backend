/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.image;

import com.damnhandy.uri.template.UriTemplate;
import dk.kb.util.string.Strings;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Collection;
import java.util.List;
import java.util.Objects;


/**
 * Implementation of HTTP(S) proxying.
 */
@SuppressWarnings("UnusedReturnValue")
public class ProxyHelper {

	final static String HEADER_ACCEPT="Accept";
	private static final Logger log = LoggerFactory.getLogger(ProxyHelper.class);

    /**
     * Streams the content from the given uri. In the case of HTTP codes outside of the 200-299 range, a matching
     * {@link ServiceException} is thrown.
     * @param request image ID or similar information used to construct exception messages to the caller.
     *                The uri is NOT stated in any exception messages as that might be considered confidential.
     * @param uri the URI to proxy.
     * @param clientRequestURI the original request URI from the client. Used only for logging.
     * @param httpHeaders the original httpHeaders from the client. Used to transfer specific header fields to image server request. 
     * @return a lambda providing the data from the given uri.
     */
    public static StreamingOutput proxy(String request, URI uri, URI clientRequestURI, HttpHeaders httpHeaders) {
            return proxy(request, uri, clientRequestURI, null, httpHeaders);
    }

    /**
     * Streams the content from the given uri. In the case of HTTP codes outside of the 200-299 range, a matching
     * {@link ServiceException} is thrown.
     * @param request image ID or similar information used to construct exception messages to the caller.
     *                The uri is NOT stated in any exception messages as that might be considered confidential.
     * @param uri the URI to proxy.
     * @param clientRequestURI the original request URI from the client. Used only for logging.
     * @param httpHeaders the original httpHeaders from the client. Used to transfer specific header fields to image server request.
     * @return a lambda providing the data from the given uri.
     */
    public static StreamingOutput proxy(String request, String uri, URI clientRequestURI, HttpHeaders httpHeaders) {
        URI realURI;
        try {
            realURI = new URI(uri);
        } catch (URISyntaxException e) {
            log.warn("Error processing '{}': Unable to create URI from '{}' for user request '{}'",
                    request, uri, clientRequestURI, e);
            throw new InternalServerErrorException(
                    "Invalid internally generated proxy-URI for request '" + clientRequestURI + "'");
        }
        return proxy(request, realURI, clientRequestURI, null, httpHeaders);
    }

    /**
     * Streams the content from the given uri. In the case of HTTP codes outside of the 200-299 range, a matching
     * {@link ServiceException} is thrown.
     * @param request image ID or similar information used to construct exception messages to the caller.
     *                The uri is NOT stated in any exception messages as that might be considered confidential.
     * @param uri the URI to proxy.
     * @param clientRequestURI the original request URI from the client. Used only for logging.
     * @param httpHeaders the original httpHeaders from the client. Used to transfer specific header fields to image server request.
     * @param httpServletResponse used for setting the {@code Content-Type} to match the one delivered form uri. Ignored if null.
     * @return a lambda providing the data from the given uri.
     */
    public static StreamingOutput proxy(
            String request, URI uri, URI clientRequestURI, HttpServletResponse httpServletResponse, HttpHeaders httpHeaders) {
            String acceptHeader = (httpHeaders != null)  ? httpHeaders.getHeaderString(HEADER_ACCEPT) : null; 

        //If more headerfields besides Accept is transfered to proxy request add them to log.
    	log.debug("proxy(request='{}', uri='{}', clientRequestURI='{}', httpServletResponse={}, acceptHeader={}) called",
                  request, uri, clientRequestURI, httpServletResponse == null ? "not present" : "present", acceptHeader);
        final HttpURLConnection connection = establishConnection(request, uri, clientRequestURI,httpHeaders);
        try {
            validateStatuscode(request, uri, clientRequestURI, connection.getResponseCode());
        } catch (IOException e) {
            log.warn("Unable to establish connection for request '{}' to '{}' for client request '{}'",
                     request, uri, clientRequestURI, e);
            throw new ServiceException("Unable to establish connection to '" + request + "'",
                                       Response.Status.BAD_GATEWAY);
        }
        if (httpServletResponse != null) {
            copyHeaders(connection, httpServletResponse);
        }
        return output -> {
            pipeContent(request, uri, clientRequestURI, connection, output);
        };
    }

    /**
     * Copy the {@code Content-Type} header from connection to httpServletResponse.
     * @param connection an established connection.
     * @param httpServletResponse servlet response for the called endpoint.
     */
    private static void copyHeaders(HttpURLConnection connection, HttpServletResponse httpServletResponse) {
        String contentType = connection.getHeaderField("Content-Type");
        if (contentType != null) {
            httpServletResponse.setContentType(contentType);
        }
    }

    /**
     * Does nothing if the status code is in the {@code 200-299} range. Else throws an exception.
     * @param request image ID or similar information used to construct error messages to the caller.
     * @param uri the URI to proxy.
     * @param clientRequestURI the original request URI from the client. Used only for logging.
     * @param statusCode the status code from the established connection.
     * @throws ServiceException if the status code is not {@code 200-299}.
     */
    private static void validateStatuscode(String request, URI uri, URI clientRequestURI, int statusCode) {
        if (statusCode >= 200 && statusCode <= 299) { // All OK
            return;
        }

        if (statusCode >= 400 && statusCode <= 499) { // Request problems
            log.warn("Client error {} for connection to '{}' for client request '{}'",
                     statusCode, uri, clientRequestURI);
            throw new ServiceException("Unable to proxy request for '" + request + "'",
                                       Response.Status.fromStatusCode(statusCode));
        }

        if (statusCode >= 500 && statusCode <= 599) { // Server problems
            log.warn("Remote server error {} for connection to '{}' for client request '{}'",
                     statusCode, uri, clientRequestURI);
            throw new ServiceException("Unable to proxy request for '" + request + "'",
                                       Response.Status.fromStatusCode(statusCode));
        }

        if (statusCode == -1) {
            log.warn("Remote response not valid HTTP (-1) for connection to '{}' for client request '{}'",
                    uri, clientRequestURI);
            throw new ServiceException("Unable to proxy request for '" + request + "' due to proxied server error -1",
                    Response.Status.fromStatusCode(500));
        }

        log.warn("Unhandled status code {} for connection to '{}' for client request '{}'",
                 statusCode, uri, clientRequestURI);
        throw new ServiceException("Unhandled status code " + statusCode + " for proxy connection for '" +
                                   request + "'",
                                   Response.Status.INTERNAL_SERVER_ERROR);
    }

    /**
     * Establishes a connection to the given uri.
     * @param request image ID or similar information used to construct error messages to the caller.
     * @param uri the URI to proxy.
     * @param clientRequestURI the original request URI from the client. Used only for logging.
     * @param httpHeaders the original httpHeaders from the client. Used to transfer specific header fields to image server request.
     * @return a connection to uri.
     * @throws ServiceException is the connection could not be established.
     */
    private static HttpURLConnection establishConnection(String request, URI uri, URI clientRequestURI,HttpHeaders httpHeaders) {
        HttpURLConnection connection;
        try  {
            connection = (HttpURLConnection) uri.toURL().openConnection();
        } catch (Exception e) {
            log.warn("Unable to create passive proxy connection with URI '{}' from client request '{}'",
                     uri, clientRequestURI, e);
            throw new InternalServiceException("Unable to create passive proxy for '" + request + "'");
        }

        connection.setInstanceFollowRedirects(true);
        try {
            connection.setRequestMethod("GET");
        } catch (ProtocolException e) {
            log.warn("Unable to set request method to 'GET' with URI '{}' from client request '{}'",
                     uri, clientRequestURI, e);
            throw new InternalServiceException("Unable to set 'GET' as request method for '" + request + "'");
        }
        connection.setRequestProperty("User-Agent", "ds-image");
     
        if (httpHeaders != null && httpHeaders.getHeaderString(HEADER_ACCEPT) != null) { //The test code will call it without httpHeaders
          connection.addRequestProperty("Accept",httpHeaders.getHeaderString(HEADER_ACCEPT));
        }
        
        // TODO: Add timeouts, but make them configurable
        //con.setConnectTimeout(1000);
        //con.setReadTimeout(1000);

        try {
            connection.connect();
        } catch (SocketTimeoutException e) {
            log.warn("Timeout establishing connection to '{}' for from client request '{}'",
                     uri, clientRequestURI, e);
            throw new ServiceException("Timeout establishing proxy connection for '" + request + "'",
                                       Response.Status.GATEWAY_TIMEOUT);
        } catch (IOException e) {
            log.warn("Unable to establish connection to '{}' for from client request '{}'",
                     uri, clientRequestURI, e);
            throw new ServiceException("Unable to establish proxy connection for '" + request + "'",
                                       Response.Status.BAD_GATEWAY);
        }
        return connection;
    }


    /**
     * Streams the content from the given connection to output.
     * Closes the inputstream from the connection after streaming.
     * @param request image ID or similar information used to construct error messages to the caller.
     * @param uri the URI to proxy.
     * @param clientRequestURI the original request URI from the client. Used only for logging.
     * @param connection a previously established connection to uri.
     * @param output the destination for the bytes received from connection.
     * @return a lambda providing the data from the given uri.
     */
    private static void pipeContent(
            String request, URI uri, URI clientRequestURI, HttpURLConnection connection, OutputStream output) {
        try (InputStream remoteStream = connection.getInputStream()) {
            long copiedBytes = IOUtils.copyLarge(remoteStream, output);
            log.debug("Proxied {} bytes for remote request '{}' for client request '{}'",
                      copiedBytes, uri, clientRequestURI);
        } catch (Exception e) {
            log.warn("Unable to proxy remote request '{}' for client request '{}'", uri, clientRequestURI);
            throw new InternalServerErrorException("Unable to serve request for image '" + request + "'");
        }
    }

    /**
     * Add a query param to the UriTemplate if a value is present and not the empty string.
     * <p>
     * If the value is a Collection, it is serialized as comma separated String-representation of the elements.
     * @return the given template for chaining.
     */
    public static UriTemplate addIfPresent(UriTemplate template, String key, Object value) {
        if (value != null && !Objects.toString(value).isEmpty()) {
            if (value instanceof Collection) {
                template.set(key, Strings.join((Collection<?>)value, ", "));
            } else {
                template.set(key, value.toString());
            }
        }
        return template;
    }

}
