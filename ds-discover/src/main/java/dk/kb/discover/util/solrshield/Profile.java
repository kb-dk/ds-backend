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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Representation of a given profile for SolrShield.
 * <p>
 * The implementation uses the <a href="https://en.wikipedia.org/wiki/Prototype_pattern">Prototype Pattern</a> to
 * avoid maintaining 1:1 mapped configuration and instance objects.
 * <p>
 * The profile is used for calculating the weight of a query. Different components (search, facet, highlight...)
 * are dependent on each other and on the fields. To handle interdependency, all elements of the profile are aware
 * of each other.
 */
public class Profile extends ProfileElement<Profile> {
    private static final Logger log = LoggerFactory.getLogger(Profile.class);

    /**
     * The base cost of issuing a request, no matter the nature of the request.
     */
    public double weightConstant = 1000;

    /**
     * Ideally all fields requested are defined in {@link #fields}. {@code unlistedFieldsAllowed} controls what
     * action SolrShield takes if an unlisted/undefined field is requested.
     * <p>
     * If true, unknown fields are accepted. They will be assigned the weight {@link #unlistedFieldsWeight}.
     * If false, specifying a field that is not in {@link #fields} will mark the request as not valid.
     */
    public boolean unlistedFieldsAllowed = false;
    /**
     * If a requested field is not present in {@link #fields}, it will be assigned this weight. 
     */
    public double unlistedFieldsWeight = 100.0;

    /**
     * Ideally all request parameters are known by SolrShield. {@code unlistedParamsAllowed} controls what
     * action SolrShield takes is an unknown parameter is requested.
     * <p>
     * If true, unknown parameters are accepted. They will be assigned the weight {@link #unlistedParamsWeight}.
     * If false, specifying a parameter that is unknown will mark the request as not valid.
     * <p>
     * It is highly recommended to set this to {@code false}, making the parameters is SolrShield act as a 
     * whitelist of what is permissible.
     */
    public boolean unlistedParamsAllowed = false;
    public double unlistedParamsWeight = 1000.0;

    /**
     * Unhandled params after call to {@link #apply(Iterable)} are stored here.
     * Each call overwrites the previous collection of unhandled params.
     */
    public Map<String, String[]> unhandledParams = new HashMap<>();

    /**
     * Fields known by SolrShield. This list should ideally contain all fields in the backing Solr(s).
     * <p>
     * {@code fields} maps from field name to field definition, where the definition currently holds the weight
     * of the field. This might be extended at a later point.
     * <p>
     * Guidelines for the weight of a field is that the scale goes from 1 to 1000, where
     * <ul>
     *   <li>"An integer field" is 1</li>
     *   <li>"A DocValued StrField, where the corpus holds a few thousand short unique values" is 5</li>
     *   <li>"A DocValued StrField, where the values are mostly unique" is 10</li>
     *   <li>"A TextField with a 5-10 words" is 50 (text fields are markedly heavier to process than StrFields)</li>
     *   <li>"A TextField with a 100-1000 words" is 100</li>
     *   <li>"A TextField with a full transcription of hours of speech" is 1000 (about 50 book pages)</li>
     *   <li>"A TextField holding a full book of hundreds of pages" is 5000 (yes, it breaks the scale)</li>
     * </ul>
     */
    public Map<String, Field> fields;

    /**
     * First class search component. Always present, always enabled.
     */
    public SearchComponent search;

    /**
     * First class facet component. Always present, but might not be enabled.
     */
    public FacetComponent facet;

    /**
     * Create a base setup for SolrShield. and initialize based on the setup specified in {@code config}.
     * <p>
     * Note: This configuration must state the SolrShield properties directly.
     * @param config a SolrShield configuration.
     */
    public Profile(YAML config) {
        super(null, "config");
        weightConstant = config.getDouble("weightConstant", weightConstant);

        unlistedFieldsAllowed = config.getBoolean("unlistedFields.allowed", unlistedFieldsAllowed);
        unlistedFieldsWeight = config.getDouble("unlistedFields.weight", unlistedFieldsWeight);
        fields = getFields(config);

        unlistedParamsAllowed = config.getBoolean("unlistedParams.allowed", unlistedParamsAllowed);
        unlistedParamsWeight = config.getDouble("unlistedParams.weight", unlistedParamsWeight);

        search = new SearchComponent(this, config.getSubMap("components.search")); // Mandatory
        facet = new FacetComponent(this, config.getSubMap("components.facet"));    // Mandatory

        log.info("Created base SorShield config " + this);
    }

