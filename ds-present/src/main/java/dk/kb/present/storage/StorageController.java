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

import dk.kb.util.yaml.YAML;
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

    public static final String DEFAULT_KEY = "default";
    public static final String ORDER_KEY = "order";
    public static final String BACKENDS_KEY = "backends";

    private static final Map<String, StorageFactory> factories = getFactories();


    /**
     * Create a storage with the given configuration, there the configuration contains a single key which is the
     * storage ID, with the value being the configuration to use for the storage.
     *
     * If the config contains multiple backends, those are wrapped in a {@link MultiStorage}.
     * @return a configured storage for the given ID, ready for use.
     * @throws NullPointerException if no storage with the given id could be located.
     * @throws Exception if the storage could not be created.
     */
    //     - test:
    //        default: true
    //        order: sequential
    //        backends:
    //          - folder:
    //              root: '/use/only/for/test/purposes'
    //          - folder:
    //              root: '/some/other/path'
    public static Storage createStorage(YAML conf) throws Exception {
        if (conf.size() != 1) {
            throw new IllegalArgumentException(
                    "Expected a configuration with a single key/value, where the key is storage ID and the value is " +
                    "the configuration for that storage");
        }
        String mainID = conf.keySet().stream().findFirst().orElseThrow();
        conf = conf.getSubMap(mainID); // There must be some properties for a storage
        //        default: true
        //        order: sequential
        //        backends:
        //          - folder:
        //              root: '/use/only/for/test/purposes'
        //          - folder:
        //              root: '/some/other/path'
        boolean isDefault = conf.getBoolean(DEFAULT_KEY, false); // TODO: Enable this
        MultiStorage.ORDER order =
                MultiStorage.ORDER.valueOf(conf.getString(ORDER_KEY, MultiStorage.ORDER.getDefault().toString()));
        List<YAML> backendsYAML = conf.getYAMLList(BACKENDS_KEY);
        if (backendsYAML.isEmpty()) {
            throw new IllegalArgumentException("No backends defined for storage " + mainID);
        }
        List<Storage> storages = new ArrayList<>(backendsYAML.size());
        //          - folder:
        //              root: '/use/only/for/test/purposes'
        //          - folder:
        //              root: '/some/other/path'
        for (YAML subStorage: backendsYAML) {
            //          - folder:
            //              root: '/use/only/for/test/purposes'
            String subStorageType = subStorage.keySet().stream().findFirst().orElseThrow();
            YAML subStorageConf = subStorage.containsKey(subStorageType) ? subStorage.getSubMap(subStorageType) : new YAML(); // Some storages might not have a config
            storages.add(createStorage(subStorageType, mainID, subStorageConf, isDefault));
        }
        return storages.size() > 1 ? new MultiStorage(mainID, storages, order, isDefault) : storages.get(0);
    }

    /**
     * Create a storage with the given configuration.
     * @param storageType the ID of the storage to create. Call {@link #getSupportedStorageIDs()} for a complete list.
     * @param storageID the ID for the storage, as specified in the configuration.
     * @param conf storage specific configuration.
     * @return a configured storage for the given ID, ready for use.
     * @throws NullPointerException if no storage with the given id could be located.
     * @throws Exception if the storage could not be created.
     */
    public static Storage createStorage(String storageType, String storageID, YAML conf, boolean isDefault) throws Exception {
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
     * @return map of [storageID, storage].
     */
    private static Map<String, StorageFactory> getFactories() {
        return ServiceLoader.load(StorageFactory.class).stream()
                .map(ServiceLoader.Provider::get)
                .peek(factory -> log.info("Discovered {} factory", factory.getStorageType()))
                .collect(Collectors.toMap(StorageFactory::getStorageType, factory -> factory));
    }

}
