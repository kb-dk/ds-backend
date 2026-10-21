package dk.kb.oauth.config;

import dk.kb.util.Resolver;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.yaml.YamlConfigSource;

/**
 * Configuration class backed by <a href="https://smallrye.io/smallrye-config/">SmallRye Config</a> (a standalone
 * implementation of MicroProfile Config; this project does not use Quarkus).
 * <p>
 * This replaces the previous kb-util {@code AutoYAML}-backed implementation. {@link #initialize(String, String)}
 * takes a single YAML file (resolved via {@link Resolver#resolveURL(String)}), typically the path configured
 * outside the project in the Tomcat context environment (see {@code conf/ocp/bff.xml}).
 * <p>
 * <b>Auto-reload.</b> Unlike the other ds-backend modules, bff must keep picking up changes to its configuration
 * file (most importantly {@code secretSalt}, used by {@link dk.kb.oauth.EncryptionHelper} to encrypt/decrypt the
 * BFF cookie) without a service restart - this replicates the old kb-util {@code AutoYAML} behaviour, which was a
 * background thread that reloaded the configuration file every minute. That behaviour is controlled by the same
 * two YAML keys as before:
 * <pre>
 * config:
 *   autoupdate:
 *     enabled: true
 *     intervalms: 60000
 * </pre>
 * A background daemon thread rebuilds the entire {@link SmallRyeConfig} from the same configFile/propertiesOverrideFile
 * paths every {@code intervalms} milliseconds and atomically swaps the {@code volatile} reference used by
 * {@link #getConfig()} - readers therefore never see a partially-rebuilt config, and never need to re-fetch or
 * re-cache anything themselves (this mirrors {@code AutoYAML}'s "replace, not mutate" design). If
 * {@code config.autoupdate.enabled} is absent or {@code false}, no thread is started and the configuration behaves
 * exactly like every other module's (loaded once at startup).
 * <p>
 * <b>The devops/operations override file.</b> A second, optional properties file - also configured outside the
 * project, via its own Tomcat context environment entry (see {@code conf/ocp/bff.xml}) - carries values operations
 * control per environment, most importantly secrets. See the other modules' {@code ServiceConfig} (e.g.
 * ds-license) and {@code ds-storage/SMALLRYE_CONFIG_MIGRATION.md} for the full picture of the ordinal scheme.
 * <p>
 * <b>Runtime property injection.</b> Besides the YAML file, the properties override file, environment variables
 * and system properties, this class registers a small in-memory {@link ConfigSource} ({@link RuntimeConfigSource})
 * with the highest ordinal of all sources. Properties set with {@link #setRuntimeProperty(String, String)} are
 * therefore visible to every subsequent config lookup immediately.
 */
public class ServiceConfig {
    private static final Logger log = LoggerFactory.getLogger(ServiceConfig.class);

    /**
     * Ordinal for the runtime-injected overrides ({@link #setRuntimeProperty(String, String)}). Higher than
     * system properties (400), environment variables (300) and everything below, so a runtime override always
     * wins - including over a fresh auto-reload of the YAML file.
     */
    public static final int RUNTIME_ORDINAL = 500;

    /**
     * Ordinal for the explicit, per-service devops/operations properties override file (see
     * {@link #initialize(String, String)}). Above SmallRye's own implicit {@code config/application.properties}
     * convention (ordinal 260, part of {@code addDefaultSources()}).
     */
    private static final int PROPERTIES_OVERRIDE_ORDINAL = 270;

    /**
     * Ordinal for the single configured YAML file (see {@link #initialize(String, String)}).
     */
    private static final int YAML_ORDINAL = 100;

    /** Matches kb-util AutoYAML's own default: auto-update disabled unless the config says otherwise. */
    private static final boolean AUTO_UPDATE_DEFAULT = false;
    /** Matches kb-util AutoYAML's own default interval: every minute. */
    private static final long AUTO_UPDATE_MS_DEFAULT = 60_000L;

    private static final RuntimeConfigSource runtimeSource = new RuntimeConfigSource(RUNTIME_ORDINAL);

    private static volatile SmallRyeConfig serviceConfig;

    // The two source paths, kept so the auto-update thread can rebuild from the exact same sources.
    private static volatile String configFile;
    private static volatile String propertiesOverrideFile;

    private static Thread autoUpdateThread;
    private static volatile boolean autoUpdating = false;

