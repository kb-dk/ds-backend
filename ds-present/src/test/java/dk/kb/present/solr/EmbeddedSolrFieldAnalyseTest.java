package dk.kb.present.solr;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

import org.apache.solr.core.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

public class EmbeddedSolrFieldAnalyseTest {

	private static final Logger log = LoggerFactory.getLogger(EmbeddedSolrTest.class);
	private static String solr_home = "target/test-classes/solr";

	private static CoreContainer coreContainer = null;
	private static EmbeddedSolrServer embeddedServer = null;

	public static final String MODS2SOLR = "xslt/mods2solr.xsl";
	public static final String RECORD_000332 = "xml/copyright_extraction/000332.tif.xml"; 
	public static final String RECORD_DPK = "xml/copyright_extraction/DPK000107.tif.xml";
	public static final String RECORD_096c9090 = "xml/copyright_extraction/096c9090-717f-11e0-82d7-002185371280.xml";

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
	void testDiacriticsStripping() throws Exception {
		
		
		
		
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


	
	
	private void addTestDocuments() throws Exception {

	try {
				SolrInputDocument document = new SolrInputDocument();
				document.addField("id", 1);
				document.addField("creator_name", "Thomas Egense"); //simple
				document.addField("creator_name", "Antoine de Saint-Exupéry");
				document.addField("creator_name", "Honoré de Balzac"); 
				document.addField("creator_name", "Søren Kierkegaard");
				
				
				embeddedServer.add(document);
				embeddedServer.commit();



		} catch (Exception e) {
			e.printStackTrace();
			fail("Error indexing 10 test documents only with id field");
		}

	}



}
