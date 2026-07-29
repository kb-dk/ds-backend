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

import dk.kb.util.Pair;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Support methods for handling continuation headers for webservices.
 */
public class ContinuationUtil {

    /**
     * Set as HTTP header by record streaming endpoints to communicate the highest mTime that any records will contain.
     * <p>
     * Note that there is no preceding {@code X-} as this is discouraged by
     * <a href="https://www.rfc-editor.org/rfc/rfc6648">rfc6648</a>.
     */
    public static final String HEADER_PAGING_CONTINUATION_TOKEN = "Paging-Continuation-Token";

    /**
     * Set as HTTP header by record streaming endpoints to communicate if subsequent calls starting from
     * {@link #HEADER_PAGING_CONTINUATION_TOKEN} are likely to yield more records.
     */
    public static final String HEADER_PAGING_HAS_MORE = "Paging-Has-More";
    
    /**
     * Set as HTTP header by record streaming endpoints to communicate the number of records in the current stream.
     */
    public static final String HEADER_PAGING_RECORD_COUNT = "Paging-Record-Count";

    /**
     * Extract the {@code #HEADER_PAGING_CONTINUATION_TOKEN} from the given {@code headerInputStream} and return it.
     * <p>
     * Note: The header can be undefined in the {@code headerInputStream}.
     * In that case in will not be present in the result.
     * @param headerInputStream provides the headers to check.
     * @param tokenMapper maps the String header {@link ContinuationUtil#HEADER_PAGING_CONTINUATION_TOKEN}
     *                    to the concrete token type.
     * @return an optional continuation.
     */
    public static <C> Optional<C> getContinuationToken(
            HeaderInputStream headerInputStream, Function<String, C> tokenMapper) {
        return getContinuationToken(headerInputStream.getResponseHeaders(), tokenMapper);
    }

    /**
     * Extract the {@code #HEADER_PAGING_CONTINUATION_TOKEN} from the given {@code headerInputStream} and return it.
     * <p>
     * Note: The header can be undefined in the {@code headers}.
     * In that case in will not be present in the result.
     * @param headers map of headers from a HTTP response.
     * @param tokenMapper maps the String header {@link #HEADER_PAGING_CONTINUATION_TOKEN}
     *                    to the concrete token type.
     * @return an optional continuation.
     */
    public static <C> Optional<C> getContinuationToken(
            Map<String, List<String>> headers, Function<String, C> tokenMapper) {
        return headers.get(HEADER_PAGING_CONTINUATION_TOKEN) == null ?
                Optional.empty() :
                Optional.of(headers.get(HEADER_PAGING_CONTINUATION_TOKEN).get(0))
                        .map(tokenMapper);
    }

    /**
     * Extract the {@link #HEADER_PAGING_HAS_MORE} from the given {@code headerInputStream} and return it.
     * <p>
     * Note: The header can be undefined in the {@code headerInputStream}.
     * In that case in will not be present in the result.
     * @param headerInputStream provides the headers to check.
     * @return an optional hasMore, signalling whether subsequent calls are likely to provide more data.
     */
    public static Optional<Boolean> getHasMore(HeaderInputStream headerInputStream) {
        return getHasMore(headerInputStream.getResponseHeaders());
    }

    /**
     * Extract the {@link #HEADER_PAGING_RECORD_COUNT} from the given {@code headerInputStream} and return it.
     * <p>
     * Note: The header can be undefined in the {@code headerInputStream}.
     * In that case in will not be present in the result.
     * @param headerInputStream provides the headers to check.
     * @return an optional record count, signalling the number of serialised records the current stream represents.
     */
    public static Optional<Long> getRecordCount(HeaderInputStream headerInputStream) {
        return getRecordCount(headerInputStream.getResponseHeaders());
    }

