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

import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Encapsulation of a connection to a https://github.com/kb-dk/ds-storage
 */
public class DSStorage {
    private static final Logger log = LoggerFactory.getLogger(DSStorage.class);
    private static final String URL_KEY = "url"; // IDs for this collection starts with <prefix>_ (note the underscore)
    private static final String DEFAULT_KEY = "default";

    private final String id;
    private final String url;
    private final boolean isDefault;

    /**
     * @param conf the full configuration.
     */
    public DSStorage(YAML conf) {
        this.url = conf.getString(URL_KEY);
        isDefault = conf.getBoolean(DEFAULT_KEY, false);
        // TODO: Create a concrete client here
        // TODO: Extract name from the conf
        id = "storage_" + new Random().nextInt(Integer.MAX_VALUE);
        log.info("Created " + this);
    }

    public String getRecord(String id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public boolean isDefault() {
        return isDefault;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "DSStorage(" +
               "id='" + id + '\'' +
               ", url='" + url + '\'' +
               ", isDefault=" + isDefault +
               ')';
    }
}
