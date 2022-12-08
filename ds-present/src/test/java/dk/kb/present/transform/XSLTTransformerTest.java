package dk.kb.present.transform;

import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
class XSLTTransformerTest {
    public static final String MODS2JSONLD = "xslt/mods2schemaorg.xsl";
    public static final String MODS2SOLR = "xslt/mods2solr.xsl";
    public static final String ALBERT = "xml/corpus/albert-einstein.xml";
    public static final String CHINESE = "xml/corpus/chinese-manuscripts.xml";

    @Test
    void testJSONLDAlbert() throws IOException {
        JSONObject jsonld = new JSONObject(getTransformed(MODS2JSONLD, ALBERT));
        assertTrue(jsonld.toString().contains("\"name\":{\"@value\":\"Einstein, Albert\",\"@language\":\"en\"}"));
    }

    @Test
    void testJSONLDChinese() throws IOException {
        JSONObject jsonld = new JSONObject(getTransformed(MODS2JSONLD, CHINESE));
        assertTrue(jsonld.toString().contains("\"name\":{\"@value\":\"周培春 Zhou Peichun\",\"@language\":\"zh\"}"));
    }

    @Test
    void testSolrAlbert() throws IOException {
        String solr = getTransformed(MODS2SOLR, ALBERT);
        // TODO: Add more detailed test
        assertTrue(solr.contains("{\"id\":\""));
    }

    private String getTransformed(String xsltResource, String xmlResource) throws IOException {
        XSLTTransformer transformer = new XSLTTransformer(xsltResource);
        String mods = Resolver.resolveUTF8String(xmlResource);
        return transformer.apply(mods, Map.of("recordID", xmlResource));
    }
}