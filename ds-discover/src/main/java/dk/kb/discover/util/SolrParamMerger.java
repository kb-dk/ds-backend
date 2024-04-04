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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Special purpose map that supports
 * <ul>
 *     <li>Default values that can be overridden by user values</li>
 *     <li>Forced values that overrides user values</li>
 * </ul>
 * The map is Solr-aware and treats {@code fq} as special-case, where default {@code fq}s are overridden by
 * user {@code fq}s and forced {@code fq}s merges with all other {@code fq}s.
 * <p>
 * Standard use case for the merger is to request an instance from {@link SolrParamMerger.Factory},
 * add user-provided parameters and used the resulting param map for sending a request to Solr.
 * <p>
 * Note that any call to a getter or similar method automatically calls {@link #freeze()}, after which
 * it is no longer possible to add more parameters. The will be reset if {@code #clear} is called.
 */
public class SolrParamMerger extends LinkedHashMap<String, List<String>> {
    private static final Logger log = LoggerFactory.getLogger(SolrParamMerger.class);

    private final Map<String, List<String>> defaultParams;
    private final Map<String, List<String>> forcedParams;

    private boolean frozen = false;

    private SolrParamMerger(Map<String, List<String>> defaultParams,
                            Map<String, List<String>> forcedParams) {
        this.defaultParams = defaultParams;
        super.putAll(defaultParams);
        this.forcedParams = forcedParams;
    }

    /**
     * Convenience method that wraps the given value as a list.
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return the previous value that was associated with the key.
     */
    public List<String> put(String key, String value) {
        failIfFrozen();
        return super.put(key, Collections.singletonList(value));
    }

    /**
     * Convenience method for adding all single value parameters in the given {@code map}
     * as lists of String.
     * @param map mappings to added.
     */
    public void putAllSingle(Map<? extends String, ? extends String> map) {
        failIfFrozen();
        map.forEach(this::put);
    }

    /**
     * If frozen, clear unfreezes the merger. Default values are not added after clear.
     * @see #clear(boolean addDefaultValues)
     */
    @Override
    public void clear() {
        clear(false);
    }

    /**
     * Clears the merger and adds default parameters.
     * <p>
     * If frozen, the merger is unfrozen.
     * @param addDefaultValues if true, {@link #defaultParams} are added after clearing existing values.
     */
    public void clear(boolean addDefaultValues) {
        frozen = false;
        super.clear();
        if (addDefaultValues) {
            super.putAll(defaultParams);
        }
    }

    @Override
    public List<String> get(Object key) {
        freeze();
        return super.get(key);
    }

    @Override
    public List<String> getOrDefault(Object key, List<String> defaultValue) {
        freeze();
        return super.getOrDefault(key, defaultValue);
    }

    @Override
    public boolean containsValue(Object value) {
        freeze();
        return super.containsValue(value);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, List<String>> eldest) {
        failIfFrozen();
        return super.removeEldestEntry(eldest);
    }

    @Override
    public Set<String> keySet() {
        freeze();
        return super.keySet();
    }

    @Override
    public Collection<List<String>> values() {
        freeze();
        return super.values();
    }

    @Override
    public Set<Map.Entry<String, List<String>>> entrySet() {
        freeze();
        return super.entrySet();
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super List<String>> action) {
        freeze();
        super.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super String, ? super List<String>, ? extends List<String>> function) {
        freeze();
        super.replaceAll(function);
    }

    @Override
    public List<String> put(String key, List<String> value) {
        failIfFrozen();
        return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<String>> m) {
        failIfFrozen();
        super.putAll(m);
    }

    @Override
    public List<String> putIfAbsent(String key, List<String> value) {
        failIfFrozen();
        return super.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        failIfFrozen();
        return super.remove(key, value);
    }

    @Override
    public boolean replace(String key, List<String> oldValue, List<String> newValue) {
        failIfFrozen();
        return super.replace(key, oldValue, newValue);
    }

    @Override
    public List<String> replace(String key, List<String> value) {
        failIfFrozen();
        return super.replace(key, value);
    }

    @Override
    public List<String> computeIfAbsent(String key, Function<? super String, ? extends List<String>> mappingFunction) {
        failIfFrozen();
        return super.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public List<String> computeIfPresent(String key, BiFunction<? super String, ? super List<String>, ? extends List<String>> remappingFunction) {
        failIfFrozen();
        return super.computeIfPresent(key, remappingFunction);
    }

    @Override
    public List<String> compute(String key, BiFunction<? super String, ? super List<String>, ? extends List<String>> remappingFunction) {
        failIfFrozen();
        return super.compute(key, remappingFunction);
    }

    @Override
    public List<String> merge(String key, List<String> value, BiFunction<? super List<String>, ? super List<String>, ? extends List<String>> remappingFunction) {
        failIfFrozen();
        return super.merge(key, value, remappingFunction);
    }

    /**
     * Covenience method that throws an exception if the merger has been frozen.
     * Called from mutators.
     */
    private void failIfFrozen() {
        if (frozen) {
            throw new IllegalStateException("Mutator method called on frozen merger." +
                                            "All mutations must have finished before any getters are called");
        }
    }

    /**
     * Apply forced parameters, effectively finalizing the params for use.
     * <p>
     * After freezing it is no longer possible to add parameters.
     */
    private void freeze() {
        if (frozen) {
            return;
        }
        forcedParams.forEach((k, v) -> {
            if ("fq".equals(k)) { // Forced fq is additive as Solr fq's always stack
                if (super.containsKey("fq")) {
                    List<String> fq = new ArrayList<>(super.get("fq")); // Ensure the list is mutable
                    fq.addAll(v);
                    super.put("fq", fq);
                    return;
                }
            }
            super.put(k, v);
        });
        frozen = true;
    }


    /**
     * Cached default- and forced-params for cheap construction of {@link SolrParamMerger}s.
     */
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
         * @param handler a Solr handler as specified in the configuration, i.e. {@code select} or {@code mlt}.
         */
        public Factory(String handler) {
            if (!ServiceConfig.getConfig().containsKey("solr." + handler)) {
                log.info("No configuration entry for 'solr.{}'. " +
                         "There will be no default or forced parameters", handler);
            }
            defaultParams = getParams("solr." + handler + ".defaultparams");
            forcedParams = getParams("solr." + handler + ".forcedparams");
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

    }

    /**
     * Converts entries to pairs, with conversion of arrays and lists to String lists.
     * @param entry a Solr param entry.
     * @return a key-value pair with the entry data or null if {@link Map.Entry#getValue()} is an empty String array.
     */
    private static Pair<String, List<String>> toPair(Map.Entry<String, Object> entry) {
        List<String> vals;
        if (entry.getValue() instanceof String[]) {
            vals = Arrays.asList((String[])entry.getValue());
        } else if (entry.getValue() instanceof List) {
            vals = ((List<?>)entry.getValue()).stream()
                    .map(Objects::toString)
                    .collect(Collectors.toList());
        } else {
            vals = Collections.singletonList(Objects.toString(entry.getValue()));
        }
        return vals.isEmpty() ? null : new Pair<>(entry.getKey(), vals);
    }
}
