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
 *     {@link YAML#resolveLayeredConfigs(String...)} from the single {@code ds-present-behaviour.yaml} (the former
 *     separate {@code ds-present-kb-origins.yaml} has been folded into it, and {@code application-config} in
 *     {@code conf/ocp/ds-present.xml} is a plain filename, not a glob, like every other module - exactly one YAML
 *     file is ever loaded), and the {@code ${path:...}} self-referencing extrapolation syntax. This is internal to
 *     {@code ServiceConfig} - no other class in ds-present should need it (a few tests that exercise generic
 *     YAML/library behaviour using fixtures unrelated to ds-present's own configuration schema are the only
 *     remaining exceptions).</li>
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
 * <p>
 * Nested-tree values ({@code imageservers}, {@code storages}, ...) are not exempt from being overridden per
 * environment - they just reach it differently than a scalar setting does. Any path reachable via
 * {@link #flatten(YAML, String, Map)} (flat or nested, e.g. both {@code licensemodule.url} and
 * {@code imageservers[0].example.url}) that a higher-ordinal source (the devops/operations properties override
 * file, an environment variable, a system property, or {@link #setRuntimeProperty(String, String)}) supplies a
 * different value for is written back into {@link #serviceConfig} itself by
 * {@link #applyOverridesToTree(Map, String, Map)}, so {@link ConfigParser} and the {@code ${path:...}}
 * self-extrapolation syntax (which only ever read {@link #serviceConfig}, never {@link #flatConfig}) see the
 * override too - see {@link #initializeWithPropertiesOverride(String, String)}. This is the <em>only</em> override
 * mechanism for the nested tree: {@code serviceConfig} is still loaded from exactly the one file named by {@code
 * application-config} (see {@code conf/ocp/ds-present.xml}); no second YAML file is ever read.
 * <p>
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
     * Initializes the configuration from the single given configFile, without a devops/operations properties
     * override file for {@link #getConfig()}.
     * <p>
     * This overload exists mainly for tests and other callers that don't need/have an override file; production
     * start-up (see {@link dk.kb.present.webservice.ContextListener}) uses
     * {@link #initializeWithPropertiesOverride(String, String)} instead.
     *
     * @param configFile the single YAML configuration file to load; see {@link YAML#resolveLayeredConfigs(String...)}.
     *                   Exactly one file, like every other {@code ds-backend} module - see the class javadoc.
     * @throws IOException if the configuration could not be loaded or parsed.
     */
    public static synchronized void initialize(String configFile) throws IOException {
        serviceConfig = YAML.resolveLayeredConfigs(configFile);
        rebuildFlatConfig(null);
        serviceConfig.setExtrapolate(true);
    }

    /**
     * Initializes the configuration from a single configFile and an optional devops/operations properties
     * override file for {@link #getConfig()}.
     * <p>
     * This should normally be called from {@link dk.kb.present.webservice.ContextListener} as part of web server
     * initialization of the container, using the two paths configured outside the project (Tomcat context
     * environment entries {@code application-config} and {@code application-properties-config}, see
     * {@code conf/ocp/ds-present.xml}).
     * <p>
     * Note: this is deliberately a separate, distinctly-named method rather than a second {@code initialize}
     * overload, so the two-argument form cannot be confused with (or accidentally called instead of) the
     * single-file form above.
     *
     * @param configFile the single YAML configuration file which the nested tree is loaded from; see
     *                    {@link #initialize(String)}.
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

        // Captured before rebuildFlatConfig() layers anything else on top, so applyOverridesToTree() below can
        // tell, for every path, whether a higher-ordinal source changed it - see that method's javadoc.
        Map<String, String> originalFlattened = new LinkedHashMap<>();
        flatten(serviceConfig, "", originalFlattened);

        // Deliberately flatten (again, inside rebuildFlatConfig - see flatten()) BEFORE extrapolating serviceConfig
        // below, not after: this way, a "${keycloak_realm}"-style placeholder reaches getConfig()'s backing
        // MapConfigSource still literally unresolved, and is resolved by SmallRye Config's own
        // ExpressionConfigSourceInterceptor (enabled via addDefaultInterceptors() in rebuildFlatConfig) against the
        // *whole* MicroProfile Config source stack - including environment variables - exactly like every other
        // module's SmallRye-native YamlConfigSource does. Resolving it here instead, via kb-util YAML's own
        // extrapolate(), would only ever check it against JVM system properties for a bare (unprefixed) name -
        // never environment variables - which is a real, previously-undetected difference from every other module
        // despite an identical YAML `security:` section.
        rebuildFlatConfig(propertiesOverrideFile);

        // Write back, into serviceConfig itself, any value that a higher-ordinal source (the properties override
        // file, an environment variable, a system property, or a runtime override) now supplies for a path that
        // also exists in the YAML tree - flat or nested (imageservers, storages, ...). See the class javadoc and
        // applyOverridesToTree()'s own javadoc: this is the only override mechanism for the nested tree, and it
        // never reads a second YAML file.
        applyOverridesToTree(serviceConfig, "", originalFlattened);

        // Only now extrapolate the nested tree itself (kb-util's own engine), for getYamlConfig()/ConfigParser
        // consumers - e.g. the ${path:...} self-referencing syntax used in the origins/views/transformers graph -
        // so a self-reference like ${path:imageservers[default=true].url} resolves against the overridden URL,
        // not the checked-in default.
        serviceConfig.setExtrapolate(true);
    }

    /**
     * (Re)builds {@link #flatConfig} from the current {@link #serviceConfig} tree plus the standard override
     * layers. Called from both {@link #initialize(String)} (with no properties override file) and
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
                // it is also called with propertiesOverrideFile=null from plain initialize(String), where no
                // exception should ever be possible, so any failure resolving/parsing the override file here is
                // logged and swallowed instead, matching this method's "does not throw" contract (see
                // initializeWithPropertiesOverride's javadoc). This genuinely deserves ERROR (unlike a routine
                // NotFoundServiceException elsewhere - see ImplBase.handleException) since it means a configured
                // file - possibly secrets - is silently missing; but the message alone (e.getMessage(), not e
                // itself) already says exactly what went wrong, so the stack trace is omitted as pure noise.
                log.error("Configured devops/operations properties override file '{}' could not be found: {}. " +
                           "Continuing without it - values that were meant to come from it (most importantly " +
                           "secrets) will be missing from getConfig() or fall back to the YAML file.",
                           propertiesOverrideFile, e.getMessage());
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
        for (Map.Entry<String, Object> mapEntry : yaml.entrySet()) {
            String key = mapEntry.getKey();
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            try {
                // Read the value straight off the Map.Entry, not via yaml.get(key): YAML overrides get(Object) to
                // always parse its argument as a dotted/indexed PATH (see YPath), never as a single literal key, so
                // yaml.get("ds.radio") would be misread as the two-segment path "ds" -> "radio" and (correctly)
                // fail to find it - even though "ds.radio" is one single, literal top-level key here. entrySet()
                // (unlike get(Object), not overridden by YAML - it is plain LinkedHashMap behaviour) sidesteps that
                // entirely. getSubMap(String)/getYAMLList(String) below still go through the same path parser, so a
                // key that might itself contain a dot is quoted first (quotePathSegment(key)) to keep it a single
                // literal segment regardless - see that method's javadoc.
                Object value = mapEntry.getValue();
                if (value instanceof Map) {
                    flatten(yaml.getSubMap(quotePathSegment(key)), path, target);
                } else if (value instanceof List) {
                    List<?> rawList = (List<?>) value;
                    if (!rawList.isEmpty() && rawList.get(0) instanceof Map) {
                        List<YAML> subList = yaml.getYAMLList(quotePathSegment(key));
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
     * Wraps {@code key} in single quotes so a {@link YAML} path-based accessor ({@link YAML#get(Object)},
     * {@link YAML#getSubMap(String)}, {@link YAML#getYAMLList(String)}) treats it as one literal path segment,
     * even if {@code key} itself contains a {@code .} (e.g. the origin key {@code ds.radio} in
     * {@code ds-present-behaviour.yaml}) - see kb-util's {@code YPath} class: a quoted segment
     * ({@code 'ds.radio'}) is never split on its internal dots, while an unquoted one always is. Quoting a key
     * that has no dot is a harmless no-op - it still parses as that same single segment - so this is always safe
     * to apply, not just for keys known to contain a dot.
     */
    private static String quotePathSegment(String key) {
        return "'" + key + "'";
    }

    /**
     * Writes any value that {@link #flatConfig} (already built by {@link #rebuildFlatConfig(String)} by the time
     * this is called) now holds differently from {@code originalFlattened} back into the actual nested
     * map/list objects making up {@code node} - i.e. a path that a higher-ordinal source (the devops/operations
     * properties override file, an environment variable, a system property, or
     * {@link #setRuntimeProperty(String, String)}) overrides. This is what lets such a source override a
     * nested-tree value such as {@code imageservers[0].example.url} or a {@code storages} backend URL - using
     * exactly the same dotted/indexed path {@link #flatten(YAML, String, Map)} already uses for every flat
     * setting - without a second YAML file ever being read: {@link #serviceConfig} is still loaded from exactly
     * the one file named by {@code application-config} (see {@code conf/ocp/ds-present.xml}); only its in-memory
     * values change.
     * <p>
     * Mirrors {@link #flatten(YAML, String, Map)}'s own traversal (same key/path/list-index conventions), but
     * walks the <em>raw</em> {@link Map}/{@link List} objects rather than {@link YAML#getSubMap(String)}/
     * {@link YAML#getYAMLList(String)} (which each return a shallow copy - fine for a read, but a write into a
     * copy would never be seen again, since {@link ConfigParser} and every other reader re-derive their own fresh
     * copy from {@link #serviceConfig} on every call). Writing into the raw nested objects instead means the
     * change is visible to every subsequent read, exactly as if it had been in the checked-in YAML file itself.
     * <p>
     * Must be called after {@link #rebuildFlatConfig(String)} and before {@link YAML#setExtrapolate(boolean)} is
     * enabled on {@link #serviceConfig} - see {@link #initializeWithPropertiesOverride(String, String)}.
     *
     * @param node a {@link Map} (sub)tree of {@link #serviceConfig} - the top-level call passes
     *             {@code serviceConfig} itself (a {@link YAML} is itself a {@code Map<String, Object>}).
     * @param prefix the property-name prefix for {@code node}, using {@link #flatten(YAML, String, Map)}'s
     *               convention (empty for the root).
     * @param originalFlattened the flat map {@link #flatten(YAML, String, Map)} built from {@link #serviceConfig}
     *               before anything was layered on top of it - used to detect whether {@link #flatConfig} now
     *               disagrees with it for a given path.
     */
    @SuppressWarnings("unchecked")
    private static void applyOverridesToTree(Map<String, Object> node, String prefix,
                                              Map<String, String> originalFlattened) {
        for (Map.Entry<String, Object> mapEntry : node.entrySet()) {
            String key = mapEntry.getKey();
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            try {
                // entrySet()/getValue(), not node.get(key): node may be the actual YAML tree itself (a subclass of
                // Map), and YAML.get(Object) always parses its argument as a dotted path - "ds.radio" would be
                // misread as "ds" -> "radio" - which entrySet() (plain, unoverridden Map behaviour) avoids. See
                // flatten()'s identical fix just above for the full explanation.
                Object value = mapEntry.getValue();
                if (value instanceof Map) {
                    applyOverridesToTree((Map<String, Object>) value, path, originalFlattened);
                } else if (value instanceof List) {
                    List<Object> rawList = (List<Object>) value;
                    for (int i = 0; i < rawList.size(); i++) {
                        String indexedPath = path + "[" + i + "]";
                        Object element = rawList.get(i);
                        if (element instanceof Map) {
                            applyOverridesToTree((Map<String, Object>) element, indexedPath, originalFlattened);
                        } else if (element != null) {
                            applyLeafOverride(rawList, i, indexedPath, originalFlattened);
                        }
                    }
                } else if (value != null) {
                    applyLeafOverride(node, key, path, originalFlattened);
                }
            } catch (RuntimeException e) {
                log.debug("Skipping configuration key '{}' while applying overrides to the configuration tree",
                           path, e);
            }
        }
    }

    /**
     * Overwrites a single scalar leaf, keyed by {@code key} in the enclosing {@link Map} {@code target}, if
     * {@link #flatConfig} now holds a different value for {@code path} than {@code originalFlattened} did. See
     * {@link #applyOverridesToTree(Map, String, Map)}.
     */
    private static void applyLeafOverride(Map<String, Object> target, String key, String path,
                                           Map<String, String> originalFlattened) {
        String original = originalFlattened.get(path);
        String current = flatConfig.getOptionalValue(path, String.class).orElse(null);
        if (current != null && !current.equals(original)) {
            logOverride(path, original, current);
            target.put(key, convertOverrideValue(target.get(key), current));
        }
    }

    /**
     * Overwrites a single scalar leaf, at {@code index} in the enclosing scalar {@link List} {@code target}, if
     * {@link #flatConfig} now holds a different value for {@code path} than {@code originalFlattened} did. See
     * {@link #applyOverridesToTree(Map, String, Map)}.
     */
    private static void applyLeafOverride(List<Object> target, int index, String path,
                                           Map<String, String> originalFlattened) {
        String original = originalFlattened.get(path);
        String current = flatConfig.getOptionalValue(path, String.class).orElse(null);
        if (current != null && !current.equals(original)) {
            logOverride(path, original, current);
            target.set(index, convertOverrideValue(target.get(index), current));
        }
    }

    /**
     * Converts {@code overrideStr} (always a {@link String}, coming from {@link #flatConfig}) to the same Java
     * type as {@code originalValue} (as SnakeYAML parsed it - {@link Boolean}, {@link Integer}, {@link Long},
     * {@link Double}, or already {@link String}), so replacing a leaf in the nested tree does not change its type
     * and break a typed getter ({@code getBoolean(...)}, {@code getInteger(...)}, ...) elsewhere. Falls back to
     * the override string itself if it cannot be parsed as {@code originalValue}'s type.
     */
    private static Object convertOverrideValue(Object originalValue, String overrideStr) {
        try {
            if (originalValue instanceof Boolean) {
                return Boolean.parseBoolean(overrideStr);
            } else if (originalValue instanceof Integer) {
                return Integer.parseInt(overrideStr);
            } else if (originalValue instanceof Long) {
                return Long.parseLong(overrideStr);
            } else if (originalValue instanceof Double) {
                return Double.parseDouble(overrideStr);
            }
        } catch (NumberFormatException e) {
            log.warn("Override value '{}' could not be parsed as a {}; using it as a literal String instead",
                      overrideStr, originalValue.getClass().getSimpleName());
        }
        return overrideStr;
    }

    private static void logOverride(String path, String original, String current) {
        boolean sensitive = looksSensitive(path);
        log.info("Overriding configuration tree value at '{}': '{}' -> '{}'", path,
                  sensitive ? "<redacted>" : original, sensitive ? "<redacted>" : current);
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
     * @return the configured origins, in the order they are defined in {@code ds-present-behaviour.yaml} (or
     * whichever file {@link #initialize(String)}/{@link #initializeWithPropertiesOverride(String, String)} was
     * called with).
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