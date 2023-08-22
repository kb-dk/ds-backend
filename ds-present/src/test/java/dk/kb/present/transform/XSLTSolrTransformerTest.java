package dk.kb.present.transform;

import dk.kb.present.TestUtil;

import dk.kb.present.config.ServiceConfig;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
class XSLTSolrTransformerTest{

    private static final Logger log = LoggerFactory.getLogger(XSLTSolrTransformerTest.class);

    public static final String MODS2SOLR = "xslt/mods2solr.xsl";
    Map<String, String> IMAGESERVER_EXAMPLE = Map.of("imageserver", "https://example.com/imageserver");
    public static final String RECORD_05fea810 = "xml/copyright_extraction/05fea810-7181-11e0-82d7-002185371280.xml";
    public static final String RECORD_3956d820 = "xml/copyright_extraction/3956d820-7b7d-11e6-b2b3-0016357f605f.xml";
    public static final String RECORD_096c9090 = "xml/copyright_extraction/096c9090-717f-11e0-82d7-002185371280.xml";
    public static final String RECORD_aaf3b130 = "xml/copyright_extraction/aaf3b130-e6e7-11e6-bdbe-00505688346e.xml";
    public static final String RECORD_54b34b50 = "xml/copyright_extraction/54b34b50-2ce6-11ed-81b4-005056882ec3.xml";
    public static final String RECORD_8e608940 = "xml/copyright_extraction/8e608940-d6db-11e3-8d2e-0016357f605f.xml";
    public static final String RECORD_45dd4830 = "xml/copyright_extraction/45dd4830-717f-11e0-82d7-002185371280.xml";
    public static final String RECORD_DNF = "xml/copyright_extraction/DNF_1951-00352_00052.tif.xml";
    public static final String RECORD_ANSK = "xml/copyright_extraction/ANSK_11614.tif.xml";
    public static final String RECORD_FM = "xml/copyright_extraction/FM103703H.tif.xml";
    public static final String RECORD_5cc1bea0 = "xml/copyright_extraction/5cc1bea0-71fa-11e2-b31c-0016357f605f.xml";
    public static final String RECORD_Elf = "xml/copyright_extraction/09222b40-dba1-11e5-9785-0016357f605f.xml";
    public static final String RECORD_e2519ce0 = "xml/copyright_extraction/e2519ce0-9fb0-11e8-8891-00505688346e.xml";
    public static final String RECORD_26d4dd60 ="xml/copyright_extraction/26d4dd60-6708-11e2-b40f-0016357f605f.xml";
    public static final String RECORD_9C = "xml/copyright_extraction/9c17a440-fe1a-11e8-9044-00505688346e.xml";
    public static final String RECORD_3B03 = "xml/copyright_extraction/3b03aa00-fee2-11e8-ab76-00505688346e.xml";
    public static final String RECORD_DB_hans = "xml/copyright_extraction/25461fb0-f664-11e0-9d29-0016357f605f.xml";
    public static final String RECORD_770379f0 = "xml/copyright_extraction/770379f0-8a0d-11e1-805f-0016357f605f.xml";
    public static final String RECORD_40221e30 = "xml/copyright_extraction/40221e30-1414-11e9-8fb8-00505688346e.xml";

    @BeforeAll
    public static void fixConfiguration() throws IOException {
        String CONFIG = Resolver.resolveGlob("conf/ds-present-behaviour.yaml").get(0).toString();
        if ("[]".equals(CONFIG)) {
            throw new IllegalStateException("Unable to locate config");
        }

        log.info("Fixing config to '{}'", CONFIG);
        ServiceConfig.initialize(CONFIG);
    }

