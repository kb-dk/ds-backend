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

/**
 * Representation of a Solr field.
 */
public class Field implements DeepCopyable<Field> {
    String name;
    double weight;

    /**
     * Construct the representation from the keys {@code name} and {@code weight} in the given {@code fieldConfig}.
     *
     * @param fieldConfig configuration for a single field.
     */
    public Field(YAML fieldConfig) {
        if (!fieldConfig.containsKey("name")) {
            throw new IllegalArgumentException("Every field must have a 'name'");
        }
        name = fieldConfig.getString("name");
        if (!fieldConfig.containsKey("weight")) {
            throw new IllegalArgumentException(
                    "Every field must have a 'weight' but field '" + name + "' did not have it");
        }
        weight = fieldConfig.getDouble("weight");
    }

    @Override
    public Field deepCopy() {
        try {
            return (Field) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(
                    "Got CloneNotSupportedException with super class Object. This should not happen", e);
        }
    }
}
