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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Constructs {@link IdentityTransformer}s.
 */
public class XSLTFactory implements DSTransformerFactory {
    public static final String STYLESHEET_KEY = "stylesheet";
    public static final String INJECTIONS_KEY = "injections";

    @Override
    public String getTransformerID() {
        return XSLTTransformer.ID;
    }

    @Override
    public DSTransformer createTransformer(YAML conf) throws IOException {
        if (!conf.containsKey(STYLESHEET_KEY)) {
            throw new IllegalArgumentException(
                    "Expected the property '" + STYLESHEET_KEY + "' to be present in the config");
        }
        Map<String, String> injections = null;
        if (conf.containsKey(INJECTIONS_KEY)) {
            injections = new HashMap<>();
            for (YAML yInjection: conf.getYAMLList(INJECTIONS_KEY)) {
                if (yInjection.size() != 1) {
                    throw new IllegalArgumentException(
                            "Expected a single entry (key-value pair) in injection '" + yInjection +
                            "' but got " + yInjection.size());
                }
                // TODO: Move away from the strange "listed maps with one entry" way of stating injections
                String firstKey = yInjection.keySet().stream().findFirst().get();
                injections.put(firstKey, yInjection.getString(firstKey));
            }
        }
        return new XSLTTransformer(conf.getString(STYLESHEET_KEY), injections);
    }
}
