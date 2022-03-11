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

import dk.kb.present.webservice.exception.NotFoundServiceException;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

    /**
     * Locate a file where the name is the recordID and deliver the content. Works with sub-folders.
     * @param recordID the ID (aka file name) for a record.
     * @return the content of the file with the given name.
     * @throws IOException if the file could not be located or the content not delivered.
     */
    @Override
    public String getRecord(String recordID) throws IOException {
        throw new NotFoundServiceException("Unable to locate record '" + recordID + "': " + message);
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
