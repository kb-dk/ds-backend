package dk.kb.present.solr;

import java.io.File;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;

import org.apache.solr.common.SolrInputDocument;

import org.apache.solr.core.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class EmbeddedSolrFieldAnalyseTest {

	private static final Logger log = LoggerFactory.getLogger(EmbeddedSolrTest.class);
	private static String solr_home = "target/solr/";

	private static CoreContainer coreContainer = null;
	private static EmbeddedSolrServer embeddedServer = null;

	/*
	 * Start Solr server and index test documents with method addTestDocuments
	 * 
	 */	
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
		
		addTestDocuments();
		
	}

	@AfterAll
	public static void tearDown() throws Exception {
		coreContainer.shutdown();
		embeddedServer.close();
	}
	
	
	/*
	 * Even diacritics must match for text_strict field 
	 */
	@Test
    void testStrictTextField() throws Exception {
        assertEquals(0,getCreatorNameStrictResultsForQuery("Thomas XEgenseX")); //Make sure default operator is AND and not OR.
        assertEquals(1,getCreatorNameStrictResultsForQuery("Thomas Egense"));
        assertEquals(1,getCreatorNameStrictResultsForQuery("Thomas"));
        assertEquals(1,getCreatorNameStrictResultsForQuery("thomas egense")); //lower case        
        assertEquals(0,getCreatorNameStrictResultsForQuery("Thomas Gunter Grass")); //No match, different multivalue fields.
        assertEquals(1,getCreatorNameStrictResultsForQuery("Antoine de Saint-Exupéry")); //Excact match
        assertEquals(0,getCreatorNameStrictResultsForQuery("Antoine de Saint Exupéry")); //Even a character '-' must match
        assertEquals(0,getCreatorNameStrictResultsForQuery("Antoine de Saint-Exupery")); //No match without diacritics
    
    }
        
        
    
	/*
	 * Test normalized field match with and without diacriticts
	 */
	@Test
	void testDiacriticsStripping() throws Exception {
		
		// Test match with diacritic stripping in 'freetext' field
		assertEquals(1,getFreeTextResultsForQuery("Thomas Grass")); // Combine names, should still match
		//Diacritics
		assertEquals(1,getFreeTextResultsForQuery("Thomas Egense"));
		assertEquals(1,getFreeTextResultsForQuery("thomas egense")); //lower case
		assertEquals(1,getFreeTextResultsForQuery("Antoine de Saint-Exupéry")); //with diacritics
		assertEquals(1,getFreeTextResultsForQuery("Saint-Exupery")); //without diacritics
		assertEquals(1,getFreeTextResultsForQuery("Saint Exupery")); // '-' removed
		assertEquals(1,getFreeTextResultsForQuery("Honoré de Balzac")); 
		assertEquals(1,getFreeTextResultsForQuery("Honore de Balzac"));		
		assertEquals(1,getFreeTextResultsForQuery("Søren Kierkegaard"));
		assertEquals(1,getFreeTextResultsForQuery("soren kierkegaard")); //Do we want OE match?		
		assertEquals(1,getFreeTextResultsForQuery("Juliusz Słowacki"));
		assertEquals(1,getFreeTextResultsForQuery("Juliusz Slowacki"));		
		assertEquals(1,getFreeTextResultsForQuery("Maria Dąbrowska"));
		assertEquals(1,getFreeTextResultsForQuery("maria dabrowska"));		
		assertEquals(1,getFreeTextResultsForQuery("Gabriel García Márquez"));
		assertEquals(1,getFreeTextResultsForQuery("Gabriel Garcia Marquez"));
		assertEquals(1,getFreeTextResultsForQuery("Günter Grass"));
		assertEquals(1,getFreeTextResultsForQuery("Gunter Grass"));
		
		
		
	}

	@Test
	public void testTitleStrict() throws SolrServerException, IOException {
		assertEquals(1, getStrictTitleForQuery("\"Romeo og Julie\""));
		assertEquals(1, getStrictTitleForQuery("\"Romeo and Juliet\""));
		assertEquals(0, getStrictTitleForQuery("and"));
		assertEquals(0, getStrictTitleForQuery("Julie"));

	}

	@Test
	public void testAnalysis() throws SolrServerException, IOException {
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("id:1");
		solrQuery.setRows(10);
		QueryResponse rsp = embeddedServer.query(solrQuery, METHOD.POST);
		//System.out.println(rsp.getResults().get(0).toString());
	}
	

	private long getCreatorNameStrictResultsForQuery(String query) throws Exception{	    
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("creator_full_name_strict:("+query +")");
		solrQuery.setRows(10);           
		QueryResponse rsp = embeddedServer.query(solrQuery, METHOD.POST); 		
		return rsp.getResults().getNumFound();
	}

	

	private long getFreeTextResultsForQuery(String query) throws Exception{	    
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("freetext:("+query +")");
		solrQuery.setRows(10);           
		QueryResponse rsp = embeddedServer.query(solrQuery, METHOD.POST); 		
		return rsp.getResults().getNumFound();
	}

	private long getStrictTitleForQuery(String query) throws SolrServerException, IOException {
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("title_strict:"+query);
		solrQuery.setRows(10);
		QueryResponse rsp = embeddedServer.query(solrQuery, METHOD.POST);
		return rsp.getResults().getNumFound();

	}


	
	
	private static void addTestDocuments() throws Exception {

	try {
				SolrInputDocument document = new SolrInputDocument();
				document.addField("id", 1);
				document.addField("origin", "ds.test");
				document.addField("creator_full_name", "Thomas Egense");
				document.addField("creator_full_name", "Antoine de Saint-Exupéry");
				document.addField("creator_full_name", "Honoré de Balzac"); 
				document.addField("creator_full_name", "Søren Kierkegaard");
				document.addField("creator_full_name", "Juliusz Słowacki");
				document.addField("creator_full_name", "Maria Dąbrowska");
				document.addField("creator_full_name", "Gabriel García Márquez");
				document.addField("creator_full_name", "Günter Grass");
                document.addField("access_billede_aftale", false); //required field
                document.addField("access_foto_aftale", false);//required field
				document.addField("title", "Romeo og Julie");
				document.addField("title", "Romeo and Juliet");

				embeddedServer.add(document);
				embeddedServer.commit();



		} catch (Exception e) {
			e.printStackTrace();
			fail("Error indexing test documents");
		}

	}



}
