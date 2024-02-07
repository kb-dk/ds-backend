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
 * Generic representation of parameter config for a Solr component.
 */
public class Param implements DeepCopyable<Param> {
    public String key;
    public boolean defaultEnabled = false;
    public boolean allowed = false;
    public double weightConstant = 1000;
    public double weightFactor = 1000;
    public double maxValue = -1;

    public Param(YAML config) {
        key = config.getString("key"); // Mandatory
        defaultEnabled = config.getBoolean("default_enabled", defaultEnabled);
        allowed = config.getBoolean("allowed", allowed);
        weightConstant = config.getDouble("weight_constant", weightConstant);
        weightFactor = config.getDouble("weight_factor", weightFactor);
        maxValue = config.getDouble("max_value", maxValue);
    }

    @Override
    public Param deepCopy() {
        try {
            return (Param) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(
                    "Got CloneNotSupportedException with super class Object. This should not happen", e);
        }
    }
}
