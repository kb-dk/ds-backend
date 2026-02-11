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

import dk.kb.present.config.ServiceConfig;
import dk.kb.present.model.v1.FormatDto;
import dk.kb.present.storage.Storage;
import dk.kb.present.transform.RuntimeTransformerException;
import dk.kb.util.webservice.stream.ErrorList;
import dk.kb.util.webservice.stream.ErrorRecord;
import dk.kb.storage.model.v1.DsRecordDto;

import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.storage.model.v1.TranscriptionDto;
import dk.kb.util.Timing;
import dk.kb.util.other.ExtractionUtils;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.webservice.exception.ServiceException;

import dk.kb.util.webservice.stream.ContinuationStream;
import dk.kb.util.yaml.NotFoundException;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An origin encapsulates access to a logical collection ("samling"). It uses the same origins as ds-storage and
 * is typically backed by a ds-storage instance.
 * <p>
 * Access is read-only and always with an explicit export format. The format can be {@code raw} for direct proxying to
 * the connected ds-storage, but common use case is to request MODS, JSON-LD (schema.org) or SolrJSON representations.
 */
public class DSOrigin {
    // Origin-specific properties
    private static final Logger log = LoggerFactory.getLogger(DSOrigin.class);
    private static final Logger transformationErrorsLog = LoggerFactory.getLogger("dk.kb.transformation.errors");
    private static final String PREFIX_KEY = "prefix"; // IDs for this origin starts with <prefix>_ (note the underscore)
    private static final String DESCRIPTION_KEY = "description";
    private static final String STORAGE_KEY = "storage";
    private static final String ORIGIN_KEY = "origin";
    private static final String VIEWS_KEY = "views";
    private static final String RECORDREQUESTTYPE_KEY = "recordRequestType";

    // General properties
    public static final String STOP_ON_ERROR_KEY = "records.errorHandling.stop";

    private static final int LICENSE_BATCH_SIZE = 500;

    /**
     * The ID of the origin, primarily used for debugging and configuration.
     */
    private final String id;

    /**
     * recordIDs always starts with the origin followed by underscore, e.g. {@code images-dsfl_internalid1234}.
     * The prefix must be present for all recordIDs used for lookup.
     * This might be the same as the {@link #id} but it is not a requirement.
     */
    private final String prefix;

    /**
     * Human readable description of the origin.
     */
    private final String description;

    /**
     * Encapsulation of the backing storage for the origin. This will typically be a ds-storage service.
     */
    private final Storage storage;

    /**
     * Optional origin, with fallback to the default origin for {@link Storage}.
     */
    private final String origin;

    /**
     * Map from format -> view. A view is at the core an array of transformations and responsible for transforming
     * metadata to the requested format.
     */
    private final Map<String, View> views; // keys are lowercase

    /**
     * Type of metadata records to obtain from the service. Metadata can be structured in different files and often
     * the needed metadata is delivered in a specific type. This variable defines which type of metadata records that
     * are to be delivered.
     * These types are currently:<br/>
     * {@link RecordTypeDto#COLLECTION}
     * {@link RecordTypeDto#DELIVERABLEUNIT}
     * {@link RecordTypeDto#MANIFESTATION}
     *
     */
    private final RecordTypeDto recordRequestType;

    /**
     * If true, single record errors during records-export stops the whole flow. If false, a warning is logged.
     */
    private final boolean stopOnError;

