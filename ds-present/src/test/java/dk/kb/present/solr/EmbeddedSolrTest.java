package dk.kb.present.solr;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
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
	public static final String NEW_000332 = "xml/copyright_extraction/000332.tif.xml"; //Updated version
	
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

	@BeforeEach
	public void deleteDocs() throws Exception {
		embeddedServer.deleteByQuery("*:*");
	}

	/*
	 * Basic test. Add 10 documents with ID only
	 * 
	 */
	@Test
	public void testSolrServerIsRunning() throws Exception {

		try {
			for (int i = 0; i < 10; i++) {
				SolrInputDocument document = new SolrInputDocument();
				String id = "id" + i;
				document.addField("id", id);
				embeddedServer.add(document);

			}
			embeddedServer.commit();

			// Test number of documents
			SolrQuery solrQuery = new SolrQuery();
			solrQuery.setQuery("*:*");
			solrQuery.setRows(10);
			solrQuery.add("fl", "id");

			QueryResponse rsp = embeddedServer.query(solrQuery, METHOD.POST);

			assertEquals(10L, rsp.getResults().getNumFound());

		} catch (Exception e) {
			e.printStackTrace();
			fail("Error indexing 10 test documents only with id field");
		}

	}
	
	 
    @Test
    void testNew000332() throws IOException {
        String solrString = TestUtil.getTransformed(MODS2SOLR, NEW_000332);
        // TODO: Add more detailed test
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(solrString);
        String prettyJsonString = gson.toJson(je);
        
        System.out.println(prettyJsonString );
        assertTrue(solrString.contains("{\"id\":\""));
    }
	
	

}
