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

import dk.kb.present.config.BackendConfig;
import dk.kb.present.config.StorageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scans the classes for implementations of {@link StorageFactory} and makes it possible to create {@link Storage}s
 * from their ID and a configuration.
 */
public class StorageController {
    private static final Logger log = LoggerFactory.getLogger(StorageController.class);

    private static final Map<String, StorageFactory> factories = getFactories();

    /**
     * Create a storage with the given configuration.
     * If the config contains multiple backends, those are wrapped in a {@link MultiStorage}.
     *
     * @return a configured storage for the given ID, ready for use.
     * @throws NullPointerException if no storage with the given id could be located.
     * @throws Exception if the storage could not be created.
     */
    public static Storage createStorage(StorageConfig conf) throws Exception {
        List<BackendConfig> backends = conf.getBackends();
        if (backends.isEmpty()) {
            throw new IllegalArgumentException("No backends defined for storage " + conf.getId());
        }
        List<Storage> storages = new ArrayList<>(backends.size());
        for (BackendConfig backend : backends) {
            storages.add(createStorage(backend.getType(), conf.getId(), backend, conf.isDefault()));
        }
        return storages.size() > 1 ? new MultiStorage(conf.getId(), storages, conf.getOrder(), conf.isDefault()) :
                storages.get(0);
    }

    /**
     * Create a storage with the given configuration.
     *
     * @param storageType the ID of the storage to create. Call {@link #getSupportedStorageIDs()} for a complete list.
     * @param storageID the ID for the storage, as specified in the configuration.
     * @param conf storage specific configuration.
     * @return a configured storage for the given ID, ready for use.
     * @throws NullPointerException if no storage with the given id could be located.
     * @throws Exception if the storage could not be created.
     */
    public static Storage createStorage(String storageType, String storageID, BackendConfig conf, boolean isDefault)
            throws Exception {
        StorageFactory factory = factories.get(storageType);
        if (factory == null) {
            throw new NullPointerException(String.format(
                    Locale.ROOT, "A factory with the ID '%s' was not available. Supported factories are %s",
                    storageType, getSupportedStorageIDs()));
        }
        log.debug("Creating a {} storage", factory.getStorageType());
        return factory.createStorage(storageID, conf, isDefault);
    }

    /**
     * @return the IDs for the {@link Storage}s that can be created.
     */
    public static Set<String> getSupportedStorageIDs() {
        return factories.keySet();
    }

    /**
     * Build a map of storage factories from the classpath.
     *
     * @return map of [storageID, storage].
     */
    private static Map<String, StorageFactory> getFactories() {
        return ServiceLoader.load(StorageFactory.class).stream()
                .map(ServiceLoader.Provider::get)
                .peek(factory -> log.info("Discovered {} factory", factory.getStorageType()))
                .collect(Collectors.toMap(StorageFactory::getStorageType, factory -> factory));
    }
}
