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
    protected Param.FieldsParam fl;
    protected Param.StringParam qOp;
    protected Param.StringParam wt;
    protected Param.StringParam version;
    protected Param.BooleanParam indent;
    protected Param.StringParam debug;
    protected Param.BooleanParam debugExplainStructured;

    public SearchComponent(Profile profile, YAML config) {
        super(profile, "search", config);

        YAML paramsConf = config.getSubMap("params");
        addParam(paramsConf, "q", paramConf -> this.q = new Param.StringParam(profile, paramConf, false));
        addParam(paramsConf, "fq", paramConf -> this.fq = new Param.StringParam(profile, paramConf, true));
        addParam(paramsConf, "rows", paramConf -> this.rows = new Param.IntegerParam(profile, paramConf));
        addParam(paramsConf, "start", paramConf -> this.start = new Param.IntegerParam(profile, paramConf));
        addParam(paramsConf, "fl", paramConf -> this.fl = new Param.FieldsParam(profile, paramConf));
        // TODO: Add support for enums
        addParam(paramsConf, "q.op", paramConf -> this.qOp = new Param.StringParam(profile, paramConf, false));
        addParam(paramsConf, "wt", paramConf -> this.wt = new Param.StringParam(profile, paramConf, false));
        addParam(paramsConf, "version", paramConf -> this.version = new Param.StringParam(profile, paramConf, false));
        addParam(paramsConf, "indent", paramConf -> this.indent = new Param.BooleanParam(profile, paramConf));
        addParam(paramsConf, "debug", paramConf -> this.debug = new Param.StringParam(profile, paramConf, false));
        addParam(paramsConf, "debug.explain.structured", paramConf -> this.debugExplainStructured = new Param.BooleanParam(profile, paramConf));
    }

    @Override
    void alignParams() {
        q = getParam("q");
        fq = getParam("fq");
        rows = getParam("rows");
        start = getParam("start");
        fl = getParam("fl");
        qOp = getParam("q.op");
        wt = getParam("wt");
        version = getParam("version");
        indent = getParam("indent");
        debug = getParam("debug");
        debugExplainStructured = getParam("debug.explain.structured");
    }

    @Override
    public double getWeight() {
        return !enabled ? 0.0 :
                super.getWeight() +
                        q.getWeight() +
                        fq.getWeight() +
                        start.getWeight() +
                        rows.getWeight() + rows.getValue() * rows.weightFactor * fl.getWeight() +
                        qOp.getWeight() +
                        wt.getWeight() +
                        version.getWeight() +
                        indent.getWeight() +
                        debug.getWeight() +
                        (debug.enabled ? debugExplainStructured.getWeight() : 0.0);


    }
}
