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
 * Factory for creating a specific type of {@link AbstractTransformer}s.
 *
 * The class primarily exists as a mechanism for the ServiceLoader to discover transformers.
 *
 * IMPORTANT:
 * Implementations of AbstractTransformerFactory cannot be inner classes due to the way ServiceLoader works.
 * Implementations must be registered in src/main/resources/META-INF/services/dk.kb.present.transformer.AbstractTransformerFactory.
 */
public interface AbstractTransformerFactory {

    /**
     * @return the ID for the transformer that this factory creates, e.g. {@code mods2solr}.
     */
    String getTransformerID();

    /**
     * Create a new transformer of the supported type and return it.
     * @param conf configuration for the {@link AbstractTransformer} to create.
     * @return a {@link AbstractTransformer} of the supported type with the given configuration;
     * @throws Exception if the transformer could not be created.
     */
    AbstractTransformer createTransformer(YAML conf) throws Exception;
}