    /**
     * Create an origin based on the given conf. The storageHandler is expected to be initialized and should contain
     * the storage specified for the origin.
     * @param conf configuration for the origin, should contain a single key:value with the key being the
     *             origin ID and the value being the configuration for the origin.
     * @param storageHandler previously initialized pool of storages.
     */
    public DSOrigin(YAML conf, StorageHandler storageHandler) {
        id = conf.keySet().stream().findFirst().orElseThrow();
        try {
            // When YAML keys contain YAML syntax they need to be encapsulated in quotation marks.
            // This should probably be handled in the YAML util class.
            conf = conf.getSubMap("\"" + id + "\""); // There must be some properties for a storage
            origin = conf.getString(ORIGIN_KEY);
            prefix = conf.getString(PREFIX_KEY);
            description = conf.getString(DESCRIPTION_KEY, null);
            recordRequestType = RecordTypeDto.valueOf(conf.getString(RECORDREQUESTTYPE_KEY));
            storage = storageHandler.getStorage(conf.getString(STORAGE_KEY, null)); // null means default storage

            views = conf.getYAMLList(VIEWS_KEY)
                    .stream()
                    .map(yaml -> new View(yaml, origin))
                    .collect(Collectors.toMap(view -> view.getId().toLowerCase(Locale.ROOT), view -> view));

            // Note: stopOnError is set at the outer level, not specifically for each origin
            stopOnError = ServiceConfig.getConfig().getBoolean(STOP_ON_ERROR_KEY, true);
        } catch (NotFoundException e) {
            throw new IllegalArgumentException(
                    "Mandatory property '" + e.getPath() + "' not present for Origin '" + id + "'");
        }
        log.info("Created " + this);
    }

    /**
     * Retrieve the record with the given id and transform it to the given format before delivery.
     * @param recordID an ID for a record.
     * @param format the format of the record. See {@link #getViews()} for available formats.
     * @return the record with the given id in the given format.
     * @throws ServiceException if the record could not be retrieved or transformed.
     */
    public String getRecord(String recordID, FormatDto format) throws ServiceException {
        Timing timing = Stats.getViewTimer(id, format.getValue().toLowerCase(Locale.ROOT));

        // Timing is both overall and with sub-timings for retrieval and transformation
        return timing.measure(() -> {
            DsRecordDto record = timing.getChild("retrieve").measure(() ->
                    storage.getDSRecord(recordID));
            return timing.getChild("transform").measure(() ->
                    getView(format).apply(record));
        });
    }

    
    /**
     * Retrieve a record with the given ID in ds-storage record format.
     * Storages that are not {@link dk.kb.present.storage.DSStorage} will deliver best-effort {@link DsRecordDto}s,
     * but full representation is not guaranteed.
     * @param recordID an ID for a record.
     * @return the record in ds-storage record format.
     * @throws ServiceException if the record could not be retrieved.
     */
    public TranscriptionDto getTranscription(String fileId) throws ServiceException {
        return storage.getTranscription(fileId);
    }

    
    /**
     * Retrieve a record with the given ID in ds-storage record format.
     * Storages that are not {@link dk.kb.present.storage.DSStorage} will deliver best-effort {@link DsRecordDto}s,
     * but full representation is not guaranteed.
     * @param recordID an ID for a record.
     * @return the record in ds-storage record format.
     * @throws ServiceException if the record could not be retrieved.
     */
    public DsRecordDto getDSRecord(String recordID) throws ServiceException {
        return storage.getDSRecord(recordID);
    }

    /**
     * Return a stream of records where the data are transformed to the given format.
     * Only records of type DELIVERABLEUNIT are returned as these are the main metadata format.
     * <p>
     * The logic is complicated by the need to check for access to the IDs:
     * The raw stream of records is split into batches in order to lower the amount of external calls to ds-license.
     * The {@code flatMap(accessFilter)} processes such a batch (a list of {@code DsRecordDto}s) and flattens the
     * result to a regular stream of {@code DsRecordDto}s.
     * @param mTime        point in time (epoch * 1000) for the records to deliver, exclusive.
     * @param maxRecords   the maximum number of records to deliver. -1 means no limit.
     * @param format       the format of the record. See {@link #getViews()} for available formats.
     * @param accessFilter filters which records should be delivered.
     * @param errorList    if errorList is not null and {@link #stopOnError} is false, then all records failing the transformation are added to this list.
     * @return a stream of records in the requested format.
     * @throws ServiceException if anything went wrong during construction of the stream.
     */
    public ContinuationStream<DsRecordDto, Long> getDSRecords(
            Long mTime, Long maxRecords, FormatDto format, Function<List<DsRecordDto>, Stream<DsRecordDto>> accessFilter, ErrorList errorList) {
        View view = getView(format);
        log.debug("Calling storage.getDSRecordsRecordTypeLocalTree(origin='{}', mTime={}, maxRecords={})",
                origin, mTime, maxRecords);
        try {
            // splitToLists creates new streams so this cannot be a single long stream chain
            ContinuationStream<DsRecordDto, Long> allRecords =
                    storage.getDSRecordsByRecordTypeLocalTree(origin, recordRequestType, mTime, maxRecords);
            Stream<DsRecordDto> filteredRecords =  ExtractionUtils.splitToLists(allRecords, LICENSE_BATCH_SIZE)
                    // Apply access filter
                    .flatMap(accessFilter)
                    // Transform records.
                    .map(safeView(format, view, stopOnError(), errorList))
                    .filter(Objects::nonNull);

            return new ContinuationStream<>(filteredRecords, allRecords.getContinuationToken(), allRecords.hasMore(), allRecords.getRecordCount());
        } catch (Exception e) {
            log.warn("Exception calling getDSRecords with origin='{}', mTime={}, maxRecords={}, format='{}'",
                     getId(), mTime, maxRecords, format, e);
            throw new InternalServiceException(
                    "Internal exception requesting records from origin '" + getId() + "' in format " + format);
        }
    }

