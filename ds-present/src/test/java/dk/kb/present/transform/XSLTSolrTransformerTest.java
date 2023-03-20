package dk.kb.present.transform;

import dk.kb.present.TestUtil;

import org.junit.jupiter.api.Test;

import com.google.gson.*;

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
	// TODO: Disable printing of XSLT output

	public static final String MODS2SOLR = "xslt/mods2solr.xsl";
	public static final String RECORD_000332 = "xml/copyright_extraction/000332.tif.xml"; 
	public static final String RECORD_DPK = "xml/copyright_extraction/DPK000107.tif.xml";
	public static final String RECORD_096c9090 = "xml/copyright_extraction/096c9090-717f-11e0-82d7-002185371280.xml";
	public static final String RECORD_DT005031 = "xml/copyright_extraction/DT005031.tif.xml";
	public static final String RECORD_SKF_f_0137 = "xml/copyright_extraction/SKF_f_0137.tif.xml";
	public static final String RECORD_KHP0001_049 = "xml/copyright_extraction/KHP0001-049.tif.xml";
	public static final String RECORD_45dd4830 = "xml/copyright_extraction/45dd4830-717f-11e0-82d7-002185371280.xml";
	public static final String RECORD_DNF = "xml/copyright_extraction/DNF_1951-00352_00052.tif.xml";
	public static final String RECORD_ANSK = "xml/copyright_extraction/ANSK_11614.tif.xml";
	public static final String RECORD_FM = "xml/copyright_extraction/FM103703H.tif.xml";
	public static final String RECORD_OA = "xml/copyright_extraction/OA_102-2007-pl_0004.tif.xml";
	public static final String  RECORD_Elf = "xml/copyright_extraction/Elf_113136.tif.xml";
	public static final String RECORD_ULDALL = "xml/copyright_extraction/Uldall_186_2_Foborg.tif.xml";
	public static final String RECORD_KOB_KIN ="xml/copyright_extraction/kob_kin_pl_0027.tif.xml";
	public static final String RECORD_9C = "xml/copyright_extraction/9c17a440-fe1a-11e8-9044-00505688346e.xml";
	public static final String RECORD_3B03 = "xml/copyright_extraction/3b03aa00-fee2-11e8-ab76-00505688346e.xml";
	public static final String RECORD_DB_hans = "xml/copyright_extraction/db_hans_lollesgaard_00039.tif.xml";
	public static final String RECORD_JB000132 = "xml/copyright_extraction/JB000132_114.tif.xml";
	public static final String RECORD_beb9 = "xml/copyright_extraction/beb9f1e0-bc32-11e2-9acb-0016357f605f.xml";
	public static final String RECORD_40221e30 = "xml/copyright_extraction/40221e30-1414-11e9-8fb8-00505688346e.xml";
	@Test
	void testSolrNew() throws Exception {

		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_000332);
		// TODO: Add more detailed test

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);        
		System.out.println(prettyJsonString );

		//  assertTrue(solrString.contains("{\"id\":\""));
	}

	/**
	 * This record is tested thoroughly in EmbeddedSolrTest.testRecordDPK()
	 */
	@Test
	void testXsltNewDpkItem() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_DPK);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString );

		//  assertTrue(solrString.contains("{\"id\":\""));
	}

	@Test
	void testXslt45dd() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR,  RECORD_45dd4830 );
		// TODO: Add more detailed test
		// One test could be to check for production_date_start and production_date_end values

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString );

		//  assertTrue(solrString.contains("{\"id\":\""));
	}

	/**
	 * This record is fully tested in EmbeddedSolrTest.testRecord096c9090()
	 */
	@Test
	void testXslt096() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_096c9090);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString );

		//  assertTrue(solrString.contains("{\"id\":\""));
	}

	@Test
	void testNoNameButAffiliation() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_DNF);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString );

		//  assertTrue(solrString.contains("{\"id\":\""));
	}

	@Test
	void testNoNameMultipleNames() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_OA);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString );

		//  assertTrue(solrString.contains("{\"id\":\""));
	}

	@Test
	void testWhitespaceTrim() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_FM);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString );

		//  assertTrue(solrString.contains("{\"id\":\""));
	}

	@Test
	void testEmptyCollection() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_Elf);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString );

		//  assertTrue(solrString.contains("{\"id\":\""));
	}

	@Test
	void testNoTermsOfAddress() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_ANSK);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString );

		//  assertTrue(solrString.contains("{\"id\":\""));
	}

	@Test
	void testXsltDt005031() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_DT005031);
		// TODO: Add more detailed test
		// Test that date_created is present and that subject is not

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString );

		//  assertTrue(solrString.contains("{\"id\":\""));
	}

	@Test
	void testXsltSkfF0137() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_SKF_f_0137);
		// TODO: Add more detailed test
		// Test that date_created is present and that subject is not

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString );

		//  assertTrue(solrString.contains("{\"id\":\""));
	}

	@Test
	void testXsltKhp0001049() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_KHP0001_049);
		// TODO: Add more detailed test
		// Test that date_created is present and that subject is not

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString );

		//  assertTrue(solrString.contains("{\"id\":\""));
	}

	@Test
	void testUldall() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_ULDALL);
		// TODO: Add more detailed test
		// Test that date_created is present and that subject is not

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString );

		//  assertTrue(solrString.contains("{\"id\":\""));
	}

	@Test
	void testChineseTitels() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR,  RECORD_KOB_KIN );
		// TODO: Add more detailed test
		// One test could be to check for production_date_start and production_date_end values

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString );

		//  assertTrue(solrString.contains("{\"id\":\""));
	}

	@Test
	void testMultipleAffiliations() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR,  RECORD_9C);
		// TODO: Add more detailed test
		// One test could be to check for production_date_start and production_date_end values

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString );

		//  assertTrue(solrString.contains("{\"id\":\""));
	}

	@Test
	void testMultipleDescriptions() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR,  RECORD_3B03);
		// TODO: Add more detailed test
		// One test could be to check for production_date_start and production_date_end values

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString );

		//  assertTrue(solrString.contains("{\"id\":\""));
	}

	@Test
	void testDifferentRelatedItems() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR,  RECORD_DB_hans);
		// TODO: Add more detailed test
		// One test could be to check for production_date_start and production_date_end values

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString );

		//  assertTrue(solrString.contains("{\"id\":\""));
	}

	@Test
	void testTitleExtraction() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR,  RECORD_JB000132);
		// TODO: Add more detailed test
		// One test could be to check for production_date_start and production_date_end values

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString );

		//  assertTrue(solrString.contains("{\"id\":\""));
	}

	@Test
	void testImageResource() throws Exception {

		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_40221e30);
		// TODO: Add more detailed test

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString );

		//  assertTrue(solrString.contains("{\"id\":\""));
	}


}