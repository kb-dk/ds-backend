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
package dk.kb.present.config;

/**
 * Marker interface for the plain configuration objects handed to
 * {@link dk.kb.present.transform.DSTransformerFactory#createTransformer(TransformerConfig)}.
 * <p>
 * Each supported transformer type (as configured under {@code transformers} in a {@link ViewConfig}) has its own
 * concrete implementation of this interface, built by {@link ServiceConfig} from the underlying YAML - no
 * transformer factory implementation ever sees a {@code dk.kb.util.yaml.YAML} object. A factory downcasts the
 * {@link TransformerConfig} it receives to the concrete type matching its own {@code getTransformerID()}.
 *
 * @see XsltConfig
 * @see ReplaceConfig
 * @see FailTransformerConfig
 * @see EmptyTransformerConfig
 */
public interface TransformerConfig {
    /**
     * @return the transformer type, e.g. {@code xslt} or {@code identity}, matching
     * {@link dk.kb.present.transform.DSTransformerFactory#getTransformerID()}. Primarily used for logging and error
     * messages.
     */
    String getType();
}
