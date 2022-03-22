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

import dk.kb.present.backend.api.v1.DsStorageApi;
import dk.kb.present.backend.invoker.v1.ApiClient;
import dk.kb.present.backend.invoker.v1.ApiException;
import dk.kb.present.backend.model.v1.DsRecordDto;
import dk.kb.present.webservice.exception.InternalServiceException;
import dk.kb.present.webservice.exception.NotFoundServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Proxy for a ds-storage https://github.com/kb-dk/ds-storage instance.
 */
public class DSStorage implements Storage {
    private static final Logger log = LoggerFactory.getLogger(DSStorage.class);

    public static final String TYPE = "ds-storage";

    private final String id;
    private final String recordBase;

    private final String host;
    private final int port;
    private final String basepath;
    private final String scheme;
    private final int batchCount;

    // Used for logging and debugging
    private final String serverHuman;

    private final boolean isDefault;
    private final DsStorageApi dsStorageClient;

    /**
     * Create a Storage connection to a ds-storage server.
     * @param id the ID for the storage, used for connecting collections to storages.
     * @param recordBase the base used for requests to {@link DsStorageApi#getRecordsModifiedAfter(String, Long, Long)}.
     * @param scheme the scheme for the connection: {@code http} or {@code https}.
     * @param host the host name for the server: {@code example.com}.
     * @param port the port for the server: {@code 8080}.
     * @param basepath the basepath for the service: {@code /ds-storage/v1/}.
     * @param batchCount the number of records to request in one call when paging using
     *                   {@link DsStorageApi#getRecordsModifiedAfter(String, Long, Long)}.
     * @param isDefault if true, this is the default storage for collections.
     */
    public DSStorage(String id, String recordBase,
                     String scheme, String host, int port, String basepath,
                     int batchCount, boolean isDefault) {
        this.id = id;

        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.basepath = basepath;
        this.batchCount = batchCount;
        this.recordBase = recordBase;

        this.isDefault = isDefault;

        ApiClient apiClient = new ApiClient();
        apiClient.setHost(host);
        apiClient.setPort(port);
        apiClient.setBasePath(basepath);
        apiClient.setScheme(scheme);

        serverHuman = scheme + "://" + host + ":" + port + "/" + basepath;

        dsStorageClient = new DsStorageApi(apiClient);
        log.info("Created " + this);
    }

    @Override
    public String getRecord(String id) {
        return getDSRecord(id).getData();
    }

    @Override
    public DsRecordDto getDSRecord(String id){
        try {
            return dsStorageClient.getRecord(id);
        } catch (ApiException e) {
            log.debug("Unable to retrieve record '" + id + "' from " + serverHuman + "...", e);
            throw new NotFoundServiceException("Unable to retrieve record '" + id + "'", e);
        }
    }

    @Override
    public Stream<DsRecordDto> getDSRecords(long mTime, long maxRecords) {

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
                    records = dsStorageClient.getRecordsModifiedAfter(recordBase, lastMTime.get(), request);
                } catch (ApiException e) {
                    throw new InternalServiceException(
                            "Exception making remote call to ds-storage client.getRecordsModifiedAfter", e);
                }
                pending -= records.size();
                finished = records.isEmpty();
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
               ", host='" + host + '\'' +
               ", port=" + port +
               ", basepath='" + basepath + '\'' +
               ", scheme='" + scheme + '\'' +
               ", batchCount='" + batchCount + '\'' +
               ", isDefault=" + isDefault +
               ')';
    }
}
