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
import dk.kb.util.webservice.exception.NotFoundServiceException;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    private static final SolrManager instance = new SolrManager();
    private final Map<String, SolrService> solrs = new HashMap<>();

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

        return new SolrService(id, server, path, solrCollection);
    }
}
