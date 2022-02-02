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

import dk.kb.util.yaml.YAML;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ServiceNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Creates {@link DSStorage} from a given configuration and provides access to them based on ID-prefix.
 */
public class StorageHandler {
    private static final Logger log = LoggerFactory.getLogger(StorageHandler.class);
    private static final String STORAGES_KEY = ".config.storages";

    private final Map<String, DSStorage> storages; // storageID, storage
    private DSStorage defaultStorage;

    public StorageHandler(YAML conf) {
        storages = conf.getYAMLList(STORAGES_KEY).stream()
                .map(DSStorage::new)
                .peek(storage -> {
                    if (storage.isDefault() || defaultStorage == null) {
                        defaultStorage = storage;
                    }
                })
                .collect(Collectors.toMap(DSStorage::getId, storage -> storage));
        log.info("Created " + this);
    }

    public DSStorage getStorage(String storageID) {
        DSStorage storage = storageID == null ? defaultStorage : storages.get(storageID);
        if (storage == null) {
            throw new NullPointerException(
                    "Unable to locate a storage with ID '" + storageID + "'. Available storages: " + storages.keySet());
        }
        return storage;
    }

    public String getRecord(String id) throws NotFoundException {
        log.debug("Getting record with id '" + id + "'");
        String[] parts = id.split("_", 2);
        if (parts.length < 2) {
            throw new NotFoundException("Unable to isolate collection part for id '" + id + "'");
        }
        DSStorage storage = storages.get(parts[0]);
        if (storage == null) {
            throw new NotFoundException("The collection '" + parts[0] + "' is not available. Full ID was '" + id + "'");
        }
        return storage.getRecord(id);
    }

    @Override
    public String toString() {
        return "StorageHandler(" +
               "storages=" + storages.values() +
               ')';
    }
}
