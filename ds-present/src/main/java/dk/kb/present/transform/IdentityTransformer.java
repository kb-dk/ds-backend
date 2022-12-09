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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * The identity transformer returns the given input unchanged.
 */
public class IdentityTransformer extends DSTransformer {
    private static final Logger log = LoggerFactory.getLogger(IdentityTransformer.class);
    public static final String ID = "identity";

    /**
     * Construct a transformer that returns its input unchanged.
     */
    public IdentityTransformer() {
        log.debug("Constructed " + this);
    }

    @Override
    public String getID() {
        return ID;
    }

    // A "real" transformer would do something here
    @Override
    public String apply(String s, Map<String, String> metadata) {
        return s;
    }

    @Override
    public String toString() {
        return "IdentityTransformer()";
    }

}
