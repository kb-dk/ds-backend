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

import dk.kb.present.client.v1.ServiceApi;
import dk.kb.present.client.v1.DsPresentApi;
import dk.kb.present.client.v1.IiifPresentationApi;
import dk.kb.present.invoker.v1.ApiClient;
import dk.kb.present.invoker.v1.Configuration;
import dk.kb.present.model.v1.FormatDto;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.util.webservice.stream.ContinuationInputStream;
import dk.kb.util.webservice.stream.ContinuationStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

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
public class DsPresentClient extends DsPresentApi {
    private static final Logger log = LoggerFactory.getLogger(DsPresentClient.class);
    private final String serviceURI;

    public static final String PRESENT_SERVER_URL_KEY = ".present.url";

    /**
     * Sub-client for the IIIF endpoints.
     */
    public final IiifPresentationApi iiif;
    /**
     * Sub-client for the service endpoints.
     */
    public final ServiceApi service;

    /**
     * Creates a client for the remote ds-present service.
     * <p>
     * When working with YAML configs, it is suggested to define the ds-present URI as the structure
     * <pre>
     * storage:
     *   url: 'http://localhost:9073/ds-storage/v1'
     * </pre>
     * Then use the path {@link #PRESENT_SERVER_URL_KEY} to extract the URL.
     * @param serviceURI the URI for the service, e.g. {@code https://example.com/ds-present/v1}.
     */
    @SuppressWarnings("JavadocLinkAsPlainText")
    public DsPresentClient(String serviceURI) {
        super(createClient(serviceURI));
        this.serviceURI = serviceURI;
        iiif = new IiifPresentationApi(createClient(serviceURI));
        service = new ServiceApi(createClient(serviceURI));
        log.info("Created OpenAPI client for '" + serviceURI + "'");
    }

    /**
     * Deconstruct the given URI and use the components to create an ApiClient.
     * @param serviceURIString an URI to a service.
     * @return an ApiClient constructed from the serviceURIString.
     */
    private static ApiClient createClient(String serviceURIString) {
        log.debug("Creating OpenAPI client with URI '{}'", serviceURIString);

        URI serviceURI = URI.create(serviceURIString);
        // No mechanism for just providing the full URI. We have to deconstruct it
        return Configuration.getDefaultApiClient().
                setScheme(serviceURI.getScheme()).
                setHost(serviceURI.getHost()).
                setPort(serviceURI.getPort()).
                setBasePath(serviceURI.getRawPath());
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
                .queryParam("format", format)
                .build();
        log.debug("Opening streaming connection to '{}'", uri);
        return ContinuationInputStream.from(uri, Long::valueOf);
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
        return ContinuationInputStream.from(uri, Long::valueOf);
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

}
