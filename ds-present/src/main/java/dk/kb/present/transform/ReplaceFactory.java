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

import dk.kb.present.config.ReplaceConfig;
import dk.kb.present.config.TransformerConfig;

/**
 * Constructs {@link ReplaceFactory}s.
 */
public class ReplaceFactory implements DSTransformerFactory {

    @Override
    public String getTransformerID() {
        return ReplaceTransformer.ID;
    }

    @Override
    public DSTransformer createTransformer(TransformerConfig conf) {
        ReplaceConfig c = (ReplaceConfig) conf;
        return new ReplaceTransformer(c.getRegexp(), c.getReplacement(), c.isReplaceAll());
    }
}
