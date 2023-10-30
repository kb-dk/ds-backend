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
import dk.kb.util.webservice.exception.NotFoundServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

/**
 * Storage that always fails. Can  used for signalling unsupported formats.
 */
public class FailStorage implements Storage {
    private static final Logger log = LoggerFactory.getLogger(FailStorage.class);

    public static final String TYPE = "fail";

    private final String id;
    private final String message;
    private final boolean isDefault;

    /**
     * Create a storage where all lookups fails.
     * @param id the ID for the storage, used for connecting collections to storages.
     * @param message the message to deliver when {@link #getRecord(String)} is called.
     * @param isDefault if true, this is the default storage for collections.
     */
    public FailStorage(String id, String message, boolean isDefault) {
        this.id = id;
        this.message = message;
        this.isDefault = isDefault;
        log.info("Created " + this);
    }

    @Override
    public String getRecord(String recordID) {
        throw new NotFoundServiceException("Unable to locate record '" + recordID + "': " + message);
    }

    @Override
    public DsRecordDto getDSRecord(String id) {
        throw new NotFoundServiceException("Unable to locate record '" + id + "': " + message);
    }

    @Override
    public DsRecordDto getDSRecordTreeLocal(String id) {
        throw new NotFoundServiceException("Unable to locate record '" + id + "': " + message);
    }

    @Override
    public Stream<DsRecordDto> getDSRecords(String origin, long mTime, long maxRecords) {
        throw new NotFoundServiceException("Unable to locate any records after mTime " + mTime);
    }

    @Override
    public Stream<DsRecordDto> getDSRecordsByRecordTypeLocalTree(String origin, RecordTypeDto recordType, long mTime, long maxRecords) {
        return null;
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
        return "FileStorage(" +
               "id='" + id + '\'' +
               ", message=" + message +
               ", isDefault=" + isDefault +
               ')';
    }
}
