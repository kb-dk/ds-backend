package dk.kb.present.transform;

import dk.kb.present.TestUtil;
import dk.kb.present.util.TestFileProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

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
    public void testExtraction() {
        testPreservica(RECORD_44979f67,
                solrDoc -> assertTrue(solrDoc.contains("\"id\":\"ds.test:44979f67-b563-462e-9bf1-c970167a5c5f.xml\""))
        );
    }

    @Test
    public void testTitles() {
        testPreservica(RECORD_44979f67,
                solrDoc -> assertTrue(solrDoc.contains("\"title\":\"Backstage II\""))
        );

        testPreservica(RECORD_5a5357be,
                solrDoc -> assertTrue(solrDoc.contains("\"title\":\"Dr. Pimple Popper: Before The Pop\""))
        );
    }

    @Test
    public void testDirectDuration() {
        testPreservica(RECORD_44979f67,
                solrDoc -> assertTrue(solrDoc.contains("\"duration\":\"950000\""))
        );
    }

    @Test
    public void testCalculatedDuration() {
        testPreservica(RECORD_5a5357be,
                solrDoc -> assertTrue(solrDoc.contains("\"duration\":\"1800000\""))
        );
    }

    @Test
    public void testGenrePresent() {
        testPreservica(RECORD_44979f67,
                solrDoc ->  assertTrue(solrDoc.contains("\"genre\":[\""))
        );
    }

    @Test
    public void testNoGenre() {
        testPreservica(RECORD_5a5357be,
                solrDoc -> assertFalse(solrDoc.contains("\"genre\":[\""))
        );
    }

    @Test
    public void testResourceDescription() {
        testPreservica(RECORD_5a5357be,
                solrDoc -> assertFalse(solrDoc.contains("\"resource_description\": \"Moving Image\""))
        );
    }

    @Test
    public void testCollection() {
        testPreservica(RECORD_5a5357be,
                solrDoc -> assertFalse(solrDoc.contains("\"collection\": \"Det Kgl. Bibliotek; Radio/TV-Samlingen\""))
        );
    }

    @Test
    public void testCreatorAffiliation() {
        testPreservica(RECORD_5a5357be,
                solrDoc -> assertFalse(solrDoc.contains("\"creator_affiliation\": \"DR Ultra\""))
        );
    }

    @Test
    public void testNotes() {
        testPreservica(RECORD_5a5357be,
                solrDoc -> assertFalse(solrDoc.contains("\"notes\": " +
                        "\"Backstage er en børne-sitcom som foregår bag kulisserne på sæbeoperaen Skadestuen, hvor"))
        );
    }


    @Test
    public void prettyPrintTransformation() throws Exception {
        TestUtil.prettyPrintSolrJsonFromMetadata(PRESERVICA2SOLR, RECORD_5a5357be);
    }

    /**
     * Checks that internal test files are available and if not, logs a warning and returns.
     * <p>
     * If the check passes, the content of the file {@code record} is transformed using XSLT {@link #PRESERVICA2SOLR}
     * and the given tests are performed on the result.
     * @param record file with a record that is to be transformed using {@link #PRESERVICA2SOLR}.
     * @param tests Zero or more tests to perform on the transformed record.
     */
    @SafeVarargs
    private void testPreservica(String record, Consumer<String>... tests) {
        if (!TestFileProvider.hasSomeTestFiles()) {
            return;  // ensureTestFiles takes care of logging is there are no internal test files
        }
        String solrString;
        try {
            solrString = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SOLR, record);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to fetch and transform '" + record + "' using XSLT '" + PRESERVICA2SOLR + "'", e);
        }

        Arrays.stream(tests).forEach(test -> test.accept(solrString));
    }

}
