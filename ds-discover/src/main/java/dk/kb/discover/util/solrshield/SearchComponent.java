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

import dk.kb.discover.util.solrshield.params.*;
import dk.kb.util.yaml.YAML;

/**
 * Representation of a Solr Search component.
 */
public class SearchComponent extends Component<SearchComponent> {

    protected StringParam q;
    protected StringParam fq;
    protected IntegerParam rows;
    protected IntegerParam start;
    protected FieldsParam fl;
    protected StringParam qOp;
    protected StringParam wt;
    protected StringParam version;
    protected BooleanParam indent;
    protected StringParam debug;
    protected BooleanParam debugExplainStructured;

    public SearchComponent(Profile profile, YAML config) {
        super(profile, "search", config);

        YAML paramsConf = config.getSubMap("params");
        addParam(paramsConf, "q", paramConf -> this.q = new StringParam(profile, paramConf, false));
        addParam(paramsConf, "fq", paramConf -> this.fq = new StringParam(profile, paramConf, true));
        addParam(paramsConf, "rows", paramConf -> this.rows = new IntegerParam(profile, paramConf));
        addParam(paramsConf, "start", paramConf -> this.start = new IntegerParam(profile, paramConf));
        addParam(paramsConf, "fl", paramConf -> this.fl = new FieldsParam(profile, paramConf));
        // TODO: Add support for enums
        addParam(paramsConf, "q.op", paramConf -> this.qOp = new StringParam(profile, paramConf, false));
        addParam(paramsConf, "wt", paramConf -> this.wt = new StringParam(profile, paramConf, false));
        addParam(paramsConf, "version", paramConf -> this.version = new StringParam(profile, paramConf, false));
        addParam(paramsConf, "indent", paramConf -> this.indent = new BooleanParam(profile, paramConf));
        addParam(paramsConf, "debug", paramConf -> this.debug = new StringParam(profile, paramConf, false));
        addParam(paramsConf, "debug.explain.structured", paramConf -> this.debugExplainStructured = new BooleanParam(profile, paramConf));
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
