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

import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;

/**
 * Implementation of HTTP(S) proxying.
 */
@SuppressWarnings("UnusedReturnValue")
public class ProxyHelper {
    private static final Logger log = LoggerFactory.getLogger(ProxyHelper.class);

    /**
     * Streams the content from the given uri. In the case of HTTP codes outside of the 200-299 range, a matching
     * {@link ServiceException} is thrown.
     * @param request image ID or similar information used to construct error messages to the caller.
     * @param uri the URI to proxy.
     * @param clientRequestURI the original request URI from the client. Used only for logging.
     * @return a lambda providing the data from the given uri.
     */
    public static StreamingOutput proxy(String request, URI uri, URI clientRequestURI) {
        return output -> {
            HttpURLConnection connection;

            try  {
                connection = (HttpURLConnection)uri.toURL().openConnection();
            } catch (Exception e) {
                log.warn("Unable to create passive proxy connection with URI '{}' from client request '{}'",
                                        uri, clientRequestURI, e);
                throw new InternalServiceException("Unable to create passive proxy for '" + request + "'");
            }

            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "ds-image");
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

            int statusCode = connection.getResponseCode();

            if (statusCode >= 200 && statusCode <= 299) { // All OK
                try (InputStream remoteStream = uri.toURL().openStream()) {
                    long copiedBytes = IOUtils.copyLarge(remoteStream, output);
                    log.debug("Proxied {} bytes for remote request '{}' for client request '{}'",
                                             copiedBytes, uri, clientRequestURI);
                } catch (Exception e) {
                    log.warn("Unable to proxy remote request '{}' for client request '{}'", uri, clientRequestURI);
                    throw new InternalServerErrorException("Unable to serve request for image '" + request + "'");
                }
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

            log.warn("Unhandled status code {} for connection to '{}' for client request '{}'",
                                    statusCode, uri, clientRequestURI);
            throw new ServiceException("Unhandled status code " + statusCode + " for proxy connection for '" +
                                       request + "'",
                                       Response.Status.INTERNAL_SERVER_ERROR);
        };
    }

    /**
     * Add a query param to the UriBuilder if a value is present.
     * @return the given builder for chaining.
     */
    public static UriBuilder addIfPresent(UriBuilder builder, String key, Object value) {
        if (value != null) {
            builder.queryParam(key, value);
        }
        return builder;
    }

    /**
     * Add a query param to the UriBuilder if a value is present.
     * The value will be serialized as comma-deparated values.
     * @return the given builder for chaining.
     */
    public static UriBuilder addIfPresent(UriBuilder builder, String key, List<? extends Object> values) {
        if (values != null && !values.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Object value: values) {
                if (sb.length() != 0) {
                    sb.append(",");
                }
                sb.append(value.toString());
            }
            builder.queryParam(key, sb.toString());
        }
        return builder;
    }
}
