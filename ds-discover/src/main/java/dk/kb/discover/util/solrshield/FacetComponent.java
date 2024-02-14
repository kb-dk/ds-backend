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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Representation of a Solr Facet component.
 */
public class FacetComponent extends Component<FacetComponent> {
    private static final Logger log = LoggerFactory.getLogger(FacetComponent.class);

    public FacetComponent(Profile profile, YAML config) {
        super(profile, "facet", config);
    }

    @Override
    public Set<String> apply(Iterable<Map.Entry<String, String[]>> request) {
        log.warn("Not implemented yet"); // TODO: Implement this
        return Collections.emptySet();
    }
}
