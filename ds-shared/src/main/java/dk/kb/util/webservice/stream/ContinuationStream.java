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

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Stream wrapper with first class support for continuation token, used for requesting extra data that follows
 * logically after the last element in the current stream.
 * <p>
 * This is akin to Solr's {@code cursorMark} and OAI-PMH's {@code resumptionToken}.
 * 
 * @param <T> the class of objects for the stream.
 * @param <C> the class for the {@code continuationToken}, typically {@code String>} or {@code Long}.
 */
public class ContinuationStream<T, C> extends HeaderStream<T> implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ContinuationStream.class);

    private final C continuationToken;
    private final Boolean hasMore;
    private final Long recordCount;

    /**
     * Create a stream.
     *
     * @deprecated use {@link ContinuationStream#ContinuationStream(Stream, Object, Boolean, Long)} instead.
     *
     * @param inner             the provider of the elements.
     * @param continuationToken used for requesting a new stream that continues after the last element of the
     *                          current stream. If {@code null}, no continuation information is available.
     * @param hasMore           whether or not a subsequent request for a stream is likely to produce any elements.
     */
    public ContinuationStream(Stream<T> inner, C continuationToken, Boolean hasMore) {
        this(inner, continuationToken, hasMore, null, null);
    }

    /**
     * Create a stream.
     *
     * @param inner             the provider of the elements.
     * @param continuationToken used for requesting a new stream that continues after the last element of the
     *                          current stream. If {@code null}, no continuation information is available.
     * @param hasMore           whether or not a subsequent request for a stream is likely to produce any elements.
     * @param recordCount       the amount of records in the stream.
     */
    public ContinuationStream(Stream<T> inner, C continuationToken, Boolean hasMore, Long recordCount) {
        this(inner, continuationToken, hasMore, recordCount, null);
    }

    /**
     * Create a stream.
     *
     * @deprecated use {@link ContinuationStream#ContinuationStream(Stream, Object, Boolean, Long, Map)} instead.
     *
     * @param inner             the provider of the elements.
     * @param continuationToken used for requesting a new stream that continues after the last element of the
     *                          current stream. If {@code null}, no continuation information is available.
     * @param hasMore           whether or not a subsequent request for a stream is likely to produce any elements.
     * @param responseHeaders HTTP headers from the response that {@code inner} was constructed from.
     */
    public ContinuationStream(Stream<T> inner, C continuationToken, Boolean hasMore,
                              Map<String, List<String>> responseHeaders) {
        this(inner, continuationToken, hasMore, null, responseHeaders);
    }

    /**
     * Create a stream.
     *
     * @param inner             the provider of the elements.
     * @param continuationToken used for requesting a new stream that continues after the last element of the
     *                          current stream. If {@code null}, no continuation information is available.
     * @param hasMore           whether or not a subsequent request for a stream is likely to produce any elements.
     * @param recordCount       the amount of records in the stream. This can be {@code null}.
     * @param responseHeaders HTTP headers from the response that {@code inner} was constructed from.
     */
    public ContinuationStream(Stream<T> inner, C continuationToken, Boolean hasMore, Long recordCount,
                              Map<String, List<String>> responseHeaders) {
        super(inner, responseHeaders);
        this.continuationToken = continuationToken;
        this.hasMore = hasMore;
        this.recordCount = recordCount;
        log.debug("Creating ContinuationStream with continuationToken='{}', hasMore={}, recordCount={}, responseHeaders={}",
                  continuationToken, hasMore, recordCount, responseHeaders);
    }

    /**
     * Set the continuation token, hasMore and recordCount as HTTP headers on the given {@code httpServletResponse}
     * @param httpServletResponse headers will be assigned here.
     * @return this continuation stream, usable for chaining.
     */
    public ContinuationStream<T, C> setHeaders(HttpServletResponse httpServletResponse) {
        ContinuationUtil.setHeaderContinuation(httpServletResponse, getContinuationToken());
        ContinuationUtil.setHeaderHasMore(httpServletResponse, hasMore());
        ContinuationUtil.setHeaderRecordCount(httpServletResponse, getRecordCount());
        return this;
    }

    /**
     * @return continuation token intended for requesting a new stream that delivers from the point where the
     * current stream stops. If {@code null}, no continuation information is available.
     * @see #hasMore()
     */
    public C getContinuationToken() {
        return continuationToken;
    }

    /**
     * Non-authoritative indicator for whether or not an immediate request for a new stream using
     * {@link #getContinuationToken()} will return any results.
     *
     * @return true if a request for a new stream using {@link #getContinuationToken()} will result in data,
     * false if such a call will <em>probably</em> not give any data at the current time, but might
     * result is extra data at a later point in time.
     * @see #getContinuationToken()
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
