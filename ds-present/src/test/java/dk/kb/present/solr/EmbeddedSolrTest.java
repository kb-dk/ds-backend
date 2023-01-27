package dk.kb.present.solr;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.HashMap;





import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class EmbeddedSolrTest {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedSolrTest.class);        
    private static String solr_home= "target/test-classes/solr"; 
    //private static NetarchiveSolrClient server = null;
    
    private static CoreContainer coreContainer = null;
    private static EmbeddedSolrServer embeddedServer = null;
    
    @BeforeAll
    public static void setUp() throws Exception {
        File solrHomeDir = new File("/home/teg/workspace/ds-present/target/test-classes/solr");
        Path solrHome = solrHomeDir.toPath();
 
        System.setProperty( "solr.solr.home", solrHomeDir.getAbsolutePath() );
        System.setProperty( "solr.install.dir", solrHomeDir.getAbsolutePath() );
        
        coreContainer  = new CoreContainer(solrHome, null );     
        coreContainer.load();
       embeddedServer = new EmbeddedSolrServer(coreContainer,"dssolr");
      // NetarchiveSolrTestClient.initializeOverLoadUnitTest(embeddedServer);
      // server = NetarchiveSolrClient.getInstance();
       
        // Remove any items from previous executions:
  //     embeddedServer.deleteByQuery("*:*"); //This is not on the NetarchiveSolrClient API!
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterAll
    public static void tearDown() throws Exception {
      coreContainer.shutdown(); 
      embeddedServer.close();
    }
    
  
    @Test
    public void testDateSortBug() throws Exception {
    
       String url = "http://testurl.dk/test";
      
       ArrayList<String> crawlTimes = new ArrayList<String>();
       crawlTimes.add("2018-03-15T12:31:51Z");
       crawlTimes.add("2018-03-15T12:34:37Z");
       crawlTimes.add("2018-03-15T12:35:56Z");
       crawlTimes.add("2018-03-15T12:36:14Z");
       crawlTimes.add("2018-03-15T12:36:43Z"); //  <-- Excact match test #1
       crawlTimes.add("2018-03-15T12:37:32Z");//   <-- nearest for test #3
       crawlTimes.add("2018-03-15T12:37:52Z"); //  <-- nearest for test #2 
       crawlTimes.add("2018-03-15T12:39:15Z");
       crawlTimes.add("2018-03-15T12:40:09Z");
             
       int i =1;
       for (String crawl : crawlTimes){
         SolrInputDocument document = new SolrInputDocument();
         String id = ""+i++; 
                         
         
         document.addField("id", id);
          
         embeddedServer.add(document);
         
       }              
       embeddedServer.commit();    
       
       
    }
    

}
