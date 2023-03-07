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
		Collection<Object> typeResources = record.getFieldValues("type_of_resource");
		assertEquals(2,typeResources.size());
		assertTrue(typeResources.contains("Billede, Todimensionalt billedmateriale"));
		assertTrue(typeResources.contains("Grafik"));

		Collection<Object> createrDateOfDeath = record.getFieldValues("creator_date_of_death");
		assertEquals(3,createrDateOfDeath.size());
		assertTrue(createrDateOfDeath.contains("1868-2-14"));
		assertTrue(createrDateOfDeath.contains("1895-6-25"));
		assertTrue(createrDateOfDeath.contains("1865-3-8"));

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

		//Single value fields
		assertEquals("DPK000107.tif",record.getFieldValue("identifier_local"));
		assertEquals("da",record.getFieldValue("cataloging_language"));
		assertEquals("Billedsamlingen. Postkortsamlingen, Vestindien, Sankt Thomas, Charlotte Amalie, Det gamle fort/politistation",record.getFieldValue("shelf_location"));
		assertEquals("Postkortsamlingen, Vestindien, Postkort, Vestindien, CAR- BLO katagori, Postkortsamlingen, 2022-09-01 15:06:39, 2022-09-01 15:11:09",record.getFieldValue("categories"));
		assertEquals("Samlingsbilleder",record.getFieldValue("catalog_name"));
		assertEquals("Billedsamlingen", record.getFieldValue("collection"));
		assertEquals("Vestindien, Sankt Thomas, Charlotte Amalie, Fort Christian", record.getFieldValue("area"));
		assertEquals(9657172, record.getFieldValue("file_size"));
		assertEquals(1429, record.getFieldValue("image_height"));
		assertEquals(2247, record.getFieldValue("image_width"));

		//Multivalue fields
		// type_of_resource
		Collection<Object> typeResources = record.getFieldValues("type_of_resource");
		assertEquals(2,typeResources.size());
		assertTrue(typeResources.contains("Billede, Todimensionalt billedmateriale"));
		assertTrue(typeResources.contains("Postkort"));

		// topic
		Collection<Object> topic = record.getFieldValues("topic");
		List<String> topicContent = Arrays.asList("postkort","forter","Dannebrog", "børn", "arkitekturer",
													"postcards", "forts", "Dannebrog", "children", "architectures") ;
		assertEquals(10,topic.size());
		assertTrue(topic.containsAll(topicContent));
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

		//Single value field
		assertEquals("Billedsamlingen. Danske portrætter, 4°, Egede, Poul (1708-1789)",record.getFieldValue("shelf_location"));
		assertEquals("000225.tif",record.getFieldValue("identifier_local"));
		assertEquals("Danske portrætter, X-langtidsbevaring test - BLO, Diverse, 2022-09-01 15:06:39, 2022-09-01 15:11:09, 2022-09-02 09:01:13",record.getFieldValue("categories"));
		assertEquals("Samlingsbilleder",record.getFieldValue("catalog_name"));
		assertEquals("Billedsamlingen",record.getFieldValue("collection"));
		assertEquals("1755",record.getFieldValue("production_date_start"));
		assertEquals("1831",record.getFieldValue("production_date_end"));
		assertEquals(6691996, record.getFieldValue("file_size"));
		assertEquals(1812, record.getFieldValue("image_height"));
		assertEquals(1227, record.getFieldValue("image_width"));

		//multivalue field
		// creator_name
		Collection<Object> creatorName = record.getFieldValues("creator_name");
		assertEquals(1, creatorName.size());
		assertTrue(creatorName.contains("Clemens, Johann Friderich"));

		// creator_full_name
		Collection<Object> creatorFullName = record.getFieldValues("creator_full_name");
		assertEquals(1, creatorFullName.size());
		assertTrue(creatorFullName.contains("Johann Friderich Clemens"));

		// creator_family_name
		Collection<Object> creatorFamilyName = record.getFieldValues("creator_family_name");
		assertEquals(1, creatorFamilyName.size());
		assertTrue(creatorFamilyName.contains("Clemens"));

		// creator_given_name
		Collection<Object> creatorGivenName = record.getFieldValues("creator_given_name");
		assertEquals(1, creatorGivenName.size());
		assertTrue(creatorGivenName.contains("Johann Friderich"));

		// creator_terms_of_address
		Collection<Object> creatorTermsOfAddress = record.getFieldValues("creator_terms_of_address");
		assertEquals(1, creatorTermsOfAddress.size());
		assertTrue(creatorTermsOfAddress.contains("kobberstikker"));

		// type_of_resource
		Collection<Object> typeResources = record.getFieldValues("type_of_resource");
		assertEquals(2,typeResources.size());
		assertTrue(typeResources.contains("Billede, Todimensionalt billedmateriale"));
		assertTrue(typeResources.contains("Grafik"));

		// topic
		Collection<Object> topic = record.getFieldValues("topic");
		assertEquals(2,topic.size());
		assertTrue(topic.contains("Poul Egede. 1911,7507."));
		assertTrue(topic.contains("Billedet befinder sig i Kort- og Billedafdelingen, Det Kongelige Bibliotek"));

		// subject_name
		Collection<Object> subjectName = record.getFieldValues("subject_name");
		assertEquals(1,subjectName.size());
		assertTrue(subjectName.contains("Egede, Poul Hansen"));

		// subject_full_name
		Collection<Object> subjectFullName = record.getFieldValues("subject_full_name");
		assertEquals(1,subjectFullName.size());
		assertTrue(subjectFullName.contains("Poul Hansen Egede"));

		// subject_family_name
		Collection<Object> subjectFamilyName = record.getFieldValues("subject_family_name");
		assertEquals(1,subjectFamilyName.size());
		assertTrue(subjectFamilyName.contains("Egede"));

		// subject_given_name
		Collection<Object> subjectGivenName = record.getFieldValues("subject_given_name");
		assertEquals(1,subjectGivenName.size());
		assertTrue(subjectGivenName.contains("Poul Hansen"));

		// subject_date_of_birth
		Collection<Object> subjectDateOfBirth = record.getFieldValues("subject_date_of_birth");
		assertEquals(1,subjectDateOfBirth.size());
		assertTrue(subjectDateOfBirth.contains("1708-0-0"));

		// subject_date_of_death
		Collection<Object> subjectDateOfDeath = record.getFieldValues("subject_date_of_death");
		assertEquals(1,subjectDateOfDeath.size());
		assertTrue(subjectDateOfDeath.contains("1789-0-0"));

		// subject_terms_of_address
		Collection<Object> subjectTermsOfAddress = record.getFieldValues("subject_terms_of_address");
		assertEquals(1,subjectTermsOfAddress.size());
		assertTrue(subjectTermsOfAddress.contains("teolog, missionær, grønlandsfarer og biskop"));
		//TODO more fields

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
		Collection<Object> typeResources = record.getFieldValues("type_of_resource");
		assertEquals(2,typeResources.size());
		assertTrue(typeResources.contains("Billede, Todimensionalt billedmateriale"));
		assertTrue(typeResources.contains("Tegning"));

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
		Collection<Object> typeResources = record.getFieldValues("type_of_resource");
		assertEquals(2,typeResources.size());
		assertTrue(typeResources.contains("Billede, Todimensionalt billedmateriale"));
		assertTrue(typeResources.contains("Anskuelsesbillede"));

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
		Collection<Object> typeResources = record.getFieldValues("type_of_resource");
		assertEquals(2,typeResources.size());
		assertTrue(typeResources.contains("Billede, Todimensionalt billedmateriale"));
		assertTrue(typeResources.contains("Fotografi"));

		//TODO more fields

	}

	@Test
	void testRecordKhp() throws Exception {
		String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, RECORD_KHP0001_049);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		//System.out.println(prettyJsonString);

		SolrInputDocument document = convertJsonToSolrJavaDoc(prettyJsonString);
		embeddedServer.add(document);
		embeddedServer.commit();
		assertEquals(1, getNumberOfTotalDocuments());

		//Full life cycle test
		SolrDocument record = getRecordById("8e608940-d6db-11e3-8d2e-0016357f605f");

		//Single value field
		assertEquals("KHP0001-049.tif",record.getFieldValue("identifier_local"));

		//multivalue field
		Collection<Object> typeResources = record.getFieldValues("type_of_resource");
		assertEquals(2,typeResources.size());
		assertTrue(typeResources.contains("Billede, Todimensionalt billedmateriale"));
		assertTrue(typeResources.contains("Dia"));

		Collection<Object> listOfCategories = record.getFieldValues("list_of_categories");
		assertEquals(7, listOfCategories.size());
		assertTrue(listOfCategories.contains("KHP"));
		assertTrue(listOfCategories.contains("1940-1950"));
		assertTrue(listOfCategories.contains("ikke UA"));

		//TODO more fields
	}

	@Test
	void testRecordUldallForTitle() throws Exception {
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

		/*
		//multivalue field
		Collection<Object> typeResources = record.getFieldValues("type_of_resource");
		assertEquals(2,typeResources.size());
		assertTrue(typeResources.contains("Billede, Todimensionalt billedmateriale"));
		assertTrue(typeResources.contains("Dia"));

		 */
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

	// TODO: Add test for list_of_categories
	

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



}