    /**
     * Extract the {@link #HEADER_PAGING_HAS_MORE} from the given {@code headers} and return it.
     * <p>
     * Note: The header can be undefined in the {@code headers}.
     * In that case in will not be present in the result.
     * @param headers map of headers from a HTTP response.
     * @return an optional hasMore, signalling whether subsequent calls are likely to provide more data.
     */
    public static Optional<Boolean> getHasMore(Map<String, List<String>> headers) {
        return headers.get(HEADER_PAGING_HAS_MORE) == null ?
                Optional.empty() :
                Optional.of(Boolean.parseBoolean(headers.get(HEADER_PAGING_HAS_MORE).get(0)));
    }

    /**
     * Extract the {@link #HEADER_PAGING_RECORD_COUNT} from the given {@code headers} and return it.
     * <p>
     * Note: The header can be undefined in the {@code headers}.
     * In that case in will not be present in the result.
     * @param headers map of headers from a HTTP response.
     * @return an optional record count, signalling the number of serialised records the current stream represents.
     */
    public static Optional<Long> getRecordCount(Map<String, List<String>> headers) {
        return headers.get(HEADER_PAGING_RECORD_COUNT) == null ?
                Optional.empty() : Optional.of(Long.valueOf(headers.get(HEADER_PAGING_RECORD_COUNT).get(0)));
    }

    /**
     * Use {@link Pair#getLeft()} as {@code continuationToken} for {@link #HEADER_PAGING_CONTINUATION_TOKEN}.
     * Use {@link Pair#getRight()} as {@code hasMore} for {@link #HEADER_PAGING_HAS_MORE}.
     * <p>
     * Note: This does not set {@link #setHeaderRecordCount}.
     * @param httpServletResponse headers are assigned with {@link HttpServletResponse#setHeader(String, String)}.
     * @param continuationAndHasMore pair containing the continuation token and the has more boolean.
     * @param <C> the type of continuation token, typically {@code String} or {@code Long}.
     */
    public static <C> void setHeaders(HttpServletResponse httpServletResponse, Pair<C, Boolean> continuationAndHasMore) {
        setHeaderContinuation(httpServletResponse, continuationAndHasMore.getLeft());
        setHeaderHasMore(httpServletResponse, continuationAndHasMore.getRight());
    }

    /**
     * Set {@code continuationToken} as the value for {@link #HEADER_PAGING_CONTINUATION_TOKEN} if it exists.
     * @param httpServletResponse header is assigned with {@link HttpServletResponse#setHeader(String, String)}.
     * @param continuationToken the continuation token or {@code null} is none exists.
     * @param <C> the type of continuation token, typically {@code String} or {@code Long}.
     */
    public static <C> void setHeaderContinuation(HttpServletResponse httpServletResponse, C continuationToken) {
        if (continuationToken == null) {
            return;
        }
        httpServletResponse.setHeader(HEADER_PAGING_CONTINUATION_TOKEN, Objects.toString(continuationToken));
    }

    /**
     * Set {@code hasMore} as the value for {@link #HEADER_PAGING_HAS_MORE} if it exists.
     * @param httpServletResponse header is assigned with {@link HttpServletResponse#setHeader(String, String)}.
     * @param hasMore the has more token or {@code null} is none exists.
     */
    public static void setHeaderHasMore(HttpServletResponse httpServletResponse, Boolean hasMore) {
        if (hasMore == null) {
            return;
        }
        httpServletResponse.setHeader(HEADER_PAGING_HAS_MORE, Boolean.toString(hasMore));
    }

    /**
     * Set {@code recordCount} as the value for {@link #HEADER_PAGING_RECORD_COUNT} if it exists.
     * @param httpServletResponse header is assigned with {@link HttpServletResponse#setHeader(String, String)}.
     * @param recordCount the number of records or {@code null} if unknown.
     */
    public static void setHeaderRecordCount(HttpServletResponse httpServletResponse, Long recordCount) {
        if (recordCount == null) {
            return;
        }
        httpServletResponse.setHeader(HEADER_PAGING_RECORD_COUNT, Long.toString(recordCount));
    }

}
