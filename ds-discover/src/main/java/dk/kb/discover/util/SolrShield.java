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
package dk.kb.discover.util;

import dk.kb.discover.config.ServiceConfig;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Gateway for Solr calls, using a combination of whitelisted arguments as well as weighing of queries with an upper
 * limit on how much load the caller is allowed to effect on the backing server.
 * <p>
 * The {@code weight} is a score for how heavy (memory and/or processing power) an incoming request is expected
 * to be when issued against a Solr installation.
 * <p>
 * As of 2024-01-29 the architecture of SolrShield is simple on purpose, focusing of handling the current needs for
 * the Digitale Samlinger / DR Arkiv project at the Royal Danish Library. It is expected that future increase in
 * request complexity will cause rewriting of both code and configuration in the future.
 */
public class SolrShield {
    private static final Logger log = LoggerFactory.getLogger(SolrShield.class);

    public static final String ROOT_KEY = "solr.shield";
    public static final String ENABLED_KEY = "enabled";

    /**
     * If no explicit maxWeight is given when testing, whis weight is used.
     */
    public static final String MAX_WEIGHT_DEFAULT_KEY = "default.maxweight";

    private static YAML conf = null;
    private static boolean enabled = true;
    private static double defaultMaxWeight = -1;

    /**
     * Estimate the weight of the {@code request} and construct a {@link ShieldResponse} with the weight as well
     * as a boolean stated if the request is allowed to be issued.
     * <p>
     * This method used {@link #defaultMaxWeight} as {@code maxWeight}.
     * @param request a Solr request.
     * @return calculated weight etc.
     * @see #test(Map, double)
     */
    public static ShieldResponse test(Map<String, List<String>> request) {
        return test(request, defaultMaxWeight);
    }

    /**
     * Estimate the weight of the {@code request} and construct a {@link ShieldResponse} with the weight as well
     * as a boolean stated if the request is allowed to be issued.
     * @param request a Solr request.
     * @param maxWeight the maximum weight allowed.
     * @return calculated weight etc.
     * @see #test(Map)
     */
    public static ShieldResponse test(Map<String, List<String>> request, double maxWeight) {
        ensureConfig();
        if (!enabled) {
            return new ShieldResponse(request, maxWeight, true, "enabled==false: All requests allowed", -1);
        }

        ShieldResponse response = weigh(request).maxWeight(maxWeight);

        // Was a hard limit or an illegal argument ancounteres?
        if (!response.allowed) {
            return response;
        }

        // Is the weight acceptable?
        if (response.maxWeight < response.weight) {
            return response
                    .allowed(false)
                    .reason("maxWeight " + response.maxWeight + " < " + response.weight + ": Weight exceeded");
        }

        // All OK
        return response;
    }

    /**
     * Estimate the weight of the {@code request}.
     * <p>
     * This also checks for hard limits or non-allowed arguments. If any of those are triggered,
     * {@link ShieldResponse#allowed} is set to false, else it is set to true.
     * @param request a Solr request.
     * @return calculated weight etc.
     * @see #test(Map)
     */
    static ShieldResponse weigh(Map<String, List<String>> request) {
        // TODO: Implement this
        throw new UnsupportedOperationException("Implement this");
    }

    /**
     * Load the configuration for SolrShield, if not already loaded. The configuration for SolrShield is a sub-config
     * in the overall application config, located at {@code solr.shield}.
     */
    static void ensureConfig() {
        if (conf == null) {
            return;
        }
        if (!ServiceConfig.getConfig().containsKey(ROOT_KEY)) {
            log.warn("Warning: The sub-config for Solr Shield, expected at '{}', is unavailable. " +
                    "No Solr calls will be allowed. Set 'solr.shield-.enabled=false in config to disable SolrShield",
                    ROOT_KEY);
            conf = new YAML();
        }
        setConfig(ServiceConfig.getConfig().getSubMap(ROOT_KEY));
    }

    /**
     * Set the configuration for SolrShield.
     * This is kept as a separate method to enable other configuration sources than the default.
     * <p>
     *  Note: This configuration must state the SolrShield properties directly.
     *        It does not use the sub-configuration structure from {@link #ensureConfig()}.
     * @param solrShieldConf the configuration for SolrShield.
     */
    static void setConfig(YAML solrShieldConf) {
        conf = solrShieldConf;
        if (conf.containsKey(ROOT_KEY)) {
            log.warn("setConfig received a config which contains the key '{}'. This looks like an error: " +
                    "The setConfig method expects the SolrShield properties to be stated directly at root level",
                    ROOT_KEY);
        }
        enabled = conf.getBoolean(ENABLED_KEY, enabled);
        defaultMaxWeight = conf.getDouble(MAX_WEIGHT_DEFAULT_KEY, defaultMaxWeight);
        log.info("Initialized SolrShield: enabled={}, defaultMaxWeight={}",
                enabled, defaultMaxWeight);
    }

}
