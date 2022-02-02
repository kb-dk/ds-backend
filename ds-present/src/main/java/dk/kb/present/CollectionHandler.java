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

import dk.kb.present.config.ServiceConfig;
import dk.kb.present.webservice.exception.NotFoundServiceException;
import dk.kb.util.yaml.YAML;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Creates {@link DSStorage} from a given configuration and provides access to them based on ID-prefix.
 */
public class CollectionHandler {
    private static final Logger log = LoggerFactory.getLogger(CollectionHandler.class);
    private static final String COLLECTIONS_KEY = ".config.collections";

    private final StorageHandler storageHandler;
    private final Map<String, DSCollection> collections; // prefix, collection

    public CollectionHandler(YAML conf) {
        storageHandler = new StorageHandler(ServiceConfig.getConfig());
        collections = conf.getYAMLList(COLLECTIONS_KEY).stream()
                .map(collectionConf -> new DSCollection(collectionConf, storageHandler))
                .collect(Collectors.toMap(DSCollection::getId, storage -> storage));
        log.info("Created " + this);
    }

    public String getRecord(String id, String format) throws NotFoundServiceException {
        String[] parts = id.split("_", 2);
        if (parts.length < 2) {
            throw new NotFoundServiceException("Unable to isolate collection part for id '" + id + "'");
        }
        DSCollection collection = collections.get(parts[0]);
        if (collection == null) {
            throw new NotFoundServiceException(
                    "The collection '" + parts[0] + "' is not available. Full ID was '" + id + "'");
        }
        return collection.getRecord(id, format);
    }

    /**
     * @param id an ID for a collection.
     * @return a collection with the given ID or null if it does not exist.
     */
    public DSCollection getCollection(String id) {
        return collections.get(id);
    }

    /**
     * @return all collections.
     */
    public Collection<DSCollection> getCollections() {
        return collections.values();
    }

    @Override
    public String toString() {
        return "CollectionHandler(" +
               "collections=" + collections +
               ')';
    }
}
