package dk.kb.present;

import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;
import org.junit.jupiter.api.Test;


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
        YAML dsflConf = conf.getYAMLList(".config.origins").get(0);
        View view = new View(dsflConf.getSubMap("dsfl").getYAMLList("views").get(0), dsflConf.getSubMap("dsfl").getString("origin"));
        DsRecordDto record = new DsRecordDto();
        record.setData("SameAsInput");
        assertEquals("SameAsInput", view.apply(record)); // Identity view
    }


    // Should still work after update of XSLT to JSON-LD, might fail and need reassessment
    @Test
    void jsonldMods() throws Exception {
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML dsflConf = conf.getYAMLList(".config.origins").get(0);
        View jsonldView = new View(dsflConf.getSubMap("dsfl").getYAMLList("views").get(1), dsflConf.getSubMap("dsfl").getString("origin"));
        String mods = Resolver.resolveUTF8String(TestFiles.CUMULUS_RECORD_40221e30);

        DsRecordDto recordDto = new DsRecordDto().data(mods).id("test.id");

        String jsonld = jsonldView.apply(recordDto);
        assertTrue(jsonld.contains("\"headline\":[{\"value\":\"Christian VIII\",\"@language\":\"da\"}]"));
    }

    @Test
    void jsonldPvica() throws Exception {
        if (Resolver.getPathFromClasspath(TestFiles.PVICA_RECORD_df3dc9cf) == null){
            return;
        }
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML radiotvConf = conf.getYAMLList(".config.origins").get(1);
        View jsonldView = new View(radiotvConf.getSubMap("\"ds.radiotv\"").getYAMLList("views").get(1), radiotvConf.getSubMap("\"ds.radiotv\"").getString("origin"));
        String pvica = Resolver.resolveUTF8String(TestFiles.PVICA_RECORD_df3dc9cf);

        DsRecordDto recordDto = new DsRecordDto().data(pvica).id("test.id");

        String jsonld = jsonldView.apply(recordDto);
        assertTrue(jsonld.contains("\"name\":\"Før Bjørnen Er Skudt\""));
    }

    @Test
    void solrFromPvica() throws Exception {
        if (Resolver.getPathFromClasspath(TestFiles.PVICA_RECORD_df3dc9cf) == null){
            return;
        }
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML radiotvConf = conf.getYAMLList(".config.origins").get(1);
        View solrView = new View(radiotvConf.getSubMap("\"ds.radiotv\"").getYAMLList("views").get(2), radiotvConf.getSubMap("\"ds.radiotv\"").getString("origin"));
        String pvica = Resolver.resolveUTF8String("internal_test_files/tvMetadata/df3dc9cf-43f6-4a8a-8909-de8b0fb7bd00.xml");

        DsRecordDto recordDto = new DsRecordDto().data(pvica).id("test.id");

        String solrdoc = solrView.apply(recordDto);
        System.out.println(solrdoc);
        assertTrue(solrdoc.contains("\"title\":\"Før Bjørnen Er Skudt\""));
    }

    @Test
    void solrJson() throws Exception {
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML dsflConf = conf.getYAMLList(".config.origins").get(0);
        View solrView = new View(dsflConf.getSubMap("dsfl").getYAMLList("views").get(2), dsflConf.getSubMap("dsfl").getString("origin"));
        String mods = Resolver.resolveUTF8String(TestFiles.CUMULUS_RECORD_40221e30);

        DsRecordDto recordDto = new DsRecordDto().data(mods).id("test.id");

        String solrJson = solrView.apply(recordDto);
        assertTrue(solrJson.contains("\"origin\":\"ds.test\""));
        assertTrue(solrJson.contains("\"resource_id\":[\"\\/DAMJP2\\/DAM\\/Samlingsbilleder\\/0000\\/624\\/420\\/KE070592\"]"), "SolrJSON does not contain correct resource_id");
        assertTrue(solrJson.contains("\"thumbnail\":\"https:\\/\\/example.com\\/imageserver\\/%2FDAMJP2%2FDAM%2FSamlingsbilleder%2F0000%2F624%2F420%2FKE070592\\/full\\/%21150%2C150\\/0\\/default.jpg\"")
                , "SolrJson does not contain correct thumbnail");
    }
}