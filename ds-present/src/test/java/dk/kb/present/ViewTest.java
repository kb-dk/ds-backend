package dk.kb.present;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dk.kb.present.api.v1.impl.DsPresentApiServiceImpl;
import dk.kb.present.client.v1.DsPresentApi;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
class ViewTest {

    @Test
    void identity() throws Exception {
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML dsflConf = conf.getYAMLList(".config.collections").get(0);
        View view = new View(dsflConf.getSubMap("dsfl").getYAMLList("views").get(0), dsflConf.getSubMap("dsfl").getString("origin"));
        assertEquals("SameAsInput", view.apply("someID", "SameAsInput", "")); // Identity view
    }


    // Should still work after update of XSLT to JSON-LD, might fail and need reassessment
    @Test
    void jsonld() throws Exception {
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML dsflConf = conf.getYAMLList(".config.collections").get(0);
        View jsonldView = new View(dsflConf.getSubMap("dsfl").getYAMLList("views").get(1), dsflConf.getSubMap("dsfl").getString("origin"));
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/40221e30-1414-11e9-8fb8-00505688346e.xml");
        String jsonld = jsonldView.apply("40221e30-1414-11e9-8fb8-00505688346e", mods, "");
        assertTrue(jsonld.contains("\"headline\":[{\"value\":\"Christian VIII\",\"@language\":\"da\"}]"));
    }

    @Test
    void solrJson() throws Exception {
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML dsflConf = conf.getYAMLList(".config.collections").get(0);
        View solrView = new View(dsflConf.getSubMap("dsfl").getYAMLList("views").get(2), dsflConf.getSubMap("dsfl").getString("origin"));
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/40221e30-1414-11e9-8fb8-00505688346e.xml");
        String solrJson = solrView.apply("40221e30-1414-11e9-8fb8-00505688346e", mods, "");
        assertTrue(solrJson.contains("\"origin\":\"ds.test\""));
        assertTrue(solrJson.contains("\"resource_id\":[\"\\/DAMJP2\\/DAM\\/Samlingsbilleder\\/0000\\/624\\/420\\/KE070592\"]"), "SolrJSON does not contain correct resource_id");
        assertTrue(solrJson.contains("\"thumbnail\":\"https:\\/\\/example.com\\/imageserver\\/%2FDAMJP2%2FDAM%2FSamlingsbilleder%2F0000%2F624%2F420%2FKE070592\\/full\\/%21150%2C150\\/0\\/default.jpg\"")
                , "SolrJson does not contain correct thumbnail");
    }


    //TODO: Fix this test by creating correct test config.
    /*
    @Test
    void relationInView() throws Exception {
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML radiotvConf = conf.getYAMLList(".config.collections").get(0);
        View solrView = new View(radiotvConf.getSubMap("dsfl").getYAMLList("views").get(2), radiotvConf.getSubMap("dsfl").getString("origin"));

        // Running the unit test locally. Elsewhere, the child uri should point to a ds-present record endpoint to get the raw value.
        String pvicaRecord = Resolver.resolveUTF8String("internal_test_files/tvMetadata/e683b0b8-425b-45aa-be86-78ac2b4ef0ca.xml");
        String localChildUri = String.valueOf(Resolver.getPathFromClasspath("internal_test_files/tvMetadata/53bf323c-5a8a-48b9-a29a-0b1616a58af9.xml"));

        // Getting child remote
        String childEndpoint = radiotvConf.getSubMap("dsfl").getString("getchildendpoint");
        String childId = URLEncoder.encode("ds.radiotv:oai:man:53bf323c-5a8a-48b9-a29a-0b1616a58af9", StandardCharsets.UTF_8);
        String childURI = childEndpoint + childId + "?format=raw";


        String solrJson = solrView.apply("40221e30-1414-11e9-8fb8-00505688346e", pvicaRecord, localChildUri);

        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(solrJson);
        String prettyJsonString = gson.toJson(je);
        System.out.println(prettyJsonString);
    }

     */



}