    @Test
    void testSolrNew() throws Exception {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_05fea810, IMAGESERVER_EXAMPLE);
        String expectedID = "ds.test:" + Path.of(RECORD_05fea810).getFileName().toString();
        assertTrue(solrString.contains("\"id\":\"" + expectedID + "\""));
    }

    /**
     * This record is tested thoroughly in EmbeddedSolrTest.testRecordDPK()
     */
    @Test
    void testXsltNewDpkItem() throws Exception {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_3956d820, IMAGESERVER_EXAMPLE);
        assertTrue(solrString.contains("\"location\":\"Billedsamlingen. Postkortsamlingen, Vestindien, Sankt Thomas, Charlotte Amalie, Det gamle fort\\/politistation\""));
    }

    @Test
    void testXslt45dd() throws Exception {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_45dd4830);
        assertTrue(solrString.contains("\"catalog\":\"Samlingsbilleder\",\"collection\":\"Billedsamlingen\""));
    }

    /**
     * This record is fully tested in EmbeddedSolrTest.testRecord096c9090()
     */
    @Test
    void testXslt096() throws Exception {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_096c9090, IMAGESERVER_EXAMPLE);
        String expectedID = "ds.test:" + Path.of(RECORD_096c9090).getFileName().toString();
        assertTrue(solrString.contains("\"id\":\"" + expectedID + "\""));
    }

    @Test
    void testNoNameButAffiliation() throws Exception {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_DNF, IMAGESERVER_EXAMPLE);
        assertTrue(solrString.contains("\"creator_affiliation\":[\"Bisson frères\"],\"creator_affiliation_description\":[\"fransk korporation\"]"));
        assertFalse(solrString.contains("creator_given_name"));
    }

    @Test
    void testNoNameMultipleNames() throws Exception {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_5cc1bea0, IMAGESERVER_EXAMPLE);
        assertTrue(solrString.contains("\"creator_given_name\":[\"Chen\"]"));
        assertTrue(solrString.contains("\"creator_date_of_birth\":[\"1945-0-0\",\"1945-0-0\"]"));
    }

    @Test
    void testEmptyCollection() throws Exception {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_Elf, IMAGESERVER_EXAMPLE);
        assertFalse(solrString.contains("collection"));
    }

    @Test
    void testNoTermsOfAddress() throws Exception {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_ANSK, IMAGESERVER_EXAMPLE);
        assertFalse(solrString.contains("creator_terms_of_address"));
    }

    @Test
    void testXsltSkfF0137() throws Exception {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_54b34b50, IMAGESERVER_EXAMPLE);
        assertTrue(solrString.contains("\"list_of_categories\":[\"Rytterskoler x\",\"Skolehistorie\",\"69 testfiler\"]"));
    }

    @Test
    void testCombinationOfContentAndDescription() throws Exception {
        String ansk = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_ANSK, IMAGESERVER_EXAMPLE);
        String record54b = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_54b34b50, IMAGESERVER_EXAMPLE);
        assertTrue(ansk.contains("\"111, 4: Forskellige former for fald. Pap med opklæbet billede. Forestiller træ og mennesker, der vælter og falder. Ingen tekst på billedet.\""));
        assertTrue(record54b.contains("\"Den gamle rytterskole i Hørning (Sønder-Hørning). Facadebillede. Fotografi kopi udført af Rudolph Jørgensen Helsingør (etabi 1897)\""));
    }

    @Test
    void testUldall() throws Exception {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_e2519ce0, IMAGESERVER_EXAMPLE);
        assertTrue(solrString.contains("\"catalog\":\"Maps\""));
    }

    @Test
    void testChineseTitels() throws Exception {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_26d4dd60, IMAGESERVER_EXAMPLE);
        assertTrue(solrString.contains("\"creator_affiliation\":[\"Haidian Yangfang dian jiedao zhongxin xiaoxue (海淀区羊坊店街道中心小学)\"]"));
    }

    @Test
    void testMultipleAffiliations() throws Exception {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR,  RECORD_9C, IMAGESERVER_EXAMPLE);
        assertTrue(solrString.contains("\"creator_affiliation\":[\"Billedbladet\",\"Nordisk Pressefoto\"]"));
    }

    @Test
    void testMultipleDescriptions() throws Exception {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR,  RECORD_3B03, IMAGESERVER_EXAMPLE);
        assertTrue(solrString.contains("\"creator_affiliation\":[\"Aftenbladet\",\"Associated Press\"],\"creator_affiliation_description\":[\"dansk avis\",\"amerikansk nyhedsbureau\"]"));
    }

    @Test
    void testDifferentRelatedItems() throws Exception {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_DB_hans, IMAGESERVER_EXAMPLE);
        assertTrue(solrString.contains("\"collection\":\"Bladtegnersamlingen\",\"published_in\":\"Aktuelt\""));
    }

    @Test
    void testTitleExtraction() throws Exception {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_770379f0);
        assertTrue(solrString.contains("\"title\":[\"Romeo og Julie\"]"));
    }

    @Test
    void testImageResource() throws Exception {
        String yamlStr =
                "stylesheet: '" + MODS2SOLR + "'\n" +
                        "injections:\n" +
                        "  - imageserver: 'https://example.com/imageserver'\n";
        YAML yaml = YAML.parse(new ByteArrayInputStream(yamlStr.getBytes(StandardCharsets.UTF_8)));
        String solrString = TestUtil.getTransformedFromConfigWithAccessFields(yaml, RECORD_40221e30);
        assertTrue(solrString.contains("\"https:\\/\\/example.com\\/imageserver\\/DAMJP2\\/DAM\\/Samlingsbilleder\\/0000\\/624\\/420\\/KE070592\""));
    }

    @Test
    void testSurrogateProduction() throws Exception{
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_FM, IMAGESERVER_EXAMPLE);
        System.out.println(solrString);
        assertTrue(solrString.contains("\"production_date_digital_surrogate\":\"2018-01-15T12:26:00.000+01:00\""));
    }

    @Test
    void prettyPrinter() throws Exception {
        prettyPrintSolrJsonFromMods(RECORD_aaf3b130);
    }

    /**
     * Transform the input MODS record with an XSLT and return as pretty JSON
     */
    private void prettyPrintSolrJsonFromMods(String record) throws Exception {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, record);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(solrString);
        String prettyJsonString = gson.toJson(je);
        System.out.println(prettyJsonString);
    }


}