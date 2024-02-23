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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Fields param have constant weight plus (weight factor * sum(fields weight)).
 */
public class FieldsParam extends Param<FieldsParam, List<String>> {
    public boolean supportsStar = false;
    public boolean starEncountered = false;
    public Set<String> allowedFields;
    public Set<String> deniedFields;

    public FieldsParam(Profile profile, YAML config) {
        super(profile, config, true); // Solr fields params are always multi valued
        value = config.getList("default_fields", value);
        this.supportsStar = config.getBoolean("supports_star", supportsStar);
        allowedFields = new HashSet<>(config.getList("allowed_fields", Collections.emptyList()));
        deniedFields = new HashSet<>(config.getList("denied_fields", Collections.emptyList()));
    }

    @Override
    protected void deepCopyNonAtomicAttributes(dk.kb.discover.util.solrshield.params.FieldsParam clone) {
        super.deepCopyNonAtomicAttributes(clone);
        clone.value = value == null ? null : new ArrayList<>(value);
        clone.allowedFields = new HashSet<>(allowedFields);
        clone.deniedFields = new HashSet<>(deniedFields);
    }

    @Override
    protected void applyTypes(String[] values) {
        if (values == null || values.length == 0) {
            return;
        }
        if (values.length == 1 && values[0].contains(", ")) {
            // Comma separated fields are supported by Solr
            values = values[0].split(" *, *");
        }
        value = new ArrayList<>(Arrays.asList(values));
        if (value.contains("*")) { // '*' expands to all fields
            starEncountered = true;
            if (supportsStar) {
                Set<String> fields = new LinkedHashSet<>(value);
                fields.remove("*");
                fields.addAll(profile.fields.keySet());
                value = new ArrayList<>(fields);
            }
        }
    }

    @Override
    public double getWeight() {
        return !enabled ? 0.0 : super.getWeight() +
                // TODO: Consider if weightFactor is correct here
                weightFactor * profile.getFieldsWeight(value);
    }

    @Override
    public boolean isAllowed(List<String> reasons) {
        boolean allowed = super.isAllowed(reasons);
        if (!enabled || value == null) {
            return allowed;
        }
        if (!supportsStar && starEncountered) {
            allowed = false;
            reasons.add("Param '" + name + " contained '*' which is not allowed");
        }
        if (!profile.unlistedFieldsAllowed) {
            List<String> unknown = value.stream()
                    .filter(field -> !profile.fields.containsKey(field))
                    .collect(Collectors.toList());
            allowed &= unknown.isEmpty();
            if (!unknown.isEmpty()) {
                reasons.add("Param " + name + " contained fields " + unknown +
                        " which are not defined in SolrShield. Defined fields are " + profile.fields.keySet());
            }
        }
        if (!allowedFields.isEmpty()) {
            List<String> notAllowed = value.stream()
                    .filter(field -> !allowedFields.contains(field))
                    .collect(Collectors.toList());
            allowed &= notAllowed.isEmpty();
            if (!notAllowed.isEmpty()) {
                reasons.add("Param " + name + " contained fields " + notAllowed +
                        " which are not on the allowed list. Allowed fields are " + allowedFields);
            }
        }
        if (!deniedFields.isEmpty()) {
            List<String> isDenied = value.stream()
                    .filter(field -> deniedFields.contains(field))
                    .collect(Collectors.toList());
            allowed &= isDenied.isEmpty();
            if (!isDenied.isEmpty()) {
                reasons.add("Param " + name + " contained fields " + isDenied +
                        " which are on the denied list. denied fields are " + deniedFields);
            }
        }
        return allowed;
    }
}
