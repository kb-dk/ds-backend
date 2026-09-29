package dk.kb.discover.config;

import dk.kb.util.Resolver;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.yaml.YamlConfigSource;

/**
 * Configuration class backed by <a href="https://smallrye.io/smallrye-config/">SmallRye Config</a> (a standalone
 * implementation of MicroProfile Config; this project does not use Quarkus).
 * <p>
 * This replaces the previous kb-util {@code YAML}/{@code AutoYAML}-backed implementation. {@link
 * #initialize(String, String)} takes a single YAML file (resolved via {@link Resolver#resolveURL(String)}:
 * verbatim as a file, then on the classpath, then under the user's home), typically the one path configured
 * outside the project in the Tomcat context environment (see {@code conf/ocp/ds-discover.xml}).
 * <p>
 * The old {@code AutoYAML} base class's busy-wait file-watching/{@code Observer} callback mechanism (enabled via
 * an {@code autoupdate:} YAML section) has been dropped: it defaulted to disabled, no {@code autoupdate:} key was
 * ever configured for this service, and the sole implementer of the observer interface ({@code SolrManager}) had
 * its registration call commented out - it was unused template scaffolding. {@code SolrManager} now loads the
 * Solr collection setup by explicitly calling {@link dk.kb.discover.SolrManager#loadSolrServices()} once, from
 * {@link dk.kb.discover.webservice.ContextListener}, instead of being pushed a config snapshot via the observer
 * callback.
 * <p>
 * <b>The devops/operations override file.</b> A second, optional properties file - also configured outside the
 * project, via its own Tomcat context environment entry (see {@code conf/ocp/ds-discover.xml}) - carries values
 * operations control per environment, most importantly secrets. This is deliberately <em>not</em> SmallRye's own
 * implicit {@code config/application.properties} convention (part of {@code addDefaultSources()} below): that
 * convention is keyed off the JVM's current working directory, which is shared by every webapp in a Tomcat
 * instance that hosts several WARs (as the development server does) - so it cannot tell one service's override
 * file apart from another's. The explicit path instead flows through the same per-webapp JNDI mechanism as the
 * YAML file itself. See {@code ds-storage/SMALLRYE_CONFIG_MIGRATION.md} for the full picture, including why the
 * implicit convention is still left enabled (harmless as long as no file is ever placed at that shared location)
 * and other ways to inject configuration - environment variables, system properties, and/or
 * {@link #setRuntimeProperty(String, String)} - all of which sit above the single YAML file in priority.
 * <p>
 * <b>Runtime property injection.</b> Besides the YAML file, the properties override file, environment variables
 * and system properties, this class registers a small in-memory {@link ConfigSource} ({@link RuntimeConfigSource})
 * with the highest ordinal of all sources. Properties set with {@link #setRuntimeProperty(String, String)} are
 * therefore visible to every subsequent config lookup immediately, without a restart and without touching any
 * file: unlike the old {@code YAML} class (which produced an immutable snapshot), SmallRye Config re-reads all
 * sources on every {@code getValue}/{@code getOptionalValue} call.
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
     * {@link #initialize(String, String)}). This is deliberately above SmallRye's own implicit
     * {@code config/application.properties} convention (ordinal 260, part of {@code addDefaultSources()}), so
     * that if a file is ever accidentally left at that shared location in a multi-webapp Tomcat instance, it can
     * never silently outrank the correct, explicitly-configured file for a given service.
     */
    private static final int PROPERTIES_OVERRIDE_ORDINAL = 270;

    /**
     * Ordinal for the single configured YAML file (see {@link #initialize(String, String)}). This is comfortably
     * below environment variables (300) and system properties (400), so operations can override any individual
     * configured value without editing YAML at all.
     */
    private static final int YAML_ORDINAL = 100;

    private static final RuntimeConfigSource runtimeSource = new RuntimeConfigSource(RUNTIME_ORDINAL);

    private static SmallRyeConfig serviceConfig;

    /**
     * Initializes the configuration from the provided configFile, without a devops/operations properties
     * override file. Equivalent to {@code initialize(configFile, null)}.
     * <p>
     * This overload exists mainly for tests and other callers that don't need/have an override file; production
     * start-up (see {@link dk.kb.discover.webservice.ContextListener}) should use
     * {@link #initialize(String, String)} instead.
     *
     * @param configFile the single YAML file which the configuration is loaded from; see
     *                    {@link #initialize(String, String)}.
     * @throws IOException if the configuration could not be located, loaded or parsed.
     */
    public static synchronized void initialize(String configFile) throws IOException {
        initialize(configFile, null);
    }

    /**
     * Initializes the configuration from the provided configFile and (optional) devops/operations properties
     * override file.
     * This should normally be called from {@link dk.kb.discover.webservice.ContextListener} as
     * part of web server initialization of the container, using the two paths configured outside the project
     * (Tomcat context environment entries {@code application-config} and {@code application-properties-config},
     * see {@code conf/ocp/ds-discover.xml}).
     *
     * @param configFile the single YAML file which the configuration is loaded from: a plain file path, a
     *                    classpath resource name, or a path relative to the user's home
     *                    (see {@link Resolver#resolveURL(String)}).
     * @param propertiesOverrideFile the devops/operations properties override file (same path syntax as
     *                    {@code configFile}), or {@code null}/blank if none is configured. If a path is given but
     *                    cannot be resolved to an existing file, this is logged as an error and startup continues
     *                    without that source - values that were meant to come from it (most importantly secrets)
     *                    will then be missing or fall back to the YAML file.
     * @throws IOException if the YAML configuration could not be located, loaded or parsed. A missing/unresolvable
     *                    {@code propertiesOverrideFile} does <em>not</em> throw - see above.
     */
    public static synchronized void initialize(String configFile, String propertiesOverrideFile) throws IOException {
        URL configUrl = Resolver.resolveURL(configFile);

        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                // Enables ${other.property} / ${other.property:default} expression resolution.
                .addDefaultInterceptors()
                // System properties (400), environment variables (300), an optional .env file (295), an optional
                // config/application.properties (260, see the class javadoc for why this is not what
                // propertiesOverrideFile below uses) or classpath application.properties (250), and
                // META-INF/microprofile-config.properties (100), if present.
                .addDefaultSources()
                .withSources(runtimeSource)
                .withSources(new YamlConfigSource(configUrl, YAML_ORDINAL));

        if (propertiesOverrideFile == null || propertiesOverrideFile.isBlank()) {
            log.info("No devops/operations properties override file configured; continuing with only the YAML " +
                      "file '{}' (plus environment variables/system properties/runtime injection)", configFile);
        } else {
            try {
                URL propertiesUrl = Resolver.resolveURL(propertiesOverrideFile);
                builder.withSources(new PropertiesConfigSource(propertiesUrl, PROPERTIES_OVERRIDE_ORDINAL));
                log.info("Loaded devops/operations properties override file '{}'", propertiesOverrideFile);
            } catch (FileNotFoundException | MalformedURLException e) {
                log.error("Configured devops/operations properties override file '{}' could not be found. " +
                           "Continuing without it - values that were meant to come from it (most importantly " +
                           "secrets) will be missing or fall back to the YAML file.", propertiesOverrideFile, e);
            }
        }

        serviceConfig = builder.build();
    }

    /**
     * Direct access to the backing MicroProfile {@link Config}, for configurations with more flexible content
     * and/or if the service developer prefers key-based property access.
     *
     * @return the backing SmallRye Config-handler for the configuration.
     */
    public static Config getConfig() {
        if (serviceConfig == null) {
            throw new IllegalStateException("The configuration should have been loaded, but was not");
        }
        return serviceConfig;
    }

    /**
     * @param key a configuration property path.
     * @return true if the given key is defined in the configuration.
     */
    public static boolean containsKey(String key) {
        return getConfig().getOptionalValue(key, String.class).isPresent();
    }

    /**
     * @return the URL for the ds-present service, as configured under {@code present.url}.
     */
    public static String getDsPresentUrl() {
        return getConfig().getValue("present.url", String.class);
    }

    private static final Pattern SOLR_COLLECTION_ENTRY_PATTERN =
            Pattern.compile("^solr\\.collections\\[(\\d+)]\\.([^.\\[]+)\\..+$");

    /**
     * One entry from the {@code solr.collections} list in the configuration, e.g.
     * <pre>
     * solr:
     *   collections:
     *     - ds: # the abstract collection ID
     *         server: 'http://localhost:10007'
     *         path: 'solr'
     *         collection: 'ds'
     *         shield: 'solrshield-ds.yaml'
     * </pre>
     */
    public static final class SolrCollectionConfig {
        private final String id;
        private final String server;
        private final String path;
        private final String solrCollection;
        private final String shield;

        private SolrCollectionConfig(String id, String server, String path, String solrCollection, String shield) {
            this.id = id;
            this.server = server;
            this.path = path;
            this.solrCollection = solrCollection;
            this.shield = shield;
        }

        /** @return the abstract collection ID: the YAML key for this entry under {@code solr.collections}. */
        public String getId() {
            return id;
        }

        /** @return the Solr server, including port, or {@code null} if not configured. */
        public String getServer() {
            return server;
        }

        /** @return the path for the Solr service. Defaults to {@code "solr"} if not configured. */
        public String getPath() {
            return path;
        }

        /** @return the real Solr collection ID, or {@code null} if not configured. */
        public String getSolrCollection() {
            return solrCollection;
        }

        /** @return the path to the SolrShield config for this collection, or {@code null} if none is configured. */
        public String getShield() {
            return shield;
        }
    }

    /**
     * Scans the configuration for all entries under {@code solr.collections} - a YAML list where each entry is a
     * single-key map, keyed by a dynamic abstract collection ID - and returns them as a list, in configuration
     * order. Since the abstract collection ID is itself a dynamically-named YAML key (not a fixed field name),
     * this cannot use the same fixed-field indexed-list scanning used elsewhere; instead the property names
     * themselves are parsed to discover which ID belongs to which list index.
     *
     * @return the configured Solr collections. Empty if none are configured.
     * @apiNote Since this method discovers which IDs exist by scanning {@link Config#getPropertyNames()} rather
     * than reading a single, already-known key, it only sees {@code solr.collections} entries that were present
     * when the current configuration was built (i.e. defined in the YAML file, an environment variable, a system
     * property, or the devops/operations properties override file, all read once by {@link #initialize(String,
     * String)}). A key added afterwards via {@link #setRuntimeProperty(String, String)} is <em>not</em>
     * guaranteed to be picked up here, unlike a plain {@code getValue}/{@code getOptionalValue} lookup of an
     * already-known key, which SmallRye Config always re-resolves live - {@code getPropertyNames()} is not
     * required to (and in practice does not) reflect properties added to a source after the configuration was
     * built. Tests that need an ad-hoc {@code solr.collections} setup should build a real temporary YAML file and
     * call {@link #initialize(String)} with it (see {@code SolrShieldTest}), not inject individual properties.
     */
    public static List<SolrCollectionConfig> getSolrCollections() {
        Map<Integer, String> idsByIndex = new TreeMap<>();
        for (String propertyName : getConfig().getPropertyNames()) {
            Matcher m = SOLR_COLLECTION_ENTRY_PATTERN.matcher(propertyName);
            if (m.matches()) {
                idsByIndex.putIfAbsent(Integer.parseInt(m.group(1)), m.group(2));
            }
        }

        List<SolrCollectionConfig> result = new ArrayList<>();
        idsByIndex.forEach((index, id) -> {
            String prefix = "solr.collections[" + index + "]." + id;
            String server = getConfig().getOptionalValue(prefix + ".server", String.class).orElse(null);
            String path = getConfig().getOptionalValue(prefix + ".path", String.class).orElse("solr");
            String solrCollection = getConfig().getOptionalValue(prefix + ".collection", String.class).orElse(null);
            String shield = getConfig().getOptionalValue(prefix + ".shield", String.class).orElse(null);
            result.add(new SolrCollectionConfig(id, server, path, solrCollection, shield));
        });
        return result;
    }

    /**
     * Set (or overwrite) a single configuration property at runtime, without touching any configuration file
     * and without restarting the service.
     * <p>
     * The change is visible to every subsequent config lookup immediately: it takes precedence over every
     * other configuration source (the YAML file, the properties override file, environment variables and
     * system properties, see {@link #RUNTIME_ORDINAL}). This is intended for short-lived operational overrides
     * (temporarily raising a limit, flipping a behaviour flag, etc.) or for exercising the config system from a
     * test. It is <em>not</em> persisted: restarting the service reverts to the values from the configuration
     * file.
     *
     * @param key   dotted property path, using the exact same syntax as the YAML configuration
     *              (e.g. {@code "solr.suggestMinimumLength"}).
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
     * Removes a previously injected runtime override for the given key, reverting to whatever value the
     * YAML file, environment variables or system properties provide.
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
}
