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
package dk.kb.discover;

import dk.kb.discover.config.ServiceConfig;
import dk.kb.discover.util.solrshield.SolrShield;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Singleton. Sets up {@link SolrService}s based on config and provides lookup of the services.
 */
public class SolrManager implements ServiceConfig.Observer {
    private static final Logger log = LoggerFactory.getLogger(SolrManager.class);

    private static final String SOLR_KEY = ".solr";
    private static final String COLLECTIONS_KEY = ".collections";
    private static final String SOLR_COLLECTION_KEY = ".collection";
    private static final String SOLR_SERVER_KEY = ".server";
    private static final String SOLR_PATH_KEY = ".path";
    private static final String SOLR_PATH_DEFAULT = "solr";
    private static final String SOLR_SHIELD_KEY = ".shield";

    private static final SolrManager instance = new SolrManager();
    private final Map<String, SolrService> solrs = new HashMap<>();
    private final Map<String, String> shieldPaths = new HashMap<>();
    private final Map<String, Optional<SolrShield>> shields = new HashMap<>();
    private Path configBaseDir;

    public SolrManager() {
        log.info("Creating SolrManager");
     //   ServiceConfig.registerObserver(this);
    }

    /**
     * @return the singleton SolrManager.
     */
    public static SolrManager getInstance() {
        return instance;
    }

    /**
     * Set the base directory for resolving relative shield config paths.
     * Relative shield paths (e.g. {@code dr-solrshield.yaml}) will be resolved against this directory.
     * @param configBaseDir the directory containing the application config files.
     */
    public void setConfigBaseDir(Path configBaseDir) {
        this.configBaseDir = configBaseDir;
        log.info("Config base directory set to '{}'", configBaseDir);
    }

    /**
     * Sets up SolrService instances as defined in the given config.
     * Called automatically when the configuration changes.
     * @param config setup for {@link SolrService}s.
     */
    @Override
    public synchronized void setConfig(YAML config) {       
        YAML majorConf = config.getSubMap(SOLR_KEY);
        List<YAML> solrConfs = majorConf.getYAMLList(COLLECTIONS_KEY);
        log.debug("setConfig called with with {} solr collections", solrConfs.size());

        solrs.values().forEach(SolrService::shutdown);
        solrs.clear();
        shieldPaths.clear();
        shields.clear();

        solrConfs.stream()
                .map(this::createSolrService)
                .filter(Objects::nonNull)
                .forEach(solrService -> solrs.put(solrService.getID(), solrService));

        log.debug("setConfig finished, SolrManager now contains solr services: {}", majorConf.keySet());
    }

    /**
     *
     * @param collection the abstract collection ID fot the {@link SolrService} to retrieve.
     * @return the {@link SolrService} with the given abstract collection ID.
     * @throws NotFoundServiceException if no Solr service with the given abstract collection ID could be found.
     */
    public static synchronized SolrService getSolrService(String collection) {        
        SolrService solrService = instance.solrs.get(collection);
        if (solrService == null) {
            throw new NotFoundServiceException("The Solr collection '{}' was not available", collection);
        }
        return solrService;
    }

    private SolrService createSolrService(YAML conf) {
        if (conf.size() != 1) {
            log.error("createSolrService: Expected a single entry in the configuration but there was {}." +
                     "Maybe indenting was not correct in the config file?", conf.size());
            return null;
        }
        String id = conf.keySet().stream().findFirst().orElseThrow();
        if (!conf.containsKey(id)) {
            log.error("createSolrService: No Solr configuration defined for collection '{}'", id);
        }
        YAML solrConf = conf.getSubMap(id);

        String solrCollection = solrConf.getString(SOLR_COLLECTION_KEY, null);
        if (solrCollection == null) {
            log.error("createSolrService: No solr collection (key={}) defined for abstract collection '{}'",
                      SOLR_COLLECTION_KEY, id);
            return null;
        }

        String server = solrConf.getString(SOLR_SERVER_KEY, null);
        if (server == null) {
            log.error("createSolrService: No server (key={}) defined for abstract collection '{}'",
                      SOLR_SERVER_KEY, id);
            return null;
        }

        String path = solrConf.getString(SOLR_PATH_KEY, SOLR_PATH_DEFAULT);

        String shieldPath = solrConf.getString(SOLR_SHIELD_KEY, null);
        if (shieldPath != null) {
            shieldPaths.put(id, shieldPath);
            log.info("Registered shield config path for collection '{}': {}", id, shieldPath);
        }

        return new SolrService(id, server, path, solrCollection);
    }

    /**
     * Get the {@link SolrShield} for the given collection, creating it lazily on first access.
     * @param collection the abstract collection ID.
     * @return the SolrShield for the collection, or empty if no shield is configured.
     */
    public static synchronized Optional<SolrShield> getShield(String collection) {
        return instance.getShieldInstance(collection);
    }

    private Optional<SolrShield> getShieldInstance(String collection) {
        if (shields.containsKey(collection)) {
            log.debug("Shield already loaded for collection '{}'", collection);
            return shields.get(collection);
        }

        String shieldPath = shieldPaths.get(collection);
        if (shieldPath == null) {
            log.debug("No shield configured for collection '{}'", collection);
            shields.put(collection, Optional.empty());
            return Optional.empty();
        }

        // Resolve relative shield paths against the config base directory
        if (configBaseDir != null && !Paths.get(shieldPath).isAbsolute()) {
            shieldPath = configBaseDir.resolve(shieldPath).toString();
        }

        try {
            log.info("Loading SolrShield for collection '{}' from '{}'", collection, shieldPath);
            YAML shieldConf = YAML.resolveLayeredConfigs(shieldPath);
            SolrShield shield = new SolrShield(shieldConf);
            Optional<SolrShield> result = Optional.of(shield);
            shields.put(collection, result);
            return result;
        } catch (Exception e) {
            log.error("Failed to load SolrShield for collection '{}' from '{}'. " +
                      "No shield will be active for this collection.", collection, shieldPath, e);
            shields.put(collection, Optional.empty());
            return Optional.empty();
        }
    }
}
