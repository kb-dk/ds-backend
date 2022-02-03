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

/**
 * Constructs {@link FailTransformer}s.
 */
public class FailFactory implements DSTransformerFactory {
    private static final String ID = "fail";

    @Override
    public String getTransformerID() {
        return ID;
    }

    @Override
    public FailTransformer createTransformer(YAML conf) {
        return new FailTransformer(conf);
    }
}
