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

import dk.kb.present.TestFiles;
import dk.kb.present.TestUtil;

import dk.kb.present.config.ServiceConfig;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class XSLTCumulusToSolrTransformerTest extends XSLTTransformerTestBase {

	private static final Logger log = LoggerFactory.getLogger(XSLTCumulusToSolrTransformerTest.class);

	public static final String MODS2SOLR = "xslt/mods2solr.xsl";
	Map<String, String> IMAGESERVER_EXAMPLE = Map.of("imageserver", "https://example.com/imageserver/");

    @Override
    String getXSLT() {
        return MODS2SOLR;
    }

    @Override
    public Map<String, String> getInjections() {
        return IMAGESERVER_EXAMPLE;
    }

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
	void testSolrNew() {
        assertContains(TestFiles.CUMULUS_RECORD_05fea810, "\"id\":\"ds.test:05fea810-7181-11e0-82d7-002185371280.xml\"");
	}

	/**
	 * This record is tested thoroughly in EmbeddedSolrTest.testRecordDPK()
	 */
	@Test
	void testXsltNewDpkItem() {
		assertContains(TestFiles.CUMULUS_RECORD_3956d820, "\"location\":\"Billedsamlingen. Postkortsamlingen, Vestindien, Sankt Thomas," +
                " Charlotte Amalie, Det gamle fort\\/politistation\"");
	}

	@Test
	void testXslt45dd() {
        assertContains(TestFiles.CUMULUS_RECORD_45dd4830, "\"catalog\":\"Samlingsbilleder\",\"collection\":\"Billedsamlingen\"");
	}

	/**
	 * This record is fully tested in EmbeddedSolrTest.testRecord096c9090()
	 */
	@Test
	void testXslt096() {
		assertContains(TestFiles.CUMULUS_RECORD_096c9090,
		"\"id\":\"ds.test:096c9090-717f-11e0-82d7-002185371280.xml\"");
	}

	@Test
	void testNoNameButAffiliation() {
		assertContains(TestFiles.CUMULUS_RECORD_DNF,
		"\"creator_affiliation\":[\"Bisson frères\"],\"creator_affiliation_description\":[\"fransk korporation\"]");
		assertNotContains(TestFiles.CUMULUS_RECORD_DNF, "creator_given_name");
	}

	@Test
	void testNoNameMultipleNames() {
		assertContains(TestFiles.CUMULUS_RECORD_5cc1bea0, "\"creator_given_name\":[\"Chen\"]");
		assertContains(TestFiles.CUMULUS_RECORD_5cc1bea0, "\"creator_date_of_birth\":[\"1945-0-0\",\"1945-0-0\"]");
	}

	@Test
	void testEmptyCollection() {
		assertNotContains(TestFiles.CUMULUS_RECORD_09222b40, "collection");
	}

	@Test
	void testNoTermsOfAddress() {
		assertNotContains(TestFiles.CUMULUS_RECORD_ANSK, "creator_terms_of_address");
	}

	@Test
	void testXsltSkfF0137() {
		assertContains(TestFiles.CUMULUS_RECORD_54b34b50,
                "\"categories\":[\"Rytterskoler x\",\"Skolehistorie\",\"69 testfiler\"]");
	}

	@Test
	void testCombinationOfContentAndDescription() {
        assertContains(TestFiles.CUMULUS_RECORD_ANSK, "\"111, 4: Forskellige former for fald. Pap med opklæbet billede. " +
                "Forestiller træ og mennesker, der vælter og falder. Ingen tekst på billedet.\"");
		assertContains(TestFiles.CUMULUS_RECORD_54b34b50,"\"Den gamle rytterskole i Hørning (Sønder-Hørning). Facadebillede. " +
                "Fotografi kopi udført af Rudolph Jørgensen Helsingør (etabi 1897)\"");
	}

	@Test
	void testUldall() {
		assertContains(TestFiles.CUMULUS_RECORD_e2519ce0, "\"catalog\":\"Maps\"");
	}

	@Test
	void testChineseTitels() {
		assertContains(TestFiles.CUMULUS_RECORD_26d4dd60,
                "\"creator_affiliation\":[\"Haidian Yangfang dian jiedao zhongxin xiaoxue (海淀区羊坊店街道中心小学)\"]");
	}

	@Test
	void testMultipleAffiliations() {
		assertContains(TestFiles.CUMULUS_RECORD_9c17a440, "\"creator_affiliation\":[\"Billedbladet\",\"Nordisk Pressefoto\"]");
	}

	@Test
	void testMultipleDescriptions() {
		assertContains(TestFiles.CUMULUS_RECORD_3b03aa00, "\"creator_affiliation\":[\"Aftenbladet\",\"Associated Press\"]," +
                "\"creator_affiliation_description\":[\"dansk avis\",\"amerikansk nyhedsbureau\"]");
	}

	@Test
	void testDifferentRelatedItems() {
		assertContains(TestFiles.CUMULUS_RECORD_25461fb0, "\"collection\":\"Bladtegnersamlingen\",\"published_in\":\"Aktuelt\"");
	}

	@Test
	void testTitleExtraction() {
		assertContains(TestFiles.CUMULUS_RECORD_770379f0, "\"title\":[\"Romeo og Julie\"]");
	}

	@Test
	void testImageResource() throws Exception {
		String yamlStr =
				"stylesheet: '" + MODS2SOLR + "'\n" +
				"injections:\n" +
				"  - imageserver: 'https://example.com/imageserver/'\n" +
		        "  - old_imageserver: 'http://kb-images.kb.dk'\n";
		YAML yaml = YAML.parse(new ByteArrayInputStream(yamlStr.getBytes(StandardCharsets.UTF_8)));
		String solrString = TestUtil.getTransformedFromConfigWithAccessFields(yaml, TestFiles.CUMULUS_RECORD_40221e30);
		assertTrue(solrString.contains("\"resource_id\":[\"\\/DAMJP2\\/DAM\\/Samlingsbilleder\\/0000\\/624\\/420\\/KE070592\"]"));
		assertTrue(solrString.contains("\"thumbnail\":\"https:\\/\\/example.com\\/imageserver\\/%2FDAMJP2%2FDAM%2FSamlingsbilleder%2F0000%2F624%2F420%2FKE070592\\/full\\/%21150%2C150\\/0\\/default.jpg\""));
	}

	@Test
	void testSurrogateProduction() {
		assertContains(TestFiles.CUMULUS_RECORD_FM, "\"production_date_digital_surrogate\":\"2018-01-15T12:26:00.000+01:00\"");
	}

	@Test
	void testOrigin() throws Exception {
		String yamlStr =
				"stylesheet: '" + MODS2SOLR + "'\n" +
						"injections:\n" +
						"  - imageserver: 'https://example.com/imageserver/'\n" +
						"  - old_imageserver: 'http://kb-images.kb.dk'\n";
		YAML yaml = YAML.parse(new ByteArrayInputStream(yamlStr.getBytes(StandardCharsets.UTF_8)));
		String solrString = TestUtil.getTransformedFromConfigWithAccessFields(yaml, TestFiles.CUMULUS_RECORD_40221e30);
		assertTrue(solrString.contains("\"origin\":\"ds.test\""));
	}


}