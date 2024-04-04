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
import dk.kb.util.Pair;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Special purpose map that supports
 * <ul>
 *     <li>Default values that can be overridden by user values</li>
 *     <li>Forced values that overrides user values</li>
 * </ul>
 * The map is Solr-aware and treats {@code fq} as special-case, where default {@code fq}s are overridden by
 * user {@code fq}s and forced {@code fq}s merges with all other {@code fq}s.
 */
public class SolrParamMerger extends LinkedHashMap<String, List<String>> {
    private static final Logger log = LoggerFactory.getLogger(SolrParamMerger.class);

    private final Map<String, List<String>> defaultParams;
    private final Map<String, List<String>> forcedParams;

    private SolrParamMerger(Map<String, List<String>> defaultParams,
                            Map<String, List<String>> forcedParams) {
        this.defaultParams = defaultParams;
        putAll(defaultParams);
        this.forcedParams = forcedParams;
    }

    

    public static class Factory {
        private final Map<String, List<String>> defaultParams;
        private final Map<String, List<String>> forcedParams;

        /**
         * Create a merger factory for the given handler. Handlers are defined in the application configuration:
         * <pre>
         *solr:
         *   # /select specific config
         *   select:
         *     # Parameters that are default for all queries, but can be overridden by the request.
         *     # The values of the params are either scalars or lists of scalars.
         *     # Unless there are special reasons not to, default params should be specified
         *     # in solrconfig.xml.
         *     # The 'fq' param has no special status here: If the request contains 1 or more fq
         *     # values, they will override any fq specified under defaultparams.
         *     defaultparams:
         *       # Compensate for a Solr bug causing crashes when the config has this
         *       # parameter as default for the /select handler.
         *       # This param should be made part of solrconfig.xml when the Solr bug
         *       # has been resolved
         *       maxCollationRetries: 10
         *     # Parameters that are forced for all queries, overriding params from the request.
         *     # The values of the params are either scalars or lists of scalars.
         *     # The param 'fq' is special as it appends to any existing 'fq' while all other
         *     # params are overwritten
         *     forcedparams:
         * </pre>
         * @param handler a Solr handler as specified in the configuration.
         */
        public Factory(String handler) {
            defaultParams = getParams("select.defaultparams");
            forcedParams = getParams("select.forcedparams");
        }

        /**
         * Create a merger, ready for adding user provided params.
         * @return a merger ready for input.
         */
        public SolrParamMerger createMerger() {
            return new SolrParamMerger(defaultParams, forcedParams);
        }

        /**
         * Create a Solr param map from the given YAML path.
         * @param yPath the location in the config for the params.
         * @return a Solr param map.
         */
        private Map<String, List<String>> getParams(String yPath) {
            YAML conf = ServiceConfig.getConfig().containsKey(yPath) ?
                    ServiceConfig.getConfig().getYAML(yPath) :
                    null;
            if (conf == null) {
                return Collections.emptyMap();
            }

            return conf.entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .filter(e -> !e.getValue().toString().isEmpty())
                    .map(SolrParamMerger::toPair)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
        }

        /**
         * Create a new merger for the given handler. Handlers are defined in the application configuration.
         * <p>
         * When the addition of user provided parameters to the merger has finished, call {@link SolrParamMerger#applyForced}
         * before extracting values from the merger.
         * @return a new merger ready for receiving user provided parameters.
         */
        public static SolrParamMerger createMerger(String handler) {
        }
    }

    /**
     * Converts entries to pairs, with conversion of arrays to lists.
     * @param entry a Solr param entry.
     * @return a key-value pair with the entry data or null if {@link Map.Entry#getValue()} is an empty String array.
     */
    private static Pair<String, List<String>> toPair(Map.Entry<String, Object> entry) {
        if (entry.getValue() instanceof String[]) {
            String[] vals = (String[])entry.getValue();
            return vals.length == 0 ? null :
                    new Pair<>(entry.getKey(), Arrays.asList(vals));
        }
        return new Pair<>(entry.getKey(), Collections.singletonList(Objects.toString(entry.getValue())));
    }
}
