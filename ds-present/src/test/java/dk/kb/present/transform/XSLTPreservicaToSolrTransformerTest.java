package dk.kb.present.transform;

import dk.kb.present.TestUtil;
import dk.kb.util.Resolver;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 *  To run these tests, the test metadata has to be fetched from the internal aegis project.
 *  With aegis running this can be done by running 'kb init' in this repository.
 */
public class XSLTPreservicaToSolrTransformerTest extends XSLTTransformerTestBase {

    public static final String PRESERVICA2SOLR = "xslt/preservica2solr.xsl";
    public static final String RECORD_44979f67 = "internal_test_files/tvMetadata/44979f67-b563-462e-9bf1-c970167a5c5f.xml";
    public static final String RECORD_5a5357be = "internal_test_files/tvMetadata/5a5357be-5890-472a-a294-41a99f108936.xml";
    public static final String RECORD_a8afb121 = "internal_test_files/tvMetadata/a8afb121-e8b8-467a-8704-10dc42356ac4.xml";
    private static final Logger log = LoggerFactory.getLogger(XSLTPreservicaToSolrTransformerTest.class);


    @Override
    String getXSLT() {
        return PRESERVICA2SOLR;
    }

    @BeforeAll
    public static void beforeMethod() {
        if (Resolver.getPathFromClasspath("internal_test_files/tvMetadata") == null){
            log.warn("Internal test files are not present. Unittest 'XSLTPreservicaToSolrTransformerTest' is therefore not run.");
        }
        Assumptions.assumeTrue(Resolver.getPathFromClasspath("internal_test_files/tvMetadata") != null);
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
    public void testGenrePresent() throws Exception {
        TestUtil.prettyPrintSolrJsonFromPreservica(RECORD_44979f67);
        assertContains(RECORD_44979f67, "\"genre\":[\"");
    }

    // TODO: Fix XLST to not include 'ritzau' in output below."
    @Test
    void testGenreContent() throws Exception {
        TestUtil.prettyPrintSolrJsonFromPreservica(RECORD_a8afb121);
        assertContains(RECORD_a8afb121,"\"genre\":[\"hovedgenre: Serier\",\"undergenre: Krimiserie\"");
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
    public void testOrigin(){
        assertContains(RECORD_5a5357be, "\"origin\":\"ds.test\"");
    }
    @Test
    public void prettyPrintTransformation() throws Exception {
        TestUtil.prettyPrintSolrJsonFromPreservica(RECORD_5a5357be);
    }


}
