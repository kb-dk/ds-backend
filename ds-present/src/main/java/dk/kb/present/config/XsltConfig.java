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

import java.util.Map;

/**
 * Configuration for the {@code xslt} and {@code xsltsolr} transformer types, used by
 * {@link dk.kb.present.transform.XSLTFactory} and {@link dk.kb.present.transform.XSLTSolrFromSchemaFactory}.
 */
public class XsltConfig implements TransformerConfig {
    private final String type;
    private final String stylesheet;
    private final Map<String, String> injections;

    /**
     * @param type the transformer type ({@code xslt} or {@code xsltsolr}).
     * @param stylesheet classpath-resolvable path to the XSLT stylesheet. Mandatory.
     * @param injections fixed key-value pairs passed to the XSLT transformation as parameters, or {@code null} if
     *                    none are configured (as opposed to an empty map).
     */
    public XsltConfig(String type, String stylesheet, Map<String, String> injections) {
        this.type = type;
        this.stylesheet = stylesheet;
        this.injections = injections;
    }

    @Override
    public String getType() {
        return type;
    }

    public String getStylesheet() {
        return stylesheet;
    }

    /**
     * @return fixed key-value pairs for the XSLT transformation, or {@code null} if none are configured.
     */
    public Map<String, String> getInjections() {
        return injections;
    }

    @Override
    public String toString() {
        return "XsltConfig(type='" + type + "', stylesheet='" + stylesheet + "', injections=" + injections + ')';
    }
}
