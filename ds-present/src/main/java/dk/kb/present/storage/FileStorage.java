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
 * Simple storage backed by static files on the file system.
 *
 * WARNING: This implementation is for test purposes only!
 * It is in no way hardened and can easily be used for requesting arbitrary files outside of the designated folder.
 */
public class FileStorage implements Storage {
    private static final Logger log = LoggerFactory.getLogger(FileStorage.class);

    public static final String TYPE = "folder";
    public static final String FOLDER_KEY = "root";
    public static final String STRIP_PREFIX_KEY = "stripprefix";

    private final String id;
    private final Path folder;
    private final boolean isDefault;
    private final boolean stripPrefix;

    public FileStorage(String id, YAML conf, boolean isDefault) throws IOException {
        this.id = id;
        String folderStr = conf.getString(FOLDER_KEY);
        if (folderStr == null) {
            throw new NullPointerException("The root folder was not specified under the key '" + FOLDER_KEY + "'");
        }
        folder = Path.of(folderStr);
        if (!Files.isReadable(folder)) {
            throw new IOException("Unable to access the folder '" + folder + "'");
        }
        this.isDefault = isDefault;
        this.stripPrefix = conf.getBoolean(STRIP_PREFIX_KEY, true);
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
        if (stripPrefix) {
            String[] tokens = recordID.split("_", 2);
            if (tokens.length < 2) {
                log.warn("Attemped to strip prefix from '" + recordID + "' but there was no '_' delimiter");
            } else {
                recordID = tokens[1];
            }
        }
        Path file = folder.resolve(recordID);
        if (!Files.isReadable(file)) {
            throw new NotFoundServiceException("Unable to locate record '" + recordID + "'");
        }
        return Resolver.resolveUTF8String(file.toAbsolutePath().toString());
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
               ", folder=" + folder +
               ", isDefault=" + isDefault +
               ", stripPrefix=" + stripPrefix +
               ')';
    }
}
