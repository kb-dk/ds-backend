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
package dk.kb.discover.util.solrshield.params;

import dk.kb.discover.util.solrshield.Profile;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Integer params have constant weight plus (weight factor * value).
 */
public class IntegerParam extends Param<IntegerParam, Integer> {
    public double maxValue = Double.maxValue;

    public IntegerParam(Profile profile, YAML config) {
        super(profile, config, false); // Solr integer params are never multiValue
        maxValue = config.getDouble("maxValue", maxValue);
        value = config.getInteger("default_value", value);
    }

    @Override
    protected void applyTypes(String[] values) {
        value = Integer.parseInt(values[0]);
    }

    @Override
    public double getWeight() {
        return !enabled ? 0.0 : super.getWeight() +
                weightFactor * value;
    }

    @Override
    public boolean isAllowed(List<String> reasons) {
        boolean allowed = super.isAllowed(reasons);
        if (!enabled || value == null) {
            return allowed;
        }
        if (value > maxValue) {
            reasons.add("Param " + name + "=" + value + " not allowed as it is larger than maxValue=" + maxValue);
            allowed = false;
        }
        return allowed;
    }
}
