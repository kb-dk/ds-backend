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

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Takes a textual input and a map of metadata and transforms it to another textual input.
 * The metadata map will initially contain the pair {@code recordID:<recordID>}.
 * Changes to metadata will be passed through the chain of {@code DSTransformer}s.
 * One example of chaining it to have one transformer resolve copyright rules, store them as key-values in the
 * metadata and pass the input unchanged, then having another transformer responsible for transforming to Solr-JSON
 * with extra fields added from the metadata delivered by the copyright extractor.
 */
public abstract class DSTransformer implements BiFunction<String, Map<String, String>, String> {

    /**
     * @return the ID for the transformer, e.g. {@code mods2solr}.
     */
    abstract public String getID();

    /**
     * @return the stylesheet used by the transformer, if any existst. If stylesheet does not exist return null.
     */
    abstract public String getStylesheet();



}
