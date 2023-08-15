package dk.kb.present.transform;

import dk.kb.present.TestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class XSLTPreservicaToSolrTransformerTest {
    public static final String PRESERVICA2SOLR = "xslt/preservica2solr.xsl";
    public static final String PRESERVICA_TEST_RECORD = "PreservicaTest.xml";

    @Test
    public void testSetup() throws Exception {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SOLR, PRESERVICA_TEST_RECORD);
        System.out.println(solrString);

    }
}
