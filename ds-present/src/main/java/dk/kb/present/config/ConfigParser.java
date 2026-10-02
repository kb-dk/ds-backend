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
import dk.kb.util.yaml.NotFoundException;
import dk.kb.util.yaml.YAML;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses the {@code origins}/{@code storages}/{@code views}/{@code transformers} section of the nested
 * {@link YAML} configuration tree into the plain Java DTOs in this package ({@link OriginConfig},
 * {@link StorageConfig}, {@link ViewConfig}, {@link TransformerConfig}, {@link BackendConfig}, ...).
 * <p>
 * This is the only class in ds-present (besides {@link ServiceConfig} itself, which hands it the raw tree) that
 * walks the nested YAML structure. {@link ServiceConfig} is left with only configuration loading/initialization
 * and the simple, flat, scalar property getters - all "cutting" of the nested tree into typed objects lives here.
 */
final class ConfigParser {
    private ConfigParser() { }

    private static final String ORIGIN_ID_KEY = "origin";
    private static final String PREFIX_KEY = "prefix";
    private static final String DESCRIPTION_KEY = "description";
    private static final String STORAGE_KEY = "storage";
    private static final String RECORDREQUESTTYPE_KEY = "recordRequestType";
    private static final String VIEWS_KEY = "views";

    private static final String MIME_KEY = "mime";
    private static final String STRATEGY_KEY = "strategy";
    private static final String STRATEGY_DEFAULT = "NONE";
    private static final String TRANSFORMERS_KEY = "transformers";

    private static final String XSLT_STYLESHEET_KEY = "stylesheet";
    private static final String XSLT_INJECTIONS_KEY = "injections";
    private static final String REPLACE_REGEXP_KEY = "regexp";
    private static final String REPLACE_REPLACEMENT_KEY = "replacement";
    private static final String REPLACE_REPLACEALL_KEY = "replaceall";
    private static final boolean REPLACE_REPLACEALL_DEFAULT = true;
    private static final String FAIL_MESSAGE_KEY = "message";
    private static final String FAIL_TRANSFORMER_MESSAGE_DEFAULT = "This view always fails";
    private static final String IDENTITY_TYPE = "identity";
    private static final String IMAGERIGHTS_TYPE = "imagerights";
    private static final String XSLT_TYPE = "xslt";
    private static final String XSLTSOLR_TYPE = "xsltsolr";
    private static final String REPLACE_TYPE = "replace";
    private static final String FAIL_TYPE = "fail";

    private static final String STORAGES_KEY = ".storages";
    private static final String ORIGINS_KEY = ".origins";
    private static final String STORAGE_DEFAULT_KEY = "default";
    private static final String STORAGE_ORDER_KEY = "order";
    private static final String BACKENDS_KEY = "backends";

    private static final String DS_STORAGE_TYPE = "ds-storage";
    private static final String DS_STORAGE_ORIGIN_KEY = "origin";
    private static final String DS_STORAGE_URL_KEY = "url";
    private static final String DS_STORAGE_BATCH_COUNT_KEY = "batch.count";
    private static final int DS_STORAGE_BATCH_COUNT_DEFAULT = 100;

    private static final String FOLDER_TYPE = "folder";
    private static final String FOLDER_ROOT_KEY = "root";
    private static final String FOLDER_EXTENSION_KEY = "extension";
    private static final String FOLDER_EXTENSION_DEFAULT = ""; // All extensions
    private static final String FOLDER_STRIP_PREFIX_KEY = "prefix.strip";
    private static final boolean FOLDER_STRIP_PREFIX_DEFAULT = true;
    private static final String FOLDER_WHITELIST_KEY = "whitelist";
    private static final String FOLDER_BLACKLIST_KEY = "blacklist";

    private static final String FAIL_BACKEND_MESSAGE_DEFAULT = "No records can be delivered from this Storage";

    /**
     * @param config the full, nested configuration tree (see {@link ServiceConfig#getConfig()}).
     * @return the configured origins, in the order they are defined in the configuration.
     */
    static List<OriginConfig> parseOrigins(YAML config) {
        List<OriginConfig> origins = new ArrayList<>();
        for (YAML originYAML : config.getYAMLList(ORIGINS_KEY)) {
            origins.add(parseOrigin(originYAML));
        }
        return origins;
    }

    /**
     * @param config the full, nested configuration tree (see {@link ServiceConfig#getConfig()}).
     * @return the configured storages, in the order they are defined in the configuration.
     */
    static List<StorageConfig> parseStorages(YAML config) {
        List<StorageConfig> storages = new ArrayList<>();
        for (YAML storageYAML : config.getYAMLList(STORAGES_KEY)) {
            storages.add(parseStorage(storageYAML));
        }
        return storages;
    }

