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
import dk.kb.present.config.DsStorageBackendConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constructs {@link DSStorage}s.
 */
public class DSStorageFactory implements StorageFactory {
    private static final Logger log = LoggerFactory.getLogger(DSStorageFactory.class);

    @Override
    public String getStorageType() {
        return DSStorage.TYPE;
    }

    @Override
    public Storage createStorage(String id, BackendConfig conf, boolean isDefault) throws Exception {
        DsStorageBackendConfig c = (DsStorageBackendConfig) conf;
        if (c.getOrigin() == null) {
            log.warn("For the DSStorage '" + id + "', the origin==null, calls to getRecords will fail");
        }
        return new DSStorage(id, c.getOrigin(), c.getUrl(), c.getBatchCount(), isDefault);
    }
}