    /**
     * Return a stream of records where the data are transformed to the given format.
     * Only records of type DELIVERABLEUNIT are returned as these are the main metadata format.
     * <p>
     * This method does not include failing records as part of the returned stream.
     * <p>
     * The logic is complicated by the need to check for access to the IDs:
     * The raw stream of records is split into batches in order to lower the amount of external calls to ds-license.
     * The {@code flatMap(accessFilter)} processes such a batch (a list of {@code DsRecordDto}s) and flattens the
     * result to a regular stream of {@code DsRecordDto}s.
     * @param mTime        point in time (epoch * 1000) for the records to deliver, exclusive.
     * @param maxRecords   the maximum number of records to deliver. -1 means no limit.
     * @param format       the format of the record. See {@link #getViews()} for available formats.
     * @param accessFilter filters which records should be delivered.
     * @return a stream of records in the requested format.
     * @throws ServiceException if anything went wrong during construction of the stream.
     */
    public ContinuationStream<DsRecordDto, Long> getDSRecords(
            Long mTime, Long maxRecords, FormatDto format, Function<List<DsRecordDto>, Stream<DsRecordDto>> accessFilter){
        return getDSRecords(mTime, maxRecords, format, accessFilter, null);
    }

    /**
     * Returns a stream of records with IDs of parent and child records.
     * All types of records are returned.
     * <p>
     * The logic is complicated by the need to check for access to the IDs:
     * The raw stream of records is split into batches in order to lower the amount of external calls to ds-license.
     * The {@code flatMap(accessFilter)} processes such a batch (a list of {@code DsRecordDto}s) and flattens the
     * result to a regular stream of {@code DsRecordDto}s.
     * @param mTime  starting point in time (epoch * 1000) for the records to deliver, exclusive.
     * @param maxRecords the maximum number of records to deliver. -1 means no limit.
     * @param format the format of the record. See {@link #getViews()} for available formats.
     * @param accessFilter filters which records should be delivered.
     * @return a stream of records in the requested format.
     * @throws ServiceException if anything went wrong during construction of the stream.
     */
    public ContinuationStream<DsRecordDto, Long> getDSRecordsAll(
            Long mTime, Long maxRecords, FormatDto format, Function<List<DsRecordDto>, Stream<DsRecordDto>> accessFilter) {
        View view = getView(format);
        log.debug("Extracting with the following view: '{}'", view);
        log.debug("Calling storage.getDSRecords(origin='{}', mTime={}, maxRecords={})",
                origin, mTime, maxRecords);
        try {
            ContinuationStream<DsRecordDto, Long> allRecords = storage.getDSRecords(origin, mTime, maxRecords);
            Stream<DsRecordDto> filteredRecords = ExtractionUtils.splitToLists(allRecords, LICENSE_BATCH_SIZE)
                    // Apply license filter
                    .flatMap(accessFilter)
                    // Transform records. No errorList is provided.
                    .map(safeView(format, view, stopOnError(), null))
                    .filter(Objects::nonNull);

            return new ContinuationStream<>(filteredRecords, allRecords.getContinuationToken(), allRecords.hasMore());
        } catch (Exception e) {
            log.warn("Exception calling getDSRecordsAll with origin='{}', mTime={}, maxRecords={}, format='{}'",
                    getId(), mTime, maxRecords, format, e);
            throw new InternalServiceException(
                    "Internal exception requesting records from origin '" + getId() + "' in format " + format);
        }
    }

