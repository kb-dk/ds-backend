package dk.kb.present;

import dk.kb.present.config.ServiceConfig;
import dk.kb.present.dr.holdback.HoldbackDatePicker;
import dk.kb.present.dr.restrictions.ProductionIdLookup;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.util.concurrent.*;

import static dk.kb.present.TestUtil.prettyPrintJson;
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
            ServiceConfig.initialize("conf/ds-present-behaviour.yaml", "internal-test-setup.yaml");
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
        HoldbackDatePicker.init();
        View jsonldView = getPreservicaRadioJsonView();
        String pvica = Resolver.resolveUTF8String(TestFiles.PVICA_RECORD_df3dc9cf);
        DsRecordDto recordDto = new DsRecordDto().data(pvica).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L)
                                                    .origin("ds.radio").kalturaId("randomKalturaId");


        String jsonld = jsonldView.apply(recordDto);
        prettyPrintJson(jsonld);
        assertTrue(jsonld.contains("\"name\":\"Før Bjørnen Er Skudt\""));
        assertTrue(jsonld.contains("\"kb:holdback_date\":\"2022-07-06T08:05:00Z\""));
        assertTrue(jsonld.contains("\"@type\":\"PropertyValue\"," +
                                    "\"PropertyID\":\"KalturaID\"," +
                                    "\"value\":\"randomKalturaId\""));
    }
    
    @Test
    @Tag("integration")
    void testNoKalturaIdPvica() throws Exception {
        HoldbackDatePicker.init();
        View jsonldView = getPreservicaRadioJsonView();
        String preservicaData = Resolver.resolveUTF8String(TestFiles.PVICA_RECORD_df3dc9cf);

        // Test with kalturaId = null
        DsRecordDto testRecord = new DsRecordDto().data(preservicaData).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L)
                .origin("ds.radio");
        String jsonldResult = jsonldView.apply(testRecord);
        assertFalse(jsonldResult.contains("\"@type\":\"PropertyValue\"," +
                                            "\"PropertyID\":\"KalturaID\""));

        // test with kalturaId = ""
        testRecord.setKalturaId("");
        jsonldResult = jsonldView.apply(testRecord);
        assertFalse(jsonldResult.contains("\"@type\":\"PropertyValue\"," +
                                            "\"PropertyID\":\"KalturaID\""));
    }

    @Test
    @Tag("integration")
    void solrFromPvica() throws Exception {
        HoldbackDatePicker.init();
        if (Resolver.getPathFromClasspath(TestFiles.PVICA_RECORD_df3dc9cf) == null){
            fail("Missing internal test files");
        }
        View solrView = getSolrTvViewForPreservicaRecord();
        String pvica = Resolver.resolveUTF8String("internal_test_files/preservica7/df3dc9cf-43f6-4a8a-8909-de8b0fb7bd00.xml");

        DsRecordDto recordDto = new DsRecordDto().data(pvica).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L)
                                    .origin("ds.tv").kalturaId("someRandomKalturaId");

        String solrdoc = solrView.apply(recordDto);

        assertTrue(solrdoc.contains("\"title\":\"Før Bjørnen Er Skudt\""));
        assertTrue(solrdoc.contains("\"holdback_expired_date\":\"9999-01-01T00:00:00Z\""));
        assertTrue(solrdoc.contains("\"kaltura_id\":\"someRandomKalturaId\""));
    }

    @Test
    @Tag("integration")
    void testPreservicaSolrNoKalturaId() throws Exception {
        HoldbackDatePicker.init();
        if (Resolver.getPathFromClasspath(TestFiles.PVICA_RECORD_df3dc9cf) == null){
            fail("Missing internal test files");
        }
        View solrView = getSolrTvViewForPreservicaRecord();
        String pvica = Resolver.resolveUTF8String("internal_test_files/preservica7/df3dc9cf-43f6-4a8a-8909-de8b0fb7bd00.xml");

        // Test with Kaltura ID = null
        DsRecordDto recordDto = new DsRecordDto().data(pvica).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L).origin("ds.tv");
        String solrdoc = solrView.apply(recordDto);
        assertFalse(solrdoc.contains("\"kaltura_id\":"));

        // Test with Kaltura ID = ""
        recordDto.setKalturaId("");
        solrdoc = solrView.apply(recordDto);
        assertFalse(solrdoc.contains("\"kaltura_id\":"));
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
    @Tag("integration")
    void testConcurrency() throws InterruptedException, ExecutionException, IOException {
        HoldbackDatePicker.init();
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

    @Test
    @Tag("integration")
    void ownProductionTrueTestTvmeter() throws Exception {
        HoldbackDatePicker.init();
        View jsonldView = getPreservicaTvJsonView();
        String pvica = Resolver.resolveUTF8String(TestFiles.PVICA_HOMEMADE_DOMS_MIG_WITH_TVMETER_ADDED);
        DsRecordDto recordDto = new DsRecordDto().data(pvica).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L)
                .origin("ds.tv").kalturaId("randomKalturaId");


        String jsonld = jsonldView.apply(recordDto);

        assertTrue(jsonld.contains("\"kb:production_code_allowed\":true," +
                                    "\"kb:production_code_value\":1000"));
    }

    @Test
    @Tag("integration")
    void ownProductionRadioTest() throws Exception {
        View solrView = getPreservicaRadioSolrView();
        String pvica = Resolver.resolveUTF8String(TestFiles.PVICA_RECORD_2b462c63);
        DsRecordDto recordDto = new DsRecordDto().data(pvica).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100")
                .mTime(1701261949625000L).origin("ds.radio").kalturaId("randomKalturaId");

        String solrDoc = solrView.apply(recordDto);
        prettyPrintJson(solrDoc);
        assertTrue(solrDoc.contains("\"production_code_allowed\":\"true\""));
    }

    @Test
    @Tag("integration")
    void holdbackNameTestNielsen() throws Exception {
        HoldbackDatePicker.init();
        View jsonldView = getPreservicaTvJsonView();
        String pvica = Resolver.resolveUTF8String(TestFiles.PVICA_RECORD_0e89456b);
        DsRecordDto recordDto = new DsRecordDto().data(pvica).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L)
                .origin("ds.tv").kalturaId("randomKalturaId");


        String jsonld = jsonldView.apply(recordDto);
        assertTrue(jsonld.contains("\"kb:holdback_date\":\"2024-02-27T04:49:52Z\"," +
                "\"kb:holdback_name\":\"Underholdning\""));
    }
    @Test
    @Tag("integration")
    void holdbackNameEducationTvMeterTest() throws Exception {
        HoldbackDatePicker.init();
        View jsonldView = getPreservicaTvJsonView();
        String pvica = Resolver.resolveUTF8String(TestFiles.PVICA_DOMS_MIG_f1a6492f);
        DsRecordDto recordDto = new DsRecordDto().data(pvica).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L)
                .origin("ds.tv").kalturaId("randomKalturaId");


        String jsonld = jsonldView.apply(recordDto);
        assertTrue(jsonld.contains("\"kb:holdback_name\":\"Undervisning\""));
    }
    @Test
    @Tag("integration")
    void holdbackNameEducationNielsenTest() throws Exception {
        HoldbackDatePicker.init();
        View jsonldView = getPreservicaTvJsonView();
        String pvica = Resolver.resolveUTF8String(TestFiles.PVICA_RECORD_e8c664f9);
        DsRecordDto recordDto = new DsRecordDto().data(pvica).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L)
                .origin("ds.tv").kalturaId("randomKalturaId");


        String jsonld = jsonldView.apply(recordDto);
        assertTrue(jsonld.contains("\"kb:holdback_name\":\"Undervisning\""));
    }

    @Test
    @Tag("integration")
    void ownProductionFalseTest() throws Exception {
        HoldbackDatePicker.init();
        View jsonldView = getPreservicaTvJsonView();
        String pvica = Resolver.resolveUTF8String(TestFiles.PVICA_HOMEMADE_NOT_OWNPROD);
        DsRecordDto recordDto = new DsRecordDto().data(pvica).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L)
                .origin("ds.tv").kalturaId("randomKalturaId");


        String jsonld = jsonldView.apply(recordDto);
        assertTrue(jsonld.contains("\"kb:production_code_allowed\":false," +
                                    "\"kb:production_code_value\":3600"));
    }


    @Test
    @Tag("integration")
    void holdbackNameSolrTest() throws Exception {
        HoldbackDatePicker.init();
        View jsonldView = getSolrTvViewForPreservicaRecord();
        String pvica = Resolver.resolveUTF8String(TestFiles.PVICA_RECORD_0e89456b);
        DsRecordDto recordDto = new DsRecordDto().data(pvica).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L)
                .origin("ds.tv").kalturaId("randomKalturaId");


        String solrdoc = jsonldView.apply(recordDto);
        assertTrue(solrdoc.contains("\"holdback_name\":\"Underholdning\","));
    }

    @Test
    @Tag("integration")
    void testFormAndContentInTransformation() throws Exception {
        HoldbackDatePicker.init();
        View jsonldView = getSolrTvViewForPreservicaRecord();
        String pvica = Resolver.resolveUTF8String(TestFiles.PVICA_RECORD_0e89456b);
        DsRecordDto recordDto = new DsRecordDto().data(pvica).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L)
                .origin("ds.tv").kalturaId("randomKalturaId");

        String solrdoc = jsonldView.apply(recordDto);
        assertTrue(solrdoc.contains("\"holdback_form_value\":\"1300\",\"holdback_content_value\":\"6700\","));
    }

    @Test
    @Tag("integration")
    void testDRProductionID() throws IOException {
        HoldbackDatePicker.init();
        View jsonldView = getSolrTvViewForPreservicaRecord();
        String pvica = Resolver.resolveUTF8String(TestFiles.PVICA_HOMEMADE_DOMS_MIG_WITH_TVMETER_ADDED);
        DsRecordDto recordDto = new DsRecordDto().data(pvica).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L)
                .origin("ds.tv").kalturaId("randomKalturaId");

        String solrdoc = jsonldView.apply(recordDto);
        assertTrue(solrdoc.contains("\"dr_production_id\":\"8030782300\""));

        assertTrue(solrdoc.contains("\"dr_id_restricted\":\"false\""));
    }

    @Test
    @Tag("integration")
    void testRestrictedDRProductionID() throws IOException {
        HoldbackDatePicker.init();
        ProductionIdLookup.init();
        View jsonldView = getSolrTvViewForPreservicaRecord();
        String pvica = Resolver.resolveUTF8String(TestFiles.PVICA_RECORD_4d61dcb3);
        DsRecordDto recordDto = new DsRecordDto().data(pvica).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L)
                .origin("ds.tv").kalturaId("randomKalturaId");

        String solrdoc = jsonldView.apply(recordDto);
        assertTrue(solrdoc.contains("\"dr_id_restricted\":\"true\""));
    }

    @Test
    @Tag("integration")
    void testOwnProductionCorrectValue() throws IOException {
        HoldbackDatePicker.init();
        View jsonldView = getSolrTvViewForPreservicaRecord();
        String pvica = Resolver.resolveUTF8String(TestFiles.PVICA_DOMS_MIG_5d6db06e);
        DsRecordDto recordDto = new DsRecordDto().data(pvica).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L)
                .origin("ds.tv").kalturaId("randomKalturaId");

        String solrdoc = jsonldView.apply(recordDto);
        assertTrue(solrdoc.contains("\"production_code_value\":\"4400\""));

    }

    @Test
    @Tag("integration")
    void testHasKalturaIdBoolean() throws IOException {
        HoldbackDatePicker.init();
        View solrView = getSolrTvViewForPreservicaRecord();
        String preservicaRecord = Resolver.resolveUTF8String(TestFiles.PVICA_RECORD_4d61dcb3);
        DsRecordDto recordDto = new DsRecordDto().data(preservicaRecord).id("test.id").mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L)
                .origin("ds.tv").kalturaId("randomKalturaId");

        String solrdoc = solrView.apply(recordDto);
        assertTrue(solrdoc.contains("\"has_kaltura_id\":\"true\""));
    }

    @Test
    @Tag("integration")
    void testNoKalturaIdBoolean() throws IOException {
        HoldbackDatePicker.init();
        View solrView = getSolrTvViewForPreservicaRecord();
        String preservicaRecord = Resolver.resolveUTF8String(TestFiles.PVICA_RECORD_4d61dcb3);
        DsRecordDto recordDto = new DsRecordDto().data(preservicaRecord).id("test.id")
                .mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L).origin("ds.tv");

        String solrdoc = solrView.apply(recordDto);
        assertTrue(solrdoc.contains("\"has_kaltura_id\":\"false\""));
    }

    @Test
    void alwaysFailingRecordTest() throws IOException {
        HoldbackDatePicker.init();
        View solrView = getSolrTvViewForPreservicaRecord();
        String preservicaRecord = Resolver.resolveUTF8String("internal_test_files/preservica7/errorRecord.xml");
        DsRecordDto recordDto = new DsRecordDto().data(preservicaRecord).id("test.id")
                .mTimeHuman("2023-11-29 13:45:49+0100").mTime(1701261949625000L).origin("ds.tv");


        assertThrows(RuntimeException.class, () -> {
            solrView.apply(recordDto);
        } );
    }

    //********************************************** PRIVATE HELPER METHODS BELOW ***************************************************************

    /**
     * Create test view for Preservica Schema.org transformation
     * @return Schema.org JSON view for preservica records.
     */
    private static View getPreservicaRadioJsonView() throws IOException {
        if (Resolver.getPathFromClasspath(TestFiles.PVICA_RECORD_df3dc9cf) == null){
            fail("Missing internal test files");
        }
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML radioConf = conf.getYAMLList("origins").get(2);
        View jsonldView = new View(radioConf.getSubMap("\"ds.radio\"").getYAMLList("views").get(1),
                radioConf.getSubMap("\"ds.radio\"").getString("origin"));
        return jsonldView;
    }

    /**
     * Create test view for Preservica Schema.org transformation
     * @return Schema.org JSON view for preservica records.
     */
    private static View getPreservicaTvJsonView() throws IOException {
        if (Resolver.getPathFromClasspath(TestFiles.PVICA_RECORD_df3dc9cf) == null){
            fail("Missing internal test files");
        }
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML tvConf = conf.getYAMLList("origins").get(3);
        return new View(tvConf.getSubMap("\"ds.tv\"").getYAMLList("views").get(1),
                tvConf.getSubMap("\"ds.tv\"").getString("origin"));
    }

    /**
     * Create test view for Preservica solr transformation
     * @return solr view for preservica records.
     */
    private static View getSolrTvViewForPreservicaRecord() throws IOException {
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML tvConf = conf.getYAMLList(".origins").get(3);
        View solrView = new View(tvConf.getSubMap("\"ds.tv\"").getYAMLList("views").get(2),
                tvConf.getSubMap("\"ds.tv\"").getString("origin"));
        return solrView;
    }

    /**
     * Create test view for Preservica solr transformation
     * @return Solr JSON view for preservica records.
     */
    private static View getPreservicaRadioSolrView() throws IOException {
        if (Resolver.getPathFromClasspath(TestFiles.PVICA_RECORD_df3dc9cf) == null){
            fail("Missing internal test files");
        }
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML radioConf = conf.getYAMLList("origins").get(2);
        View jsonldView = new View(radioConf.getSubMap("\"ds.radio\"").getYAMLList("views").get(2),
                radioConf.getSubMap("\"ds.radio\"").getString("origin"));
        return jsonldView;
    }
}
