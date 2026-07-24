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

/**
 * Factory for creating a specific type of {@link Storage}.
 *
 * The class primarily exists as a mechanism for the ServiceLoader to discover storages.
 *
 * IMPORTANT:
 * Implementations of StorageFactory cannot be inner classes due to the way ServiceLoader works.
 * Implementations must be registered in src/main/resources/META-INF/services/dk.kb.present.storage.StorageFactory.
 */
public interface StorageFactory {

    /**
     * @return the type of the storage that this factory creates, e.g. {@code ds-storage}.
     */
    String getStorageType();

    /**
     * Create a new storage of the supported type and return it.
     * @param id the ID for this storage, as specified in the configuration.
     * @param conf configuration for the {@link Storage} to create.
     * @param isDefault whether or not the storage is the default storage.
     * @return a {@link Storage} of the supported type with the given configuration;
     * @throws Exception if the storage could not be created.
     */
    Storage createStorage(String id, YAML conf, boolean isDefault) throws Exception;
}
