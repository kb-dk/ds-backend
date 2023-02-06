package dk.kb.present.transform;

import dk.kb.present.TestUtil;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;

import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;

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
class XSLTSolrTransformerTest{

	public static final String MODS2SOLR_OLD = "xslt/mods2solr_OLD.xsl";
	public static final String MODS2SOLR = "xslt/mods2solr.xsl";
	public static final String RECORD_000332 = "xml/copyright_extraction/000332.tif.xml"; 
	public static final String RECORD_DPK = "xml/copyright_extraction/DPK000107.tif.xml";
	public static final String RECORD_096c9090 = "xml/copyright_extraction/096c9090-717f-11e0-82d7-002185371280.xml";
	public static final String RECORD_DT005031 = "xml/copyright_extraction/DT005031.tif.xml";


	@Test
	void testSolOld() throws IOException {
		String solrString = TestUtil.getTransformed(MODS2SOLR_OLD,  RECORD_000332);
		// TODO: Add more detailed test
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);                
		String prettyJsonString = gson.toJson(je);        
		System.out.println(prettyJsonString );

	}

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

	@Test
	void testXsltNewDpkItem() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_DPK);
		// TODO: Add more detailed test
		// Some tests could be to check for area and genre fields.


		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString );

		//  assertTrue(solrString.contains("{\"id\":\""));
	}

	@Test
	void testXslt096() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_096c9090);
		// TODO: Add more detailed test
		// One test could be to check for production_date_start and production_date_end values

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




}