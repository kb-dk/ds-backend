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
package dk.kb.present.transform;

import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The identity transformer returns the given input unchanged.
 */
public class IdentityTransformer extends AbstractTransformer {
    private static final Logger log = LoggerFactory.getLogger(IdentityTransformer.class);
    public static final String ID = "identity";

    public IdentityTransformer(YAML conf) {
        super(conf);
        log.debug("Constructed " + this);
    }

    @Override
    String getID() {
        return ID;
    }

    // A "real" transformer would do something here
    @Override
    public String apply(String s) {
        return s;
    }

    @Override
    public String toString() {
        return "IdentityTransformer()";
    }

}
