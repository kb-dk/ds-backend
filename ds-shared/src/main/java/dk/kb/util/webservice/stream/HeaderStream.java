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

import dk.kb.util.FilterStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * HTTP specific {@link Stream} wrapper that keeps track of the headers returned by the server.
 * @param <T> the class of objects for the stream.
 */
public class HeaderStream<T> extends FilterStream<T> implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(HeaderStream.class);

    protected final Map<String, List<String>> responseHeaders;

    /**
     * Take the {@code inner} stream and wrap it, without setting headers.
     * Except for the class, this is effectively the same as the non-wrapped {@code inner}.
     *
     * @param inner the stream to wrap as a {@code HeaderStream}.
     * @see #HeaderStream(Stream, Map)
     */
    public HeaderStream(Stream<T> inner) {
        super(inner);
        this.responseHeaders = null;
        log.debug("Creating HeaderStream without headers");
    }

    /**
     * Take the {@code inner} stream and wrap it along with the provided {@code responseHeaders}.
     * All stream functionality for {@code inner} is unchanged.
     *
     * @param inner           the stream to wrap as a {@code HeaderStream}.
     * @param responseHeaders HTTP headers from the response that {@code inner} was constructed from.
     */
    public HeaderStream(Stream<T> inner, Map<String, List<String>> responseHeaders) {
        super(inner);
        this.responseHeaders = responseHeaders;
        log.debug("Creating HeaderStream with headers {}", responseHeaders);
    }

    /**
     * @return the HTTP headers from the response that this stream was constructed from. Can be null.
     */
    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * Extract the header with the given key from the response headers.
     * In case of multiple values, only the first one is returned.
     * In case of no values, {@code null} is returned.
     * @param key HTTP response header key.
     * @return First HTTP response header value for the given {@code key} or null is there are no values.
     */
    public String getResponseHeader(String key) {
        return responseHeaders == null || !responseHeaders.containsKey(key) || responseHeaders.get(key).isEmpty() ?
                null :
                responseHeaders.get(key).get(0);
    }

    /**
     * Extract the header values for the given key from the response headers.
     * In case of no values, the empty list is returned.
     * @param key HTTP response header key.
     * @return the HTTP response header values for the given {@code key}.
     */
    public List<String> getResponseHeaders(String key) {
        return responseHeaders == null || !responseHeaders.containsKey(key) || responseHeaders.get(key).isEmpty() ?
                Collections.emptyList() :
                responseHeaders.get(key);
    }


}
