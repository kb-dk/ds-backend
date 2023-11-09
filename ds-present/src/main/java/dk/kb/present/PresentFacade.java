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
package dk.kb.present;

import dk.kb.present.api.v1.impl.DsPresentApiServiceImpl;
import dk.kb.present.config.ServiceConfig;
import dk.kb.present.model.v1.CollectionDto;
import dk.kb.present.model.v1.ViewDto;
import dk.kb.present.util.DataCleanup;
import dk.kb.util.webservice.stream.ExportWriterFactory;
import dk.kb.storage.model.v1.DsRecordDto;

import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import dk.kb.util.webservice.stream.ExportWriter;
import dk.kb.util.webservice.stream.JSONStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.StreamingOutput;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 *
 */
public class PresentFacade {
    private static final Logger log = LoggerFactory.getLogger(PresentFacade.class);

    private static CollectionHandler collectionHandler;
    // TODO: The whole retrieval of raw records through getRecords does nok look very clean
    static String recordView = "raw"; // View used when retrieving the full records

    /**
     * Optional warmUp (initialization) for fail early.
     */
    public static void warmUp() {
        getCollectionHandler();
    }

    private static CollectionHandler getCollectionHandler() {
        if (collectionHandler == null) {
            collectionHandler = new CollectionHandler(ServiceConfig.getConfig());
        }
        return collectionHandler;
    }

    // TODO: What about setting the MIME type?

    /**
     * Derived a collection from the recordID and requests the record from that, with the specified format.
     * @param recordID an ID for a record in any known collection.
     * @param format the wanted format (collection specific).
     * @return the record in the given format.
     * @throws NotFoundServiceException if the record or the format was unknown.
     */
    public static String getRecord(String recordID, String format) {
        return getCollectionHandler().getRecord(recordID, format);
    }

    /**
     * @param id ID for a collection.
     * @return the collection with the given ID.
     * @throws NotFoundServiceException if the collection was not known.
     */
    public static CollectionDto getCollection(String id) {
        DSCollection collection = getCollectionHandler().getCollection(id);
        if (collection == null) {
            throw new NotFoundServiceException("A collection with the id '" + id + "' could not be located. " +
                                               "Supported collections are " + collectionHandler.getCollectionIDs());
        }
        return toDto(collection);
    }

    /**
     * @return all known collections.
     */
    public static List<CollectionDto> getCollections() {
        return getCollectionHandler().getCollections().stream()
                .map(PresentFacade::toDto)
                .collect(Collectors.toList());
    }

    // TODO: storage is not returned as that is internal information. With elevated privileges this might be added?
    private static CollectionDto toDto(DSCollection collection) {
        return new CollectionDto()
                .id(collection.getId())
                .prefix(collection.getPrefix())
                .description(collection.getDescription())
                .views(collection.getViews().values().stream()
                               .map(PresentFacade::toDto)
                               .collect(Collectors.toList()));
    }

    private static ViewDto toDto(View view) {
        return new ViewDto()
                .id(view.getId())
                .mime(view.getMime().toString());
    }

