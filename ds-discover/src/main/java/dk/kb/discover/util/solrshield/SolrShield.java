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
package dk.kb.discover.util.solrshield;

import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Gateway for Solr calls, using a combination of whitelisted arguments as well as weighing of queries with an upper
 * limit on how much load the caller is allowed to effect on the backing server.
 * <p>
 * The {@code weight} is a score for how heavy (memory and/or processing power) an incoming request is expected
 * to be when issued against a Solr installation.
 * <p>
 * Each instance of SolrShield holds its own configuration and can be used independently, allowing per-collection
 * shield configurations.
 * <p>
 * The input for SolrShield is an {@code Iterable<Map.Entry<String, String[]>>}. This was chosen to align with
 * {@code SolrParams} / {@code SolrQuery} from SolrJ, in anticipation of a future switch to SolrJ and/or reuse of
 * SolrShield in a context where SolrJ is used.
 * Since a basic {@code Map<String, String[]>} implements the iterable signature, the choice should not make the
 * current use more cumbersome than other sane choices.
 */
public class SolrShield {
    private static final Logger log = LoggerFactory.getLogger(SolrShield.class);

    public static final String ENABLED_KEY = "enabled";

    /**
     * If no explicit maxWeight is given when testing, this weight is used.
     */
    public static final String MAX_WEIGHT_DEFAULT_KEY = "defaultMaxWeight";

    // --- Instance fields ---

    private YAML conf;
    private boolean enabled = true;
    private double defaultMaxWeight = -1;
    Profile profile;

    /**
     * Create a SolrShield instance from the given configuration.
     * The configuration must state the SolrShield properties directly at root level
     * (i.e. not wrapped under {@code solr.shield}).
     * @param solrShieldConf the configuration for this SolrShield instance.
     */
    public SolrShield(YAML solrShieldConf) {
        this.conf = solrShieldConf;
        if (conf.containsKey("solr.shield")) {
            log.warn("SolrShield constructor received a config which contains the key 'solr.shield'. This looks like an error: " +
                    "The config should state SolrShield properties directly at root level");
        }
        enabled = conf.getBoolean(ENABLED_KEY, enabled);
        defaultMaxWeight = conf.getDouble(MAX_WEIGHT_DEFAULT_KEY, defaultMaxWeight);
        profile = new Profile(conf);
        log.info("Initialized SolrShield: enabled={}, defaultMaxWeight={}, profile={}",
                enabled, defaultMaxWeight, profile);
    }

    // --- Instance methods ---

    /**
     * Estimate the weight of the {@code request} and construct a {@link Response} with the weight as well
     * as a boolean stated if the request is allowed to be issued.
     * This method uses {@link #defaultMaxWeight} as {@code maxWeight}.
     * @param request a Solr request.
     * @return calculated weight etc.
     */
    public Response evaluateRequest(Map<String, String[]> request) {
        return evaluateRequest(request.entrySet(), defaultMaxWeight);
    }

    /**
     * Estimate the weight of the {@code request} and construct a {@link Response} with the weight as well
     * as a boolean stated if the request is allowed to be issued.
     * This method uses {@link #defaultMaxWeight} as {@code maxWeight}.
     * @param request a Solr request.
     * @return calculated weight etc.
     */
    public Response evaluateRequest(Iterable<Map.Entry<String, String[]>> request) {
        return evaluateRequest(request, defaultMaxWeight);
    }

    /**
     * Estimate the weight of the {@code request} and construct a {@link Response} with the weight as well
     * as a boolean stated if the request is allowed to be issued.
     * @param request a Solr request.
     * @param maxWeight the maximum weight allowed.
     * @return calculated weight etc.
     */
    public Response evaluateRequest(Iterable<Map.Entry<String, String[]>> request, Double maxWeight) {
        Response response = weighRequest(request).maxWeight(maxWeight);

        // Is the weight acceptable?
        if (response.maxWeight < response.weight) {
            response = response
                    .allowed(false)
                    .addReason("maxWeight " + response.maxWeight + " < " + response.weight + ": Weight exceeded");
        }
        if (!enabled) {
            if (response.isAllowed()) {
                log.info("Solr request allowed: " + response);
            } else {
                log.warn("Solr request not allowed, but SolrShield is not enabled and will not raise that signal: " + response);
                response = response.allowed(true);
            }
        }

        return response;
    }

    /**
     * Estimate the weight of the {@code request}.
     * This also checks for hard limits or non-allowed arguments. If any of those are triggered,
     * {@link Response#allowed} is set to false, else it is set to true.
     * @param request a Solr request.
     * @return calculated weight etc.
     */
    Response weighRequest(Iterable<Map.Entry<String, String[]>> request) {
        Profile applied = profile.apply(request);
        List<String> reasons = new ArrayList<>();
        boolean allowed = applied.isAllowed(reasons);

        return new Response(request, defaultMaxWeight, allowed, reasons, applied.getWeight());
    }

}
