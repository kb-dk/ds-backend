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

import dk.kb.present.storage.DSStorage;
import dk.kb.present.storage.Storage;
import dk.kb.present.storage.StorageController;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Creates {@link DSStorage}s from a given configuration. Storages are used for retrieving raw records by
 * {@link DSOrigin} and can be shared between multiple origins.
 */
public class StorageHandler {
    private static final Logger log = LoggerFactory.getLogger(StorageHandler.class);
    private static final String STORAGES_KEY = ".config.storages";

    private final Map<String, Storage> storages; // storageID, storage
    private Storage defaultStorage; // If the ID for the requested storage is null

    /**
     * Given a top-level configuration, iterate the storages defined under {@link #STORAGES_KEY} and create
     * storages with the given sub-configurations.
     * @param conf top-level configuration.
     */
    public StorageHandler(YAML conf) {
        storages = conf.getYAMLList(STORAGES_KEY).stream()
                .map(storageConf -> {
                    try {
                        return StorageController.createStorage(storageConf);
                    } catch (Exception e) {
                        throw new RuntimeException("Exception creating Storage", e);
                    }
                })
                .peek(storage -> {
                    if (storage.isDefault() || defaultStorage == null) {
                        defaultStorage = storage;
                    }
                })
                .collect(Collectors.toMap(Storage::getID, storage -> storage));
        log.info("Created " + this);
    }

    /**
     * Get the storage with the given ID.
     * @param id storage ID, as defined in the configuration. If null is provided, the default storage is returned.
     * @return a storage for the given id.
     * @throws NullPointerException is the storage could not be located.
     */
    public Storage getStorage(String id) {
        Storage storage = id == null ? defaultStorage : storages.get(id);
        if (storage == null) {
            throw new NullPointerException(
                    "Unable to locate a storage with ID '" + id + "'. Available storages: " + storages.keySet());
        }
        return storage;
    }

    @Override
    public String toString() {
        return "StorageHandler(" +
               "storages=" + storages.values() +
               ')';
    }
}
