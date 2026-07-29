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

import dk.kb.util.json.JSONStreamUtil;
import dk.kb.util.webservice.Service2ServiceRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Stream wrapper with first class support for continuation token, used for requesting extra data that follows
 * logically after the last element in the current stream.
 * <p>
 * This is akin to Solr's {@code cursorMark} and OAI-PMH's {@code resumptionToken}.
 *
 * @param <C> the class for the {@code continuationToken}, typically {@code String>} or {@code Long}.
 */
public class ContinuationInputStream<C> extends HeaderInputStream implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ContinuationInputStream.class);

    private final C continuationToken;
    private final Boolean hasMore;
    private final Long recordCount;

    /**
     * Establish a connection to the given {@code uri}, extract all headers and construct a
     * {@link ContinuationInputStream} with the headers and response stream from the {@code uri}.
     * @param uri full URI for a call to a webservice.
     * @param tokenMapper maps the String header {@link ContinuationUtil#HEADER_PAGING_CONTINUATION_TOKEN}
     *                    to the concrete token type.
     * @return an {@code InputStream} with the response.
     * @throws IOException if the connection failed.
     */
    public static <C2> ContinuationInputStream<C2> from(URI uri, Function<String, C2> tokenMapper) throws IOException {
        return from(uri, tokenMapper, null);
    }

    /**
     * Establish a connection to the given {@code uri}, extract all headers and construct a
     * {@link ContinuationInputStream} with the headers and response stream from the {@code uri}.
     * @param uri full URI for a call to a webservice.
     * @param tokenMapper maps the String header {@link ContinuationUtil#HEADER_PAGING_CONTINUATION_TOKEN}
     *                    to the concrete token type.
     * @param requestHeaders optional headers for the connection. Can be null.
     * @return an {@code InputStream} with the response.
     * @throws IOException if the connection failed.
     */
    public static <C2> ContinuationInputStream<C2> from(
            URI uri, Function<String, C2> tokenMapper, Map<String, String> requestHeaders) throws IOException {
      
        if (requestHeaders== null) { //in case this is called with null map.
            requestHeaders= new HashMap<String, String>(); 
        }
        
        String token= Service2ServiceRequest.getOAuth2Token();   
        if (token != null) {           
            log.debug("OAuth2 Bearer token added to service2service call");
            requestHeaders.put("Authorization","Bearer "+token);
        }
        else {
            log.debug("No OAuth token was found for service2service request");
        }
        
        
        
        HttpURLConnection con = getHttpURLConnection(uri, requestHeaders);
        Map<String, List<String>> headers = con.getHeaderFields();
        C2 continuationToken = ContinuationUtil.getContinuationToken(headers, tokenMapper).orElse(null);
        Boolean hasMore = ContinuationUtil.getHasMore(headers).orElse(null);
        Long recordCount = ContinuationUtil.getRecordCount(headers).orElse(null);
        log.debug("Established connection with continuation token '{}', hasMore {} and recordCount {} to '{}'",
                continuationToken, hasMore, recordCount, uri);
        return new ContinuationInputStream<>(con.getInputStream(), continuationToken, hasMore, recordCount, headers);
    }

    /**
     * Construct a continuation stream directly from the given parameters.
     *
     * @param in                the content stream.
     * @param continuationToken the continuation token. Can be {@code null}.
     * @param hasMore           has more signal. Can be {@code null}
     * @param recordCount       the number od records in the stream. Can be {@code null}.
     * @param responseHeaders   HTTP headers from a HTTP connection. Can be {@code null}.
     */
    public ContinuationInputStream(
            InputStream in, C continuationToken, Boolean hasMore, Long recordCount,
            Map<String, List<String>> responseHeaders) {
        super(responseHeaders, in);
        this.continuationToken = continuationToken;
        this.hasMore = hasMore;
        this.recordCount = recordCount;
    }

    /**
     * Construct a continuation stream directly from the given parameters.
     *
     * @param in                the content stream.
     * @param continuationToken the continuation token. Can be {@code null}.
     * @param hasMore           has more signal. Can be {@code null}
     */
    public ContinuationInputStream(InputStream in, C continuationToken, Boolean hasMore) {
        this(in, continuationToken, hasMore, null, null);
    }

    /**
     * Construct a continuation stream directly from the given parameters.
     *
     * @param in                the content stream.
     * @param continuationToken the continuation token. Can be {@code null}.
     * @param hasMore           has more signal. Can be {@code null}
     * @param recordCount       the number od records in the stream. Can be {@code null}.
     */
    public ContinuationInputStream(InputStream in, C continuationToken, Boolean hasMore, Long recordCount) {
        this(in, continuationToken, hasMore, recordCount, null);
    }

    /**
     * Create an object stream from the JSON byte stream by deserializing to instances of the given {@code clazz}.
     * @param clazz the class to deserialize the JSON nodes to.
     * @return a stream of objects created from this byte stream.
     */
    public <T> ContinuationStream<T, C> stream(Class<T> clazz) {
        try {
            return new ContinuationStream<>(
                    JSONStreamUtil.jsonToObjectsStream(this, clazz),
                    getContinuationToken(), hasMore(), getRecordCount(), getResponseHeaders());
        } catch (IOException e) {
            throw new RuntimeException("IOException constructing object stream", e);
        }
    }

    /**
     * Set the continuation token and hasMore as HTTP headers on the given {@code httpServletResponse}
     * @param httpServletResponse headers will be assigned here.
     * @return this continuation byte stream, usable for chaining.
     */
    public ContinuationInputStream<C> setHeaders(HttpServletResponse httpServletResponse) {
        ContinuationUtil.setHeaderContinuation(httpServletResponse, getContinuationToken());
        ContinuationUtil.setHeaderHasMore(httpServletResponse, hasMore());
        ContinuationUtil.setHeaderRecordCount(httpServletResponse, getRecordCount());
        return this;
    }

    /**
     * @return continuation token intended for requesting a new stream that delivers from the point where the
     * current stream stops. If {@code null}, no continuation information is available.
     * @see #hasMore()
     * @see #getRecordCount()
     */
    public C getContinuationToken() {
        return continuationToken;
    }

    /**
     * Non-authoritative indicator for whether an immediate request for a new stream using
     * {@link #getContinuationToken()} will return any results.
     *
     * @return true if a request for a new stream using {@link #getContinuationToken()} will result in data,
     * false if such a call will <em>probably</em> not give any data at the current time, but might
     * result is extra data at a later point in time. {@code null} signals undecided.
     * @see #getContinuationToken()
     * @see #getRecordCount()
     */
    public Boolean hasMore() {
        return hasMore;
    }

    /**
     * Non-authoritative indicator for the number of records that are serialised in this stream.
     *
     * @return number of records in the stream. {@code null} signals undecided.
     * @see #getContinuationToken()
     * @see #hasMore()
     */
    public Long getRecordCount() {
        return recordCount;
    }
}
