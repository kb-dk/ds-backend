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

import java.io.IOException;

/**
 * Proxy for a ds-storage https://github.com/kb-dk/ds-storage instance.
 */
public class DSStorage implements Storage {
    private static final Logger log = LoggerFactory.getLogger(DSStorage.class);

    public static final String TYPE = "ds-storage";
    private static final String URL_KEY = "url"; // IDs for this collection starts with <prefix>_ (note the underscore)

    private final String id;
    private final String url;
    private final boolean isDefault;

    public DSStorage(String id, YAML conf, boolean isDefault) {
        this.id = id;
        this.url = conf.getString(URL_KEY);
        this.isDefault = isDefault;
        log.info("Created " + this);
    }

    @Override
    public String getRecord(String id) throws IOException {
        // TODO: Create a client for ds-storage and use that
        throw new UnsupportedOperationException("Not implemented yet!");
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
               ", url='" + url + '\'' +
               ", isDefault=" + isDefault +
               ')';
    }
}
