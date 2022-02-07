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

import dk.kb.present.backend.api.v1.DsStorageApi;
import dk.kb.present.backend.invoker.v1.ApiClient;
import dk.kb.present.backend.invoker.v1.ApiException;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Proxy for a ds-storage https://github.com/kb-dk/ds-storage instance.
 */
public class DSStorage implements Storage {
    private static final Logger log = LoggerFactory.getLogger(DSStorage.class);

    public static final String TYPE = "ds-storage";
    private static final String HOST_KEY = "host";
    private static final String PORT_KEY = "port";
    private static final String BASEPATH_KEY = "basepath";
    private static final String BASEPATH_DEFAULT = "ds-storage/v1/";
    private static final String SCHEME_KEY = "scheme";
    private static final String SCHEME_DEFAULT = "https"; // Special handling: Will be 'http' if host = localhost

    private final String id;

    private final String host;
    private final int port;
    private final String basepath;
    private final String scheme;

    private final boolean isDefault;
    private final DsStorageApi dsStorageClient;

    public DSStorage(String id, YAML conf, boolean isDefault) {
        this.id = id;

        this.host = conf.getString(HOST_KEY);
        this.port = conf.getInteger(PORT_KEY);
        this.basepath = conf.getString(BASEPATH_KEY,
                                       BASEPATH_DEFAULT);
        this.scheme = conf.getString(SCHEME_KEY,
                                     "localhost".equals(host) || "127.0.0.1".equals(host) ? "http" : SCHEME_DEFAULT);

        this.isDefault = isDefault;

        ApiClient apiClient = new ApiClient();
        apiClient.setHost(host);
        apiClient.setPort(port);
        apiClient.setBasePath(basepath);
        apiClient.setScheme(scheme);

        dsStorageClient = new DsStorageApi(apiClient);
        log.info("Created " + this);
    }

    @Override
    public String getRecord(String id) throws IOException {
        try {
            return dsStorageClient.getRecord(id).getData();
        } catch (ApiException e) {
            throw new IOException("Unable to retrieve record '" + id + "'", e);
        }
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public String toString() {
        return "DSStorage(" +
               "id='" + id + '\'' +
               ", host='" + host + '\'' +
               ", port=" + port +
               ", basepath='" + basepath + '\'' +
               ", scheme='" + scheme + '\'' +
               ", isDefault=" + isDefault +
               ')';
    }
}
