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

/**
 * Representation of a Solr Search component.
 */
public class SearchComponent extends Component<SearchComponent> {

    protected Param.StringParam q;
    protected Param.StringParam fq;
    protected Param.IntegerParam rows;
    protected Param.IntegerParam start;
    protected Param.FieldsParam fields;

    public SearchComponent(Profile profile, YAML config) {
        super(profile, "search", config);

        YAML paramsConf = config.getSubMap("params");
        addParam(paramsConf, "q", paramConf -> this.q = new Param.StringParam(profile, paramConf, false));
        addParam(paramsConf, "fq", paramConf -> this.fq = new Param.StringParam(profile, paramConf, true));
        addParam(paramsConf, "rows", paramConf -> this.rows = new Param.IntegerParam(profile, paramConf));
        addParam(paramsConf, "start", paramConf -> this.start = new Param.IntegerParam(profile, paramConf));
        addParam(paramsConf, "fl", paramConf -> this.fields = new Param.FieldsParam(profile, paramConf));
    }

    @Override
    public double getWeight() {
        return !enabled ? 0.0 :
                super.getWeight() +
                        q.getWeight() +
                        start.getWeight() +
                        rows.getWeight() + rows.getValue() * rows.weightFactor * fields.getWeight();
    }
}
