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

import dk.kb.present.transform.DSTransformer;
import dk.kb.present.webservice.exception.NotFoundServiceException;
import dk.kb.present.webservice.exception.ServiceException;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Encapsulation of backend and transformations for a collection.
 */
public class DSCollection {
    private static final Logger log = LoggerFactory.getLogger(DSCollection.class);
    private static final String PREFIX_KEY = "prefix"; // IDs for this collection starts with <prefix>_ (note the underscore)
    private static final String DESCRIPTION_KEY = "description";
    private static final String STORAGE_KEY = "storage";
    private static final String VIEWS_KEY = "views";

    private final String id;
    private final String prefix;
    private final String description;
    private final DSStorage storage;
    private final Map<String, View> views;

    public DSCollection(YAML conf, StorageHandler storageHandler) {
        // TODO: Derive id properly
        id = "collection_" + new Random().nextInt(Integer.MAX_VALUE);
        prefix = conf.getString(PREFIX_KEY);
        description = conf.getString(DESCRIPTION_KEY, null);
        storage = storageHandler.getStorage(conf.getString(STORAGE_KEY, null)); // null means default storage
        views = conf.getYAMLList(VIEWS_KEY).stream()
                .map(View::new)
                .collect(Collectors.toMap(View::getId, view -> view));
        log.info("Created " + this);
    }

    public String getRecord(String recordID, String format) throws ServiceException {
        View view = views.get(format);
        if (view == null) {
            throw new NotFoundServiceException(
                    "The format '" + format + "' is unsupported for collection '" + id + "'");
        }
        return view.apply(storage.getRecord(recordID));
    }

    public String getId() {
        return id;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, View> getViews() {
        return views;
    }

    @Override
    public String toString() {
        return "DSCollection(" +
               "id='" + id + '\'' +
               ", prefix='" + prefix + '\'' +
               ", description='" + description + '\'' +
               ", storage=" + storage +
               ", views=" + views +
               ')';
    }
}