    /**
     * Deliver streaming output for serialized records from a given collection.
     * @param httpServletResponse used for setting MIME type.
     * @param collectionID the collection to retrieve records for.
     * @param mTime        only records after this time (Epoch milliseconds) will be delivered.
     * @param maxRecords   the maximum number of records to deliver.
     * @param format       the serialized format of the records. Valid values are {@code JSON-LD},
     * {@code JSON-LD-LINES}, {@code MODS}, {@code STORAGERECORD} , {@code STORAGERECORD-LINES} and {@code SOLRJSON}.
     * @param accessChecker only IDs evaluating to {@link DsPresentApiServiceImpl.ACCESS#ok} are delivered.
     * @return a stream of serialized records.
     */
    public static StreamingOutput getRecords(
            HttpServletResponse httpServletResponse, String collectionID, Long mTime, Long maxRecords, String format,
            Function<List<String>, List<String>> accessChecker) {
        DSCollection collection = collectionHandler.getCollection(collectionID);
        if (collection == null) {
            throw new InvalidArgumentServiceException(String.format(
                    Locale.ROOT, "The collection '%s' was unknown. Known collections are %s",
                    collectionID,
                    collectionHandler.getCollections().stream().map(DSCollection::getId).collect(Collectors.toList())));
        }

        // Batch-oriented filter that only passed records that are allowed
        Function<List<DsRecordDto>, Stream<DsRecordDto>> accessFilter = records -> {
            List<String> allIDs = records.stream().map(DsRecordDto::getId).collect(Collectors.toList());
            Set<String> allowedIDs = new HashSet<>(accessChecker.apply(allIDs));
            return records.size() == allowedIDs.size() ?
                    records.stream() : // No need for checking as all IDs passed
                    records.stream().filter(record -> allowedIDs.contains(record.getId()));
        };

        // enum:  ['JSON-LD', 'JSON-LD-Lines', 'MODS', 'SolrJSON', "StorageRecord"]
        switch (format.toUpperCase(Locale.ROOT)) {
            case "JSON-LD": return getRecordsData(
                    collection, mTime, maxRecords,
                    httpServletResponse, "JSON-LD", ExportWriterFactory.FORMAT.json,
                    accessFilter);
            case "JSON-LD-LINES": return getRecordsData(
                    collection, mTime, maxRecords,
                    httpServletResponse, "JSON-LD", ExportWriterFactory.FORMAT.jsonl, accessFilter);
            case "MODS": return getRecordsData(
                    collection, mTime, maxRecords,
                    httpServletResponse, "MODS", ExportWriterFactory.FORMAT.xml, accessFilter);
            case "STORAGERECORD": return getRecordsFull(
                    collection, mTime, maxRecords,
                    httpServletResponse, ExportWriterFactory.FORMAT.json, accessFilter);
            case "STORAGERECORD-LINES": return getRecordsFull(
                    collection, mTime, maxRecords,
                    httpServletResponse, ExportWriterFactory.FORMAT.jsonl, accessFilter);
            case "SOLRJSON": return getRecordsSolr(collection, mTime, maxRecords, httpServletResponse, accessFilter);
            default: throw new InvalidArgumentServiceException("The format '" + format + "' is not supported");
        }
    }

    // Only deliver the data-part of the Records
    private static StreamingOutput getRecordsData(
            DSCollection collection, Long mTime, Long maxRecords,
            HttpServletResponse httpServletResponse, String recordFormat, ExportWriterFactory.FORMAT deliveryFormat,
            Function<List<DsRecordDto>, Stream<DsRecordDto>> accessFilter) {
        setFilename(httpServletResponse, mTime, maxRecords, deliveryFormat);
        return output -> {
            try (ExportWriter writer = ExportWriterFactory.wrap(
                    output, httpServletResponse, deliveryFormat, false, "records")) {
                collection.getDSRecords(mTime, maxRecords, recordFormat, accessFilter)
                        .map(DsRecordDto::getData)
                        .map(DataCleanup::removeXMLDeclaration)
                        .forEach(writer::write);
            }
        };
    }

    // Retrieve full records to support deletions
    private static StreamingOutput getRecordsFull(
            DSCollection collection, Long mTime, Long maxRecords,
            HttpServletResponse httpServletResponse, ExportWriterFactory.FORMAT deliveryFormat,
            Function<List<DsRecordDto>, Stream<DsRecordDto>> accessFilter) {
        setFilename(httpServletResponse, mTime, maxRecords, deliveryFormat);
        return output -> {
            try (ExportWriter writer = ExportWriterFactory.wrap(
                    output, httpServletResponse, deliveryFormat, false, "records")) {
                collection.getDSRecords(mTime, maxRecords, recordView, accessFilter) // Does not contain deleted records
                        .peek(record -> record.setData(DataCleanup.removeXMLDeclaration(record.getData())))
                        .forEach(writer::write);
            }
        };
    }

