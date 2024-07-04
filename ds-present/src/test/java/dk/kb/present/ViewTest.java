package dk.kb.present;

import dk.kb.present.config.ServiceConfig;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

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
    private static final Logger log = LoggerFactory.getLogger(ViewTest.class);

    private static YAML config;

    @BeforeAll
    static void setup() {
        try {
            ServiceConfig.initialize("internal-test-setup.yaml");
            config = ServiceConfig.getConfig();
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    void identity() throws Exception {
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML dsflConf = conf.getYAMLList(".origins").get(0);
        View view = new View(dsflConf.getSubMap("dsfl").getYAMLList("views").get(0), dsflConf.getSubMap("dsfl").getString("origin"));
        DsRecordDto record = new DsRecordDto().mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L);
        record.setData("SameAsInput");
        assertEquals("SameAsInput", view.apply(record)); // Identity view
    }


    // Should still work after update of XSLT to JSON-LD, might fail and need reassessment
    @Test
    void jsonldMods() throws Exception {
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML dsflConf = conf.getYAMLList(".origins").get(0);
        View jsonldView = new View(dsflConf.getSubMap("dsfl").getYAMLList("views").get(1), dsflConf.getSubMap("dsfl").getString("origin"));
        String mods = Resolver.resolveUTF8String(TestFiles.CUMULUS_RECORD_40221e30);

        DsRecordDto recordDto = new DsRecordDto().data(mods).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100")
                .mTime(1701261949625000L);

        String jsonld = jsonldView.apply(recordDto);
        assertTrue(jsonld.contains("\"headline\":[{\"value\":\"Christian VIII\",\"@language\":\"da\"}]"));
    }

    @Test
    @Tag("integration")
    void jsonldPvica() throws Exception {
        if (Resolver.getPathFromClasspath(TestFiles.PVICA_RECORD_df3dc9cf) == null){
            fail("Missing internal test files");
        }
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML radioConf = conf.getYAMLList("origins").get(2);
        View jsonldView = new View(radioConf.getSubMap("\"ds.radio\"").getYAMLList("views").get(1),
                                    radioConf.getSubMap("\"ds.radio\"").getString("origin"));
        String pvica = Resolver.resolveUTF8String(TestFiles.PVICA_RECORD_df3dc9cf);

        DsRecordDto recordDto = new DsRecordDto().data(pvica).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L);

        String jsonld = jsonldView.apply(recordDto);
        assertTrue(jsonld.contains("\"name\":\"Før Bjørnen Er Skudt\""));
    }

    @Test
    @Tag("integration")
    void pvicaEmptyChild() throws Exception {
        if (Resolver.getPathFromClasspath(TestFiles.PVICA_RECORD_df3dc9cf) == null){
            fail("Missing internal test files");
        }
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML radioConf = conf.getYAMLList(".origins").get(2);
        View jsonldView = new View(radioConf.getSubMap("\"ds.radio\"").getYAMLList("views").get(1),
                                    radioConf.getSubMap("\"ds.radio\"").getString("origin"));
        String pvica = Resolver.resolveUTF8String(TestFiles.PVICA_RECORD_df3dc9cf);

        DsRecordDto recordDto = new DsRecordDto().data(pvica).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L);

        DsRecordDto emptyChildDto = new DsRecordDto().id("test.emptyChild").mTime(1701261949625000L);
        recordDto.setChildren(List.of(emptyChildDto));

        String jsonld = jsonldView.apply(recordDto);
        assertFalse(jsonld.contains("\"contentUrl\":"));
    }

    @Test
    @Tag("integration")
    void solrFromPvica() throws Exception {
        if (Resolver.getPathFromClasspath(TestFiles.PVICA_RECORD_df3dc9cf) == null){
            fail("Missing internal test files");
        }
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML tvConf = conf.getYAMLList(".origins").get(3);
        View solrView = new View(tvConf.getSubMap("\"ds.tv\"").getYAMLList("views").get(2),
                                 tvConf.getSubMap("\"ds.tv\"").getString("origin"));
        String pvica = Resolver.resolveUTF8String("internal_test_files/preservica7/df3dc9cf-43f6-4a8a-8909-de8b0fb7bd00.xml");

        DsRecordDto recordDto = new DsRecordDto().data(pvica).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L);

        String solrdoc = solrView.apply(recordDto);
        assertTrue(solrdoc.contains("\"title\":\"Før Bjørnen Er Skudt\""));
    }

    @Test
    void solrJson() throws Exception {
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML dsflConf = conf.getYAMLList(".origins").get(0);
        View solrView = new View(dsflConf.getSubMap("dsfl").getYAMLList("views").get(2), dsflConf.getSubMap("dsfl").getString("origin"));
        String mods = Resolver.resolveUTF8String(TestFiles.CUMULUS_RECORD_40221e30);

        DsRecordDto recordDto = new DsRecordDto().data(mods).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L);

        String solrJson = solrView.apply(recordDto);
        assertTrue(solrJson.contains("\"origin\":\"ds.test\""));
        assertTrue(solrJson.contains("\"resource_id\":[\"\\/DAMJP2\\/DAM\\/Samlingsbilleder\\/0000\\/624\\/420\\/KE070592\"]"), "SolrJSON does not contain correct resource_id");
        assertTrue(solrJson.contains("\"thumbnail\":\"https:\\/\\/example.com\\/imageserver\\/%2FDAMJP2%2FDAM%2FSamlingsbilleder%2F0000%2F624%2F420%2FKE070592\\/full\\/%21150%2C150\\/0\\/default.jpg\"")
                , "SolrJson does not contain correct thumbnail");
    }

    @Test
    void testConcurrency() throws InterruptedException, ExecutionException, IOException {
        String pvica = Resolver.resolveUTF8String(TestFiles.PVICA_RECORD_df3dc9cf);
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML tvConf = conf.getYAMLList(".origins").get(3);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        //Use CountDownLatches to make sure threads are executed in parallel
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<String> future1 = executorService.submit(() -> {
            try {
                View solrView = new View(tvConf.getSubMap("\"ds.tv\"").getYAMLList("views").get(2),
                        tvConf.getSubMap("\"ds.tv\"").getString("origin"));
                DsRecordDto recordDto = new DsRecordDto().data(pvica).mTimeHuman("2023-11-29 13:45:49+0100").id("test.id1").mTime(1701111111111000L);
                readyLatch.countDown();
                startLatch.await();
                String result = solrView.apply(recordDto);
                return result;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        Future<String> future2 = executorService.submit(() -> {
            try {
                View solrView = new View(tvConf.getSubMap("\"ds.tv\"").getYAMLList("views").get(2),
                        tvConf.getSubMap("\"ds.tv\"").getString("origin"));
                DsRecordDto recordDto = new DsRecordDto().data(pvica).mTimeHuman("2023-11-30 13:45:49+0100").id("test.id2").mTime(1702222222222000L);
                readyLatch.countDown();
                startLatch.await();
                String result = solrView.apply(recordDto);
                return result;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        readyLatch.await();
        startLatch.countDown();
        String result1 = future1.get();
        String result2 = future2.get();
        assertTrue(result1.contains("\"id\":\"test.id1\""));
        assertTrue(result1.contains("\"internal_storage_mTime\":\"1701111111111000\""));
        assertTrue(result2.contains("\"id\":\"test.id2\""));
        assertTrue(result2.contains("\"internal_storage_mTime\":\"1702222222222000\""));

        executorService.shutdown();
    }
}
