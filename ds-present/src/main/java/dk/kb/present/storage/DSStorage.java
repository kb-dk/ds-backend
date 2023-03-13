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


import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.storage.client.v1.DsStorageApi;
import dk.kb.storage.invoker.v1.ApiException;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.NotFoundServiceException;

/**
 * Proxy for a ds-storage https://github.com/kb-dk/ds-storage instance.
 */
public class DSStorage implements Storage {
    private static final Logger log = LoggerFactory.getLogger(DSStorage.class);

    public static final String TYPE = "ds-storage";

    private final String id;
    private final String recordBase;

    private final String storageUrl;
    private final int batchCount;


    private final boolean isDefault;
    
    private static DsStorageApi storageClient;  

    /**
     * Create a Storage connection to a ds-storage server.
     * @param id the ID for the storage, used for connecting collections to storages.
     * @param recordBase the base used for requests to {@link DsStorageApi#getRecordsModifiedAfter(String, Long, Long)}. 
     * @param storageUrl The full url to the service. Example: http://localhost:9072/ds-storage/v1/
     * @param batchCount the number of records to request in one call when paging using
     *                   {@link DsStorageApi#getRecordsModifiedAfter(String, Long, Long)}.
     * @param isDefault if true, this is the default storage for collections.
     */
    public DSStorage(String id, String recordBase,
                     String storageUrl,
                     int batchCount, boolean isDefault) {
        this.id = id;
        
        this.storageUrl = storageUrl;
        this.batchCount = batchCount;
        this.recordBase = recordBase;

        this.isDefault = isDefault;
       

        storageClient = getDsStorageApiClient(storageUrl);
//        if (isEmpty(id) || isEmpty(scheme) || isEmpty(host) || isEmpty(basepath) || isEmpty(recordBase)) {
//            throw new IllegalArgumentException("All parameters must be specified for " + this);
//        }
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
    public DsRecordDto getDSRecord(String id){
        log.debug("getDSRecord(id='{}') called", id);
        try {
            return storageClient.getRecord(id);
        } catch (ApiException e) {
            log.debug("Unable to retrieve record '" + id + "' from " + storageUrl + "...", e);
            throw new NotFoundServiceException("Unable to retrieve record '" + id + "'", e);
        }
    }

    @Override
    public Stream<DsRecordDto> getDSRecords(final String recordBase, long mTime, long maxRecords) {
        log.debug("getDSRecords(recordBase='{}', mTime={}, maxRecords={}) called", recordBase, mTime, maxRecords);
        String finalRecordBase = recordBase == null ? this.recordBase : recordBase;

        if (finalRecordBase == null || finalRecordBase.isEmpty()) {
            throw new InternalServiceException(
                    "recordBase not defined for DSStorage '" + getID() + "'. Only single record lookups are possible");
        }

        // Unfortunately the OpenAPI generator creates a client which requests all records as a list in a single call
        // instead of doing streaming, so we need to page.

        Iterator<DsRecordDto> iterator = new Iterator<DsRecordDto>() {
            long pending = maxRecords == -1 ? Long.MAX_VALUE : maxRecords; // -1 = all records
            final AtomicLong lastMTime = new AtomicLong(mTime);
            List<DsRecordDto> records = null;
            boolean finished = pending == 0;

            void ensureFilled() {
                if (finished || (records != null && !records.isEmpty())) {
                    return;
                }
                if (pending <= 0) {
                    finished = true;
                    return;
                }

                long request = pending < batchCount ? (int) pending : batchCount;
                try {
                    records = storageClient.getRecordsModifiedAfter(finalRecordBase, lastMTime.get(), request);
                } catch (ApiException e) {
                    String message = String.format(
                            Locale.ROOT,
                            "Exception making remote call to ds-storage client " +
                            "getRecordsModifiedAfter(base='%s', mTime=%d, maxRecords=%d)",
                            finalRecordBase, mTime, maxRecords);
                    throw new InternalServiceException(message, e);
                }
                pending -= records.size();
                finished = records.isEmpty();
                if (!finished) {
                    Long lastRecordMTime = records.get(records.size()-1).getmTime();
                    if (lastRecordMTime == null) {
                        throw new InternalServiceException(
                                "Got null as mTime for record '" + records.get(records.size()-1).getId());
                    }
                    lastMTime.set(lastRecordMTime);
                }
            }
            @Override
            public boolean hasNext() {
                ensureFilled();
                return !finished;
            }

            @Override
            public DsRecordDto next() {
                ensureFilled();
                if (finished) {
                    throw new NoSuchElementException("No more records");
                }
                return records.remove(0);
            }
        };

        return StreamSupport.stream(((Iterable<DsRecordDto>) () -> iterator).spliterator(), false);
    }

    private static DsStorageApi getDsStorageApiClient(String storageUrl) {       
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
               ", recordBase='" + recordBase + '\'' +
               ", isDefault=" + isDefault +
               ')';
    }
}
