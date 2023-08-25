package dk.kb.present.transform;

import dk.kb.present.TestUtil;
import org.junit.jupiter.api.Test;

/**
 *
 *  To run these tests, the test metadata has to be fetched from the internal aegis project.
 *  With aegis running this can be done by running 'kb init' in this repository.
 */
public class XSLTPreservicaToSolrTransformerTest extends XSLTTransformerTestBase {

    public static final String PRESERVICA2SOLR = "xslt/preservica2solr.xsl";
    public static final String RECORD_44979f67 = "internal_test_files/tvMetadata/44979f67-b563-462e-9bf1-c970167a5c5f.xml";
    public static final String RECORD_5a5357be = "internal_test_files/tvMetadata/5a5357be-5890-472a-a294-41a99f108936.xml";

    @Override
    String getXSLT() {
        return PRESERVICA2SOLR;
    }

    @Test
    public void testExtraction() {
        assertContains(RECORD_44979f67, "\"id\":\"ds.test:44979f67-b563-462e-9bf1-c970167a5c5f.xml\"");
    }

    @Test
    public void testTitles() {
        assertContains(RECORD_44979f67, "\"title\":\"Backstage II\"");

        assertContains(RECORD_5a5357be, "\"title\":\"Dr. Pimple Popper: Before The Pop\"");
    }

    @Test
    public void testDirectDuration() {
        assertContains(RECORD_44979f67,"\"duration_ms\":\"950000\"");
    }

    @Test
    public void testCalculatedDuration() {
        assertContains(RECORD_5a5357be,"\"duration_ms\":\"1800000\"");
    }

    @Test
    public void testGenrePresent() {
        assertContains(RECORD_44979f67, "\"genre\":[\"");
    }

    @Test
    public void testNoGenre() {
        assertNotContains(RECORD_5a5357be, "\"genre\":[\"");
    }

    @Test
    public void testResourceDescription() {
        assertNotContains(RECORD_5a5357be, "\"resource_description\": \"Moving Image\"");
    }

    @Test
    public void testCollection() {
        assertNotContains(RECORD_5a5357be, "\"collection\": \"Det Kgl. Bibliotek; Radio/TV-Samlingen\"");
    }

    @Test
    public void testCreatorAffiliation() {
        assertNotContains(RECORD_5a5357be, "\"creator_affiliation\": \"DR Ultra\"");
    }

    @Test
    public void testNotes() {
        assertNotContains(RECORD_5a5357be, "\"notes\": " +
                        "\"Backstage er en børne-sitcom som foregår bag kulisserne på sæbeoperaen Skadestuen, hvor");
    }


    @Test
    public void prettyPrintTransformation() throws Exception {
        TestUtil.prettyPrintSolrJsonFromMetadata(PRESERVICA2SOLR, RECORD_5a5357be);
    }


}
