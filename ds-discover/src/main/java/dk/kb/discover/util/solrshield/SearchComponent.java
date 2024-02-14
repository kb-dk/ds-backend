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

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Representation of a Solr Search component.
 */
public class SearchComponent extends Component<SearchComponent> {
    private static final Logger log = LoggerFactory.getLogger(SearchComponent.class);

    protected Param.StringParam q;
    protected Param.IntegerParam rows;
    protected Param.IntegerParam start;
    protected Param.FieldsParam fields;

    public SearchComponent(Profile profile, YAML config) {
        super(profile, "search", config);
        if (!config.containsKey("params")) {
            log.warn("No configuration key 'params' for the search component. This is most likely an error");
            return;
        }
        YAML paramsConf = config.getSubMap("params");
        addParam(paramsConf, "q", paramConf -> this.q = new Param.StringParam(profile, paramConf));
        addParam(paramsConf, "rows", paramConf -> this.rows = new Param.IntegerParam(profile, paramConf));
        addParam(paramsConf, "start", paramConf -> this.start = new Param.IntegerParam(profile, paramConf));
        addParam(paramsConf, "fl", paramConf -> this.fields = new Param.FieldsParam(profile, paramConf));
    }

    private void addParam(YAML config, String name, Function<YAML, Param<?>> constructor) {
        if (config.containsKey(name)) {
            Param<?> param = constructor.apply(config.getSubMap(name));
            param.name = name;
            params.put(name, param);
        }
    }

    @Override
    public Set<String> apply(Iterable<Map.Entry<String, String[]>> request) {
        Set<String> usedKeys = new HashSet<>();
        log.warn("Not implemented yet"); // TODO: Implement this

        return usedKeys;
    }

    @Override
    double getWeight() {
        return super.getWeight() +
                q.getWeight() +
                rows.getWeight() + rows.value * fields.getWeight() +
                start.getWeight();
    }
}
