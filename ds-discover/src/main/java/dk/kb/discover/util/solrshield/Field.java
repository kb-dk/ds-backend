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

/**
 * Representation of a Solr field.
 */
public class Field extends ProfileElement<Field> {
    String name;
    double weight;

    /**
     * Construct the representation from {@code name} and {@code weight} in the given {@code fieldConfig}.
     *
     * @param name the name of the field.
     * @param fieldConfig configuration for a single field.
     * @param defaultWeight used if no {@code weight} is stated in {@code fieldConfig}.
     */
    public Field(Profile profile, String name, YAML fieldConfig, double defaultWeight) {
        super(profile, name);
        this.name = name;
        weight = fieldConfig.getDouble("weight", defaultWeight);
    }

    @Override
    double getWeight() {
        return weight;
    }
}
