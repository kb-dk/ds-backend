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

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Representation of a given profile for SolrShield.
 * <p>
 * The implementation uses the <a href="https://en.wikipedia.org/wiki/Prototype_pattern">Prototype Pattern</a> to
 * avoid maintaining 1:1 mapped configuration and instance objects.
 */
public class Profile implements DeepCopyable<Profile> {
    private static final Logger log = LoggerFactory.getLogger(Profile.class);

    /*
    # Ideally all fields are listed under 'fields'.
    # 'unlisted_fields' controls handling of requests for unlisted fields
    unlisted_fields:
      allowed: true
      weight: 100
     */
    public boolean unlistedFieldsAllowed = false;
    public double unlistedFieldsWeight = 100.0;

    /*
    # How to handle parameters not specified in SolrShield.
    # This is shared between all components
     */
    public boolean unlistedParamsAllowed = false;
    public double unlistedParamsWeight = 1000.0;

    /*
    # The fields section assign base weight to each field.
    # The scale goes from 1 to 1000, where
    # "An integer field" is 1
    # "A DocValued StrField, where the corpus holds a few thousand short unique values" is 5
    # "A DocValued StrField, where the values are mostly unique" is 10
    # "A TextField with a 5-10 words" is 50 (text fields are markedly heavier to process than StrFields)
    # "A TextField with a 100-1000 words" is 100
    # "A TextField with a full transcription of hours of speech" is 1000 (about 50 book pages)
    # "A TextField holding a full book of hundreds of pages" is 5000 (yes, it breaks the scale)
    fields:
      - name: id
        weight: 10
      - name: resource_id
        weight: 10
      - name: origin
        weight: 5
     */
    public Map<String, Field> fields;

    /*
    # The component section covers the major Solr handlers, such as faceting and highlighting.
    # It also covers grouping and faceting, which are technically not handlers but conceptually on par.
     */
    public Map<String, Component> components;

    /**
     * Create a base setup for SolrShield. and initialize based on the setup specified in {@code config}.
     * <p>
     * Note: This configuration must state the SolrShield properties directly.
     * @param config a SolrShield configuration.
     */
    public Profile(YAML config) {
        unlistedFieldsAllowed = config.getBoolean("unlisted_fields.allowed", unlistedFieldsAllowed);
        unlistedFieldsWeight = config.getDouble("unlisted_fields.weight", unlistedFieldsWeight);
        fields = config.containsKey("fields") ?
                config.getYAMLList("fields").stream()
                        .map(Field::new)
                        .collect(Collectors.toMap(k -> k.name, v -> v)) :
                Collections.emptyMap();

        unlistedParamsAllowed = config.getBoolean("unlisted_farams.allowed", unlistedParamsAllowed);
        unlistedParamsWeight = config.getDouble("unlisted_farams.weight", unlistedParamsWeight);
        components = config.containsKey("components") ?
                config.getYAMLList("components").stream()
                        .map(Profile::createComponent)
                        .collect(Collectors.toMap(k -> k.name, v -> v)) :
                Collections.emptyMap();
    }

    private static Component createComponent(YAML config) {
        String name = config.getString("name", null);
        if (name == null) {
            throw new IllegalArgumentException("No 'name' key defined for Component");
        }
        switch (name) {
            case "search": return new SearchComponent(config);
            default: throw new UnsupportedOperationException("Component '" + name + "' not supported");
        }
    }

    @Override
    public Profile deepCopy() {
        Profile clone;
        try {
            clone = (Profile) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(
                    "Got CloneNotSupportedException with super class Object. This should not happen", e);
        }
        clone.fields = fields.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().deepCopy()));
        clone.components = components.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().deepCopy()));

        return clone;
    }
}
