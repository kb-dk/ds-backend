package dk.kb.datahandler.config;

import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.model.v1.OaiTargetDto.DateStampFormatEnum;
import dk.kb.util.Resolver;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.yaml.YamlConfigSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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

/**
 * Configuration class backed by <a href="https://smallrye.io/smallrye-config/">SmallRye Config</a> (a standalone
 * implementation of MicroProfile Config; this project does not use Quarkus).
 * <p>
 * This replaces the previous kb-util {@code YAML}-backed implementation, mirroring the same migration already
 * done for {@code ds-storage} (see that module's {@code SMALLRYE_CONFIG_MIGRATION.md} for the full background).
 * {@link #initialize(String, String)} takes a single YAML file (resolved via {@link Resolver#resolveURL(String)}),
 * typically the one path configured outside the project in the Tomcat context environment (see
 * {@code conf/ocp/ds-datahandler.xml}). This is a deliberate simplification over the old behaviour/environment
 * two-file globbing convention (previously {@code /app/conf/ds-datahandler*.yaml}): environment- or
 * operator-specific overrides - most importantly secrets such as OAI target credentials and the Kaltura admin
 * secret - are now expressed either via the devops/operations properties override file below, or with SmallRye
 * Config's own layering (environment variables, system properties).
 * <p>
 * <b>The devops/operations override file.</b> A second, optional properties file - also configured outside the
 * project, via its own Tomcat context environment entry (see {@code conf/ocp/ds-datahandler.xml}) - carries values
 * operations control per environment, most importantly secrets. This is deliberately <em>not</em> SmallRye's own
 * implicit {@code config/application.properties} convention (part of {@code addDefaultSources()} below): that
 * convention is keyed off the JVM's current working directory, which is shared by every webapp in a Tomcat
 * instance that hosts several WARs (as the development server does) - so it cannot tell one service's override
 * file apart from another's. The explicit path instead flows through the same per-webapp JNDI mechanism as the
 * YAML file itself.
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

    private static final HashMap<String, OaiTargetDto> oaiTargets = new HashMap<>();
    private static String oaiTimestampFolder = null;
    private static String dsStorageUrl = null;
    /**
     * Url to solr write collection with updateHandler added as URL path. Most likely the url has "/update" appened.
     * To get the URL for the write collection without the updateHanler appended use this: {@link #solrWriteCollectionUrl}.
     */
    private static String solrUpdateUrl = null;
    /**
     * Solr write collection. This contains the URL to the write collection in solr. If an updateHandler gets appended the path should resemble {@link #solrUpdateUrl}.
     */
    private static String solrWriteCollectionUrl = null;
    private static String solrQueryUrl = null;
    private static String dsPresentUrl = null;
    private static int solrBatchSize = 100;

    private static int oaiRetryTimes = 5;
    private static int oaiRetrySeconds = 600;

    private static String kalturaUrl = null;
    private static Integer kalturaPartnerId = null;
    private static String kalturaUserId = null;
    private static String kalturaToken = null;
    private static String kalturaTokenId = null;
    private static String kalturaAdminSecret = null;
    private static int kalturaSessionDurationSeconds = 0;
    private static int kalturaSessionRefreshThreshold = 0;
    private static int conversionProfileIdVideo = 0;
    private static int conversionProfileIdAudio = 0;
    private static int conversionQueueThreshold = 0;
    private static int conversionQueueDelaySeconds = 0;

    private static String streamPathDomsRadioTv = null;
    private static String streamPathPreservicaTv = null;
    private static String streamPathPreservicaRadio = null;

    private static String transcriptionsDropFolder;
    private static String transcriptionsCompletedFolder;

    private static final RuntimeConfigSource runtimeSource = new RuntimeConfigSource(RUNTIME_ORDINAL);

    private static SmallRyeConfig serviceConfig;

    /**
     * Initializes the configuration from the provided configFile, without a devops/operations properties
     * override file. Equivalent to {@code initialize(configFile, null)}.
     * <p>
     * This overload exists mainly for tests and other callers that don't need/have an override file; production
     * start-up (see {@link dk.kb.datahandler.webservice.ContextListener}) should use
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
     * Initialized the configuration from the provided configFile and (optional) devops/operations properties
     * override file.
     * This should normally be called from {@link dk.kb.datahandler.webservice.ContextListener} as
     * part of web server initialization of the container, using the two paths configured outside the project
     * (Tomcat context environment entries {@code application-config} and {@code application-properties-config},
     * see {@code conf/ocp/ds-datahandler.xml}).
     *
     * @param configFile the single YAML file which the configuration is loaded from: a plain file path, a
     *                    classpath resource name, or a path relative to the user's home
     *                    (see {@link Resolver#resolveURL(String)}).
     * @param propertiesOverrideFile the devops/operations properties override file (same path syntax as
     *                    {@code configFile}), or {@code null}/blank if none is configured. If a path is given but
     *                    cannot be resolved to an existing file, this is logged as an error and startup continues
     *                    without that source.
     * @throws IOException if the YAML configuration could not be located, loaded or parsed. A missing/unresolvable
     *                    {@code propertiesOverrideFile} does <em>not</em> throw - see above.
     */
    public static synchronized void initialize(String configFile, String propertiesOverrideFile) throws IOException {
        URL configUrl = Resolver.resolveURL(configFile);

        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
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
                           "secrets such as OAI target credentials and the Kaltura admin secret) will be missing " +
                           "or fall back to the YAML file.", propertiesOverrideFile, e);
            }
        }

        serviceConfig = builder.build();
        loadOaiTargets();

        oaiTimestampFolder = serviceConfig.getValue("timestamps.folder", String.class);
        dsStorageUrl = serviceConfig.getValue("storage.url", String.class);
        solrUpdateUrl = createSolrUpdateUrl();
        solrWriteCollectionUrl = serviceConfig.getValue("solr.update.url", String.class);
        solrQueryUrl = serviceConfig.getValue("solr.queryUrl", String.class);
        solrBatchSize = serviceConfig.getValue("solr.batchSize", Integer.class);
        dsPresentUrl = serviceConfig.getValue("present.url", String.class);

        // The kaltura.* block below is frequently absent or filled with non-functional placeholder values in the
        // checked-in ds-datahandler-behaviour.yaml (real values are supplied per-environment, e.g. via the
        // devops/operations properties override file) - so every read here is defensive: a missing key or a
        // value that cannot be parsed as the target type falls back to a safe default rather than failing
        // ServiceConfig.initialize(...) (and, transitively, every unit test that calls it) altogether.
        kalturaUrl = getOptionalString("kaltura.url", null);
        kalturaPartnerId = getOptionalInteger("kaltura.partnerId", null);
        kalturaUserId = getOptionalString("kaltura.userId", null);
        kalturaToken = getOptionalString("kaltura.token", null);
        kalturaTokenId = getOptionalString("kaltura.tokenId", null);
        //Do not use kaltura adminsecret, use token and tokenId instead.
        //Must not be shared or exposed. Use token,tokenId.
        kalturaAdminSecret = getOptionalString("kaltura.adminSecret", "");

        kalturaSessionDurationSeconds = getOptionalInt("kaltura.sessionDurationSeconds", 86400);
        kalturaSessionRefreshThreshold = getOptionalInt("kaltura.sessionRefreshThreshold", 3600);
        conversionProfileIdVideo = getOptionalInt("kaltura.conversionProfileIdVideo", 0);
        conversionProfileIdAudio = getOptionalInt("kaltura.conversionProfileIdAudio", 0);
        conversionQueueThreshold = getOptionalInt("kaltura.conversionQueueThreshold", 0);
        conversionQueueDelaySeconds = getOptionalInt("kaltura.conversionQueueDelaySeconds", 0);

        streamPathDomsRadioTv = getOptionalString("streams.domsRadioTvPath", null);
        streamPathPreservicaTv = getOptionalString("streams.preservicaTvPath", null);
        streamPathPreservicaRadio = getOptionalString("streams.preservicaRadioPath", null);

        oaiRetryTimes = getOptionalInt("oaiSettings.retryTimes", 5); // Defaulting to 5 retries
        oaiRetrySeconds = getOptionalInt("oaiSettings.retrySeconds", 600); // Defaulting to 10 minuts

        transcriptionsDropFolder = getOptionalString("transcriptions.dropFolder", null);
        transcriptionsCompletedFolder = getOptionalString("transcriptions.completedFolder", null);

        log.info("Initialised from config: '{}' with the following values: solrUpdateUrl: '{}', solrQueryUrl: '{}', " +
                "solrBatchSize: '{}', dsStorageUrl: '{}', dsPresentUrl: '{}', oaiRetryTimes: '{}', oaiRetrySeconds: '{}', transcriptionDropFolder: '{}', transcriptionCompletedFolder: '{}'",
               configFile, solrUpdateUrl, solrQueryUrl, solrBatchSize, dsStorageUrl, dsPresentUrl, oaiRetryTimes, oaiRetrySeconds, transcriptionsDropFolder
               ,transcriptionsCompletedFolder);

        Path folderPath = Paths.get(oaiTimestampFolder);
        if (Files.exists(folderPath)) {
            log.info("Oai timestamp folder:"+oaiTimestampFolder);
        }
        else {
            log.info("Oai timestamp folder not found:"+oaiTimestampFolder +" .Creating new folder:"+oaiTimestampFolder);
            Files.createDirectories(Paths.get(oaiTimestampFolder));
        }
    }

    /**
     * Combine configuration solr.update.url and solr.update.requestHandler to a solrUpdateUrl.
     * @return a string representing a URL to a solr request handler for updating the index.
     */
    private static String createSolrUpdateUrl() {
        String updateUrl = serviceConfig.getValue("solr.update.url", String.class);
        // Not present in every checked-in behaviour.yaml (see the kaltura.* comment above) - default to empty
        // so a URL is still produced (pointing straight at solr.update.url, with no request handler suffix).
        String requestHandler = getOptionalString("solr.update.requestHandler", "");
        try {
            return new URIBuilder(updateUrl + requestHandler).build().toURL().toString();
        } catch (MalformedURLException | URISyntaxException e) {
            log.warn("Error creating solr update url from URL: '{}' and requestHandler: '{}'", updateUrl, requestHandler);
            throw new RuntimeException(e);
        }
    }

    /**
     * @param key configuration key.
     * @param defaultValue value to use if {@code key} is absent.
     * @return the configured string, or {@code defaultValue} if the key is not present in any configuration source.
     */
    private static String getOptionalString(String key, String defaultValue) {
        return serviceConfig.getOptionalValue(key, String.class).orElse(defaultValue);
    }

    /**
     * @param key configuration key.
     * @param defaultValue value to use if {@code key} is absent, or its value cannot be parsed as an integer.
     * @return the configured integer, or {@code defaultValue}.
     */
    private static int getOptionalInt(String key, int defaultValue) {
        try {
            return serviceConfig.getOptionalValue(key, Integer.class).orElse(defaultValue);
        } catch (RuntimeException e) {
            log.warn("Configuration key '{}' could not be parsed as an integer. Falling back to default {}",
                     key, defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * @param key configuration key.
     * @param defaultValue value to use if {@code key} is absent, or its value cannot be parsed as an integer.
     * @return the configured integer, or {@code defaultValue} (possibly {@code null}).
     */
    private static Integer getOptionalInteger(String key, Integer defaultValue) {
        try {
            return serviceConfig.getOptionalValue(key, Integer.class).orElse(defaultValue);
        } catch (RuntimeException e) {
            log.warn("Configuration key '{}' could not be parsed as an integer. Falling back to default {}",
                     key, defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * Loads the {@code oaiTargets} list from configuration into {@link #oaiTargets}.
     * <p>
     * Unlike a simple atomic list (see e.g. {@code ds-storage}'s {@code allowedOrigins}), individual fields of an
     * individual target - most importantly {@code user}/{@code password} - are expected to be overridden
     * per-environment via the devops/operations properties override file, while the rest of a target's definition
     * (name, url, datasource, ...) stays in the checked-in YAML file. So instead of picking a single config source
     * to read the whole list from, this collects the set of target indices from <em>all</em> sources, then
     * resolves each individual field through the normal merged {@link #serviceConfig}, which lets a higher-ordinal
     * source (the properties override file, an environment variable, {@link #setRuntimeProperty}) override just
     * one field of one target without needing to redefine the whole target.
     */
    private static void loadOaiTargets() {
        SortedSet<Integer> indices = listIndices("oaiTargets");

        oaiTargets.clear();
        for (int i : indices) {
            String prefix = "oaiTargets[" + i + "].";
            String name = serviceConfig.getValue(prefix + "name", String.class);
            String url = serviceConfig.getValue(prefix + "url", String.class);
            String set = serviceConfig.getOptionalValue(prefix + "set", String.class).orElse(null);
            String datasource = serviceConfig.getValue(prefix + "datasource", String.class);
            String metadataPrefix = serviceConfig.getValue(prefix + "metadataPrefix", String.class);
            String description = serviceConfig.getValue(prefix + "description", String.class);
            String user = serviceConfig.getOptionalValue(prefix + "user", String.class).orElse(null);
            String password = serviceConfig.getOptionalValue(prefix + "password", String.class).orElse(null);
            String filterStr = serviceConfig.getOptionalValue(prefix + "filter", String.class).orElse("direct");
            String dateStampFormat = serviceConfig.getOptionalValue(prefix + "dateStampFormat", String.class).orElse("date");
            String fragmentServiceUrl = serviceConfig.getOptionalValue(prefix + "fragmentServiceUrl", String.class).orElse(null);

            OaiTargetDto.FilterEnum filter;
            try {
                filter = OaiTargetDto.FilterEnum.fromValue(filterStr);
            } catch (IllegalArgumentException e) {
                log.error("Filter '{}' for target name '{}' not supported. Supported filters are: {}",
                        filterStr, name, Arrays.toString(OaiTargetDto.FilterEnum.values()));
                throw e;
            }

            OaiTargetDto oaiTarget = new OaiTargetDto();
            oaiTarget.setName(name);
            oaiTarget.setUrl(url);
            oaiTarget.setSet(set);
            oaiTarget.setMetadataprefix(metadataPrefix);
            oaiTarget.setUsername(user);
            oaiTarget.setPassword(password);
            oaiTarget.setDatasource(datasource);
            oaiTarget.setDecription(description);
            oaiTarget.setFilter(filter);
            try {
                oaiTarget.setDateStampFormat(DateStampFormatEnum.fromValue(dateStampFormat));
            }
            catch(Exception e) {
                log.warn("dateStampFormat not 'day' or 'date':"+dateStampFormat);
            }
            oaiTarget.setFragmentServiceUrl(fragmentServiceUrl);

            oaiTargets.put(name, oaiTarget);

            log.info("Load OAI target from config:"+name);
        }

        log.info("Number of OAI targets loaded:"+oaiTargets.size());
    }

    /**
     * @param listKey the dotted path of a configured list, e.g. {@code "oaiTargets"}.
     * @return the set of indices ({@code listKey[i]}) defined by any currently loaded config source.
     */
    private static SortedSet<Integer> listIndices(String listKey) {
        Pattern indexPattern = Pattern.compile("^" + Pattern.quote(listKey) + "\\[(\\d+)]\\.");
        SortedSet<Integer> indices = new TreeSet<>();
        for (ConfigSource source : serviceConfig.getConfigSources()) {
            for (String name : source.getPropertyNames()) {
                Matcher m = indexPattern.matcher(name);
                if (m.find()) {
                    indices.add(Integer.parseInt(m.group(1)));
                }
            }
        }
        return indices;
    }

    /**
     * Direct access to the backing MicroProfile {@link Config}, for configurations with more flexible content
     * and/or if the service developer prefers key-based property access.
     * @return the backing SmallRye Config-handler for the configuration.
     */
    public static Config getConfig() {
        if (serviceConfig == null) {
            throw new IllegalStateException("The configuration should have been loaded, but was not");
        }
        return serviceConfig;
    }

    public static int getSolrBatchSize() {
    	return solrBatchSize;
    }

    /**
     * Get URL to solr write collection with updateHandler added as URL path. Most likely the url has "/update" appened.
     * To get the URL for the write collection without the updateHanler appended use this: {@link #solrWriteCollectionUrl}.
     */
    public static String getSolrUpdateUrl() {
        return solrUpdateUrl;
    }

    public static String getSolrWriteCollectionUrl() {
        return solrWriteCollectionUrl;
    }

    public static void setSolrWriteCollectionUrl(String solrWriteCollectionUrl) {
        ServiceConfig.solrWriteCollectionUrl = solrWriteCollectionUrl;
    }

    public static String getSolrQueryUrl() {
        return solrQueryUrl;
    }

    public static String getDsPresentUrl() {
        return dsPresentUrl;
    }

    public static String getDsStorageUrl() {
        return dsStorageUrl;
    }

    public static String getOaiTimestampFolder() {
        return oaiTimestampFolder;
    }

    public static HashMap<String, OaiTargetDto> getOaiTargets() {
        return oaiTargets;
    }

    public static int getOaiRetryTimes() {
        return oaiRetryTimes;
    }

    public static int getOaiRetrySeconds() {
        return oaiRetrySeconds;
    }

    public static String getKalturaUrl() {
        return kalturaUrl;
    }

    public static Integer getKalturaPartnerId() {
        return kalturaPartnerId;
    }

    public static String getKalturaUserId() {
        return kalturaUserId;
    }

    public static String getKalturaToken() {
        return kalturaToken;
    }

    public static String getKalturaTokenId() {
        return kalturaTokenId;
    }

    public static int getKalturaSessionDurationSeconds() {
        return kalturaSessionDurationSeconds;
    }

    public static int getKalturaSessionRefreshThreshold() {
        return kalturaSessionRefreshThreshold;
    }

    public static int getConversionProfileIdVideo() {
        return conversionProfileIdVideo;
    }

    public static int getConversionProfileIdAudio() {
        return conversionProfileIdAudio;
    }

    public static int getConversionQueueThreshold() {
        return conversionQueueThreshold;
    }

    public static int getConversionQueueDelaySeconds() {
        return conversionQueueDelaySeconds;
    }


    public static String getKalturaAdminSecret() {
        return kalturaAdminSecret;
    }

    public static String getTranscriptionsDropFolder() {
        return transcriptionsDropFolder;
    }

    public static String getTranscriptionsCompletedFolder() {
        return transcriptionsCompletedFolder;
    }

    public static String getStreamPathDomsRadioTv() {
        return streamPathDomsRadioTv;
    }

    public static String getStreamPathPreservicaTv() {
        return streamPathPreservicaTv;
    }

    public static String getStreamPathPreservicaRadio() {
        return streamPathPreservicaRadio;
    }

    public static  String getDBDriver() {
        return serviceConfig.getValue("db.driver", String.class);
    }

    public static  String getDBUrl() {
        return serviceConfig.getValue("db.url", String.class);
    }

    public static  String getDBUserName() {
        return serviceConfig.getValue("db.username", String.class);
    }

    public static  String getDBPassword() {
        // An empty password is a legitimate, intentional value (e.g. local databases with no authentication).
        // SmallRye's built-in String converter treats "" as "no value", which makes the non-optional getValue(...)
        // throw NoSuchElementException (SRCFG00040) instead of returning "" - so this uses getOptionalValue(...)
        // with an empty-string default to preserve the old kb-util behaviour.
        return serviceConfig.getOptionalValue("db.password", String.class).orElse("");
    }

    public static int getConnectionPoolSize() {
        return serviceConfig.getOptionalValue("db.connectionPoolSize", Integer.class).orElse(10); //Default 10
    }

    /**
     * Set (or overwrite) a single configuration property at runtime, without touching any configuration file
     * and without restarting the service. See {@code ds-storage}'s {@code ServiceConfig} for the full rationale.
     *
     * @param key   dotted property path, using the exact same syntax as the YAML configuration.
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
     * YAML file, properties override file, environment variables or system properties provide.
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
