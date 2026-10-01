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

import java.util.List;

/**
 * Configuration for a single entry under {@code views} in an {@link OriginConfig}, consumed by
 * {@link dk.kb.present.View}.
 */
public class ViewConfig {
    private final String id;
    private final String mime;
    private final String strategy;
    private final List<TransformerConfig> transformers;

    /**
     * @param id the view ID, e.g. {@code mods} or {@code json-ld}.
     * @param mime the MIME type of the view's output, as {@code type/subtype}.
     * @param strategy one of {@link dk.kb.present.View.Strategy}, as a raw (not-yet-parsed) string. Defaults to
     *                 {@code "NONE"} when not configured.
     * @param transformers the transformers to apply, in order.
     */
    public ViewConfig(String id, String mime, String strategy, List<TransformerConfig> transformers) {
        this.id = id;
        this.mime = mime;
        this.strategy = strategy;
        this.transformers = transformers;
    }

    public String getId() {
        return id;
    }

    public String getMime() {
        return mime;
    }

    public String getStrategy() {
        return strategy;
    }

    public List<TransformerConfig> getTransformers() {
        return transformers;
    }

    @Override
    public String toString() {
        return "ViewConfig(id='" + id + "', mime='" + mime + "', strategy='" + strategy + "', transformers=" +
               transformers + ')';
    }
}
