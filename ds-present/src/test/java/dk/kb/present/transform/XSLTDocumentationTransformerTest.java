package dk.kb.present.transform;

import dk.kb.present.TestUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class XSLTDocumentationTransformerTest {

    public static final String SCHEMA2DOC = "xslt/schema2doc.xsl";
    public static final String SCHEMA = "src/main/solr/dssolr/conf/schema.xml";

    @Test
    public void testExtractionOfProcessingInstruction() throws IOException {
        String documentation = TestUtil.getTransformed(SCHEMA2DOC, SCHEMA);
        assertTrue(documentation.contains("Fields in this schema should be described with two metatags. " +
                                            "?Description should contain a description of the field"));
    }

    @Test
    public void testMultipleExamples() throws IOException {
        String documentation = TestUtil.getTransformed(SCHEMA2DOC, SCHEMA);
        assertTrue(documentation.contains("Example: KBK Depot\n" +
                                            "Example: Billedsamlingen. John R. Johnsen. Balletfotografier"));
    }

    private void printDocumentation() throws IOException {
        String documentation = TestUtil.getTransformed(SCHEMA2DOC, SCHEMA);
        System.out.println(documentation);
    }
}