    /**
     * Applies the given view to record
     * @param format which the transformation is transforming to.
     * @param view to apply to record.
     * @param stopOnError representing how the program should handle errors. If true, then the program stops, otherwise it continues and handles errors based on the presence of
     *                    an errorList.
     * @param errorList if not null, all failing records are added to the list.
     * @return a transformed record, transformed with input view.
     */
    private static UnaryOperator<DsRecordDto> safeView(FormatDto format, View view, boolean stopOnError, ErrorList errorList) {
        return record -> {
            try {
                record.data(view.apply(record));
                return record;
            } catch (Exception e) {
                transformationErrorsLog.error("An error occurred when transforming record with ID: '{}' to format '{}'. Error is caused by: '{}'.", record.getId(), format,
                        e.toString());
                if (stopOnError) {
                    // Execution stops because stopOnError = true (This can be set)
                    log.error("A transformation error has occurred. Execution of program has stopped as stopOnError=true. See transformations log for further debugging.");
                    throw new RuntimeTransformerException(
                            "Exception transforming record '" + record.getId() + "' to format '" + format + "'. The following exception was thrown: '" + e + "'.");
                } else if (errorList != null) {
                    // Program keeps running, and returns failed records at the end of the response.
                    ErrorRecord failedRecord = new ErrorRecord(record.getId(), e.getMessage());
                    errorList.addErrorToList(failedRecord);

                    log.debug("Added record with id: '{}' to failed list, which gets returned with transformed records.", record.getId());
                    // Return null to not include the error record in the middle of the stream
                    return null;
                } else {
                    // No errorList has been provided. Program continues to run, failed records are not appended to the result.
                    log.warn("A transformation error has occurred. Execution of program continues as stopOnError=false. See transformations log for further debugging.");
                    // Return null to not include the error record in the middle of the stream
                    return null;
                }
            }
        };
    }

    /**
     * @return the ID of the origin, primarily used for debugging and configuration.
     */
    public String getId() {
        return id;
    }

    /**
     * recordIDs always starts with the origin followed by underscore, e.g. {@code images-dsfl_internalid1234}.
     * The prefix must be present for all recordIDs used for lookup.
     * This might be the same as the {@link #id} but it is not a requirement.
     * @return the prefix for recordIDs in the origin.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @return human readable description of the origin.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Map from format -> view. A view is at the core an array of transformations and responsible for transforming
     * metadata to the requested format.
     * @return the available views (aka formats) for this origin. Keys are lowercase.
     */
    public Map<String, View> getViews() {
        return views;
    }

    /**
     * Locate the View which matches the given format (View ID == format.toLowerCase()).
     * @param format the ID of the View.
     * @return a View matching the given format aka ID.
     * @throws InvalidArgumentServiceException if no View could be located.
     */
    public View getView(FormatDto format) {
        View view = views.get(format.getValue().toLowerCase(Locale.ROOT));
        if (view == null) {
            throw new InvalidArgumentServiceException(
                    "The format '" + format + "' is not supported for origin '" + id + "'");
        }
        return view;
    }

    /**
     * @param view any given view, e.g. {@code DOMS} or {@code raw}.
     * @return true if the origin supports the view.
     */
    public boolean supportsView(String view) {
        return views.containsKey(view.toLowerCase(Locale.ROOT));
    }


    /**
     * If true, single record errors during records-export stops the whole flow. If false, a warning is logged.
     */
    public boolean stopOnError() {
        return stopOnError;
    }

    @Override
    public String toString() {
        return "DSOrigin(" +
               "id='" + id + '\'' +
               ", prefix='" + prefix + '\'' +
               ", description='" + description + '\'' +
               ", storage=" + storage +
               ", origin=" + origin +
               ", recordtype= " + recordRequestType +
               ", views=" + views +
               ", stopOnError=" + stopOnError +
               ')';
    }

}
