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
import org.eclipse.microprofile.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Special purpose map that supports
 * <ul>
 *     <li>Default values that can be overridden by user values</li>
 *     <li>Forced values that overrides user values</li>
 * </ul>
 * The map is Solr-aware and treats {@code fq} as special-case, where default {@code fq}s are overridden by
 * user {@code fq}s and forced {@code fq}s merges with all other {@code fq}s.
 * The map ignores attempts of adding {@code null} values.
 * Standard use case for the merger is to request an instance from {@link SolrParamMerger.Factory},
 * add user-provided parameters and used the resulting param map for sending a request to Solr.
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
     * Convenience method that wraps the String representation of the given value as a list.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key. If null, it will be ignored.
     * @return the previous value that was associated with the key.
     */
    public List<String> put(String key, Object value) {
        failIfFrozen();
        if (value == null || Objects.toString(value).isEmpty()) {
            return super.get(key);
        }
        return super.put(key, Collections.singletonList(Objects.toString(value)));
    }

    /**
     * Convenience method that adds the given value to the existing values (if any) for the given key.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key. If null, it will be ignored.
     * @return the previous values that were associated with the key.
     */
    public List<String> add(String key, Object value) {
        failIfFrozen();
        if (value == null) {
            return super.get(key);
        }
        if (value instanceof List) {
            return add(key, (List<?>)value);
        }
        if (value instanceof Object[]) {
            return addArray(key, (Object[])value);
        }
        if (value.getClass().isArray()) { // int[], boolean[] etc.
            throw new UnsupportedOperationException("Adding atomic arrays is not currently supported");
        }
        if (Objects.toString(value).isEmpty()) {
            return super.get(key);
        }
        ArrayList<String> values = super.containsKey(key) ?
                new ArrayList<>(super.get(key)) :
                new ArrayList<>();
        values.add(Objects.toString(value));
        return super.put(key, values);
    }

    /**
     * Convenience method that adds the given values to the existing values (if any) for the given key.
     *
     * @param key key with which the specified value is to be associated.
     * @param values value to be associated with the specified key. If null, it will be ignored.
     * @return the previous values that were associated with the key.
     */
    public List<String> addArray(String key, Object[] values) {
        failIfFrozen();
        if (values == null || values.length == 0) {
            return super.get(key);
        }
        return add(key, Arrays.asList(values));
    }

    /**
     * Convenience method that adds the given values to the existing values (if any) for the given key.
     *
     * @param key key with which the specified value is to be associated.
     * @param values value to be associated with the specified key. If null, it will be ignored.
     * @return the previous values that were associated with the key.
     */
    public List<String> add(String key, List<?> values) {
        failIfFrozen();
        if (values == null || values.isEmpty()) {
            return super.get(key);
        }
        ArrayList<String> merged = super.containsKey(key) ?
                new ArrayList<>(super.get(key)) :
                new ArrayList<>();
        values.stream()
                .filter(Objects::nonNull)
                .map(Objects::toString)
                .forEach(merged::add);
        return super.put(key, merged);
    }

    /**
     * Convenience method for putting all single value parameters in the given {@code map}
     * as lists of String representations.
     *
     * @param map mappings to put.
     */
    public void putAllSingle(Map<? extends String, ?> map) {
        failIfFrozen();
        map.forEach(this::put);
    }

    /**
     * Add the values from {@code map} to the existing values for the matching parameters.
     *
     * @param map mappings to add.
     */
    public void addAll(Map<? extends String, ?> map) {
        failIfFrozen();
        map.forEach(this::add);
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
     * If frozen, the merger is unfrozen.
     *
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
        if (value == null || value.isEmpty()) {
            return super.get(key);
        }
        return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<String>> m) {
        failIfFrozen();
        m.forEach(this::put);
    }

    @Override
    public List<String> putIfAbsent(String key, List<String> value) {
        failIfFrozen();
        if (value == null || value.isEmpty()) {
            return super.get(key);
        }
        return super.putIfAbsent(key, value);
    }

    @Override
    public boolean replace(String key, List<String> oldValue, List<String> newValue) {
        failIfFrozen();
        if (newValue == null || newValue.isEmpty()) {
            return false;
        }
        return super.replace(key, oldValue, newValue);
    }

    @Override
    public List<String> replace(String key, List<String> value) {
        failIfFrozen();
        if (value == null || value.isEmpty()) {
            return super.get(key);
        }
        return super.replace(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        failIfFrozen();
        return super.remove(key, value);
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

    public boolean isFrozen() {
        return frozen;
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
         *     # values, they will override any fq specified under defaultParams.
         *     defaultParams:
         *       # Compensate for a Solr bug causing crashes when the config has this
         *       # parameter as default for the /select handler.
         *       # This param should be made part of solrconfig.xml when the Solr bug
         *       # has been resolved
         *       maxCollationRetries: 10
         *     # Parameters that are forced for all queries, overriding params from the request.
         *     # The values of the params are either scalars or lists of scalars.
         *     # The param 'fq' is special as it appends to any existing 'fq' while all other
         *     # params are overwritten
         *     forcedParams:
         * </pre>
         *
         * @param handler a Solr handler as specified in the configuration, i.e. {@code select} or {@code mlt}.
         */
        public Factory(String handler) {
            defaultParams = getParams("solr." + handler + ".defaultParams");
            forcedParams = getParams("solr." + handler + ".forcedParams");
            if (defaultParams.isEmpty() && forcedParams.isEmpty()) {
                log.info("No configuration entry for 'solr.{}'. " +
                         "There will be no default or forced parameters", handler);
            }
        }

        /**
         * Create a merger, ready for adding user provided params.
         *
         * @return a merger ready for input.
         */
        public SolrParamMerger createMerger() {
            return new SolrParamMerger(defaultParams, forcedParams);
        }

        /**
         * Pattern matching a single flattened list entry below a {@code keyPrefix}, e.g. for
         * {@code keyPrefix = "solr.select.defaultParams"} it matches
         * {@code solr.select.defaultParams.facet.field[3]}, capturing {@code facet.field} (group 1, the literal
         * Solr param name - which may itself contain dots, as Solr param names commonly do, e.g.
         * {@code spellcheck.maxCollationRetries}) and {@code 3} (group 2, the list index).
         */
        private static final Pattern INDEXED_ENTRY_PATTERN = Pattern.compile("^(.+)\\[(\\d+)]$");

        /**
         * Create a Solr param map from the given dotted config path (e.g. {@code solr.select.defaultParams}).
         * Each direct child of {@code keyPrefix} becomes one Solr param, using its full remaining property name
         * (which may itself contain dots, as Solr param names commonly do) as the map key. A child may be a plain
         * scalar or a YAML list of scalars (flattened by SmallRye Config's YAML source into
         * {@code keyPrefix.paramName[0]}, {@code keyPrefix.paramName[1]}, ...).
         *
         * @param keyPrefix the location in the config for the params.
         * @return a Solr param map. Empty if nothing is configured under {@code keyPrefix}.
         */
        private static Map<String, List<String>> getParams(String keyPrefix) {
            Config config = ServiceConfig.getConfig();
            String prefix = keyPrefix + ".";

            Map<String, String> scalars = new LinkedHashMap<>();
            Map<String, SortedMap<Integer, String>> indexed = new LinkedHashMap<>();

            for (String propertyName : config.getPropertyNames()) {
                if (!propertyName.startsWith(prefix)) {
                    continue;
                }
                String remainder = propertyName.substring(prefix.length());
                Matcher indexedMatcher = INDEXED_ENTRY_PATTERN.matcher(remainder);
                String value = config.getOptionalValue(propertyName, String.class).orElse(null);
                if (value == null || value.isEmpty()) {
                    continue;
                }
                if (indexedMatcher.matches()) {
                    String paramName = indexedMatcher.group(1);
                    int index = Integer.parseInt(indexedMatcher.group(2));
                    indexed.computeIfAbsent(paramName, k -> new TreeMap<>()).put(index, value);
                } else {
                    scalars.put(remainder, value);
                }
            }

            Map<String, List<String>> result = new LinkedHashMap<>();
            scalars.forEach((paramName, value) -> result.put(paramName, Collections.singletonList(value)));
            indexed.forEach((paramName, valuesByIndex) -> result.put(paramName, new ArrayList<>(valuesByIndex.values())));
            return result;
        }
    }
}
