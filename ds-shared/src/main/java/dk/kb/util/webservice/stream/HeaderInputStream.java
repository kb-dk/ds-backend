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
package dk.kb.util.webservice.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * HTTP specific {@link InputStream} wrapper that keeps track of the headers returned by the server.
 */
public class HeaderInputStream extends FilterInputStream implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(HeaderInputStream.class);

    private final Map<String, List<String>> headers;

    /**
     * Establish a connection to the given {@code uri}, extract all headers and construct a
     * {@link HeaderInputStream} with the headers and response stream from the {@code uri}.
     * @param uri full URI for a call to a webservice.
     * @return an {@code InputStream} with the response.
     * @throws IOException if the connection failed.
     */
    public static HeaderInputStream from(URI uri) throws IOException {
        return from(uri, Collections.emptyMap());
    }

    /**
     * Establish a connection to the given {@code uri}, extract all headers and construct a
     * {@link HeaderInputStream} with the headers and response stream from the {@code uri}.
     * @param uri full URI for a call to a webservice.
     * @param requestHeaders headers to set for the request for content from {@code uri}.
     * @return an {@code InputStream} with the response.
     * @throws IOException if the connection failed.
     */
    public static HeaderInputStream from(URI uri, Map<String, String> requestHeaders) throws IOException {
        HttpURLConnection con = getHttpURLConnection(uri, requestHeaders);
        Map<String, List<String>> headers = con.getHeaderFields();
        return new HeaderInputStream(headers, con.getInputStream());
    }

    /**
     * @param headers the headers from a HTTP response.
     * @param in      the content from a HTTP response.
     */
    public HeaderInputStream(Map<String, List<String>> headers, InputStream in) {
        super(in);
        this.headers = headers;
    }

    /**
     * @return the HTTP headers from the response that this stream was constructed from. Can be null.
     * @deprecated use {@link #getResponseHeaders()} instead.
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * @return the HTTP headers from the response that this stream was constructed from. Can be null.
     */
    public Map<String, List<String>> getResponseHeaders() {
        return headers;
    }

    /**
     * Extract the header with the given key from the response headers.
     * In case of multiple values, only the first one is returned.
     * In case of no values, {@code null} is returned.
     * @param key HTTP response header key.
     * @return the first HTTP response header value for the given {@code key} or null is there are no values.
     */
    public String getResponseHeader(String key) {
        return headers == null || !headers.containsKey(key) || headers.get(key).isEmpty() ?
                null : 
                headers.get(key).get(0);
    }

    /**
     * Extract the header values for the given key from the response headers.
     * In case of no values, the empty list is returned.
     * @param key HTTP response header key.
     * @return the HTTP response header values for the given {@code key}.
     */
    public List<String> getResponseHeaders(String key) {
        return headers == null || !headers.containsKey(key) || headers.get(key).isEmpty() ?
                Collections.emptyList() :
                headers.get(key);
    }

    /**
     * Establish a connection to the given {@code uri}, throwing an exception if the response is not {@code >= 200}
     * and {@code <= 299}.
     * @param uri the URI to establish a connection to.
     * @return a connection to the given {@code uri}.
     * @throws IOException if the connection response was not {@code >= 200} and {@code <= 299}.
     */
    protected static HttpURLConnection getHttpURLConnection(URI uri) throws IOException {
        return getHttpURLConnection(uri, Collections.emptyMap());
    }

    /**
     * Establish a connection to the given {@code uri}, throwing an exception if the response is not {@code >= 200}
     * and {@code <= 299}.
     * @param uri the URI to establish a connection to.
     * @param requestHeaders headers to set for the request for content from {@code uri}.
     * @return a connection to the given {@code uri}.
     * @throws IOException if the connection response was not {@code >= 200} and {@code <= 299}.
     */
    protected static HttpURLConnection getHttpURLConnection(
            URI uri, Map<String, String> requestHeaders) throws IOException {
        log.debug("Opening streaming connection to '{}' with headers {}", uri, requestHeaders);
        HttpURLConnection con = (HttpURLConnection) uri.toURL().openConnection();
        con.setInstanceFollowRedirects(true);
        if (requestHeaders != null) {
            requestHeaders.forEach(con::setRequestProperty);
        }
        int status = con.getResponseCode();
        if (status < 200 || status > 299) {
            throw new IOException("Got HTTP " + status + " establishing connection to '" + uri + "'");
            // TODO: Consider if the error stream should be logged. It can be arbitrarily large
        }
        return con;
    }

}
