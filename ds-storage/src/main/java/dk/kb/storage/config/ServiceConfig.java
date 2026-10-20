package dk.kb.storage.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.yaml.YamlConfigSource;

import dk.kb.storage.model.v1.OriginDto;
import dk.kb.storage.model.v1.UpdateStrategyDto;
import dk.kb.storage.util.IdNormaliser;
import dk.kb.util.Resolver;

/**
 * Configuration class backed by <a href="https://smallrye.io/smallrye-config/">SmallRye Config</a> (a standalone
 * implementation of MicroProfile Config; this project does not use Quarkus).
 * <p>
 * This replaces the previous kb-util {@code YAML}-backed implementation. {@link #initialize(String, String)}
 * takes a single YAML file (resolved via {@link Resolver#resolveURL(String)}: verbatim as a file, then on the
 * classpath, then under the user's home), typically the one path configured outside the project in the Tomcat
 * context environment (see {@code conf/ocp/ds-storage.xml}). This is a deliberate simplification over the old
 * behaviour/environment/local three-file layering convention: environment- or operator-specific overrides are
 * now expressed with SmallRye Config's own layering instead of a second or third YAML file.
 * <p>
 * <b>The devops/operations override file.</b> A second, optional properties file - also configured outside the
 * project, via its own Tomcat context environment entry (see {@code conf/ocp/ds-storage.xml}) - carries values
 * operations control per environment, most importantly secrets such as the real database password. This is
 * deliberately <em>not</em> SmallRye's own implicit {@code config/application.properties} convention (part of
 * {@code addDefaultSources()} below): that convention is keyed off the JVM's current working directory, which is
 * shared by every webapp in a Tomcat instance that hosts several WARs (as the development server does) - so it
 * cannot tell one service's override file apart from another's. The explicit path instead flows through the same
 * per-webapp JNDI mechanism as the YAML file itself, exactly like {@code ds-storage-behaviour.yaml} and
 * {@code ds-present-behaviour.yaml} are already two distinct context entries for two distinct WARs. See
 * {@code SMALLRYE_CONFIG_MIGRATION.md} for the full picture, including why the implicit convention is still left
 * enabled (harmless as long as no file is ever placed at that shared location) and other ways to inject
 * configuration &mdash; environment variables, system properties, and/or {@link #setRuntimeProperty(String,
 * String)} &mdash; all of which sit above the single YAML file in priority.
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

    public static final int DB_BATCH_SIZE_DEFAULT = 100;

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

    // key is origin
    private static final HashMap<String, OriginDto> allowedOrigins = new HashMap<>();

    private static final RuntimeConfigSource runtimeSource = new RuntimeConfigSource(RUNTIME_ORDINAL);

    private static SmallRyeConfig serviceConfig;

    /**
     * Initializes the configuration from the provided configFile, without a devops/operations properties
     * override file. Equivalent to {@code initialize(configFile, null)}.
     * <p>
     * This overload exists mainly for tests and other callers that don't need/have an override file; production
     * start-up (see {@link dk.kb.storage.webservice.ContextListener}) should use
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
     * This should normally be called from {@link dk.kb.storage.webservice.ContextListener} as
     * part of web server initialization of the container, using the two paths configured outside the project
     * (Tomcat context environment entries {@code application-config} and {@code application-properties-config},
     * see {@code conf/ocp/ds-storage.xml}).
     *
     * @param configFile the single YAML file which the configuration is loaded from: a plain file path, a
     *                    classpath resource name, or a path relative to the user's home
     *                    (see {@link Resolver#resolveURL(String)}).
     * @param propertiesOverrideFile the devops/operations properties override file (same path syntax as
     *                    {@code configFile}), or {@code null}/blank if none is configured. If a path is given but
     *                    cannot be resolved to an existing file, this is logged as an error and startup continues
     *                    without that source - values that were meant to come from it (most importantly secrets
     *                    such as the database password) will then be missing or fall back to the YAML file.
     * @throws IOException if the YAML configuration could not be located, loaded or parsed. A missing/unresolvable
     *                    {@code propertiesOverrideFile} does <em>not</em> throw - see above.
     */
    public static synchronized void initialize(String configFile, String propertiesOverrideFile) throws IOException {
        URL configUrl = Resolver.resolveURL(configFile);

        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                // Enables ${other.property} / ${other.property:default} expression resolution, used by e.g.
                // db.url's '${TMPDIR:/tmp}' and by the self-referencing '${openapi.serverurl}'-style values.
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
                           "secrets such as the database password) will be missing or fall back to the YAML file.",
                           propertiesOverrideFile, e);
            }
        }

        serviceConfig = builder.build();
        loadAllowedOrigins();
    }

    /**
     * Loads the {@code origins} list from configuration into {@link #allowedOrigins}.
     * <p>
     * Note on overriding lists: SmallRye Config flattens a YAML list into indexed properties
     * (e.g. {@code origins[0].name}, {@code origins[1].name}, ...) and resolves each property individually
     * across sources. Since {@link #initialize(String)} only loads a single YAML file, this rarely matters in
     * practice&nbsp;&mdash; but if {@code origins} is ever also (partially) redefined via a higher-priority
     * source (a {@code config/application.properties} file, or {@link #setRuntimeProperty(String, String)}),
     * a naive per-index read would mix entries from both sources instead of one replacing the other. To avoid
     * that, this method picks the single highest-ordinal config source that defines any {@code origins[...]}
     * entry and reads the full list from that source alone.
     */
    private static void loadAllowedOrigins() throws IOException {
        ConfigSource originsSource = null;
        for (ConfigSource source : serviceConfig.getConfigSources()) { // already sorted by descending ordinal
            if (source.getPropertyNames().stream().anyMatch(name -> name.startsWith("origins["))) {
                originsSource = source;
                break;
            }
        }
        if (originsSource == null) {
            throw new IOException("No 'origins' entries found in configuration");
        }

        Pattern indexPattern = Pattern.compile("^origins\\[(\\d+)]\\.");
        SortedSet<Integer> indices = new TreeSet<>();
        for (String name : originsSource.getPropertyNames()) {
            Matcher m = indexPattern.matcher(name);
            if (m.find()) {
                indices.add(Integer.parseInt(m.group(1)));
            }
        }

        allowedOrigins.clear();
        for (int i : indices) {
            String name = originsSource.getValue("origins[" + i + "].name");
            String updateStrategy = originsSource.getValue("origins[" + i + "].updateStrategy");
            if (!IdNormaliser.validateOrigin(name)) {
                throw new IOException("Configured origin: '" + name + "' does not validate to regexp for origin");
            }

            OriginDto originDto = new OriginDto();
            originDto.setName(name);
            originDto.setUpdateStrategy(UpdateStrategyDto.valueOf(updateStrategy));
            allowedOrigins.put(name, originDto);
            log.info("Updatestrategy loaded for origin: '{}' with update strategy: '{}'",
                     originDto.getName(), originDto.getUpdateStrategy());
        }

        log.info("Allowed origin loaded from config. Number of origins: '{}'", allowedOrigins.size());
    }

    public static String getDBDriver() {
        return serviceConfig.getValue("db.driver", String.class);
    }

    public static String getDBUrl() {
        return serviceConfig.getValue("db.url", String.class);
    }

    public static String getDBUserName() {
        return serviceConfig.getValue("db.username", String.class);
    }

    public static String getDBPassword() {
        // Unlike db.driver/db.url/db.username, an empty password is a legitimate, intentional value (e.g. local
        // databases with no authentication). SmallRye's built-in String converter treats "" as "no value", which
        // makes the non-optional getValue(...) throw NoSuchElementException (SRCFG00040) instead of returning "" -
        // so this uses getOptionalValue(...) with an empty-string default to preserve the old kb-util behaviour.
        return serviceConfig.getOptionalValue("db.password", String.class).orElse("");
    }

    public static int getConnectionPoolSize() {
        return serviceConfig.getOptionalValue("db.connectionPoolSize", Integer.class).orElse(10); // Default 10
    }

    public static int getDBBatchSize() {
        return serviceConfig.getOptionalValue("db.batch.size", Integer.class).orElse(DB_BATCH_SIZE_DEFAULT);
    }

    public static HashMap<String, OriginDto> getAllowedOrigins() {
        return allowedOrigins;
    }

    /**
     * Direct access to the backing MicroProfile {@link Config}, for configurations with more flexible content
     * and/or if the service developer prefers key-based property access (e.g. {@code getConfig().getValue(
     * "security.baseurl", String.class)}).
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
     * Set (or overwrite) a single configuration property at runtime, without touching any configuration file
     * and without restarting the service.
     * <p>
     * The change is visible to every subsequent config lookup immediately: it takes precedence over every
     * other configuration source (the YAML file, the properties override file, environment variables and
     * system properties, see {@link #RUNTIME_ORDINAL}). This is intended for short-lived operational overrides
     * (temporarily raising a
     * limit, flipping a behaviour flag, etc.) or for exercising the config system from a test. It is
     * <em>not</em> persisted: restarting the service reverts to the values from the configuration file.
     *
     * @param key   dotted property path, using the exact same syntax as the YAML configuration
     *              (e.g. {@code "db.connectionPoolSize"}).
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
