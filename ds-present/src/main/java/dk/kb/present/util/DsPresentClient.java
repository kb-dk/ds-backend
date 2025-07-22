package dk.kb.present.util;


import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.present.model.v1.FormatDto;
import dk.kb.present.model.v1.OriginDto;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.util.webservice.Service2ServiceRequest;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import dk.kb.util.webservice.stream.ContinuationInputStream;
import dk.kb.util.webservice.stream.ContinuationStream;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.InterruptedByTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.hc.core5.net.URIBuilder;


/**
 * Client for the service. Intended for use by other projects that calls this service.
 * See the {@code README.md} for details on usage.
 * </p>
 * This class is not used internally.
 * </p>
 * The client is Thread safe and handles parallel requests independently.
 * It is recommended to persist the client and to re-use it between calls.
 */
public class DsPresentClient {
    private static final Logger log = LoggerFactory.getLogger(DsPresentClient.class);
    private final static String CLIENT_URL_EXCEPTION="The client url was not constructed correct";
    private final String serviceURI;
    
    /**
     * <pr>
     * Creates a client for the remote ds-present service.
     * <p>
     * @param serviceURI the URI for the service, e.g. {@code https://example.com/ds-present/v1}.
     */
    @SuppressWarnings("JavadocLinkAsPlainText")
    public DsPresentClient(String serviceURI) {
        this.serviceURI = serviceURI;
        log.info("Created OpenAPI client for '{}'", serviceURI);
    }

    
    /**
     * Retrieve a formal description of a single origin.
     * 
     * @param id The ID of the origin (required)
     * @return OriginDto
     * @throws ServiceException if fails to make API call
     */
    public OriginDto getOrigin(String id) throws ServiceException {        
        try {
            URI uri = new URIBuilder(serviceURI)
                     .appendPathSegments("origin",id)                                                                                   
                     .build();            
            return Service2ServiceRequest.httpCallWithOAuthToken(uri,"GET", new OriginDto(),null);              
        }
        catch (URISyntaxException e) {
            log.error("Invalid url:"+e.getMessage());
            throw new InternalServiceException(CLIENT_URL_EXCEPTION);               
         }          
    }
    
    
    /**
    * Retrieve a formal description of all available origins.
    * 
    * @return List&lt;OriginDto&gt;
    * @throws ServiceException if fails to make API call
    */
    public List<OriginDto> getOrigins() throws ServiceException {
        try {
            URI uri = new URIBuilder(serviceURI)
                    .appendPathSegments("origins")                                                                                   
                    .build();            
            return Service2ServiceRequest.httpCallWithOAuthToken(uri,"GET", new ArrayList<OriginDto>(),null);              
        }
        catch (URISyntaxException e) {
            log.error("Invalid url:"+e.getMessage());
            throw new InternalServiceException(CLIENT_URL_EXCEPTION);               
         }          
        
    }
    
    /**
     * Retrieve metadata for the record with the given ID and in the given format.
     * 
     * @param id The ID of the record (required)
     * @param format The delivery format for the record: * JSON-LD: [Linked Data in JSON](https://json-ld.org/) (default) * MODS: [Metadata Object Description Schema](http://www.loc.gov/standards/mods/) * SolrJSON: [Solr JSON Formatted Index Updates](https://solr.apache.org/guide/8_8/uploading-data-with-index-handlers.html#json-formatted-index-updates) * raw: Metadata unchanged from the source.  (optional, default to JSON-LD)
     * @return String
     * @throws ServiceException if fails to make API call
     */
    public String getRecord(String id, FormatDto format) throws ServiceException {
        try {
        URI uri = new URIBuilder(serviceURI)
                .appendPathSegments("record",id)                                                                                   
                .addParameter("format",""+format.toString())
                .build();           
        return Service2ServiceRequest.httpCallWithOAuthToken(uri,"GET", new String(),null);       
        }
        catch (URISyntaxException e) {
            log.error("Invalid url:"+e.getMessage());
            throw new InternalServiceException(CLIENT_URL_EXCEPTION);               
         }          
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
        URI uri;
        try {
            uri = new URIBuilder(serviceURI)
                    .appendPath("records")                                        
                    .addParameter("origin", origin)
                    .addParameter("mTime", Long.toString(mTime == null ? 0L : mTime))
                    .addParameter("maxRecords", Long.toString(maxRecords == null ? 10 : maxRecords))
                    .addParameter("format", format.getValue().toUpperCase(Locale.ROOT))
                    .build();
        } catch (URISyntaxException e) {
            String message = String.format(Locale.ROOT,
                    "getRecordsJSON(origin='%s', mTime=%d, maxRecords=%d, format='%s'): " +
                            "Unable to construct URI",
                    origin, mTime, maxRecords, format);
            log.warn(message, e);
            throw new InternalServiceException(message);
        }

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
        URI uri;
        try {
            uri = new URIBuilder(serviceURI + "recordsraw")
                    // .setPath overrides all paths given in the serviceURI. Should be changed using pathSegments in the future
                    //.setPath("recordsraw")
                    .addParameter("origin", origin)
                    .addParameter("mTime", Long.toString(mTime == null ? 0L : mTime))
                    .addParameter("maxRecords", Long.toString(maxRecords == null ? 10 : maxRecords))
                    .addParameter("asJsonLines", Boolean.toString(asJsonLines != null && asJsonLines))
                    .build();
        } catch (URISyntaxException e) {
            String message = String.format(Locale.ROOT,
                    "getRecordsRawJSON(origin='%s', mTime=%d, maxRecords=%d, asJsonLines=%b): " +
                            "Unable to construct URI",
                    origin, mTime, maxRecords, asJsonLines != null && asJsonLines);
            log.warn(message, e);
            throw new InternalServiceException(message);
        }

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

    /**
     * Converts a raw solr schema to a human-readable version.
     * @param rawSchema the schema to convert.
     * @param format the format which it gets converted to.
     * @return the transformed solr schema in the specified format.
     */
    public String transformSolrSchema(String rawSchema, String format) throws IOException {
        URI uri;
        try {
            uri = new URIBuilder(serviceURI + "/transformsolrschema")
                    .addParameter("format",format)
                    .build();
            Map<String, String> requestHeaders = Map.of("content-type", "application/xml");
            String token = Service2ServiceRequest.getOAuth2Token();
            if (token != null) {
                requestHeaders.put("Authorization", "Bearer " + token);
            }
            HttpURLConnection con = (HttpURLConnection)uri.toURL().openConnection();
            con.setRequestMethod("POST");
            requestHeaders.forEach(con::setRequestProperty);
            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = rawSchema.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            int status = con.getResponseCode();
            if (status >= 200 && status <= 299) {
                return IOUtils.toString(con.getInputStream(), StandardCharsets.UTF_8);
            } else {
                throw new InternalServiceException("transformation of schema failed. Got http status "+status);
            }

        } catch (URISyntaxException e) {
            throw new InternalServiceException(e);
        }
    }
}
