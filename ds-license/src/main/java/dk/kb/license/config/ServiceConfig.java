package dk.kb.license.config;

import dk.kb.license.model.v1.PlatformEnumDto;
import dk.kb.license.solr.SolrServerClient;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Sample configuration class using the Singleton pattern.
 * This should work well for most projects with non-dynamic properties.
 */
public class ServiceConfig {

    private static final Logger log = LoggerFactory.getLogger(ServiceConfig.class);

    public static String SOLR_FILTER_ID_FIELD = null;
    public static String SOLR_FILTER_RESOURCE_ID_FIELD = null;
    public static List<SolrServerClient> SOLR_SERVERS = Collections.emptyList();

    /**
     * Gets called when the code needs to call Solr backend servers
     * Updates the list of Solr servers, if the YAML file has been updated since last method call
     *
     * @return list of Solr servers
     */
    public static List<SolrServerClient> getSolrServers() {
        List<String> solr_servers = serviceConfig.getList("solr.servers");
        SOLR_SERVERS = solr_servers.stream().map(String::trim).map(SolrServerClient::new).collect(Collectors.toList());
        return SOLR_SERVERS;
    }

    /**
     * Besides parsing of YAML files using SnakeYAML, the YAML helper class provides convenience
     * methods like {@code getInteger("someKey", defaultValue)} and {@code getSubMap("config.sub1.sub2")}.
     */
    private static YAML serviceConfig;

    /**
     * Initialized the configuration from the provided configFile.
     * This should normally be called from {@link dk.kb.license.webservice.ContextListener} as
     * part of web server initialization of the container.
     *
     * @param configFile the configuration to load.
     * @throws IOException if the configuration could not be loaded or parsed.
     */
    public static synchronized void initialize(String... configFile) throws IOException {
        serviceConfig = YAML.resolveLayeredConfigs(configFile);
        serviceConfig.setExtrapolate(true);

        List<String> solr_servers = serviceConfig.getList("solr.servers");
        SOLR_FILTER_ID_FIELD = serviceConfig.getString("solr.idField");
        SOLR_FILTER_RESOURCE_ID_FIELD = serviceConfig.getString("solr.resourceField");

        SOLR_SERVERS = solr_servers.stream().map(String::trim).map(SolrServerClient::new).collect(Collectors.toList());

        log.info("Loaded solr-servers: " + solr_servers);
        log.info("Loaded solr id filter field: " + SOLR_FILTER_ID_FIELD);
        log.info("Loaded solr resourceId filter field: " + SOLR_FILTER_RESOURCE_ID_FIELD);
    }

    //For unittest
    public static void setSOLR_FILTER_FIELD(String sOLR_FILTER_FIELD) {
        SOLR_FILTER_ID_FIELD = sOLR_FILTER_FIELD;
    }

    /**
     * Direct access to the backing YAML-class is used for configurations with more flexible content
     * and/or if the service developer prefers key-based property access.
     *
     * @return the backing YAML-handler for the configuration.
     */
    public static YAML getConfig() {
        if (serviceConfig == null) {
            throw new IllegalStateException("The configuration should have been loaded, but was not");
        }
        return serviceConfig;
    }

    public static String getDBDriver() {
        String dbDriver = serviceConfig.getString("db.driver");
        return dbDriver;
    }

    public static String getDBUrl() {
        String dbUrl = serviceConfig.getString("db.url");
        return dbUrl;
    }

    public static String getDBUserName() {
        String dbUserName = serviceConfig.getString("db.username");
        return dbUserName;
    }

    public static String getDBPassword() {
        String dbPassword = serviceConfig.getString("db.password");
        return dbPassword;
    }

    public static String getKeycloakRealmTokenUrl() {
        String realmTokenUrl = serviceConfig.getString("keycloak.realmTokenUrl");
        return realmTokenUrl;
    }

    public static String getKeycloakClientSecret() {
        String clientSecret = serviceConfig.getString("keycloak.clientSecret");
        return clientSecret;
    }

    public static String getKeycloakRedirectUrl() {
        String redirectUrl = serviceConfig.getString("keycloak.redirectUrl");
        return redirectUrl;
    }

    public static String getKeycloakLoginUrl() {
        String loginUrl = serviceConfig.getString("keycloak.loginUrl");
        return loginUrl;
    }

    public static String getKeycloakRsaPublicKey() {
        String loginUrl = serviceConfig.getString("keycloak.rsaPublicKey");
        return loginUrl;
    }

    public static boolean isAdminGuiEnabled() {
        return serviceConfig.getBoolean("gui.adminGuiEnabled", false); //Default not enabled if property not set
    }

    public static YAML getRightsPlatformConfig(String platform) {
        Optional<YAML> result = serviceConfig.getYAMLList("rights.platforms").stream().filter(yaml -> yaml.getString("name", "").equals(platform)).findFirst();

        if (result.isPresent()) {
            return result.get();
        }
        return new YAML();
    }

    public static int getDrHoldbackLogicChangeDays() {
        YAML drPlatform = getRightsPlatformConfig(PlatformEnumDto.DRARKIV.getValue());

        if (drPlatform.isEmpty()) {
            throw new IllegalStateException("The DR platform config should have been loaded, but was not. Holdback cannot be calculated correctly.");
        }

        return drPlatform.getInteger("holdbackLogicChangeDays", 365);
    }

    public static int getDrHoldbackYearsForRadio() {
        YAML drPlatform = getRightsPlatformConfig(PlatformEnumDto.DRARKIV.getValue());

        if (drPlatform.isEmpty()) {
            throw new IllegalStateException("The DR platform config should have been loaded, but was not. Holdback cannot be calculated correctly.");
        }

        return drPlatform.getInteger("holdbackYearsForRadio", 3);
    }

    public static int getCacheRefreshTimeInSeconds() {
        return serviceConfig.getInteger("cache.reloadInSeconds", 300);
    }
}
