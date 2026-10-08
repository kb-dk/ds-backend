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

import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration class for ds-present.
 * <p>
 * Unlike the other {@code ds-backend} services migrated to <a href="https://smallrye.io/smallrye-config/">SmallRye
 * Config</a> (a standalone MicroProfile Config implementation; this project does not use Quarkus), ds-present keeps
 * <em>two</em> parallel representations of its configuration, because a large part of its configuration - the
 * {@code origins}/{@code storages}/{@code views}/{@code transformers} object graph - has a shape (nested, with
 * data-driven cardinality) that SmallRye Config's flat property model has no direct equivalent for.
 * <ul>
 *     <li>{@link #getYamlConfig()} - the original kb-util {@link YAML} tree, produced by
 *     {@link YAML#resolveLayeredConfigs(String...)}, including the multi-file glob merge ({@code
 *     ds-present-behaviour.yaml} + {@code ds-present-kb-origins.yaml} + an optional environment-specific file,
 *     merged in alphanumeric order - see {@code conf/ocp/ds-present.xml}) and the {@code ${path:...}}
 *     self-referencing extrapolation syntax. This is internal to {@code ServiceConfig} - no other class in
 *     ds-present should need it (a few tests that exercise generic YAML/library behaviour using fixtures unrelated
 *     to ds-present's own configuration schema are the only remaining exceptions).</li>
 *     <li>{@link #getOrigins()}, {@link #getStorages()} and the other typed getters below - plain Java DTOs (see
 *     the {@code dk.kb.present.config} package) built on demand from the tree above by {@link ConfigParser}, the
 *     only other class that walks the nested {@link YAML} tree (it never touches {@link #getYamlConfig()} itself -
 *     {@code ServiceConfig} hands it the already-loaded tree). This is how every other class in ds-present (and
 *     its tests) should read the {@code origins}/{@code storages}/{@code views}/{@code transformers} sections and
 *     every scalar setting - outside of {@code ServiceConfig} and {@link ConfigParser}, no class in ds-present
 *     knows about {@link YAML} or MicroProfile {@link Config}.</li>
 *     <li>{@link #getConfig()} - a MicroProfile {@link Config}, named to match every other module's {@code
 *     ServiceConfig.getConfig()} so that code which needs a plain {@link Config} (the OpenAPI endpoint, {@code
 *     KBOAuth2Handler}) can be written identically across every module - see those classes. It also gives every
 *     scalar setting the same environment variable/system property/devops properties override file layering the
 *     other services have. It is built by flattening the fully merged {@link YAML} tree above into a property map
 *     (see {@link #flatten(YAML, String, Map)}) <em>before</em> that tree is extrapolated (see
 *     {@link #initializeWithPropertiesOverride(String, String)} for why the order matters) and layering it under
 *     runtime injection, system properties, environment variables, an optional {@code .env} file, and the optional
 *     devops/operations properties override file.</li>
 * </ul>
 * See the ds-storage module's {@code SMALLRYE_CONFIG_MIGRATION.md} for the general single-YAML-file pattern this
 * follows for every other service, and for the devops/operations properties override file mechanism reused here.
 */
public class ServiceConfig {
    private static final Logger log = LoggerFactory.getLogger(ServiceConfig.class);

    /**
     * Ordinal for the runtime-injected overrides ({@link #setRuntimeProperty(String, String)}). This is higher
     * than system properties (400), environment variables (300) and everything below, so a runtime override
     * always wins.
     */
    public static final int RUNTIME_ORDINAL = 500;

    /**
     * Ordinal for the explicit, per-service devops/operations properties override file (see
     * {@link #initializeWithPropertiesOverride(String, String)}). This is deliberately above SmallRye's own
     * implicit {@code config/application.properties} convention (ordinal 260, part of {@code addDefaultSources()}),
     * so that if a file is ever accidentally left at that shared location in a multi-webapp Tomcat instance, it can
     * never silently outrank the correct, explicitly-configured file for this service.
     */
    private static final int PROPERTIES_OVERRIDE_ORDINAL = 270;

    /**
     * Ordinal for the flattened YAML tree (see {@link #flatten(YAML, String, Map)}) backing {@link
     * #getConfig()}. This is comfortably below environment variables (300) and system properties (400), so
     * operations can override any individual configured value without editing YAML at all.
     */
    private static final int YAML_ORDINAL = 100;

    private static final RuntimeConfigSource runtimeSource = new RuntimeConfigSource(RUNTIME_ORDINAL);

    /** The nested YAML tree. See the class javadoc. */
    private static YAML serviceConfig;

    /** The flattened, MicroProfile-Config-backed view of the same tree. See the class javadoc. */
    private static SmallRyeConfig flatConfig;

    /**
     * Initializes the configuration from the provided configFiles, without a devops/operations properties override
     * file for {@link #getConfig()}. Equivalent to how this method worked before this migration: multiple
     * globs/paths are resolved and merged, in order, into a single nested {@link YAML} tree.
     * <p>
     * This overload exists mainly for tests and other callers that don't need/have an override file; production
     * start-up (see {@link dk.kb.present.webservice.ContextListener}) uses
     * {@link #initializeWithPropertiesOverride(String, String)} instead.
     *
     * @param configFiles globs/paths for the YAML configurations to load and merge, in order; see
     *                    {@link YAML#resolveLayeredConfigs(String...)}.
     * @throws IOException if the configurations could not be loaded or parsed.
     */
    public static synchronized void initialize(String... configFiles) throws IOException {
        serviceConfig = YAML.resolveLayeredConfigs(configFiles);
        rebuildFlatConfig(null);
        serviceConfig.setExtrapolate(true);
    }

    /**
     * Initializes the configuration from a single configFile (itself possibly a glob resolving to several YAML
     * files, merged as before - see {@link #initialize(String...)}) and an optional devops/operations properties
     * override file for {@link #getConfig()}.
     * <p>
     * This should normally be called from {@link dk.kb.present.webservice.ContextListener} as part of web server
     * initialization of the container, using the two paths configured outside the project (Tomcat context
     * environment entries {@code application-config} and {@code application-properties-config}, see
     * {@code conf/ocp/ds-present.xml}).
     * <p>
     * Note: this is deliberately a separate, distinctly-named method rather than a second {@code initialize}
     * overload, so that it cannot be confused with (or accidentally shadow) the multi-file glob-merge form above,
     * which several existing tests already call with two or three YAML file arguments.
     *
     * @param configFile the YAML configuration (glob or plain path) which the nested tree is loaded from; see
     *                    {@link #initialize(String...)}.
     * @param propertiesOverrideFile the devops/operations properties override file for {@link #getConfig()}
     *                    (a plain file path, a classpath resource name, or a path relative to the user's home, see
     *                    {@link Resolver#resolveURL(String)}), or {@code null}/blank if none is configured. If a
     *                    path is given but cannot be resolved to an existing file, this is logged as an error and
     *                    startup continues without that source - values that were meant to come from it (most
     *                    importantly secrets) will then be missing from {@link #getConfig()} or fall back to
     *                    the YAML file.
     * @throws IOException if the YAML configuration could not be located, loaded or parsed. A missing/unresolvable
     *                    {@code propertiesOverrideFile} does <em>not</em> throw - see above.
     */
    public static synchronized void initializeWithPropertiesOverride(String configFile, String propertiesOverrideFile)
            throws IOException {
        serviceConfig = YAML.resolveLayeredConfigs(configFile);
        // Deliberately flatten (see rebuildFlatConfig/flatten) BEFORE extrapolating serviceConfig below, not after:
        // this way, a "${keycloak_realm}"-style placeholder reaches getConfig()'s backing MapConfigSource still
        // literally unresolved, and is resolved by SmallRye Config's own ExpressionConfigSourceInterceptor (enabled
        // via addDefaultInterceptors() in rebuildFlatConfig) against the *whole* MicroProfile Config source stack -
        // including environment variables - exactly like every other module's SmallRye-native YamlConfigSource
        // does. Resolving it here instead, via kb-util YAML's own extrapolate(), would only ever check it against
        // JVM system properties for a bare (unprefixed) name - never environment variables - which is a real,
        // previously-undetected difference from every other module despite an identical YAML `security:` section.
        rebuildFlatConfig(propertiesOverrideFile);
        // Only now extrapolate the nested tree itself (kb-util's own engine), for getYamlConfig()/ConfigParser
        // consumers - e.g. the ${path:...} self-referencing syntax used in the origins/views/transformers graph.
        serviceConfig.setExtrapolate(true);
    }

    /**
     * (Re)builds {@link #flatConfig} from the current {@link #serviceConfig} tree plus the standard override
     * layers. Called from both {@link #initialize(String...)} (with no properties override file) and
     * {@link #initializeWithPropertiesOverride(String, String)}.
     *
     * @param propertiesOverrideFile see {@link #initializeWithPropertiesOverride(String, String)}, or {@code null}.
     */
    private static void rebuildFlatConfig(String propertiesOverrideFile) {
        Map<String, String> flattened = new LinkedHashMap<>();
        flatten(serviceConfig, "", flattened);

        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                // Enables ${other.property} / ${other.property:default} expression resolution.
                .addDefaultInterceptors()
                // System properties (400), environment variables (300), an optional .env file (295), an optional
                // config/application.properties (260, see ds-storage/SMALLRYE_CONFIG_MIGRATION.md for why this is
                // not what propertiesOverrideFile below uses) or classpath application.properties (250), and
                // META-INF/microprofile-config.properties (100), if present.
                .addDefaultSources()
                .withSources(runtimeSource)
                .withSources(new MapConfigSource("ds-present flattened YAML", flattened, YAML_ORDINAL));

        if (propertiesOverrideFile == null || propertiesOverrideFile.isBlank()) {
            log.info("No devops/operations properties override file configured for the flat configuration view; " +
                      "continuing with only the YAML-derived settings (plus environment variables/system " +
                      "properties/runtime injection)");
        } else {
            try {
                URL propertiesUrl = Resolver.resolveURL(propertiesOverrideFile);
                builder.withSources(new PropertiesConfigSource(propertiesUrl, PROPERTIES_OVERRIDE_ORDINAL));
                log.info("Loaded devops/operations properties override file '{}'", propertiesOverrideFile);
            } catch (IOException e) {
                // Broad IOException, not just FileNotFoundException/MalformedURLException: unlike initialize(...)
                // (which declares throws IOException for the YAML tree), this private helper must not propagate -
                // it is also called with propertiesOverrideFile=null from plain initialize(String...), where no
                // exception should ever be possible, so any failure resolving/parsing the override file here is
                // logged and swallowed instead, matching this method's "does not throw" contract (see
                // initializeWithPropertiesOverride's javadoc).
                log.error("Configured devops/operations properties override file '{}' could not be found. " +
                           "Continuing without it - values that were meant to come from it (most importantly " +
                           "secrets) will be missing from getConfig() or fall back to the YAML file.",
                           propertiesOverrideFile, e);
            }
        }

        flatConfig = builder.build();
    }

    /**
     * Flattens the given, already fully merged but <em>not yet extrapolated</em> YAML tree into a flat property
     * map, using the same indexed-list convention SmallRye Config itself uses when it flattens a YAML list (e.g.
     * {@code origins[0].ds.radio.description}), so that code which already knows how to scan a MicroProfile
     * {@link Config#getPropertyNames()} for an indexed list (see the other ds-backend services, or
     * {@link dk.kb.util.webservice.OpenApiResource}) works unchanged against {@link #getConfig()} too.
     * <p>
     * Deliberately <em>not</em> extrapolated: a value such as {@code "${keycloak_realm}"} is copied into the
     * returned map as that literal, unresolved string. {@link #rebuildFlatConfig(String)} registers this map as a
     * plain {@link MapConfigSource} and relies on SmallRye Config's own {@code ExpressionConfigSourceInterceptor}
     * (enabled via {@code addDefaultInterceptors()}) to resolve {@code ${...}} placeholders found in it against the
     * <em>whole</em> MicroProfile Config source stack - including environment variables - exactly like every other
     * module's SmallRye-native {@code YamlConfigSource} does. (The separate {@link #serviceConfig} tree is
     * extrapolated afterward, by kb-util's own engine - see {@link #initializeWithPropertiesOverride(String,
     * String)} - but that tree is never read via this method.)
     * <p>
     * This deliberately flattens the <em>whole</em> tree, including sections ({@code origins}, {@code storages},
     * ...) that {@link #getConfig()} is not actually meant to be used for in ds-present's own code (those are
     * read from the typed getters below instead, see the class javadoc). Flattening them too is a bit of unused
     * work at startup, but keeps this method simple and generic rather than hardcoding the specific set of "flat"
     * keys that happen to be read through {@link #getConfig()} today - and it is what makes the {@code
     * origins[*].*.origin} wildcard substitution in the OpenAPI specification work (see
     * {@link dk.kb.util.webservice.OpenApiResource}).
     *
     * @param yaml a YAML (sub)tree.
     * @param prefix the property-name prefix for {@code yaml} (empty for the root).
     * @param target the flat map to add {@code prefix}-relative properties to.
     */
    private static void flatten(YAML yaml, String prefix, Map<String, String> target) {
        for (String key : yaml.keySet()) {
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            try {
                Object value = yaml.get(key);
                if (value instanceof Map) {
                    flatten(yaml.getSubMap(key), path, target);
                } else if (value instanceof List) {
                    List<?> rawList = (List<?>) value;
                    if (!rawList.isEmpty() && rawList.get(0) instanceof Map) {
                        List<YAML> subList = yaml.getYAMLList(key);
                        for (int i = 0; i < subList.size(); i++) {
                            flatten(subList.get(i), path + "[" + i + "]", target);
                        }
                    } else {
                        for (int i = 0; i < rawList.size(); i++) {
                            Object element = rawList.get(i);
                            if (element != null) {
                                target.put(path + "[" + i + "]", String.valueOf(element));
                            }
                        }
                    }
                } else if (value != null) {
                    target.put(path, String.valueOf(value));
                }
            } catch (RuntimeException e) {
                log.debug("Skipping configuration key '{}' while building the flat configuration view", path, e);
            }
        }
    }

    /**
     * Direct access to the backing nested {@link YAML} tree. This is internal to {@code ServiceConfig} - use the
     * typed getters below ({@link #getOrigins()}, {@link #getStorages()}, {@link #getRecordIdPattern()}, ...)
     * instead. The only remaining external callers are tests exercising generic YAML/library behaviour with
     * fixtures unrelated to ds-present's own configuration schema.
     *
     * @return the backing YAML-handler for the configuration.
     */
    public static YAML getYamlConfig() {
        if (serviceConfig == null) {
            throw new IllegalStateException("The configuration should have been loaded, but was not");
        }
        return serviceConfig;
    }

    /**
     * Direct access to the flattened, MicroProfile-Config-backed view of the configuration. Named {@code
     * getConfig()}, not {@code getFlatConfig()}, so that code which needs a plain {@link Config} - a
     * third-party/MicroProfile-aware library (the OpenAPI endpoint, {@code KBOAuth2Handler}) - can be written
     * identically to every other module's {@code ServiceConfig.getConfig()}; everything else should use the typed
     * getters below instead, so that {@code ServiceConfig} remains the only class aware of {@link Config}/{@link
     * YAML}.
     *
     * @return the backing SmallRye Config-handler for the flattened configuration.
     */
    public static Config getConfig() {
        if (flatConfig == null) {
            throw new IllegalStateException("The configuration should have been loaded, but was not");
        }
        return flatConfig;
    }

    // -----------------------------------------------------------------------------------------------------------
    // Typed scalar getters. Each of these used to be read ad-hoc, with the key duplicated, at every call site.
    // -----------------------------------------------------------------------------------------------------------

    private static final String LICENSE_URL_KEY = "licensemodule.url";
    private static final String LICENSE_ALLOWALL_KEY = "licensemodule.allowall";
    private static final String USE_TRANSCRIPTIONS_KEY = "index.useTransriptions";
    private static final String STOP_ON_ERROR_KEY = "records.errorHandling.stop";
    private static final String RECORD_ID_PATTERN_KEY = ".record.id.pattern";
    private static final String ORIGIN_PREFIX_PATTERN_KEY = ".origin.prefix.pattern";

    /**
     * @return the URL of the ds-license instance to use, or {@code null} if not configured.
     */
    public static String getLicenseModuleUrl() {
        return getConfig().getOptionalValue(LICENSE_URL_KEY, String.class).orElse(null);
    }

    /**
     * @return whether access checking should be bypassed entirely (used for devel/test setups). Defaults to
     * {@code false}.
     */
    public static boolean getLicenseModuleAllowAll() {
        return getConfig().getOptionalValue(LICENSE_ALLOWALL_KEY, Boolean.class).orElse(false);
    }

    /**
     * @return whether transcriptions should be looked up and added to records during DR-strategy transformation.
     * Mandatory - throws if not configured.
     */
    public static boolean isUseTranscriptionsEnabled() {
        return getConfig().getValue(USE_TRANSCRIPTIONS_KEY, Boolean.class);
    }

    /**
     * @return if true, a single record error during records-export stops the whole flow. If false, it is logged
     * and skipped. Defaults to {@code true}.
     */
    public static boolean isStopOnErrorEnabled() {
        return getConfig().getOptionalValue(STOP_ON_ERROR_KEY, Boolean.class).orElse(true);
    }

    /**
     * @return the pattern acceptable record IDs must conform to; see {@code conf/ds-present-behaviour.yaml}.
     */
    public static String getRecordIdPattern() {
        return getYamlConfig().getString(RECORD_ID_PATTERN_KEY);
    }

    /**
     * @return the pattern acceptable origin prefixes must conform to; see {@code conf/ds-present-behaviour.yaml}.
     */
    public static String getOriginPrefixPattern() {
        return getYamlConfig().getString(ORIGIN_PREFIX_PATTERN_KEY);
    }

    // -----------------------------------------------------------------------------------------------------------
    // Typed tree getters: origins/views/transformers and storages/backends. Built on demand from the current
    // getYamlConfig() tree - cheap enough that there is no need to cache the result. The actual walking of the nested
    // YAML tree for these sections lives in {@link ConfigParser}, not here - every consumer (DSOrigin, View,
    // OriginHandler, StorageHandler, StorageController, the storage/transformer factories, ...) works with these
    // plain DTOs only.
    // -----------------------------------------------------------------------------------------------------------

    /**
     * @return the configured origins, in the order they are defined in {@code conf/ds-present-kb-origins.yaml}
     * (or whichever file(s) {@link #initialize(String...)}/{@link #initializeWithPropertiesOverride(String, String)}
     * were called with).
     */
    public static List<OriginConfig> getOrigins() {
        return ConfigParser.parseOrigins(getYamlConfig());
    }

    /**
     * @return the configured storages, in the order they are defined in the configuration.
     */
    public static List<StorageConfig> getStorages() {
        return ConfigParser.parseStorages(getYamlConfig());
    }

    /**
     * Set (or overwrite) a single configuration property at runtime, without touching any configuration file and
     * without restarting the service. Only affects {@link #getConfig()} (and the typed scalar getters above,
     * which are backed by it) - the nested {@link #getYamlConfig()} tree (and the typed tree getters above) are
     * unaffected, since they are a plain immutable snapshot as before this migration.
     * <p>
     * The change is visible to every subsequent {@link #getConfig()} lookup immediately: it takes precedence
     * over every other configuration source (the flattened YAML tree, the properties override file, environment
     * variables and system properties, see {@link #RUNTIME_ORDINAL}). This is intended for short-lived operational
     * overrides (temporarily raising a limit, flipping a behaviour flag, etc.) or for exercising the config system
     * from a test. It is <em>not</em> persisted: restarting the service reverts to the values from the
     * configuration file.
     *
     * @param key   dotted property path, using the exact same syntax as the YAML configuration
     *              (e.g. {@code "licensemodule.allowall"}).
     * @param value the value to use, or {@code null} to remove a previously injected override.
     */
    public static void setRuntimeProperty(String key, String value) {
        if (value == null) {
            clearRuntimeProperty(key);
            return;
        }
        String logValue = looksSensitive(key) ? "<redacted>" : value;
        log.info("Setting runtime configuration override '{}' = '{}'", key, logValue);
        runtimeSource.set(key, value);
    }

    /**
     * Removes a previously injected runtime override for the given key, reverting to whatever value the flattened
     * YAML tree, the properties override file, environment variables or system properties provide.
     *
     * @param key dotted property path previously passed to {@link #setRuntimeProperty(String, String)}.
     */
    public static void clearRuntimeProperty(String key) {
        log.info("Clearing runtime configuration override '{}'", key);
        runtimeSource.clear(key);
    }

    /**
     * @return the keys currently overridden at runtime through {@link #setRuntimeProperty(String, String)}.
     */
    public static Set<String> getRuntimePropertyNames() {
        return runtimeSource.getPropertyNames();
    }

    private static boolean looksSensitive(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.contains("password") || lower.contains("secret") || lower.contains("token");
    }

    /**
     * A tiny mutable {@link ConfigSource} that allows properties to be injected or changed at runtime. See
     * {@link #setRuntimeProperty(String, String)}.
     */
    static final class RuntimeConfigSource implements ConfigSource {
        private final Map<String, String> properties = new ConcurrentHashMap<>();
        private final int ordinal;

        RuntimeConfigSource(int ordinal) {
            this.ordinal = ordinal;
        }

        void set(String key, String value) {
            properties.put(key, value);
        }

        void clear(String key) {
            properties.remove(key);
        }

        @Override
        public Map<String, String> getProperties() {
            return Collections.unmodifiableMap(properties);
        }

        @Override
        public Set<String> getPropertyNames() {
            return properties.keySet();
        }

        @Override
        public String getValue(String propertyName) {
            return properties.get(propertyName);
        }

        @Override
        public String getName() {
            return "ServiceConfig runtime overrides";
        }

        @Override
        public int getOrdinal() {
            return ordinal;
        }
    }

    /**
     * A simple, immutable, fixed-content {@link ConfigSource}, used to expose the flattened YAML tree (see
     * {@link #flatten(YAML, String, Map)}) as a MicroProfile Config source.
     */
    private static final class MapConfigSource implements ConfigSource {
        private final String name;
        private final Map<String, String> properties;
        private final int ordinal;

        MapConfigSource(String name, Map<String, String> properties, int ordinal) {
            this.name = name;
            this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
            this.ordinal = ordinal;
        }

        @Override
        public Map<String, String> getProperties() {
            return properties;
        }

        @Override
        public Set<String> getPropertyNames() {
            return properties.keySet();
        }

        @Override
        public String getValue(String propertyName) {
            return properties.get(propertyName);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getOrdinal() {
            return ordinal;
        }
    }
}