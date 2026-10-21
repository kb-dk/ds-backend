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
package dk.kb.present.config;

import dk.kb.present.storage.MultiStorage;

import java.util.List;

/**
 * Configuration for a single entry under {@code storages}, consumed by
 * {@link dk.kb.present.StorageHandler}/{@link dk.kb.present.storage.StorageController}. A storage can have multiple
 * backends, in which case they are combined into a {@link MultiStorage} using {@link #getOrder()}.
 */
public class StorageConfig {
    private final String id;
    private final boolean isDefault;
    private final MultiStorage.ORDER order;
    private final List<BackendConfig> backends;

    public StorageConfig(String id, boolean isDefault, MultiStorage.ORDER order, List<BackendConfig> backends) {
        this.id = id;
        this.isDefault = isDefault;
        this.order = order;
        this.backends = backends;
    }

    public String getId() {
        return id;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public MultiStorage.ORDER getOrder() {
        return order;
    }

    public List<BackendConfig> getBackends() {
        return backends;
    }

    @Override
    public String toString() {
        return "StorageConfig(id='" + id + "', isDefault=" + isDefault + ", order=" + order +
               ", backends=" + backends + ')';
    }
}
