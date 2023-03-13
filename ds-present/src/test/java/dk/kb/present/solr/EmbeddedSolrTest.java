package dk.kb.present.solr;

import java.io.File;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

import org.apache.solr.core.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;



import dk.kb.present.TestUtil;

public class EmbeddedSolrTest {

	private static final Logger log = LoggerFactory.getLogger(EmbeddedSolrTest.class);
	private static String solr_home = "target/test-classes/solr";

	private static CoreContainer coreContainer = null;
	private static EmbeddedSolrServer embeddedServer = null;

	public static final String MODS2SOLR = "xslt/mods2solr.xsl";
	public static final String RECORD_000332 = "xml/copyright_extraction/000332.tif.xml"; 
	public static final String RECORD_DPK = "xml/copyright_extraction/DPK000107.tif.xml";
	public static final String RECORD_096c9090 = "xml/copyright_extraction/096c9090-717f-11e0-82d7-002185371280.xml";
	public static final String RECORD_DT005031 = "xml/copyright_extraction/DT005031.tif.xml";
	public static final String RECORD_SKF_f_0137 = "xml/copyright_extraction/SKF_f_0137.tif.xml";
	public static final String RECORD_KHP0001_049 = "xml/copyright_extraction/KHP0001-049.tif.xml";
	public static final String RECORD_DNF = "xml/copyright_extraction/DNF_1951-00352_00052.tif.xml";
	public static final String RECORD_ANSK = "xml/copyright_extraction/ANSK_11614.tif.xml";
	public static final String RECORD_ULDALL = "xml/copyright_extraction/Uldall_186_2_Foborg.tif.xml";
	public static final String RECORD_FM = "xml/copyright_extraction/FM103703H.tif.xml";
	public static final String RECORD_DB_hans = "xml/copyright_extraction/db_hans_lollesgaard_00039.tif.xml";
	public static final String RECORD_JB000132 = "xml/copyright_extraction/JB000132_114.tif.xml";



	@BeforeAll
	public static void startEmbeddedSolrServer() throws Exception {

		File solrHomeDir = new File(solr_home);		
		String solrHomeAbsoluteDir= solrHomeDir.getAbsolutePath();	
		Path solrHome =  Paths.get(solrHomeAbsoluteDir);
		System.setProperty("solr.install.dir", solrHomeAbsoluteDir);
		Properties props = new Properties();
		// props.put("solr.install.dir", solrHomeDir.getAbsolutePath()); //Does not
		// work. Use system property above for now
		coreContainer = new CoreContainer(solrHome, props);
		coreContainer.load();
		embeddedServer = new EmbeddedSolrServer(coreContainer, "dssolr");
	}

	@AfterAll
	public static void tearDown() throws Exception {
		coreContainer.shutdown();
		embeddedServer.close();
	}

	/*
	 * Delete all documents in solr between tests, so each unittest gets a clean solr.
	 */
	@BeforeEach
	public void deleteDocs() throws Exception {
		embeddedServer.deleteByQuery("*:*");
	}

	@Test
	void testRecord000332() throws Exception {

		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_000332);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		//System.out.println(prettyJsonString);

		SolrInputDocument document = convertJsonToSolrJavaDoc(prettyJsonString);
		embeddedServer.add(document);
		embeddedServer.commit();                
		assertEquals(1, getNumberOfTotalDocuments());


		//Full life cycle test
		SolrDocument record = getRecordById("05fea810-7181-11e0-82d7-002185371280");


		//Single value field
		assertEquals("000332.tif",record.getFieldValue("identifier_local"));

		//multivalue field
		assertMultivalueField(record, "type_of_resource", "Billede, Todimensionalt billedmateriale", "Grafik" );
		// Creator date of death
		assertMultivalueField(record, "creator_date_of_death", "1868-2-14", "1895-6-25", "1865-3-8" );

