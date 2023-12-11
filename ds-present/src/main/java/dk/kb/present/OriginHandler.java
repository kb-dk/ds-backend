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

import dk.kb.present.model.v1.FormatDto;
import dk.kb.present.storage.Storage;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import dk.kb.util.yaml.YAML;
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
    private static final String ORIGINS_KEY = ".origins";
    private static final String RECORD_ID_PATTERN_KEY = ".record.id.pattern";
    private static final String ORIGIN_ID_PATTERN_KEY = ".origin.prefix.pattern";

    private final StorageHandler storageHandler;
    private final Map<String, DSOrigin> originsByPrefix; // prefix, origin
    private final Map<String, DSOrigin> originsByID; // id, origin
    private final Pattern recordIDPattern;
    private final Pattern originPrefixPattern;

    /**
     * Creates a {@link StorageHandler} and a set of {@link Storage}s based on the given configuration.
     * @param conf top-level configuration. The parts for this handler is expected to be found at
     * {@code .origins} and {@code .record.id.pattern}
     */
    public OriginHandler(YAML conf) {
        try {
            originPrefixPattern = Pattern.compile(conf.getString(ORIGIN_ID_PATTERN_KEY));
        } catch (Exception e) {
            String message = "Unable to create pattern from configuration, expected key " + RECORD_ID_PATTERN_KEY;
            log.warn(message, e);
            throw new RuntimeException(e);
        }

        storageHandler = new StorageHandler(conf);
        originsByPrefix = conf.getYAMLList(ORIGINS_KEY).stream()
                .map(originConf -> new DSOrigin(originConf, storageHandler))
                .peek(origin -> {
                    if (!originPrefixPattern.matcher(origin.getPrefix()).matches()) {
                        throw new IllegalStateException(
                                "The configured origin prefix '" + origin.getPrefix() + "' for origin '" +
                                origin.getId() + "' does not match the origin prefix pattern '" +
                                originPrefixPattern.pattern() + "'");
                    }})
                .collect(Collectors.toMap(DSOrigin::getPrefix, storage -> storage));
        originsByID = originsByPrefix.values().stream()
                .collect(Collectors.toMap(DSOrigin::getId, storage -> storage));
        try {
            recordIDPattern = Pattern.compile(conf.getString(RECORD_ID_PATTERN_KEY));
        } catch (Exception e) {
            String message = "Unable to create pattern from configuration, expected key " + RECORD_ID_PATTERN_KEY;
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
