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

import dk.kb.present.backend.model.v1.DsRecordDto;
import dk.kb.present.webservice.exception.InternalServiceException;
import dk.kb.present.webservice.exception.NotFoundServiceException;
import dk.kb.util.Resolver;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Simple storage backed by static files on the file system.
 *
 * WARNING: This implementation is for test purposes only!
 * It is in no way hardened and can easily be used for requesting arbitrary files outside of the designated folder.
 */
public class FileStorage implements Storage {
    private static final Logger log = LoggerFactory.getLogger(FileStorage.class);

    public static final String TYPE = "folder";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ", Locale.getDefault());

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
    public FileStorage(String id, Path folder, boolean stripPrefix, boolean isDefault){
        this.id = id;
        String current;
        try {
            current = new File(".").getCanonicalPath();
        } catch (IOException e) {
            current = "<unable to determine current folder>";
        };
        if (!Files.isReadable(folder)) {
            // We accept non-readable folders as the FileStorage is only intended for testing
            log.warn(String.format(Locale.ROOT, "Unable to access the configured folder '%s'. Current folder is '%s'",
                                   folder, current));
        } else {
            log.info(String.format(Locale.ROOT, "The configured folder '%s' is readable with current folder being '%s'",
                                   folder, current));
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
    public String getRecord(String recordID) {
        Path file;
        try {
            file = getPath(recordID);
        } catch (IOException e) {
            throw new NotFoundServiceException("Unable to locate file for '" + recordID + "'");
        }
        try {
            return Resolver.resolveUTF8String(file.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new NotFoundServiceException("Located file for '" + recordID + "' but could not fetch content");
        }
    }

    /**
     * Locate a file where the name is the recordID and deliver the content. Works with sub-folders.
     * @param recordID the ID (aka file name) for a record.
     * @return the content of the file with the given name.
     * @throws IOException if the file could not be located or the content not delivered.
     */
    @Override
    public DsRecordDto getDSRecord(String recordID) {
        Path path = null;
        try {
            path = getPath(recordID);
        } catch (IOException e) {
            throw new NotFoundServiceException("Unable to locate file for '" + recordID + "'");
        }
        long mTime = path.toFile().lastModified()*1000; // DsRecordDto used epoch * 1000

        return new DsRecordDto()
                .id(recordID)
                .data(safeRead(path))
                .deleted(false)
                .mTime(mTime)
                .mTimeHuman(DATE_FORMAT.format(new Date(mTime / 1000)));
    }

    private String safeRead(Path path) {
        try {
            return IOUtils.toString(path.toUri(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new NotFoundServiceException("Unable to read content for '" + path.toFile().getName() + "'");
        }
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

    /**
     * Resolves the recordID to a file path.
     * @param recordID a record ID.
     * @return the file path corresponding to the ID.
     * @throws IOException if the ID could not be resolved.
     */
    private Path getPath(String recordID) throws IOException {
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
        return file.toAbsolutePath();
    }

    @Override
    public Stream<DsRecordDto> getDSRecords(long mTime, long maxRecords) {
        // To keep memory usage down we create shallow DsRecordDtos (aka without data) and only
        // populate them when delivering the next stream element

        final List<DsRecordDto> shallow = getShallow(mTime, maxRecords);
        Iterator<DsRecordDto> iterator = new Iterator<DsRecordDto>() {
            int pos = 0;

            @Override
            public boolean hasNext() {
                return pos < shallow.size();
            }

            @Override
            public DsRecordDto next() {
                return populate(shallow.get(pos++));
            }
        };
        return StreamSupport.stream(((Iterable<DsRecordDto>) () -> iterator).spliterator(), false);
    }

    /**
     * Get a shallow (not populated with data) list of all the files under {@link #folder}, sorted ascending by mTime.
     * @param mTime point in time (epoch * 1000) for the records to deliver, exclusive.
     * @param maxRecords the maximum number of records to deliver. -1 means no limit.
     * @return a list of records in the folder satisfying the constraints.
     */
    private List<DsRecordDto> getShallow(long mTime, long maxRecords) {
        try {
            //noinspection ConstantConditions
            return Files.list(folder)
                    .map(Path::toFile)
                    .filter(f -> mTime/1000 < f.lastModified())
                    .map(this::getShallow)
                    .sorted(Comparator.comparingLong(DsRecordDto::getmTime)) // We know it is always set in getShallow
                    .limit(maxRecords == -1 ?Long.MAX_VALUE : maxRecords)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new InternalServiceException("Unable to resolve records");
        }
    }

    /**
     * Load the data of the record (the file is folder + id) as UTF-8, assign it to the given shallow record
     * and return the record.
     * @param shallow a record with null data.
     * @return the shallow record populated with data.
     */
    private DsRecordDto populate(DsRecordDto shallow) {
        try {
            Path path = getPath(shallow.getId());
            return shallow.data(IOUtils.toString(path.toUri(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new InternalServiceException("Unable to populate record '" + shallow.getId() + "' with data");
        }
    }

    /**
     * Represent the file as a shallow record, containing only id (file name), mTime and mTimeHuman.
     * @param file the file to represent.
     * @return a shallow representation of the file.
     */
    private DsRecordDto getShallow(File file) {
        return new DsRecordDto()
                .id(file.getName()) // Only the filename itself
                .deleted(false)
                .mTime(file.lastModified()*1000)
                .mTimeHuman(DATE_FORMAT.format(new Date(file.lastModified())));
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
