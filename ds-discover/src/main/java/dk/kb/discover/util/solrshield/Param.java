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

import java.util.ArrayList;
import java.util.List;

/**
 * Generic representation of parameter config for a Solr component.
 */
public class Param<T extends Param<T>> extends ProfileElement<T> {
    public String name;
    public boolean enabled = false;
    public boolean allowed = false;
    public double weightConstant = 1000;
    public double weightFactor = 1000;
    public double maxValue = -1; // For number based params
    public int maxChars = 2000;  // For String based params

    public Param(Profile profile, YAML config) {
        this(profile, null, config);
    }
    
    public Param(Profile profile, String name, YAML config) {
        super(profile, name);
        this.name = name;
        enabled = config.getBoolean("default_enabled", enabled);
        allowed = config.getBoolean("allowed", allowed);
        weightConstant = config.getDouble("weight_constant", weightConstant);
        weightFactor = config.getDouble("weight_factor", weightFactor);
        maxValue = config.getDouble("max_value", maxValue);
        maxChars = config.getInteger("max_chars", maxChars);
    }

    @Override
    double getWeight() {
        return !enabled ? 0.0 :
                weightConstant;
    }

    public static class StringParam extends Param<StringParam> {
        public String value = null;

        public StringParam(Profile profile, YAML config) {
            super(profile, config);
            value = config.getString("default_value", value);
        }

        public void apply(String value) {
            this.value = value;
            enabled = true;
        }
    }

    public static class IntegerParam extends Param<IntegerParam> {
        public Integer value = null;

        public IntegerParam(Profile profile, YAML config) {
            super(profile, config);
            value = config.getInteger("default_value", value);
        }

        public void apply(Integer value) {
            this.value = value;
            enabled = true;
        }

        @Override
        double getWeight() {
            return !enabled ? 0.0 : super.getWeight() +
                    weightFactor * value;
        }
    }

    public static class FieldsParam extends Param<FieldsParam> {
        public List<String> fields = null;
        public List<String> allowedFields = null; // TODO: Check validity using allowed & denied
        public List<String> deniedFields = null;

        public FieldsParam(Profile profile, YAML config) {
            super(profile, config);
            fields = config.getList("default_fields", fields);
            allowedFields = config.getList("allowed_fields", allowedFields);
            deniedFields = config.getList("denied_fields", deniedFields);
        }

        @Override
        protected void deepCopyNonAtomicAttributes(FieldsParam clone) {
            super.deepCopyNonAtomicAttributes(clone);
            clone.fields = fields == null ? null : new ArrayList<>(fields);
            clone.allowedFields = allowedFields == null ? null : new ArrayList<>(allowedFields);
            clone.deniedFields = deniedFields == null ? null : new ArrayList<>(deniedFields);
        }

        public void apply(List<String> fieldNames) {
            fields = new ArrayList<>(fieldNames);
            enabled = true;
        }

        @Override
        double getWeight() {
            return !enabled ? 0.0 : super.getWeight() +
                    weightFactor * profile.getFieldsWeight(fields);
        }
    }
}
