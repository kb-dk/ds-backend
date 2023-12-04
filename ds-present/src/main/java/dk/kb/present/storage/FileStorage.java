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

import dk.kb.present.webservice.exception.ForbiddenServiceException;
import dk.kb.storage.model.v1.DsRecordDto;

import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.NotFoundServiceException;

import dk.kb.util.Resolver;
import dk.kb.util.webservice.stream.ContinuationStream;
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
import java.util.regex.Pattern;
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
    private final String extension;
    private final boolean isDefault;
    private final boolean stripPrefix;
    private final List<Pattern> whitelist; // Applied after stripPrefix
    private final List<Pattern> blacklist; // Applied after whitelist

    /**
     * Create a file backed Storage.
     * @param id the ID for the storage, used for connecting origins to storages.
     * @param folder the folder containing the files to deliver upon request.
     * @param extension if defined, {@link #getDSRecords} will only return files with this extension.
     * @param stripPrefix if true, the ID {@code origin:subid} is reduced to {subid} before lookup.
     * @param whitelist if not null, ID's must pass the whitelist in order to be delivered.
     * @param blacklist if not null, ID's that matches the blacklist are not delivered.
     * @param isDefault if true, this is the default storage for origins.
     * @throws IOException if the given folder could not be accessed.
     */
    public FileStorage(String id, Path folder, String extension,
                       boolean stripPrefix, List<Pattern> whitelist, List<Pattern> blacklist, boolean isDefault){
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
        this.extension = extension == null ? "" : extension;
        this.stripPrefix = stripPrefix;
        this.whitelist = whitelist;
        this.blacklist = blacklist;
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
        if (!isAllowed(recordID)) {
            throw new ForbiddenServiceException("Access to rhe record with ID '" + recordID + "' is forbidden");
        }
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
        return getDsRecordDto(recordID);
    }

    /**
     * Locate a file where the name is the recordID and deliver the content. Works with sub-folders.
     * @param id the ID (aka file name) for a record.
     * @return the content of the file with the given name.
     * @throws IOException if the file could not be located or the content not delivered.
     */
    @Override
    public DsRecordDto getDSRecordTreeLocal(String id){
        return getDsRecordDto(id);
    }

    private DsRecordDto getDsRecordDto(String recordID) {
        if (!isAllowed(recordID)) {
            throw new ForbiddenServiceException("Access to rhe record with ID '" + recordID + "' is forbidden");
        }
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
                .children(addTestChildren())
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
     * Checks the given ID against {@link #whitelist} and {@link #blacklist}.
     * @param recordID any record ID.
     * @return true if record delivery is allowed. This does not guarantee that the record can be delivered.
     */
    public boolean isAllowed(String recordID) {
        recordID = stripToID(recordID);

        out:
        if (whitelist != null && !whitelist.isEmpty()) {
            for (Pattern white: whitelist) {
                if (white.matcher(recordID).matches()) {
                    break out;
                }
            }
            return false;
        }
        
        if (blacklist != null && !blacklist.isEmpty()) {
            for (Pattern black: blacklist) {
                if (black.matcher(recordID).matches()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Resolves the recordID to a file path.
     * @param recordID a record ID.
     * @return the file path corresponding to the ID.
     * @throws IOException if the ID could not be resolved.
     */
    private Path getPath(String recordID) throws IOException {
        recordID = stripToID(recordID);
        return getPathDirect(recordID);
    }

    /**
     * If {@link #stripPrefix} is true, the prefix is stripped before returning the ID.
     * @param recordID a record ID with a prefix.
     * @return the sub-ID part of the record if {@link #stripPrefix} is true, else the input record.
     */
    private String stripToID(String recordID) {
        if (!stripPrefix) {
            return recordID;
        }
        // TODO: Switch to using .record.id.pattern
        String[] tokens = recordID.split(":", 2);
        if (tokens.length < 2) {
            log.warn("Attempted to strip prefix from '" + recordID + "' but there was no '_' delimiter");
            return recordID;
        } else {
            return tokens[1];
        }
    }

    /**
     * Resolves the recordID to a file path without attempting to strip prefix.
     * @param recordID a record ID.
     * @return the file path corresponding to the ID.
     * @throws IOException if the ID could not be resolved.
     */
    private Path getPathDirect(String recordID) throws IOException {
        Path file = folder.resolve(recordID);
        if (!Files.isReadable(file)) {
            throw new NotFoundServiceException("Unable to locate record '" + recordID + "'");
        }
        return file.toAbsolutePath();
    }

    @Override
    public ContinuationStream<DsRecordDto, Long> getDSRecords(String origin, long mTime, long maxRecords) {
        // To keep memory usage down we create shallow DsRecordDtos (aka without data) and only
        // populate them when delivering the next stream element

        // We need 1 extra record to determine hasMore later on
        final List<DsRecordDto> shallowPlusOne = getShallow(mTime, maxRecords == -1 ? -1 : maxRecords+1);
        final List<DsRecordDto> shallow = maxRecords == -1 || shallowPlusOne.size() <= maxRecords ?
                shallowPlusOne :
                shallowPlusOne.subList(0, (int) maxRecords);

        Iterator<DsRecordDto> iterator = new Iterator<>() {
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
        Stream<DsRecordDto> records = StreamSupport.stream(((Iterable<DsRecordDto>) () -> iterator).spliterator(), false);
        Long continuationToken = shallow.isEmpty() ? null : populate(shallow.get(shallow.size()-1)).getmTime();
        Boolean hasMore = shallowPlusOne.size() == maxRecords+1;
        return new ContinuationStream<>(records, continuationToken, hasMore);
    }

    @Override
    public ContinuationStream<DsRecordDto, Long> getDSRecordsByRecordTypeLocalTree(
            String origin, RecordTypeDto recordType, long mTime, long maxRecords) {
        // TODO: Make a proper implementation that checks the type (e.g. make getShallow take a filter)
        return getDSRecords(origin, mTime, maxRecords);
    }

    /**
     * Get a shallow (not populated with data) list of all the files under {@link #folder}, sorted ascending by mTime.
     * @param mTime point in time (epoch * 1000) for the records to deliver, exclusive.
     * @param maxRecords the maximum number of records to deliver. -1 means no limit.
     * @return a list of records in the folder satisfying the constraints.
     */
    @SuppressWarnings("ConstantConditions")
    private List<DsRecordDto> getShallow(long mTime, long maxRecords) {
        try (Stream<Path> fileStream = Files.list(folder)) {
            return fileStream
                    .filter(path -> path.toString().endsWith(extension))
                    .filter(path -> isAllowed(path.getFileName().toString()))
                    .map(Path::toFile)
                    .filter(f -> mTime / 1000 < f.lastModified())
                    .map(this::getShallow)
                    .sorted(Comparator.comparingLong(DsRecordDto::getmTime)) // We know it is always set in getShallow
                    .limit(maxRecords == -1 ? Long.MAX_VALUE : maxRecords)
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
            Path path = getPathDirect(shallow.getId());
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
               ", extension=" + extension +
               ", isDefault=" + isDefault +
               ", stripPrefix=" + stripPrefix +
               ')';
    }

    /**
     * Method that creates test children, where one should be filtered away and the other should be returned,
     * when records are returned from a tess FileStorage through a OriginHandler. These test files are used to
     * test filtering of preservation manifestations.
     * @return a list of test children records for a deliverable unit.
     */
    private List<DsRecordDto> addTestChildren() {
        List<DsRecordDto> children = new ArrayList<>();

        DsRecordDto wrongChild = new DsRecordDto();
        wrongChild.setData("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<xip:Manifestation xmlns:xip=\"http://www.tessella.com/XIP/v4\" status=\"new\">" +
                "<ManifestationRelRef>1</ManifestationRelRef>" +
                "<TypeRef>1</TypeRef>" +
                "<ComponentManifestation status=\"same\">" +
                "<ComponentRef>wrong-reference</ComponentRef>" +
                "<ComponentManifestationRef>wrong-reference</ComponentManifestationRef>" +
                "<MasterFileRef>wrong-reference</MasterFileRef>" +
                "<FileRef>wrong-reference</FileRef>" +
                "</ComponentManifestation>" +
                "</xip:Manifestation>");

        DsRecordDto correctChild = new DsRecordDto();
        correctChild.setData("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<xip:Manifestation xmlns:xip=\"http://www.tessella.com/XIP/v4\" status=\"new\">" +
                "<ManifestationRelRef>2</ManifestationRelRef>" +
                "<TypeRef>2</TypeRef>" +
                "<ComponentManifestation status=\"same\">" +
                "<ComponentRef>correct-reference</ComponentRef>" +
                "<ComponentManifestationRef>correct-reference</ComponentManifestationRef>" +
                "<MasterFileRef>correct-reference</MasterFileRef>" +
                "<FileRef>correct-reference</FileRef>" +
                "</ComponentManifestation>" +
                "</xip:Manifestation>");

        children.add(wrongChild);
        children.add(correctChild);

        return children;
    }
}