    /**
     * Parses a single entry under {@code origins}: a single-key map where the key is the origin ID.
     *
     * @param conf the single-entry YAML map for one origin.
     * @return the parsed origin.
     */
    private static OriginConfig parseOrigin(YAML conf) {
        String id = conf.keySet().stream().findFirst().orElseThrow();
        try {
            // When YAML keys contain YAML syntax they need to be encapsulated in quotation marks.
            // This should probably be handled in the YAML util class.
            YAML sub = conf.getSubMap("\"" + id + "\""); // There must be some properties for an origin
            String origin = sub.getString(ORIGIN_ID_KEY);
            String prefix = sub.getString(PREFIX_KEY);
            String description = sub.getString(DESCRIPTION_KEY, null);
            String recordRequestType = sub.getString(RECORDREQUESTTYPE_KEY);
            String storage = sub.getString(STORAGE_KEY, null); // null means default storage

            List<ViewConfig> views = new ArrayList<>();
            for (YAML viewYAML : sub.getYAMLList(VIEWS_KEY)) {
                views.add(parseView(viewYAML));
            }
            return new OriginConfig(id, prefix, origin, description, recordRequestType, storage, views);
        } catch (NotFoundException e) {
            throw new IllegalArgumentException(
                    "Mandatory property '" + e.getPath() + "' not present for Origin '" + id + "'");
        }
    }

    /**
     * Parses a single entry under {@code views}: a single-key map where the key is the view ID.
     *
     * @param conf the single-entry YAML map for one view.
     * @return the parsed view.
     */
    private static ViewConfig parseView(YAML conf) {
        if (conf.size() != 1) {
            throw new IllegalArgumentException(
                    "Expected a single entry in the configuration but there was " + conf.size() +
                    ". Maybe indenting was not correct in the config file?");
        }
        String id = conf.keySet().stream().findFirst().orElseThrow();
        YAML sub = conf.getSubMap(id);
        String mime = sub.getString(MIME_KEY);
        String strategy = sub.getString(STRATEGY_KEY, STRATEGY_DEFAULT);

        List<TransformerConfig> transformers = new ArrayList<>();
        for (YAML transformerYAML : sub.getYAMLList(TRANSFORMERS_KEY)) {
            transformers.add(parseTransformer(transformerYAML));
        }
        return new ViewConfig(id, mime, strategy, transformers);
    }

    /**
     * Parses a single entry under {@code transformers}: a single-key map where the key is the transformer type.
     *
     * @param conf the single-entry YAML map for one transformer.
     * @return the parsed transformer configuration, as the concrete {@link TransformerConfig} implementation
     * matching the transformer type.
     */
    private static TransformerConfig parseTransformer(YAML conf) {
        if (conf.size() != 1) {
            throw new IllegalArgumentException(
                    "Expected a single entry in the configuration but there was " + conf.size() +
                    ". Maybe indenting was not correct in the config file?");
        }
        String type = conf.keySet().stream().findFirst().orElseThrow();
        YAML sub = conf.containsKey(type) ? conf.getSubMap(type) : new YAML(); // Some transformers have no config

        switch (type) {
            case XSLT_TYPE:
            case XSLTSOLR_TYPE:
                assertKeys(sub, type, XSLT_STYLESHEET_KEY);
                Map<String, String> injections = null;
                if (sub.containsKey(XSLT_INJECTIONS_KEY)) {
                    injections = new HashMap<>();
                    for (YAML yInjection : sub.getYAMLList(XSLT_INJECTIONS_KEY)) {
                        if (yInjection.size() != 1) {
                            throw new IllegalArgumentException(
                                    "Expected a single entry (key-value pair) in injection '" + yInjection +
                                    "' but got " + yInjection.size());
                        }
                        // TODO: Move away from the strange "listed maps with one entry" way of stating injections
                        String firstKey = yInjection.keySet().stream().findFirst().get();
                        injections.put(firstKey, yInjection.getString(firstKey));
                    }
                }
                return new XsltConfig(type, sub.getString(XSLT_STYLESHEET_KEY), injections);
            case REPLACE_TYPE:
                assertKeys(sub, type, REPLACE_REGEXP_KEY, REPLACE_REPLACEMENT_KEY);
                return new ReplaceConfig(
                        sub.getString(REPLACE_REGEXP_KEY),
                        sub.getString(REPLACE_REPLACEMENT_KEY),
                        sub.getBoolean(REPLACE_REPLACEALL_KEY, REPLACE_REPLACEALL_DEFAULT));
            case FAIL_TYPE:
                return new FailTransformerConfig(sub.getString(FAIL_MESSAGE_KEY, FAIL_TRANSFORMER_MESSAGE_DEFAULT));
            case IDENTITY_TYPE:
            case IMAGERIGHTS_TYPE:
                return new EmptyTransformerConfig(type);
            default:
                // Unknown transformer types are passed through as a message-only config; TransformerController
                // will fail with a clear "factory not available" error when it tries to dispatch on the type.
                return new EmptyTransformerConfig(type);
        }
    }

