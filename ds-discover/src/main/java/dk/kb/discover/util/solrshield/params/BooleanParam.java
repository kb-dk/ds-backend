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

/**
 * Boolean params have constant weight if true and zero weight if not true.
 */
public class BooleanParam extends Param<BooleanParam, Boolean> {
    public BooleanParam(Profile profile, YAML config) {
        super(profile, config, false); // Solr boolean params are never multiValue
        value = config.getBoolean("defaultValue", value);
    }

    @Override
    protected void applyTypes(String[] values) {
        this.value = Boolean.parseBoolean(values[0]);
        enabled = value;
    }
}
