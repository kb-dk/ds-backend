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
package dk.kb.present.storage;

import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import dk.kb.util.webservice.stream.ContinuationStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Proxy for a ds-storage https://github.com/kb-dk/ds-storage instance.
 */
public class DSStorage implements Storage {
    private static final Logger log = LoggerFactory.getLogger(DSStorage.class);

    public static final String TYPE = "ds-storage";

    private final String id;
    private final String origin;

    private final String storageUrl;
    private final int batchCount;


    private final boolean isDefault;
    
    private static DsStorageClient storageClient;

    /**
     * Create a Storage connection to a ds-storage server.
     * @param id the ID for the storage, used for connecting origins to storages.
     * @param origin the origin used for requests to {@link DsStorageApi#getRecordsModifiedAfter(String, RecordTypeDto, Long, Long)}.
     * @param storageUrl The full url to the service. Example: http://localhost:9072/ds-storage/v1/
     * @param batchCount the number of records to request in one call when paging using
     *                   {@link DsStorageApi#getRecordsModifiedAfter(String, RecordTypeDto, Long, Long)}.
     * @param isDefault if true, this is the default storage for origins.
     */
    @SuppressWarnings("JavadocLinkAsPlainText")
    public DSStorage(String id, String origin,
                     String storageUrl,
                     int batchCount, boolean isDefault) {
        this.id = id;
        
        this.storageUrl = storageUrl;
        this.batchCount = batchCount;
        this.origin = origin;

        this.isDefault = isDefault;
       

        storageClient = getDsStorageApiClient(storageUrl);
        log.info("Created " + this);
    }

    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    @Override
    public String getRecord(String id) {
        log.debug("getRecord(id='{}') called", id);
        return getDSRecord(id).getData();
    }

    @Override
    public DsRecordDto getDSRecord(String id) throws ServiceException{
        log.debug("getDSRecord(id='{}') called", id);
        try {
            return storageClient.getRecord(id,false);
        } catch (ServiceException e) {
            log.debug("Unable to retrieve record '" + id + "' from " + storageUrl + "...", e);
           throw e;
        }
    }

    @Override
    public DsRecordDto getDSRecordTreeLocal(String id) throws ServiceException{
        log.debug("getDSRecordTreeLocal(id='{}') called", id);
        try {
            DsRecordDto record = storageClient.getRecord(id,true);
            if (record.getRecordType() != RecordTypeDto.DELIVERABLEUNIT){
                log.warn("Requests for anything else than deliverableUnits are not allowed.");
                throw new IllegalArgumentException("Requests for anything else than deliverableUnits are not allowed.");
            }
            return record;
        } catch (ServiceException e){
            log.debug("Unable to retrieve record '" + id + "' from " + storageUrl + "...", e);
            throw e;
        }
    }


    @Override
    public ContinuationStream<DsRecordDto, Long> getDSRecords(final String origin, long mTime, long maxRecords) {
        log.debug("getDSRecords(origin='{}', mTime={}, maxRecords={}) called", origin, mTime, maxRecords);

        return getDsRecordDtoStream(mTime, maxRecords, origin, null);
    }

    @Override
    public ContinuationStream<DsRecordDto, Long> getDSRecordsByRecordTypeLocalTree(String origin, RecordTypeDto recordType,
                                                                 long mTime, long maxRecords) {
        log.debug("getDSRecordsByRecordTypeLocalTree(origin='{}', recordType={}, mTime={}, maxRecords={}) called",
                origin, recordType, mTime, maxRecords);


        return getDsRecordDtoStream(mTime, maxRecords, origin, recordType);
    }

    private ContinuationStream<DsRecordDto, Long> getDsRecordDtoStream(
            long mTime, long maxRecords, String origin, RecordTypeDto recordType) {
        String finalOrigin = origin == null ? this.origin : origin;
        if (isEmpty(finalOrigin)) {
            throw new InternalServiceException(
                    "origin not defined for DSStorage '" + getID() + "'. Only single record lookups are possible");
        }

        try {
            return recordType == null ?
                    storageClient.getRecordsModifiedAfterStream(finalOrigin, mTime, maxRecords) :
                    storageClient.getRecordsByRecordTypeModifiedAfterLocalTreeStream(finalOrigin, recordType, mTime, maxRecords);
        } catch (Exception e) {
            String message = String.format(
                    Locale.ROOT,
                    "Exception requesting records from remote storage for " +
                            "origin='%s', mTime=%d, maxRecords=%d, recordType='%s')",
                    finalOrigin, mTime, maxRecords, recordType);
            log.warn(message, e);
            throw new InternalServiceException(message, e);
        }
    }

    private static DsStorageClient getDsStorageApiClient(String storageUrl) {
        if (storageClient!= null) {            
        	return storageClient;
        }
                                                 
        storageClient = new DsStorageClient(storageUrl);               
        log.info("Ds-storage client generated from url:"+storageUrl);
        return storageClient;
      }
    
    
    @Override
    public String getID() {
        return id;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public String toString() {
        return "DSStorage(" +
               "id='" + id + '\'' +               
               ", storageUrl='" + storageUrl + '\'' +
               ", batchCount='" + batchCount + '\'' +
               ", origin='" + origin + '\'' +
               ", isDefault=" + isDefault +
               ')';
    }
}
