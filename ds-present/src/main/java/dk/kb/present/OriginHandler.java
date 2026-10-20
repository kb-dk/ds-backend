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

import dk.kb.present.config.OriginConfig;
import dk.kb.present.config.StorageConfig;
import dk.kb.present.model.v1.FormatDto;
import dk.kb.present.storage.Storage;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Creates {@link DSOrigin}s from a given configuration and provides access to them based on ID-prefix.
 */
public class OriginHandler {
    private static final Logger log = LoggerFactory.getLogger(OriginHandler.class);

    private final StorageHandler storageHandler;
    private final Map<String, DSOrigin> originsByPrefix; // prefix, origin
    private final Map<String, DSOrigin> originsByID; // id, origin
    private final Pattern recordIDPattern;
    private final Pattern originPrefixPattern;

    /**
     * Creates a {@link StorageHandler} and a set of {@link Storage}s based on the given configuration.
     *
     * @param origins the configured origins; see {@link dk.kb.present.config.ServiceConfig#getOrigins()}.
     * @param originPrefixPattern the pattern every origin's prefix must match; see
     *                            {@link dk.kb.present.config.ServiceConfig#getOriginPrefixPattern()}.
     * @param recordIdPattern the pattern every incoming record ID must match; see
     *                        {@link dk.kb.present.config.ServiceConfig#getRecordIdPattern()}.
     * @param storages the configured storages, used to create this handler's own {@link StorageHandler}; see
     *                 {@link dk.kb.present.config.ServiceConfig#getStorages()}.
     */
    public OriginHandler(List<OriginConfig> origins, String originPrefixPattern, String recordIdPattern,
                          List<StorageConfig> storages) {
        Pattern compiledOriginPrefixPattern;
        try {
            compiledOriginPrefixPattern = Pattern.compile(originPrefixPattern);
        } catch (Exception e) {
            String message = "Unable to compile origin prefix pattern '" + originPrefixPattern + "'";
            log.warn(message, e);
            throw new RuntimeException(e);
        }
        this.originPrefixPattern = compiledOriginPrefixPattern;

        storageHandler = new StorageHandler(storages);
        originsByPrefix = origins.stream()
                .map(originConf -> new DSOrigin(originConf, storageHandler))
                .peek(origin -> {
                    if (!compiledOriginPrefixPattern.matcher(origin.getPrefix()).matches()) {
                        throw new IllegalStateException(
                                "The configured origin prefix '" + origin.getPrefix() + "' for origin '" +
                                origin.getId() + "' does not match the origin prefix pattern '" +
                                compiledOriginPrefixPattern.pattern() + "'");
                    }})
                .collect(Collectors.toMap(DSOrigin::getPrefix, storage -> storage));
        originsByID = originsByPrefix.values().stream()
                .collect(Collectors.toMap(DSOrigin::getId, storage -> storage));
        try {
            recordIDPattern = Pattern.compile(recordIdPattern);
        } catch (Exception e) {
            String message = "Unable to compile record ID pattern '" + recordIdPattern + "'";
            log.warn(message, e);
            throw new RuntimeException(e);
        }
        log.info("Created " + this);
    }

    public String getRecord(String id, FormatDto format) throws NotFoundServiceException {
        Matcher matcher = recordIDPattern.matcher(id);
        if (!matcher.matches()) {
            throw new InvalidArgumentServiceException(
                    "ID '" + id + "' should conform to pattern '" + recordIDPattern + "'");
        }
        DSOrigin origin = originsByPrefix.get(matcher.group(1));
        if (origin == null) {
            throw new NotFoundServiceException(
                    "A origin for IDs with prefix '" + matcher.group(1) + "' is not available. " +
                    "Full ID was '" + id + "'. Available origin-prefixess are " + originsByPrefix.keySet());
        }
        return origin.getRecord(id, format);
    }

    /**
     * @param originID an ID for an origin.
     * @return an origin with the given ID or null if it does not exist.
     */
    public DSOrigin getOrigin(String originID) {
        return originsByID.get(originID);
    }

    /**
     * @return a complete list of supported origins.
     */
    public List<String> getOriginIDs() {
        return new ArrayList<>(originsByID.keySet());
    }

    /**
     * @return all origins.
     */
    public Collection<DSOrigin> getOrigins() {
        return originsByPrefix.values();
    }

    @Override
    public String toString() {
        return "OriginHandler(" +
               "origins=" + originsByPrefix.values() +
               "recordIDPattern: '" + recordIDPattern + "'" +
               ')';
    }
}
