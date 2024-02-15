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
 * Representation of a Solr Facet component.
 */
public class FacetComponent extends Component<FacetComponent> {

    protected Param.StringParam facetQuery;
    protected Param.IntegerParam facetLimit;
    protected Param.StringParam facetSort;
    protected Param.IntegerParam facetMincount;
    protected Param.FieldsParam facetField; // Multiple values, but the Solr param name is singular 'facet.field'
    protected Param.BooleanParam facetExists;

    // TODO: Handle field-specific tweaks (sort + limit)

    public FacetComponent(Profile profile, YAML config) {
        super(profile, "facet", config);

        YAML paramsConf = config.getSubMap("params");
        addParam(paramsConf, "\"facet.query\"", paramConf -> this.facetQuery = new Param.StringParam(profile, paramConf, false));
        addParam(paramsConf, "\"facet.field\"", paramConf -> this.facetField = new Param.FieldsParam(profile, paramConf));
        addParam(paramsConf, "\"facet.limit\"", paramConf -> this.facetLimit = new Param.IntegerParam(profile, paramConf));
        addParam(paramsConf, "\"facet.sort\"", paramConf -> this.facetSort = new Param.StringParam(profile, paramConf, false));
        addParam(paramsConf, "\"facet.mincount\"", paramConf -> this.facetMincount = new Param.IntegerParam(profile, paramConf));
        addParam(paramsConf, "\"facet.exists\"", paramConf -> this.facetExists = new Param.BooleanParam(profile, paramConf));
    }

    @Override
    public double getWeight() {
        return !enabled ? 0.0 :
                super.getWeight() +
                        facetQuery.getWeight() +
                        facetLimit.getWeight() + facetLimit.getValue() * facetLimit.weightFactor * facetField.getWeight() +
                        facetSort.getWeight() +
                        facetMincount.getWeight() +
                        facetExists.getWeight();
    }
}
