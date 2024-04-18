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
import dk.kb.discover.util.solrshield.ProfileElement;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

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
    public boolean allowed = true;
    public double weightConstant = 0.0;
    public double weightFactor = 0.0;

    public boolean multiValue;
    protected V value; // Set by inheriting classes

    public Param(Profile profile, YAML config, boolean multiValue) {
        this(profile, null, config, multiValue);
    }
    
    public Param(Profile profile, String name, YAML config, boolean multiValue) {
        super(profile, name);
        if (profile == null) {
            throw new NullPointerException("Profile was null, but must be set for Params");
        }
        this.name = name;
        enabled = config.getBoolean("defaultEnabled", enabled);
        allowed = config.getBoolean("allowed", allowed);
        weightConstant = config.getDouble("weightConstant", weightConstant);
        weightFactor = config.getDouble("weightFactor", weightFactor);
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
    public double getWeight() {
        return !enabled ? 0.0 : weightConstant;
    }

    @Override
    public boolean isAllowed(List<String> reasons) {
        if (enabled && !this.allowed) {
            reasons.add("Param " + name + "=" + value + " not allowed as the param itself is not allowed");
            return false;
        }
        return true;
    }

}