    /**
     * Initializes the configuration from the provided configFile, without a devops/operations properties
     * override file. Equivalent to {@code initialize(configFile, null)}.
     *
     * @param configFile the single YAML file which the configuration is loaded from.
     * @throws IOException if the configuration could not be located, loaded or parsed.
     */
    public static synchronized void initialize(String configFile) throws IOException {
        initialize(configFile, null);
    }

    /**
     * Initializes the configuration from the provided configFile and (optional) devops/operations properties
     * override file, and - if {@code config.autoupdate.enabled} is set in the resulting configuration - starts
     * a background thread that reloads the configuration every {@code config.autoupdate.intervalms} milliseconds.
     * <p>
     * This should normally be called from {@link dk.kb.oauth.webservice.ContextListener} as part of web server
     * initialization, using the two paths configured outside the project (Tomcat context environment entries
     * {@code application-config} and {@code application-properties-config}, see {@code conf/ocp/bff.xml}).
     *
     * @param configFile the single YAML file which the configuration is loaded from: a plain file path, a
     *                    classpath resource name, or a path relative to the user's home
     *                    (see {@link Resolver#resolveURL(String)}).
     * @param propertiesOverrideFile the devops/operations properties override file (same path syntax as
     *                    {@code configFile}), or {@code null}/blank if none is configured.
     * @throws IOException if the YAML configuration could not be located, loaded or parsed.
     */
    public static synchronized void initialize(String configFile, String propertiesOverrideFile) throws IOException {
        // Fail fast if the YAML file itself cannot be resolved, exactly like the previous AutoYAML-backed
        // implementation did on the initial load.
        Resolver.resolveURL(configFile);

        ServiceConfig.configFile = configFile;
        ServiceConfig.propertiesOverrideFile = propertiesOverrideFile;

        serviceConfig = buildConfig(configFile, propertiesOverrideFile);

        restartAutoUpdateIfNeeded();
    }

    /**
     * Builds a fresh {@link SmallRyeConfig} from the given sources. Called both by {@link #initialize} and by the
     * auto-update thread; never mutates any shared state itself.
     */
    private static SmallRyeConfig buildConfig(String configFile, String propertiesOverrideFile) throws IOException {
        URL configUrl = Resolver.resolveURL(configFile);

        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                // Enables ${other.property} / ${other.property:default} expression resolution.
                .addDefaultInterceptors()
                // System properties (400), environment variables (300), an optional .env file (295), an optional
                // config/application.properties (260) or classpath application.properties (250), and
                // META-INF/microprofile-config.properties (100), if present.
                .addDefaultSources()
                .withSources(runtimeSource)
                .withSources(new YamlConfigSource(configUrl, YAML_ORDINAL));

        if (propertiesOverrideFile == null || propertiesOverrideFile.isBlank()) {
            log.debug("No devops/operations properties override file configured; continuing with only the YAML " +
                       "file '{}' (plus environment variables/system properties/runtime injection)", configFile);
        } else {
            try {
                URL propertiesUrl = Resolver.resolveURL(propertiesOverrideFile);
                builder.withSources(new PropertiesConfigSource(propertiesUrl, PROPERTIES_OVERRIDE_ORDINAL));
                log.debug("Loaded devops/operations properties override file '{}'", propertiesOverrideFile);
            } catch (FileNotFoundException | MalformedURLException e) {
                log.error("Configured devops/operations properties override file '{}' could not be found. " +
                           "Continuing without it.", propertiesOverrideFile, e);
            }
        }

        return builder.build();
    }

    /**
     * Reads {@code config.autoupdate.enabled}/{@code config.autoupdate.intervalms} from the just-built
     * configuration and (re)starts or stops the background auto-update thread to match. Safe to call repeatedly;
     * a running thread is stopped before a new one (if any) is started, so re-{@link #initialize}-ing (as tests
     * do) never leaves two threads running.
     */
    private static synchronized void restartAutoUpdateIfNeeded() {
        stopAutoUpdateThread();

        boolean enabled = serviceConfig.getOptionalValue("config.autoupdate.enabled", Boolean.class)
                                        .orElse(AUTO_UPDATE_DEFAULT);
        long intervalMs = serviceConfig.getOptionalValue("config.autoupdate.intervalms", Long.class)
                                        .orElse(AUTO_UPDATE_MS_DEFAULT);

        if (!enabled) {
            log.info("Configuration auto-update is disabled (config.autoupdate.enabled=false or unset)");
            autoUpdating = false;
            return;
        }

        log.info("Configuration auto-update enabled: reloading '{}' every {}ms", configFile, intervalMs);
        autoUpdating = true;
        autoUpdateThread = new Thread(() -> autoUpdateLoop(intervalMs), "ServiceConfig-autoupdate");
        autoUpdateThread.setDaemon(true);
        autoUpdateThread.start();
    }

