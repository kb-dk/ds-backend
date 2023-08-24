package dk.kb.present.transform;

import dk.kb.present.TestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;


/**
 *
 *  To run these tests, the test metadata has to be fetched from the internal aegis project.
 *  With aegis running this can be done by running 'kb init' in this repository.
 */
public class XSLTPreservicaToSolrTransformerTest {

    public static final String PRESERVICA2SOLR = "xslt/preservica2solr.xsl";
    public static final String RECORD_44979f67 = "internal_test_files/tvMetadata/44979f67-b563-462e-9bf1-c970167a5c5f.xml";
    public static final String RECORD_5a5357be = "internal_test_files/tvMetadata/5a5357be-5890-472a-a294-41a99f108936.xml";

    @Test
    public void testExtraction() throws Exception {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SOLR, RECORD_44979f67);
        assertTrue(solrString.contains("\"id\":\"ds.test:44979f67-b563-462e-9bf1-c970167a5c5f.xml\""));
    }

    @Test
    public void testTitles() throws Exception {
        String firstDoc = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SOLR, RECORD_44979f67);
        assertTrue(firstDoc.contains("\"title\":\"Backstage II\""));

        String secondDoc = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SOLR, RECORD_5a5357be);
        assertTrue(secondDoc.contains("\"title\":\"Dr. Pimple Popper: Before The Pop\""));
    }

    @Test
    public void testDirectDuration() throws Exception {
        String doc = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SOLR, RECORD_44979f67);
        assertTrue(doc.contains("\"duration\":\"950000\""));
    }

    @Test
    public void testCalculatedDuration() throws Exception {
        String doc = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SOLR, RECORD_5a5357be);
        assertTrue(doc.contains("\"duration\":\"1800000\""));
    }

    @Test
    public void testGenrePresent() throws Exception {
        String doc = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SOLR, RECORD_44979f67);
        assertTrue(doc.contains("\"genre\":[\""));
    }

    @Test
    public void testNoGenre() throws Exception {
        String doc = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SOLR, RECORD_5a5357be);
        assertFalse(doc.contains("\"genre\":[\""));
    }

    //TODO: Add tests for different fields, when we've got multiple correct test records

    @Test
    public void prettyPrintTransformation() throws Exception {
        TestUtil.prettyPrintSolrJsonFromMetadata(PRESERVICA2SOLR, RECORD_5a5357be);
    }

}


