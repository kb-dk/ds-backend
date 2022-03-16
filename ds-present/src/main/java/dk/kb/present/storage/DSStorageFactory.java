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
    private static final String HOST_KEY = "host";
    private static final String PORT_KEY = "port";
    private static final String BASEPATH_KEY = "basepath";
    private static final String BASEPATH_DEFAULT = "ds-storage/v1/";
    private static final String SCHEME_KEY = "scheme";
    private static final String SCHEME_DEFAULT = "https"; // Special handling: Will be 'http' if host = localhost
    public static final String BATCH_COUNT_KEY = "batch.count";
    public static final int BATCH_COUNT_DEFAULT = 100;

    @Override
    public String getStorageType() {
        return DSStorage.TYPE;
    }

    @Override
    public Storage createStorage(String id, YAML conf, boolean isDefault) throws Exception {
        String recordBase = conf.getString(RECORD_BASE_KEY, null);
        if (recordBase == null) {
            log.warn("For the DSStorage '" + id + "', the recordBase==null, calls to getRecords will fail");
        }
        String host = conf.getString(HOST_KEY);
        int port = conf.getInteger(PORT_KEY);
        String basepath = conf.getString(BASEPATH_KEY, BASEPATH_DEFAULT);
        String scheme = conf.getString(SCHEME_KEY,
                                       "localhost".equals(host) || "127.0.0.1".equals(host) ? "http" : SCHEME_DEFAULT);
        int batchCount = conf.getInteger(BATCH_COUNT_KEY, BATCH_COUNT_DEFAULT);

        return new DSStorage(id, recordBase, scheme, host, port, basepath, batchCount, isDefault);
    }
}