		//TODO more fields

	}



	/**
	 * Full test for one item.
	 */
	@Test
	void testRecordDPK() throws Exception {

		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_DPK);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		//System.out.println(prettyJsonString);

		SolrInputDocument document = convertJsonToSolrJavaDoc(prettyJsonString);
		embeddedServer.add(document);
		embeddedServer.commit();                
		assertEquals(1, getNumberOfTotalDocuments());


		//Full life cycle test
		SolrDocument record = getRecordById("3956d820-7b7d-11e6-b2b3-0016357f605f");

		assertContentAllSingleValues(record, "DPK000107.tif", "da",
				"Billedsamlingen. Postkortsamlingen, Vestindien, Sankt Thomas, Charlotte Amalie, Det gamle fort/politistation",
				"Postkortsamlingen, Vestindien, Postkort, Vestindien, CAR- BLO katagori, Postkortsamlingen, 2022-09-01 15:06:39, 2022-09-01 15:11:09",
				"Samlingsbilleder", "Billedsamlingen", 9657172L, 1429,2247);

		//Single value fields
		assertEquals("Vestindien, Sankt Thomas, Charlotte Amalie, Fort Christian", record.getFieldValue("area"));

		//Multivalue fields
		// type_of_resource
		assertMultivalueField(record, "type_of_resource", "Billede, Todimensionalt billedmateriale", "Postkort" );

		// topic
		assertMultivalueField(record, "topic", "postkort","forter","Dannebrog", "børn", "arkitekturer",
				"postcards", "forts", "Dannebrog", "children", "architectures" );
	}



	/**
	 * Full test for item
	 */
	@Test
	void testRecord096c9090() throws Exception {

		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_096c9090);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		//System.out.println(prettyJsonString);

		SolrInputDocument document = convertJsonToSolrJavaDoc(prettyJsonString);
		embeddedServer.add(document);
		embeddedServer.commit();
		assertEquals(1, getNumberOfTotalDocuments());


		//Full life cycle test
		SolrDocument record = getRecordById("096c9090-717f-11e0-82d7-002185371280");

		assertContentAllSingleValues(record,"000225.tif", "da",
				"Billedsamlingen. Danske portrætter, 4°, Egede, Poul (1708-1789)",
				"Danske portrætter, X-langtidsbevaring test - BLO, Diverse, 2022-09-01 15:06:39, 2022-09-01 15:11:09, 2022-09-02 09:01:13",
				"Samlingsbilleder","Billedsamlingen",6691996L,1812,1227);


		//Single value field
		assertEquals("1755",record.getFieldValue("production_date_start"));
		assertEquals("1831",record.getFieldValue("production_date_end"));

		//multivalue fields
		// creator_name
		assertMultivalueField(record,"creator_name", "Clemens, Johann Friderich");

		// creator_full_name
		assertMultivalueField(record,"creator_full_name", "Johann Friderich Clemens");

		// creator_family_name
		assertMultivalueField(record,"creator_family_name", "Clemens");

		// creator_given_name
		assertMultivalueField(record,"creator_given_name", "Johann Friderich");

		// creator_terms_of_address
		assertMultivalueField(record,"creator_terms_of_address", "kobberstikker");
		Collection<Object> creatorTermsOfAddress = record.getFieldValues("creator_terms_of_address");

		// type_of_resource
		assertMultivalueField(record, "type_of_resource", "Billede, Todimensionalt billedmateriale", "Grafik" );

		// topic
		assertMultivalueField(record, "topic", "Poul Egede. 1911,7507.", "Billedet befinder sig i Kort- og Billedafdelingen, Det Kongelige Bibliotek" );

		// subject_name
		assertMultivalueField(record,"subject_name", "Egede, Poul Hansen");

		// subject_full_name
		assertMultivalueField(record,"subject_full_name", "Poul Hansen Egede");

		// subject_family_name
		assertMultivalueField(record,"subject_family_name", "Egede");

		// subject_given_name
		assertMultivalueField(record,"subject_given_name", "Poul Hansen");

		// subject_date_of_birth
		assertMultivalueField(record,"subject_date_of_birth", "1708");

		// subject_date_of_death
		assertMultivalueField(record,"subject_date_of_death", "1789");

		// subject_terms_of_address
		assertMultivalueField(record,"subject_terms_of_address", "teolog, missionær, grønlandsfarer og biskop");
	}

	@Test
	void testRecordDt005031() throws Exception {

		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_DT005031);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		//System.out.println(prettyJsonString);

		SolrInputDocument document = convertJsonToSolrJavaDoc(prettyJsonString);
		embeddedServer.add(document);
		embeddedServer.commit();
		assertEquals(1, getNumberOfTotalDocuments());

		//Full life cycle test
		SolrDocument record = getRecordById("aaf3b130-e6e7-11e6-bdbe-00505688346e");

		//Single value field
		assertEquals("DT005031.tif",record.getFieldValue("identifier_local"));

		//multivalue field
		assertMultivalueField(record, "type_of_resource", "Billede, Todimensionalt billedmateriale", "Tegning" );
		//TODO more fields
	}

	@Test
	void testRecordANSK() throws Exception {

		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_ANSK);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		//System.out.println(prettyJsonString);

		SolrInputDocument document = convertJsonToSolrJavaDoc(prettyJsonString);
		embeddedServer.add(document);
		embeddedServer.commit();
		assertEquals(1, getNumberOfTotalDocuments());

		//Full life cycle test
		SolrDocument record = getRecordById("652b8260-9d78-11ed-92f5-005056882ec3");

		//Single value field
		assertEquals("ANSK_11614.tif",record.getFieldValue("identifier_local"));

		//multivalue field
		assertMultivalueField(record, "type_of_resource", "Billede, Todimensionalt billedmateriale", "Anskuelsesbillede" );
		//TODO more fields

	}

	@Test
	void testRecordSkfF0137() throws Exception {

		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_SKF_f_0137);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		//System.out.println(prettyJsonString);

		SolrInputDocument document = convertJsonToSolrJavaDoc(prettyJsonString);
		embeddedServer.add(document);
		embeddedServer.commit();
		assertEquals(1, getNumberOfTotalDocuments());

		//Full life cycle test
		SolrDocument record = getRecordById("54b34b50-2ce6-11ed-81b4-005056882ec3");

		//Single value field
		assertEquals("SKF_f_0137.tif",record.getFieldValue("identifier_local"));

		//multivalue field
		assertMultivalueField(record, "type_of_resource", "Billede, Todimensionalt billedmateriale", "Fotografi" );
		//TODO more fields

	}

	@Test
	void testRecordKhp() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_KHP0001_049);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString);

		SolrInputDocument document = convertJsonToSolrJavaDoc(prettyJsonString);
		embeddedServer.add(document);
		embeddedServer.commit();
		assertEquals(1, getNumberOfTotalDocuments());

		//Full life cycle test
		SolrDocument record = getRecordById("8e608940-d6db-11e3-8d2e-0016357f605f");

		//Single value field
		assertEquals("KHP0001-049.tif",record.getFieldValue("identifier_local"));

		//multivalue field
		assertMultivalueField(record, "type_of_resource", "Billede, Todimensionalt billedmateriale", "Dia" );

		assertMultivalueField(record, "list_of_categories", "KHP",
				"Keld Helmer-Petersen",
				"1940-1950",
				"Helmer-Petersen",
				"Keld",
				"CAR- BLO katagori",
				"ikke UA");
		//TODO more fields
	}

	@Test
	void testRecordUldallForTitleAndPlaceOfProduction() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_ULDALL);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		//System.out.println(prettyJsonString);

		SolrInputDocument document = convertJsonToSolrJavaDoc(prettyJsonString);
		embeddedServer.add(document);
		embeddedServer.commit();
		assertEquals(1, getNumberOfTotalDocuments());

		//Full life cycle test
		SolrDocument record = getRecordById("e2519ce0-9fb0-11e8-8891-00505688346e");

		//Single value field
		assertEquals("Uldall_186_2_Foborg.tif",record.getFieldValue("identifier_local"));

		// Title field
		assertEquals("Foborg, Foburgum", record.getFieldValue("title"));

		// Place of production
		assertEquals("Danmark", record.getFieldValue("place_of_production"));

		//TODO more fields
	}

	@Test
	void testAccessionNumberExtraction() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_FM);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		//System.out.println(prettyJsonString);

		SolrInputDocument document = convertJsonToSolrJavaDoc(prettyJsonString);
		embeddedServer.add(document);
		embeddedServer.commit();
		assertEquals(1, getNumberOfTotalDocuments());

		//Full life cycle test
		SolrDocument record = getRecordById("14f4a700-f9ee-11e7-988a-00505688346e");

		//Single value field
		assertEquals("2000-3/7",record.getFieldValue("accession_number"));
	}

	@Test
	void testPublishedInAndCollection() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_DB_hans);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		//System.out.println(prettyJsonString);

		SolrInputDocument document = convertJsonToSolrJavaDoc(prettyJsonString);
		embeddedServer.add(document);
		embeddedServer.commit();
		assertEquals(1, getNumberOfTotalDocuments());

		//Full life cycle test
		SolrDocument record = getRecordById("25461fb0-f664-11e0-9d29-0016357f605f");

		//Single value field
		assertEquals("Bladtegnersamlingen",record.getFieldValue("collection"));
		assertEquals("Aktuelt", record.getFieldValue("published_in"));
	}

	@Test
	void testTitle() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_JB000132);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		//System.out.println(prettyJsonString);

		SolrInputDocument document = convertJsonToSolrJavaDoc(prettyJsonString);
		embeddedServer.add(document);
		embeddedServer.commit();
		assertEquals(1, getNumberOfTotalDocuments());

		//Full life cycle test
		SolrDocument record = getRecordById("770379f0-8a0d-11e1-805f-0016357f605f");

		//Single value field
		assertEquals("Romeo og Julie", record.getFieldValue("title"));
	}



	/*
	 * ------- Private helper methods below -------------- 
	 */

	/*
	 * Embedded solr does not have a http listener, so we can not add call and add documents as JSON.
	 * They needs to be converted to SolrInputDocument. This seems to be the simplest way to do it... 
	 * Correct me if I am wrong.
	 * 
	 */
	private  SolrInputDocument convertJsonToSolrJavaDoc(String json) throws Exception{

		//Object is string or String[] for multivalued
		Map<String, Object> map = new ObjectMapper().readValue(json, new TypeReference<Map<String, Object>>(){});

		SolrInputDocument document = new SolrInputDocument();

		for (String key : map.keySet()) {
			//Object can be String or String[]
			Object  value = map.get(key);
			if (value instanceof String) {
			//	System.out.println("Adding:"+key +"="+map.get(key));
				document.addField(key, map.get(key));

			}
			else if (value instanceof ArrayList) {
				for (Object o : (ArrayList<Object>) value) {
			//		System.out.println("Adding:"+key +"="+o.toString());
					document.addField(key, o.toString());
				}            	
			}            
			else {//sanity check, should not happen         
				log.error("Unknown json type"+value.getClass());               
				throw new Exception("Unknown json type"+value.getClass());
			}                                      
		}        
		return document;
	}


	private SolrDocument getRecordById(String id) throws Exception{	    
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("id:\""+id +"\"");
		solrQuery.setRows(10);           
		QueryResponse rsp = embeddedServer.query(solrQuery, METHOD.POST); 
		if (rsp.getResults().getNumFound() !=1) {
			throw new Exception("No record found with id:"+id);
		}
		return rsp.getResults().get(0);           
	}


	private long getNumberOfTotalDocuments() throws Exception{

		// Test number of documents
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("*:*");
		solrQuery.setRows(10);
		solrQuery.add("fl", "id");

		QueryResponse rsp = embeddedServer.query(solrQuery, METHOD.POST); 
		return rsp.getResults().getNumFound();

	}

	private void assertContentAllSingleValues(SolrDocument record, String identifierLocal, String catalogingLanguage, String shelfLocation,
											  String categories, String catalogName, String collection,
											  Long filesize, int imgHeight, int imgWidth) throws Exception {

		assertEquals(identifierLocal,record.getFieldValue("identifier_local"));
		assertEquals(catalogingLanguage,record.getFieldValue("cataloging_language"));
		assertEquals(shelfLocation,record.getFieldValue("shelf_location"));
		assertEquals(categories,record.getFieldValue("categories"));
		assertEquals(catalogName, record.getFieldValue("catalog_name"));
		assertEquals(collection, record.getFieldValue("collection"));
		assertEquals(filesize, record.getFieldValue("file_size"));
		assertEquals(imgHeight, record.getFieldValue("image_height"));
		assertEquals(imgWidth, record.getFieldValue("image_width"));
	}

	private void assertMultivalueField(SolrDocument record, String fieldName, String... contentsInField) {
		Collection<Object> fieldValues = record.getFieldValues(fieldName);
		assertEquals(contentsInField.length,fieldValues.size());
		for (String s : contentsInField) {
			assertTrue(fieldValues.contains(s));
		}
	}



}
