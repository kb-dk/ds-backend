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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Representation of a Solr component, i.e. {@code search}, {@code facet}, {@code highlight}...
 */
public abstract class Component<T extends Component<T>> extends ProfileElement<T> {
    public boolean defaultEnabled = false;
    public boolean allowed = false;
    public double weightConstant = 100;
    public Map<String, Param<?>> params = new HashMap<>(); // Not set in base Component

    public boolean enabled;

    public Component(Profile profile, String name, YAML config) {
        super(profile, name);
        defaultEnabled = config.getBoolean("default_enabled", defaultEnabled);
        allowed = config.getBoolean("allowed", allowed);
        weightConstant = config.getDouble("weight_constant", weightConstant);

        enabled = defaultEnabled;
    }

    @Override
    protected void deepCopyNonAtomicAttributes(T clone) {
        clone.params = params.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().deepCopy(clone.profile)));
    }

    /**
     * Apply the parameters in the request.
     * @param request a Solr request, represented as map of {@code key, values}.
     * @return keys for the parameters that were applies.
     */
    public abstract Set<String> apply(Iterable<Map.Entry<String, String[]>> request);

    @Override
    double getWeight() {
        return enabled ? weightConstant : 0.0;
    }
}
