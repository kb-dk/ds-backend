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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generic representation of parameter config for a Solr component.
 *
 * @param <T> the inheriting class - needed for {@link ProfileElement#deepCopy(Profile)}.
 * @param <V> the value type for {@code Param}.
 */
public abstract class Param<T extends Param<?, ?>, V> extends ProfileElement<T> {
    private static final Logger log = LoggerFactory.getLogger(Param.class);

    public String name;
    public boolean enabled = false;
    public boolean allowed = false;
    public double weightConstant = 0.0;
    public double weightFactor = 0.0;

    public boolean multiValue = false;
    protected V value; // Set by inheriting classes

    public Param(Profile profile, YAML config, boolean multiValue) {
        this(profile, null, config, multiValue);
    }
    
    public Param(Profile profile, String name, YAML config, boolean multiValue) {
        super(profile, name);
        this.name = name;
        enabled = config.getBoolean("default_enabled", enabled);
        allowed = config.getBoolean("allowed", allowed);
        weightConstant = config.getDouble("weight_constant", weightConstant);
        weightFactor = config.getDouble("weight_factor", weightFactor);
        this.multiValue = multiValue;
    }

    /**
     * Assign the given values to the Param.
     * @param values array of values, represented as Strings.
     */
    public void apply(String[] values) {
        if (values == null || values.length == 0) {
            return;
        }
        if (!multiValue && values.length > 1) {
            log.debug("Param {} is single value but got {} values. Only the first value is used",
                    name, values.length);
            // TODO: Flag as invalid
        }
        enabled = true;
        applyTypes(values);
    }

    /**
     * Assign the given values to the Param.
     * <p>
     * {@code values} are guaranteed to contain at least 1 element.
     * <p>
     * {@link #enabled} will be set to true before this method is called. If the concrete {@code values} require
     * {@link #enabled} to be set to false, it is up to the method implementation to do so (e.g. for boolean switches).
     * @param values array of values, represented as Strings.
     */
    protected abstract void applyTypes(String[] values);

    public V getValue() {
        return value;
    }

    @Override
    double getWeight() {
        return !enabled ? 0.0 : weightConstant;
    }

    // TODO: blacklist/whitelist regexp (filtering {!...} or /regexp/ from query
    public static class StringParam extends Param<StringParam, String[]> {
        public int maxChars = 2000;

        public StringParam(Profile profile, YAML config, boolean multiValue) {
            super(profile, config, multiValue);
            maxChars = config.getInteger("max_chars", maxChars);
            if (config.containsKey("default_value")) {
                value = new String[]{config.getString("default_value")};
            }
        }

        @Override
        protected void applyTypes(String[] values) {
            value = multiValue ? values : new String[]{ values[0] };
        }

        @Override
        double getWeight() {
            return !enabled ? 0.0 :
                    weightConstant + weightFactor*value.length;
        }
    }

    /**
     * Integer params have constant weight plus (weight factor * value).
     */
    public static class IntegerParam extends Param<IntegerParam, Integer> {
        public double maxValue = Double.MAX_VALUE;

        public IntegerParam(Profile profile, YAML config) {
            super(profile, config, false); // Solr integer params are never multiValue
            maxValue = config.getDouble("max_value", maxValue);
            value = config.getInteger("default_value", value);
        }

        @Override
        protected void applyTypes(String[] values) {
            value = Integer.parseInt(values[0]);
        }

        @Override
        double getWeight() {
            return !enabled ? 0.0 : super.getWeight() +
                    weightFactor * value;
        }
    }

    /**
     * Boolean params have constant weight if true and zero weight is not true.
     */
    public static class BooleanParam extends Param<BooleanParam, Boolean> {
        public BooleanParam(Profile profile, YAML config) {
            super(profile, config, false); // Solr boolean params are never multiValue
            value = config.getBoolean("default_value", value);
        }

        @Override
        protected void applyTypes(String[] values) {
            this.value = Boolean.parseBoolean(values[0]);
            enabled = value;
        }
    }

    /**
     * Fields param have constant weight plus (weight factor * sum(fields weight)).
     */
    public static class FieldsParam extends Param<FieldsParam, List<String>> {
        public List<String> allowedFields = null; // TODO: Check validity using allowed & denied
        public List<String> deniedFields = null;

        public FieldsParam(Profile profile, YAML config) {
            super(profile, config, true); // Solr fields params are always multi valued
            value = config.getList("default_fields", value);
            allowedFields = config.getList("allowed_fields", allowedFields);
            deniedFields = config.getList("denied_fields", deniedFields);
        }

        @Override
        protected void deepCopyNonAtomicAttributes(FieldsParam clone) {
            super.deepCopyNonAtomicAttributes(clone);
            clone.value = value == null ? null : new ArrayList<>(value);
            clone.allowedFields = allowedFields == null ? null : new ArrayList<>(allowedFields);
            clone.deniedFields = deniedFields == null ? null : new ArrayList<>(deniedFields);
        }

        @Override
        protected void applyTypes(String[] values) {
            value = Arrays.asList(values);
        }

        @Override
        double getWeight() {
            return !enabled ? 0.0 : super.getWeight() +
                    // TODO: Consider if weightFactor is correct here
                    weightFactor * profile.getFieldsWeight(value);
        }
    }
}
