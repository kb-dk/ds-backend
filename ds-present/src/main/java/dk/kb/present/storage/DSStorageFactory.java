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

/**
 * Constructs {@link DSStorage}s.
 */
public class DSStorageFactory implements StorageFactory {
    private static final Logger log = LoggerFactory.getLogger(DSStorageFactory.class);

    private static final String RECORD_BASE_KEY = "base";
    private static final String DBSERVERURL_KEY = "dbserverurl";
    public static final String BATCH_COUNT_KEY = "batch.count";
    public static final int BATCH_COUNT_DEFAULT = 100;

    @Override
    public String getStorageType() {
        return DSStorage.TYPE;
    }

    @Override
    public Storage createStorage(String id, YAML conf, boolean isDefault) throws Exception {
        String origin = conf.getString(RECORD_BASE_KEY, null);
        if (origin == null) {
            log.warn("For the DSStorage '" + id + "', the origin==null, calls to getRecords will fail");
        }
        String  dbServerUrl= conf.getString(DBSERVERURL_KEY);
        
        int batchCount = conf.getInteger(BATCH_COUNT_KEY, BATCH_COUNT_DEFAULT);

        return new DSStorage(id, origin, dbServerUrl, batchCount, isDefault);
    }
}
