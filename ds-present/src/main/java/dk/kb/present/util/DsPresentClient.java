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
package dk.kb.present.util;

import dk.kb.present.client.v1.DsPresentApi;
import dk.kb.present.client.v1.IiifPresentationApi;
import dk.kb.present.client.v1.ServiceApi;
import dk.kb.present.invoker.v1.ApiClient;
import dk.kb.present.invoker.v1.Configuration;
import dk.kb.present.model.v1.FormatDto;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.util.webservice.stream.ContinuationInputStream;
import dk.kb.util.webservice.stream.ContinuationStream;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Locale;

/**
 * Client for the service. Intended for use by other projects that calls this service.
 * See the {@code README.md} for details on usage.
 * <p/>
 * This class is not used internally.
 * <p/>
 * The client is Thread safe and handles parallel requests independently.
 * It is recommended to persist the client and to re-use it between calls.
 * <p/>
 * In order to call the IIIF endpoints, use {@link DsPresentClient#iiif}.
 */
@SuppressWarnings("unchecked")
public class DsPresentClient extends DsPresentApi {
    private static final Logger log = LoggerFactory.getLogger(DsPresentClient.class);
    private final String serviceURI;

    public static final String PRESENT_SERVER_URL_KEY = ".present.url";
    public static final String PRESENT_SERVER_HEADERS_KEY = ".present.headers";

    /**
     * Sub-client for the IIIF endpoints.
     */
    public final IiifPresentationApi iiif;
    /**
     * Sub-client for the service endpoints.
     */
    public final ServiceApi service;

    /**
     * Headers used for all calls to ds-license.
     */
    public Map<String, String> headers;

    /**
     * Create a client for the remote ds-present service.
     * <p>
     * This constructor expects the structure
     * <pre>
     * present:
     *   url: 'http://localhost:9073/ds-present/v1'
     *   headers:
     *     # Placeholder for authentication until OAuth2 integration is in place
     *     # Set this to 'internal_service` on local and devel to bypass license check in ds-present
     *     - Simulated-OAuth2-Group: anonymous
     * </pre>
     * @param config following the outlined structure.
     * @param headers zero or more maps of headers, which will be set for all calls by this client.
     *                Note that this compounds with headers specified in {@code config}, with the {@code clientHeaders}
     *                overriding the headers in the {@code config} in case of collisions.
     */
    @SuppressWarnings("JavadocLinkAsPlainText")
    public DsPresentClient(YAML config, Map<String, String>... headers) {
        this(config.getString(PRESENT_SERVER_URL_KEY), getAllHeaders(config, headers));
    }

    /**
     * Create a client for the remote ds-present service, without special headers.
     * <p>
     * Note: Clients created using this constructor has no special header supports. The alternative constructor
     * {@link DsPresentClient(YAML, Map)} handles both URL and fixed header setup.
     * @param serviceURI the URI for the service, e.g. {@code https://example.com/ds-present/v1}.
     * @see DsPresentClient(YAML)
     */
    public DsPresentClient(String serviceURI) {
        this(serviceURI, (Map<String, String>) null);
    }

    /**
     * Create a client for the remote ds-present service.
     * <p>
     * When working with YAML configs, it is suggested to use the structure
     * <pre>
     * present:
     *   url: 'http://localhost:9073/ds-present/v1'
     *   headers:
     *     # Placeholder for authentication until OAuth2 integration is in place
     *     # Set this to 'internal_service` on local and devel to bypass license check in ds-present
     *     - Simulated-OAuth2-Group: anonymous
     * </pre>
     * Then use the path {@link #PRESENT_SERVER_URL_KEY} to extract the URL.
     * <p>
     * Note: The alternative constructor {@link DsPresentClient(YAML, Map)} handles both URL and fixed header setup.
     * @param serviceURI the URI for the service, e.g. {@code https://example.com/ds-present/v1}.
     * @param headers zero or more maps of headers, which will be set for all calls by this client.
     * @see DsPresentClient(YAML, Map)
     */
    @SuppressWarnings("JavadocLinkAsPlainText")
    public DsPresentClient(String serviceURI, Map<String, String>... headers) {
        super(createApiClient(serviceURI, headers));
        // Unfortunately we need to recreate the ApiClient as it is not retrievable from the super class
        ApiClient apiClient = createApiClient(serviceURI, headers);
        this.serviceURI = serviceURI;
        this.headers = collapse(headers);
        iiif = new IiifPresentationApi(apiClient);
        service = new ServiceApi(apiClient);
        log.info("Created OpenAPI client for '{}' with headers {}", serviceURI, headers);
    }

    /**
     * Call the remote ds-present {@link #getRecords} and return the response unchanged as a wrapped
     * bytestream. The concrete representation of the content is controlled by {@code format}.
     * <p>
     * Important: Ensure that the returned stream is closed to avoid resource leaks.
     * @param origin     the origin for the records.
     * @param mTime      exclusive start time for records to deliver:
     *                   Epoch time in microseconds (milliseconds times 1000).
     * @param maxRecords the maximum number of records to deliver. -1 means no limit.
     * @param format     the format for the records. Valid formats are {@code }
     * @return a raw bytestream with the response from the remote ds-present.
     * @throws IOException if the connection to the remote ds-present failed.
     */
    public ContinuationInputStream<Long> getRecordsJSON(String origin, Long mTime, Long maxRecords, FormatDto format)
            throws IOException {
        URI uri = UriBuilder.fromUri(serviceURI)
                .path("records")
                .queryParam("origin", origin)
                .queryParam("mTime", mTime == null ? 0L : mTime)
                .queryParam("maxRecords", maxRecords == null ? 10 : maxRecords)
                .queryParam("format", format.getValue().toUpperCase(Locale.ROOT))
                .build();
        log.debug("Opening streaming connection to '{}'", uri);
        return ContinuationInputStream.from(uri, Long::valueOf, headers);
    }

