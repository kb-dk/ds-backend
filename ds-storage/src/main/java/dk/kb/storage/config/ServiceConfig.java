package dk.kb.storage.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * This replaces the previous kb-util {@code YAML}-backed implementation. The configuration files themselves
 * ({@code ds-storage-behaviour.yaml}, {@code ds-storage-environment.yaml}, {@code ds-storage-local.yaml}) are
 * unchanged (a single expression in {@code ds-storage-behaviour.yaml} had to be rewritten to SmallRye's
 * {@code ${property:default}} expression syntax; see the YAML file for details) and are still layered the same
 * way: the YAML files matched by the configured glob(s) are loaded in alphanumerical order and later files take
 * precedence over earlier ones, key by key.
 * <p>
 * <b>Runtime property injection.</b> Besides the layered YAML files, environment variables and system
 * properties, this class registers a small in-memory {@link ConfigSource} ({@link RuntimeConfigSource}) with the
 * highest ordinal of all sources. Properties set with {@link #setRuntimeProperty(String, String)} are therefore
 * visible to every subsequent config lookup immediately, without a restart and without touching any file: unlike
 * the old {@code YAML} class (which produced an immutable snapshot), SmallRye Config re-reads all sources on
 * every {@code getValue}/{@code getOptionalValue} call.
 */
public class ServiceConfig {
    private static final Logger log = LoggerFactory.getLogger(ServiceConfig.class);

    public static final int DB_BATCH_SIZE_DEFAULT = 100;

    /**
     * Ordinal for the runtime-injected overrides ({@link #setRuntimeProperty(String, String)}). This is higher
     * than system properties (400), environment variables (300) and the layered YAML files (see
     * {@link #YAML_BASE_ORDINAL}), so a runtime override always wins.
     */
    public static final int RUNTIME_ORDINAL = 500;

    /**
     * Base ordinal for the layered YAML config files. Each file resolved from the glob(s) passed to
     * {@link #initialize(String...)} gets {@code YAML_BASE_ORDINAL + <index in alphanumerical order>}, so later
     * files (e.g. {@code ds-storage-environment.yaml}) override earlier ones (e.g. {@code ds-storage-behaviour.yaml})
     * key by key, exactly like the old {@code YAML.resolveLayeredConfigs(...)} did. This is comfortably below
     * environment variables (300) and system properties (400), so operations can still override any single
     * configured value without editing YAML.
     */
    private static final int YAML_BASE_ORDINAL = 100;

    // key is origin
    private static final HashMap<String, OriginDto> allowedOrigins = new HashMap<>();

    private static final RuntimeConfigSource runtimeSource = new RuntimeConfigSource(RUNTIME_ORDINAL);

    private static SmallRyeConfig serviceConfig;

    /**
     * Initializes the configuration from the provided configFiles.
     * This should normally be called from {@link dk.kb.storage.webservice.ContextListener} as
     * part of web server initialization of the container.
     *
     * @param configFiles the YAML files (or globs, e.g. {@code /app/conf/ds-storage*.yaml}) which the
     *                    configuration is loaded from.
     * @throws IOException if the configuration could not be loaded or parsed.
     */
    public static synchronized void initialize(String... configFiles) throws IOException {
        List<Path> configPaths = Arrays.stream(configFiles)
                .map(Resolver::resolveGlob)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        if (configPaths.isEmpty()) {
            throw new FileNotFoundException("No paths resolved from " + Arrays.toString(configFiles));
        }

        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                // Enables ${other.property} / ${other.property:default} expression resolution, used by e.g.
                // db.url's '${TMPDIR:/tmp}' and by the self-referencing '${openapi.serverurl}'-style values.
                .addDefaultInterceptors()
                // System properties (ordinal 400), environment variables (300) and
                // META-INF/microprofile-config.properties (100), if present.
                .addDefaultSources()
                .withSources(runtimeSource);

        int ordinal = YAML_BASE_ORDINAL;
        for (Path path : configPaths) {
            builder.withSources(new YamlConfigSource(path.toUri().toURL(), ordinal++));
        }

        serviceConfig = builder.build();
        loadAllowedOrigins();
    }

    /**
     * Loads the {@code origins} list from configuration into {@link #allowedOrigins}.
     * <p>
     * Note on layered lists: SmallRye Config flattens a YAML list into indexed properties
     * (e.g. {@code origins[0].name}, {@code origins[1].name}, ...) and resolves each property individually
     * across sources. The old kb-util {@code YAML} merge instead replaced the *whole* {@code origins} list
     * when it was redefined in an overriding file (environment/local). To keep that "whole list wins"
     * behaviour for {@code origins} specifically, this method picks the single highest-ordinal config source
     * that defines any {@code origins[...]} entry and reads the full list from that source alone, rather than
     * resolving each index independently through the normal per-property override mechanism.
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
        return serviceConfig.getValue("db.password", String.class);
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
     * other configuration source (the layered YAML files, environment variables and system properties, see
     * {@link #RUNTIME_ORDINAL}). This is intended for short-lived operational overrides (temporarily raising a
     * limit, flipping a behaviour flag, etc.) or for exercising the config system from a test. It is
     * <em>not</em> persisted: restarting the service reverts to the values from the configuration files.
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
     * layered YAML files, environment variables or system properties provide.
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