    // Direct ds-storage record JSON
    private static StreamingOutput getRecordsSolr(DSCollection collection, Long mTime, Long maxRecords,
                                                  HttpServletResponse httpServletResponse,
                                                  Function<List<DsRecordDto>, Stream<DsRecordDto>> accessFilter) {
        setFilename(httpServletResponse, mTime, maxRecords, ExportWriterFactory.FORMAT.json);
        return output -> {
            ExportWriter writer = ExportWriterFactory.wrap(
                    output, httpServletResponse, ExportWriterFactory.FORMAT.json, false, "records");
            // Hacking the output to confirm to Solr's non-valid JSON: https://solr.apache.org/guide/8_10/uploading-data-with-index-handlers.html#sending-json-update-commands
            ((JSONStreamWriter) writer).setPreOutput("{\n");
            ((JSONStreamWriter) writer).setPostOutput("\n}\n");

            try {
                collection.getDSRecords(mTime, maxRecords, "SolrJSON", accessFilter)
                        .map(PresentFacade::wrapSolrJSON)
                        .forEach(writer::write);
            } catch (Exception e) {
                if (e instanceof ServiceException) {
                    throw e;
                }
                String message = String.format(
                        Locale.ROOT,
                        "Exception delivering Solr records with collection='%s', mTime=%d, maxRecords=%d",
                        collection.getId(), mTime, maxRecords);
                // We need to log here as the writer does not have information on collection, mTime and maxRecords
                log.warn(message, e);
                throw new InternalServiceException(message);
            }
            // We do not use
            // try (ExportWriter writer = ExportWriterFactory.wrap(...
            // as that absorbs any thrown Exceptions
            writer.close();
        };
    }

    /**
     * Uses information from the record object to wrap its data component in either {@code add} or {@code delete}.
     * See https://solr.apache.org/guide/8_8/uploading-data-with-index-handlers.html#json-formatted-index-updates
     * @param record a record where the data component contains a SolrJSONDocument.
     * @return the record's data component wrapped as either {@code add} or {@code delete}.
     */
    //
    private static String wrapSolrJSON(DsRecordDto record) {
        if (Boolean.TRUE.equals(record.getDeleted())) {
            return "\"delete\": { \"id\": \"" + record.getId() + "\" }";
        }
        // When we had nested solr documentds, we had to split on documents. This has been removed. See outcommented method  splitSolrJSON if it becomes relevant
        StringBuilder sb = new StringBuilder();
        sb.append("\"add\": { \"doc\" : ").append(record.getData()).append(" }");
       
        return sb.toString();
    
    }

    /*
     * If the source data contains multiple DOMS, there will also be multiple SolrJSONDocuments.
     * This method splits those from one JSON structure to one structure/document.
     * @param solrJSONs one or more JSON documents in a JSON array.
     * @return the JSON documents as a list.
     */
    // Really hacking here to handle the case of the source containing multiple MODS-sections
    // TODO: Hopefully determine that 1 record = 1 mods always
    
    /* No used 
    private static List<String> splitSolrJSON(String solrJSONs) {
        ObjectMapper mapper = new ObjectMapper();
        List<?> jsonArray = JSON.fromJson(solrJSONs, List.class);
        List<String> strArray = new ArrayList<>(jsonArray.size());
        for (int i = 0 ; i < jsonArray.size() ; i++) {
            try {
                strArray.add(mapper.writeValueAsString(jsonArray.get(i)));
            } catch (JsonProcessingException e) {
                log.error("Unable to convert map to JSONObject", e); // Should not happen as we just deserialized above
                throw new InternalServiceException("Unable to convert map to JSONObject");
            }
        }
        return strArray;
    }
*/
    private static void setFilename(
            HttpServletResponse httpServletResponse,
            Long mTime, Long maxRecords, ExportWriterFactory.FORMAT deliveryFormat) {
        if (httpServletResponse == null) {
            log.debug("setFilename: No HttpServletResponse, so no filename stated");
            return;
        }
        String filename = "records_" + mTime + ".json";
        if (maxRecords <= 2) { // The Swagger GUI is extremely sluggish for inline rendering
            // A few records is ok to show inline in the Swagger GUI:
            // Show inline in Swagger UI, inline when opened directly in browser
            httpServletResponse.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
        } else {
            // When there are a lot of records, they should not be displayed inline in the OpenAPI GUI:
            // Show download link in Swagger UI, inline when opened directly in browser
            // https://github.com/swagger-api/swagger-ui/issues/3832
            httpServletResponse.setHeader("Content-Disposition", "inline; swaggerDownload=\"attachment\"; filename=\"" + filename + "\"");
        }
    }

}
