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
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Simple storage backed by static files on the file system.
 *
 * WARNING: This implementation is for test purposes only!
 * It is in no way hardened and can easily be used for requesting arbitrary files outside of the designated folder.
 */
public class FileStorage implements Storage {
    private static final Logger log = LoggerFactory.getLogger(FileStorage.class);

    public static final String TYPE = "folder";

    private final String id;
    private final Path folder;
    private final boolean isDefault;
    private final boolean stripPrefix;

    /**
     * Create a file backed Storage.
     * @param id the ID for the storage, used for connecting collections to storages.
     * @param folder the folder containing the files to deliver upon request.
     * @param stripPrefix if true, the ID {@code collection:subid} is reduced to {subid} before lookup.
     * @param isDefault if true, this is the default storage for collections.
     * @throws IOException if the given folder could not be accessed.
     */
    public FileStorage(String id, Path folder, boolean stripPrefix, boolean isDefault) throws IOException {
        this.id = id;
        if (!Files.isReadable(folder)) {
            // We accept non-readable folders as the FileStorage is only intended for testing
            log.warn(String.format(Locale.ROOT, "Unable to access the configured folder '%s'. Current folder is '%s'",
                                   folder, new java.io.File(".").getCanonicalPath()));
        } else {
            log.info(String.format(Locale.ROOT, "The configured folder '%s' is readable with current folder being '%s'",
                                   folder, new java.io.File(".").getCanonicalPath()));
        }
        this.folder = folder;
        this.stripPrefix = stripPrefix;
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
        if (stripPrefix) {
            // TODO: Switch to using .config.record.id.pattern
            String[] tokens = recordID.split(":", 2);
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