    /**
     * Parses a single entry under {@code storages}: a single-key map where the key is the storage ID.
     *
     * @param conf the single-entry YAML map for one storage.
     * @return the parsed storage.
     */
    private static StorageConfig parseStorage(YAML conf) {
        if (conf.size() != 1) {
            throw new IllegalArgumentException(
                    "Expected a configuration with a single key/value, where the key is storage ID and the value " +
                    "is the configuration for that storage");
        }
        String id = conf.keySet().stream().findFirst().orElseThrow();
        try {
            YAML sub = conf.getSubMap(id); // There must be some properties for a storage

            boolean isDefault = sub.getBoolean(STORAGE_DEFAULT_KEY, false);
            MultiStorage.ORDER order = MultiStorage.ORDER.valueOf(
                    sub.getString(STORAGE_ORDER_KEY, MultiStorage.ORDER.getDefault().toString()));

            List<YAML> backendsYAML = sub.getYAMLList(BACKENDS_KEY);
            if (backendsYAML.isEmpty()) {
                throw new IllegalArgumentException("No backends defined for storage " + id);
            }
            List<BackendConfig> backends = new ArrayList<>(backendsYAML.size());
            for (YAML backendYAML : backendsYAML) {
                String backendType = backendYAML.keySet().stream().findFirst().orElseThrow();
                YAML backendConf = backendYAML.containsKey(backendType) ?
                        backendYAML.getSubMap(backendType) : new YAML(); // Some backends have no config
                backends.add(parseBackend(backendType, backendConf));
            }
            return new StorageConfig(id, isDefault, order, backends);
        } catch (NotFoundException e) {
            throw new IllegalArgumentException(
                    "Mandatory property '" + e.getPath() + "' not present for Storage '" + id + "'");
        }
    }

    /**
     * Parses the type-specific configuration for a single entry under {@code backends}.
     *
     * @param type the backend type, e.g. {@code folder}.
     * @param conf the backend-specific configuration.
     * @return the parsed backend configuration, as the concrete {@link BackendConfig} implementation matching the
     * backend type.
     */
    private static BackendConfig parseBackend(String type, YAML conf) {
        switch (type) {
            case DS_STORAGE_TYPE:
                String origin = conf.getString(DS_STORAGE_ORIGIN_KEY, null);
                String url = conf.getString(DS_STORAGE_URL_KEY);
                int batchCount = conf.getInteger(DS_STORAGE_BATCH_COUNT_KEY, DS_STORAGE_BATCH_COUNT_DEFAULT);
                return new DsStorageBackendConfig(origin, url, batchCount);
            case FOLDER_TYPE:
                String root = conf.getString(FOLDER_ROOT_KEY);
                String extension = conf.getString(FOLDER_EXTENSION_KEY, FOLDER_EXTENSION_DEFAULT);
                boolean stripPrefix = conf.getBoolean(FOLDER_STRIP_PREFIX_KEY, FOLDER_STRIP_PREFIX_DEFAULT);
                List<String> whitelist = conf.getList(FOLDER_WHITELIST_KEY, null);
                List<String> blacklist = conf.getList(FOLDER_BLACKLIST_KEY, null);
                return new FolderBackendConfig(root, extension, stripPrefix, whitelist, blacklist);
            case FAIL_TYPE:
                return new FailBackendConfig(conf.getString(FAIL_MESSAGE_KEY, FAIL_BACKEND_MESSAGE_DEFAULT));
            default:
                // Unknown backend type: carry the real (unmatched) type string straight through rather than
                // hardcoding one of the known types above, so that StorageController's factory lookup fails with
                // a clear "factory not available" error citing the actual configured type - exactly as before.
                return new BackendConfig() {
                    @Override
                    public String getType() {
                        return type;
                    }

                    @Override
                    public String toString() {
                        return "UnrecognizedBackendConfig(type='" + type + "')";
                    }
                };
        }
    }

    /**
     * Verifies that the given keys are all present in {@code config}, throwing a descriptive exception naming the
     * transformer type otherwise.
     *
     * @param config configuration for the concrete transformer.
     * @param transformerType the transformer type, used only for the exception message.
     * @param requiredKeys 0 or more keys that must be present in the configuration.
     */
    private static void assertKeys(YAML config, String transformerType, String... requiredKeys) {
        for (String requiredKey : requiredKeys) {
            if (!config.containsKey(requiredKey)) {
                throw new IllegalArgumentException(
                        "Expected the property '" + requiredKey + "' to be present in the configuration for " +
                        "transformer '" + transformerType + "'. The complete list of mandatory properties is " +
                        java.util.Arrays.toString(requiredKeys));
            }
        }
    }
}
