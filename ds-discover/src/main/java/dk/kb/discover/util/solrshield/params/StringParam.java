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

import java.util.Arrays;
import java.util.List;

/**
 * String params have constant weight plus (weight factor * number of Strings).
 */
public class StringParam extends Param<StringParam, String[]> {
    public int maxChars = 2000;
    // TODO: blacklist/whitelist regexp (filtering {!...} or /regexp/ from query

    public StringParam(Profile profile, YAML config, boolean multiValue) {
        super(profile, config, multiValue);
        maxChars = config.getInteger("max_chars", maxChars);
        if (config.containsKey("default_value")) {
            value = new String[]{config.getString("default_value")};
        }
    }

    @Override
    protected void applyTypes(String[] values) {
        value = multiValue ? values : new String[]{values[0]};
    }

    @Override
    public double getWeight() {
        return !enabled ? 0.0 :
                weightConstant + weightFactor * value.length;
    }

    @Override
    public boolean isAllowed(List<String> reasons) {
        boolean allowed = super.isAllowed(reasons);
        if (!enabled || value == null) {
            return allowed;
        }
        if (!multiValue && value.length > 1) {
            reasons.add("Param " + name + "=" + Arrays.toString(value) + " not allowed it takes a single value " +
                    "but got " + value.length + " values");
        }
        long length = Arrays.stream(value)
                .map(String::length)
                .mapToInt(Integer::intValue)
                .sum();
        if (length >= maxChars) {
            reasons.add("Param " + name + "=" + Arrays.toString(value) + " not allowed as the values contained " +
                    length + " characters with maxChars=" + maxChars);
            allowed = false;
        }
        return allowed;
    }
}