    /**
     * Call the remote ds-present {@link #getRecordsRaw} and return the JSON serialised
     * {@link dk.kb.storage.model.v1.DsRecordDto}s unchanged as a wrapped bytestream.
     * The concrete JSON representation is controlled by {@code asJsonLines}.
     * <p>
     * Important: Ensure that the returned stream is closed to avoid resource leaks.
     * @param origin      the origin for the records.
     * @param mTime       exclusive start time for records to deliver:
     *                    Epoch time in microseconds (milliseconds times 1000).
     * @param maxRecords  the maximum number of records to deliver. -1 means no limit.
     * @param asJsonLines if true, the {@link dk.kb.storage.model.v1.DsRecordDto} JSON entries are represented in
     *                    JSON-Lines format (one record/line, inly linebreak as divider). If false, the entries
     *                    are represented in a single JSON array.
     * @return a raw bytestream with the JSON or JSON_Lines response from the remote ds-present.
     * @throws IOException if the connection to the remote ds-present failed.
     */
    public ContinuationInputStream<Long> getRecordsRawJSON(
            String origin, Long mTime, Long maxRecords, Boolean asJsonLines) throws IOException {
        URI uri = UriBuilder.fromUri(serviceURI)
                .path("recordsraw")
                .queryParam("origin", origin)
                .queryParam("mTime", mTime == null ? 0L : mTime)
                .queryParam("maxRecords", maxRecords == null ? 10 : maxRecords)
                .queryParam("asJsonLines", asJsonLines != null && asJsonLines)
                .build();
        log.debug("Opening streaming connection to '{}'", uri);
        return ContinuationInputStream.from(uri, Long::valueOf, headers);
    }

    /**
     * Call the remote ds-storage {@link #getRecordsRaw} and return the response in the form of a Stream of records.
     * <p>
     * The stream is unbounded by memory and gives access to the highest modification time (microseconds since
     * Epoch 1970) for any record that will be delivered by the stream {@link ContinuationStream#getContinuationToken}.
     * <p>
     * Important: Ensure that the returned stream is closed to avoid resource leaks.
     * @param origin      the origin for the records.
     * @param mTime       exclusive start time for records to deliver:
     *                    Epoch time in microseconds (milliseconds times 1000).
     * @param maxRecords  the maximum number of records to deliver. -1 means no limit.
     * @return a stream of records from the remote ds-storage.
     * @throws IOException if the connection to the remote ds-storage failed.
     */
    public ContinuationStream<DsRecordDto, Long> getRecordsRawStream(
            String origin, Long mTime, Long maxRecords) throws IOException {
        return getRecordsRawJSON(origin, mTime, maxRecords, false)
                .stream(DsRecordDto.class);
    }

    /**
     * @return the headers used by this client.
     */
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * Deconstruct the given URI and use the components to create an ApiClient.
     * @param serviceURIString an URI to a ds-service.
     * @param headers zero or more maps of headers, which will be set for all calls by this client.
     * @return an ApiClient constructed from the serviceURIString.
     */
    static ApiClient createApiClient(String serviceURIString, Map<String, String>... headers) {
        log.debug("Creating OpenAPI client with URI '{}', headers {}", serviceURIString, headers);

        URI serviceURI = URI.create(serviceURIString);
        // No mechanism for just providing the full URI. We have to deconstruct it
        return Configuration.getDefaultApiClient()
                .setScheme(serviceURI.getScheme())
                .setHost(serviceURI.getHost())
                .setPort(serviceURI.getPort())
                .setBasePath(serviceURI.getRawPath())
                .setRequestInterceptor(builder -> collapse(headers).forEach(builder::header));
    }

    /**
     * Extract fixed headers for remote calls from {@link #PRESENT_SERVER_HEADERS_KEY} in {@code config} and
     * combine them with the given {@code extraHeaders}.
     * @param config  a configuration possibly containing fixed headers.
     * @param extraHeaders will override any headers given in the {@code config}.
     * @return a map of fixed headers. Note that duplicate entries are eliminated.
     */
    static Map<String, String> getAllHeaders(YAML config, Map<String, String>... extraHeaders) {
        if (!config.containsKey(PRESENT_SERVER_HEADERS_KEY)) {
            return Collections.emptyMap();
        }
        Map<String, String> combined = new HashMap<>();
        config.getYAMLList(PRESENT_SERVER_HEADERS_KEY).stream()
                .flatMap(entry -> entry.entrySet().stream())
                .forEach(entry -> combined.put(entry.getKey(), Objects.toString(entry.getValue())));
        combined.putAll(collapse(extraHeaders));
        return combined;
    }

    /**
     * Take zero or more maps and collapses them into a single map, letting later entries override earlier ones.
     * @param headers zero or more maps.
     * @return a single map with all key/values.
     */
    static Map<String, String> collapse(Map<String, String>... headers) {
        if (headers == null || headers.length == 0) {
            return Collections.emptyMap();
        }

        return Arrays.stream(headers)
                .filter(Objects::nonNull)
                .flatMap(hs -> hs.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