    private static void autoUpdateLoop(long intervalMs) {
        while (autoUpdating) {
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (!autoUpdating) {
                return;
            }
            try {
                // configFile/propertiesOverrideFile are only ever written under the synchronized initialize(),
                // so a plain volatile read here is enough to pick up the exact sources currently in use.
                SmallRyeConfig freshConfig = buildConfig(configFile, propertiesOverrideFile);
                serviceConfig = freshConfig; // Atomic reference swap: readers never see a half-built config.
                log.debug("Configuration reloaded from '{}'", configFile);
            } catch (Exception e) {
                // Never let a transient problem (file briefly missing/unparsable during a deploy, etc.) kill the
                // auto-update thread - keep serving the last known-good configuration and try again next tick.
                log.warn("Failed to reload configuration from '{}'. Keeping the previous configuration.",
                          configFile, e);
            }
        }
    }

    private static synchronized void stopAutoUpdateThread() {
        autoUpdating = false;
        if (autoUpdateThread != null) {
            autoUpdateThread.interrupt();
            autoUpdateThread = null;
        }
    }

    /**
     * @return {@code true} if the background configuration auto-update thread is currently running.
     */
    public static boolean isAutoUpdating() {
        return autoUpdating;
    }

    /**
     * Stops the background auto-update thread, if running. Should be called on service shutdown (see
     * {@link dk.kb.oauth.webservice.ContextListener#contextDestroyed}).
     */
    public static synchronized void shutdown() {
        stopAutoUpdateThread();
    }

    /**
     * Direct access to the backing MicroProfile {@link Config}, for configurations with more flexible content
     * and/or if the service developer prefers key-based property access.
     * <p>
     * This is a plain (non-volatile-looking, but backed by a volatile field) getter: it always returns whichever
     * {@link SmallRyeConfig} is current, including after an auto-reload swap.
     *
     * @return the backing SmallRye Config-handler for the configuration.
     */
    public static Config getConfig() {
        SmallRyeConfig current = serviceConfig;
        if (current == null) {
            throw new IllegalStateException("The configuration should have been loaded, but was not");
        }
        return current;
    }

    /**
     * Returns every configured {@code messages.*} entry as a plain map ({@code messages.msg1: "..."} becomes
     * {@code "msg1" -> "..."}), for JSON serialization (see {@link dk.kb.oauth.api.v1.impl.BffApiServiceImpl#getMessages()}).
     * Previously returned the raw kb-util {@code YAML} submap for {@code messages} directly; MicroProfile
     * {@link Config} has no equivalent "read back the whole sub-tree as a generic structure" operation, so this
     * reconstructs the same flat shape by hand from the merged configuration's property names. Reflects the
     * current (possibly auto-reloaded) configuration on every call.
     * <p>
     * The value type is {@code Object} (each value is actually a {@code String}) to match
     * {@code BffApi.getMessages()}'s declared return type {@code Map<String, Object>}.
     *
     * @return a map of message key to message text, in configuration order. Empty if no {@code messages} section
     *         is configured.
     */
    public static Map<String, Object> getMessagesConfig() {
        Config conf = getConfig();
        Map<String, Object> messages = new LinkedHashMap<>();
        for (String propName : new TreeSet<>(setOf(conf.getPropertyNames()))) {
            if (propName.startsWith("messages.")) {
                String key = propName.substring("messages.".length());
                conf.getOptionalValue(propName, String.class).ifPresent(value -> messages.put(key, value));
            }
        }
        return messages;
    }

    private static Set<String> setOf(Iterable<String> values) {
        Set<String> result = new java.util.LinkedHashSet<>();
        for (String value : values) {
            result.add(value);
        }
        return result;
    }

    /**
     * Set (or overwrite) a single configuration property at runtime, without touching any configuration file and
     * without restarting the service. The change is visible to every subsequent config lookup immediately, and
     * survives auto-reload ticks since the runtime source is re-added to every rebuilt config with the highest
     * ordinal of all sources.
     *
     * @param key   dotted property path (e.g. {@code "secretSalt"}).
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
     * Removes a previously injected runtime override for the given key.
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
        String lower = key.toLowerCase(java.util.Locale.ROOT);
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
