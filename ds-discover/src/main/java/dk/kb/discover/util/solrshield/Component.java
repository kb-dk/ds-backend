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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Representation of a Solr component, i.e. {@code search}, {@code facet}, {@code highlight}...
 */
public class Component implements DeepCopyable<Component> {
    public String name;
    public boolean defaultEnabled = false;
    public boolean allowed = false;
    public double weightConstant = 100;
    public Map<String, Param> params = new HashMap<>(); // Not set in base Component

    public Component(YAML config) {
        name = config.getString("name"); // Mandatory
        defaultEnabled = config.getBoolean("default_enabled", defaultEnabled);
        allowed = config.getBoolean("allowed", allowed);
        weightConstant = config.getDouble("weight_constant", weightConstant);
    }

    @Override
    public Component deepCopy() {
        Component clone;
        try {
            clone = (Component) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(
                    "Got CloneNotSupportedException with super class Object. This should not happen", e);
        }
        deepCopyNonAtomicAttributes(clone);
        return clone;
    }

    /**
     * Creates deep copies of all non-atomic attributes and assigns them to {@code dest}.
     * @param dest the destination for the non-atomic attributes.
     */
    protected void deepCopyNonAtomicAttributes(Component dest) {
        dest.params = params.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().deepCopy()));
    }

}
