package dk.kb.present.solr;

import java.io.File;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SuggesterResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
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

public class EmbeddedSolrFieldAnalyseTest {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedSolrTest.class);
    private static String solr_home = "target/solr";

    private static CoreContainer coreContainer = null;
    private static EmbeddedSolrServer embeddedServer = null;

    /*
     * Start Solr server and index test documents with method addTestDocuments
     * 
     */
    @BeforeAll
    public static void startEmbeddedSolrServer() throws Exception {

        File solrHomeDir = new File(solr_home);
        String solrHomeAbsoluteDir = solrHomeDir.getAbsolutePath();
        Path solrHome = Paths.get(solrHomeAbsoluteDir);
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
    public void deleteDocs() throws SolrServerException, IOException {
        embeddedServer.deleteByQuery("*:*");
    }
    
    /*
     * Even diacritics must match for text_strict field
     */
    @Test
    void testStrictTextField() throws Exception {
        addSimpleFieldTestDocuments();

        assertEquals(0, getCreatorNameStrictResultsForQuery("Thomas XEgenseX")); // Make sure default operator is AND
        // and not OR.
        assertEquals(1, getCreatorNameStrictResultsForQuery("Thomas Egense"));
        assertEquals(1, getCreatorNameStrictResultsForQuery("Thomas"));
        assertEquals(1, getCreatorNameStrictResultsForQuery("thomas egense")); // lower case
        assertEquals(0, getCreatorNameStrictResultsForQuery("Thomas Gunter Grass")); // No match, different multivalue
        // fields.
        assertEquals(1, getCreatorNameStrictResultsForQuery("Antoine de Saint-Exupéry")); // Excact match
        assertEquals(0, getCreatorNameStrictResultsForQuery("Antoine de Saint Exupéry")); // Even a character '-' must
        // match
        assertEquals(0, getCreatorNameStrictResultsForQuery("Antoine de Saint-Exupery")); // No match without diacritics

    }

    /*
     * Test normalized field match with and without diacriticts
     */
    @Test
    void testDiacriticsStripping() throws Exception {
        addSimpleFieldTestDocuments();
        // Test match with diacritic stripping in 'freetext' field
        assertEquals(1, getFreeTextResultsForQuery("Thomas Grass")); // Combine names, should still match
        // Diacritics
        assertEquals(1, getFreeTextResultsForQuery("Thomas Egense"));
        assertEquals(1, getFreeTextResultsForQuery("thomas egense")); // lower case
        assertEquals(1, getFreeTextResultsForQuery("Antoine de Saint-Exupéry")); // with diacritics
        assertEquals(1, getFreeTextResultsForQuery("Saint-Exupery")); // without diacritics
        assertEquals(1, getFreeTextResultsForQuery("Saint Exupery")); // '-' removed
        assertEquals(1, getFreeTextResultsForQuery("Honoré de Balzac"));
        assertEquals(1, getFreeTextResultsForQuery("Honore de Balzac"));
        assertEquals(1, getFreeTextResultsForQuery("Søren Kierkegaard"));
        assertEquals(1, getFreeTextResultsForQuery("soren kierkegaard")); // Do we want OE match?
        assertEquals(1, getFreeTextResultsForQuery("Juliusz Słowacki"));
        assertEquals(1, getFreeTextResultsForQuery("Juliusz Slowacki"));
        assertEquals(1, getFreeTextResultsForQuery("Maria Dąbrowska"));
        assertEquals(1, getFreeTextResultsForQuery("maria dabrowska"));
        assertEquals(1, getFreeTextResultsForQuery("Gabriel García Márquez"));
        assertEquals(1, getFreeTextResultsForQuery("Gabriel Garcia Marquez"));
        assertEquals(1, getFreeTextResultsForQuery("Günter Grass"));
        assertEquals(1, getFreeTextResultsForQuery("Gunter Grass"));

    }

    @Test
    public void testTitleStrict() throws SolrServerException, IOException {
        addSimpleFieldTestDocuments();
        assertEquals(1, getStrictTitleForQuery("\"Romeo og Julie\""));
        assertEquals(1, getStrictTitleForQuery("\"Romeo and Juliet\""));
        assertEquals(0, getStrictTitleForQuery("and"));
        assertEquals(0, getStrictTitleForQuery("Julie"));

    }

    @Test
    public void testAnalysis() throws SolrServerException, IOException {
        addSimpleFieldTestDocuments();
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("id:1");
        solrQuery.setRows(10);
        QueryResponse rsp = embeddedServer.query(solrQuery, METHOD.POST);
        // System.out.println(rsp.getResults().get(0).toString());
    }

    @Test
    public void testSynonymTVAvisenIndexed() throws SolrServerException, IOException {
        // Synonyms on the title field.
        addSynonymFieldTestDocuments1();//Title : Velkommen til TV-avisen
        assertEquals(1, getTitleQuery("tvavis").getNumFound());
        assertEquals(1, getTitleQuery("\"tvavis\"").getNumFound()); // "tvavis" in quotes
        assertEquals(1, getTitleQuery("tv-avisen").getNumFound()); //no quotes
        assertEquals(1, getTitleQuery("\"tv-avisen\"").getNumFound());  // "tv-avisen" in quotes
        assertEquals(1, getTitleQuery("tvavisen").getNumFound());
        assertEquals(1, getFreeTextQuery("tvavisen")); //Must also be found as freetext search

        // test title stored field is not replaced with synonyms
        ArrayList<String> titles = (ArrayList<String>) getTitleQuery("\"tv-avisen\"").get(0).getFieldValue("title");
        assertEquals("Velkommen til TVavisen", titles.get(0));        
    }

    @Test
    public void testSynonymTV_AvisenIndexed() throws SolrServerException, IOException {
        // Synonyms on the title field.
        addSynonymFieldTestDocuments2(); // Title : Velkommen til TV avisen
        assertEquals(1, getTitleQuery("tvavisen").getNumFound());        
        assertEquals(1, getTitleQuery("tv-avisen").getNumFound());
        assertEquals(1, getTitleQuery("tvavis").getNumFound());
        assertEquals(1, getTitleQuery("tv avisen").getNumFound());
        assertEquals(1, getTitleQuery("\"tv avisen\"").getNumFound());
    }

    
    @Test
    public void testSynonymTest() throws SolrServerException, IOException {
        // Synonyms on the title field.
        addSynonymFieldTestDocuments1();
        assertEquals(1, getTitleQuery("tvavis").getNumFound());
        assertEquals(1, getTitleQuery("\"tvavis\"").getNumFound()); // "tvavis" in quotes
        assertEquals(1, getTitleQuery("tv-avisen").getNumFound()); //no quotes
        assertEquals(1, getTitleQuery("\"tv-avisen\"").getNumFound());  // "tv-avisen" in quotes
        assertEquals(1, getTitleQuery("tvavisen").getNumFound());
        assertEquals(1, getTitleQuery("tv avisen").getNumFound());
        assertEquals(1, getTitleQuery("\"tv avisen\"").getNumFound());
        assertEquals(1, getFreeTextQuery("tvavisen")); //Must also be found as freetext search

        // test title stored field is not replaced with synonyms
        ArrayList<String> titles = (ArrayList<String>) getTitleQuery("\"tv-avisen\"").get(0).getFieldValue("title");
        assertEquals("Velkommen til TVavisen", titles.get(0));

    }

    @Test
    public void testBondeknolden() throws SolrServerException, IOException{
        addSynonymFieldTestBondeknolden();
        assertEquals(1, getFreeTextQuery("bondeknolden"));                 
        assertEquals(1, getFreeTextQuery("kastanjegården"));
        assertEquals(1, getFreeTextQuery("Frank og Kastanjegården"));
    }
    
    public void testMPG() throws SolrServerException, IOException{
        addSynonymFieldMPG();
        assertEquals(1, getFreeTextQuery("mgp"));                         
    }
  
    @Test
    public void testSuggest() throws SolrServerException, IOException{
        addSynonymFieldTestDocuments1();
        addDocWithWrongBroadcaster();

        SuggesterResponse response = getSuggestResult("tv");
        int amountOfSuggestedTerms = response.getSuggestedTerms().get("radiotv_title_suggest").size();
        assertEquals(1, amountOfSuggestedTerms);
    }

    @Test
    public void testNegativeSuggest() throws SolrServerException, IOException{
        addDocForNegativeSuggestTest();
        SuggesterResponse response = getSuggestResult("tv");
        int amountOfSuggestedTerms = response.getSuggestedTerms().get("radiotv_title_suggest").size();
        assertEquals(0, amountOfSuggestedTerms);
    }
    
    
    private long getCreatorNameStrictResultsForQuery(String query) throws Exception {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("creator_strict:(" + query + ")");
        solrQuery.setRows(10);
        QueryResponse rsp = embeddedServer.query(solrQuery, METHOD.POST);
        return rsp.getResults().getNumFound();
    }

    private long getFreeTextResultsForQuery(String query) throws Exception {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("freetext:(" + query + ")");
        solrQuery.setRows(10);
        QueryResponse rsp = embeddedServer.query(solrQuery, METHOD.POST);
        return rsp.getResults().getNumFound();
    }

    private long getStrictTitleForQuery(String query) throws SolrServerException, IOException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("title_strict:" + query);
        solrQuery.setRows(10);
        QueryResponse rsp = embeddedServer.query(solrQuery, METHOD.POST);
        return rsp.getResults().getNumFound();

    }

    private SolrDocumentList getTitleQuery(String query) throws SolrServerException, IOException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("title:" + query);  //text_general which has synonyms
        solrQuery.setRows(10);
        QueryResponse rsp = embeddedServer.query(solrQuery, METHOD.POST);
        return rsp.getResults();
    }

    private long getFreeTextQuery(String query) throws SolrServerException, IOException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);// Use edismax field defition which will also search in title_synonym
        solrQuery.setRows(10);
        QueryResponse rsp = embeddedServer.query(solrQuery, METHOD.POST);
        return rsp.getResults().getNumFound();

    }

    private SuggesterResponse getSuggestResult(String query) throws SolrServerException, IOException{
        SolrParams params = new ModifiableSolrParams().set("qt", "/suggest");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.add(params);
        solrQuery.add("suggest.q", query);
        solrQuery.add("suggest.build", "true");
        solrQuery.setRows(10);
        return embeddedServer.query(solrQuery, METHOD.POST).getSuggesterResponse();

    }

    private static void addSimpleFieldTestDocuments() {

        try {
            SolrInputDocument document = new SolrInputDocument();
            document.addField("id", 1);
            document.addField("origin", "ds.test");
            document.addField("creator", "Thomas Egense");
            document.addField("creator", "Antoine de Saint-Exupéry");
            document.addField("creator", "Honoré de Balzac");
            document.addField("creator", "Søren Kierkegaard");
            document.addField("creator", "Juliusz Słowacki");
            document.addField("creator", "Maria Dąbrowska");
            document.addField("creator", "Gabriel García Márquez");
            document.addField("creator", "Günter Grass");
            document.addField("access_billede_aftale", false); // required field
            document.addField("access_foto_aftale", false);// required field
            document.addField("title", "Romeo og Julie");
            document.addField("title", "Romeo and Juliet");

            embeddedServer.add(document);
            embeddedServer.commit();

        } catch (Exception e) {
            e.printStackTrace();
            fail("Error indexing test documents");
        }

    }


    /*
     * Title: Velkommen til TVavisen
     */
    private static void addSynonymFieldTestDocuments1() {

        try {
            SolrInputDocument document = new SolrInputDocument();
            document.addField("id", "synonym1");
            document.addField("origin", "ds.test");
            document.addField("title", "Velkommen til TVavisen"); // Synonym file: tv-avisen, tvavis, tvavisen, tv-avis
            document.addField("broadcaster", "DR");
            // => tv avisen

            embeddedServer.add(document);
            embeddedServer.commit();

        } catch (Exception e) {
            e.printStackTrace();
            fail("Error indexing test documents");
        }
    }
  
    /*
     * Title: Velkommen til TV avisen
     */
    private static void addSynonymFieldTestDocuments2() {

        try {
            SolrInputDocument document = new SolrInputDocument();
            document.addField("id", "synonym1");
            document.addField("origin", "ds.test");
            document.addField("title", "Velkommen til TV avisen"); // Synonym file: tv-avisen, tvavis, tvavisen, tv-avis
            document.addField("broadcaster", "DR");
            // => tv avisen

            embeddedServer.add(document);
            embeddedServer.commit();

        } catch (Exception e) {
            e.printStackTrace();
            fail("Error indexing test documents");
        }
    }
    
    /*
     * title: Frank og Kastanjegården
     */
    private static void addSynonymFieldTestBondeknolden() {

        try {
            SolrInputDocument document = new SolrInputDocument();
            document.addField("id", "synonym1");
            document.addField("origin", "ds.test");
            document.addField("title", "Frank og Kastanjegården"); // Synonym file: frank, kastanjegården, kastanjegård => bondeknolden
            document.addField("broadcaster", "DR");

            embeddedServer.add(document);
            embeddedServer.commit();

        } catch (Exception e) {
            e.printStackTrace();
            fail("Error indexing test documents");
        }

    }
    
    
    /*
     * title: ansk Melodi Grand Prix 2024
     */
    private static void addSynonymFieldMPG() {

        try {
            SolrInputDocument document = new SolrInputDocument();
            document.addField("id", "synonym1");
            document.addField("origin", "ds.test");
            document.addField("title", "Dansk Melodi Grand Prix 2024"); // Synonym file: melodi grand prix -> mgp
            document.addField("broadcaster", "DR");

            embeddedServer.add(document);
            embeddedServer.commit();

        } catch (Exception e) {
            e.printStackTrace();
            fail("Error indexing test documents");
        }

    }
    
    private static void addDocForNegativeSuggestTest() {
        try {

            SolrInputDocument document = new SolrInputDocument();
            document.addField("id", "negative1");
            document.addField("origin", "ds.test");
            document.addField("title", "Velkommen til radioavisen");
            document.addField("broadcaster", "DR");

            embeddedServer.add(document);
            embeddedServer.commit();

        } catch (Exception e) {
            e.printStackTrace();
            fail("Error indexing test documents");
        }
    }
    private static void addDocWithWrongBroadcaster() {

        try {

            SolrInputDocument document = new SolrInputDocument();
            document.addField("id", "negative1");
            document.addField("origin", "ds.test");
            document.addField("title", "Velkommen til tvavisen hos TV2");
            document.addField("broadcaster", "TV2");

            embeddedServer.add(document);
            embeddedServer.commit();

        } catch (Exception e) {
            e.printStackTrace();
            fail("Error indexing test documents");
        }
    }




}
