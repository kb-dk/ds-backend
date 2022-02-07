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

import dk.kb.present.storage.Storage;
import dk.kb.present.webservice.exception.NotFoundServiceException;
import dk.kb.present.webservice.exception.ServiceException;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A collection encapsulates access to a logical collection ("samling"). It uses the same collections as ds-storage and
 * is typically backed by a ds-storage instance.
 *
 * Access is read-only and always with an explicit export format. The format can be {@code raw} for direct proxying to
 * the connected ds-storage, but common use case is to request MODS, JSON-LD (schema.org) or SolrJSON representations.
 */
public class DSCollection {
    private static final Logger log = LoggerFactory.getLogger(DSCollection.class);
    private static final String PREFIX_KEY = "prefix"; // IDs for this collection starts with <prefix>_ (note the underscore)
    private static final String DESCRIPTION_KEY = "description";
    private static final String STORAGE_KEY = "storage";
    private static final String VIEWS_KEY = "views";

    /**
     * The ID of the collection, primarily used for debugging and configuration.
     */
    private final String id;

    /**
     * recordIDs always starts with the collection followed by underscore, e.g. {@code images-dsfl_internalid1234}.
     * The prefix must be present for all recordIDs used for lookup.
     * This might be the same as the {@link #id} but it is not a requirement.
     */
    private final String prefix;

    /**
     * Human readable description of the collection.
     */
    private final String description;

    /**
     * Encapsulation of the backing storage for the collection. This will typically be a ds-storage service.
     */
    private final Storage storage;

    /**
     * Map from format -> view. A view is at the core an array of transformations and responsible for transforming
     * metadata to the requested format.
     */
    private final Map<String, View> views;

    /**
     * Create a collection based on the given conf. The storageHandler is expected to be initialized and should contain
     * the storage specified for the collection.
     * @param conf configuration for the collection, should contain a single key:value with the key being the
     *             collection ID and the value being the configuration for the collection.
     * @param storageHandler previously initialized pool of storages.
     */
    public DSCollection(YAML conf, StorageHandler storageHandler) {

        id = conf.keySet().stream().findFirst().orElseThrow();
        conf = conf.getSubMap(id); // There must be some properties for a storage
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
        String record;
        try {
            record = storage.getRecord(recordID);
        } catch (IOException e) {
            throw new NotFoundServiceException("Unable to locate record '" + recordID + "' in collection '" + id + "'");
        }
        return view.apply(record);
    }

    /**
     * @return the ID of the collection, primarily used for debugging and configuration.
     */
    public String getId() {
        return id;
    }

    /**
     * recordIDs always starts with the collection followed by underscore, e.g. {@code images-dsfl_internalid1234}.
     * The prefix must be present for all recordIDs used for lookup.
     * This might be the same as the {@link #id} but it is not a requirement.
     * @return the prefix for recordIDs in the collection.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @return human readable description of the collection.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Map from format -> view. A view is at the core an array of transformations and responsible for transforming
     * metadata to the requested format.
     * @return the available views (aka formats) for this collection.
     */
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