    /**
     * Extract the fields and their weights from the given {@code config}.
     * @param config a SolrShield configuration.
     * @return a map of Solr fields with corresponding weights.
     */
    private Map<String, Field> getFields(YAML config) {
        if (!config.containsKey("fields")) {
            throw new NullPointerException(
                    "Attempted to construct the list of fields from 'fields', " +
                            "but no list of field configurations was found");
        }
        double defaultWeight = config.getDouble("defaultField.weight", 1000.0);
        YAML EMPTY = new YAML();

        return config.getSubMap("fields").entrySet().stream()
                .map(e -> new Field(this, e.getKey(),
                        e.getValue() == null ? EMPTY : new YAML((Map<String, Object>) e.getValue()), defaultWeight))
                .collect(Collectors.toMap(k -> k.name, v -> v));
    }

    /**
     * @return an independent/deep copy of this Profile.
     */
    public Profile deepCopy() {
        return super.deepCopy(null);
    }

    /**
     * Create a deep copy of this Profile, apply the parameters in the request and return the updated copy.
     * @param request a Solr request, represented as map of {@code key, values}.
     */
    public Profile apply(Iterable<Map.Entry<String, String[]>> request) {
        Profile clone = deepCopy();
        Set<String> processedKeys = new HashSet<>();
        processedKeys.addAll(clone.search.apply(request));
        processedKeys.addAll(clone.facet.apply(request));

        clone.unhandledParams =
                StreamSupport.stream(request.spliterator(), false)
                        .filter(e -> !processedKeys.contains(e.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return clone;
    }

    @Override
    protected void deepCopyNonAtomicAttributes(Profile clone) {
        clone.profile = clone; // Profile is the top element
        super.deepCopyNonAtomicAttributes(clone);
        clone.fields = fields.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().deepCopy(clone.profile)));
        clone.search = search.deepCopy(clone.profile);
        clone.facet = facet.deepCopy(clone.profile);
    }

    @Override
    public String toString() {
        return "Profile{" +
                "unlistedFieldsAllowed=" + unlistedFieldsAllowed +
                ", unlistedFieldsWeight=" + unlistedFieldsWeight +
                ", unlistedParamsAllowed=" + unlistedParamsAllowed +
                ", unlistedParamsWeight=" + unlistedParamsWeight +
                ", fields=" + fields +
                ", components.search=" + search +
                ", components.facet=" + facet +
                '}';
    }

    @Override
    public double getWeight() {
        return weightConstant + search.getWeight() + facet.getWeight();
        // TODO: unlistedParamsWeight
    }

    @Override
    public boolean isAllowed(List<String> reasons) {
        boolean allowed = true;
        // Bitwise and to ensure that isAllowed is evaluated so that all reasons for not allowing are collected
        allowed &= search.isAllowed(reasons);
        allowed &= facet.isAllowed(reasons);
        if (!unhandledParams.isEmpty() && !unlistedParamsAllowed) {
            reasons.add("Unlisted params not allowed but got " + toString(unhandledParams));
            allowed = false;
        }
        return allowed;
    }

    // TODO: Handle score (free?) and * (expand to all known fields)
    /**
     * Calculate the sum of weights for the given {@code fields}.
     * @param fields a list of Solr fields.
     * @return the sum of field weights.
     */
    public double getFieldsWeight(List<String> fields) {
        return fields.stream()
                .map(this::getFieldWeight)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    /**
     * Resolve the field weight. If the field is not in {@link #fields}, the weight {@link #unlistedFieldsWeight}
     * will be used.
     * @param field a Solr field.
     * @return the weight of the field.
     */
    public double getFieldWeight(String field) {
        return this.fields.containsKey(field) ?
                this.fields.get(field).getWeight() :
                unlistedFieldsWeight;
    }

    private String toString(Map<String, String[]> map) {
        return map.entrySet().stream()
                .map(e -> e.getKey() + "=" + Arrays.toString(e.getValue()))
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
