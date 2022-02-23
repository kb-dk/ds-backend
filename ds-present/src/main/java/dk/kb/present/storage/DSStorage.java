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

    private final String id;

    private final String host;
    private final int port;
    private final String basepath;
    private final String scheme;

    // Used for logging and debugging
    private final String serverHuman;

    private final boolean isDefault;
    private final DsStorageApi dsStorageClient;

    /**
     * Create a Storage connection to a ds-storage server.
     * @param id the ID for the storage, used for connecting collections to storages.
     * @param scheme the scheme for the connection: {@code http} or {@code https}.
     * @param host the host name for the server: {@code example.com}.
     * @param port the port for the server: {@code 8080}.
     * @param basepath the basepath for the service: {@code /ds-storage/v1/}.
     * @param isDefault if true, this is the default storage for collections.
     */
    public DSStorage(String id, String scheme, String host, int port, String basepath, boolean isDefault) {
        this.id = id;

        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.basepath = basepath;

        this.isDefault = isDefault;

        ApiClient apiClient = new ApiClient();
        apiClient.setHost(host);
        apiClient.setPort(port);
        apiClient.setBasePath(basepath);
        apiClient.setScheme(scheme);

        serverHuman = scheme + "://" + host + ":" + port + "/" + basepath;

        dsStorageClient = new DsStorageApi(apiClient);
        log.info("Created " + this);
    }

    @Override
    public String getRecord(String id) throws IOException {
        try {
            return dsStorageClient.getRecord(id).getData();
        } catch (ApiException e) {
            log.debug("Unable to retrieve record '" + id + "' from " + serverHuman + "...", e);
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
