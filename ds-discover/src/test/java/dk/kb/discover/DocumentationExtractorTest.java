package dk.kb.discover;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DocumentationExtractorTest {

    public static final String SCHEMA2DOC = "schema2markdown.xsl";
    public static final String SCHEMA = "solr-test-schema.xml";

    @Test
    public void testExtractionOfProcessingInstruction() throws IOException {
        String documentation = DocumentationExtractor.getTransformed(SCHEMA2DOC, SCHEMA);
        assertTrue(documentation.contains("Fields in this schema should be described with two metatags. " +
                                            "?Description should contain a description of the field"));
    }

    @Test
    public void testMultipleExamples() throws IOException {
        printDocumentation();
        String documentation = DocumentationExtractor.getTransformed(SCHEMA2DOC, SCHEMA);
        assertTrue(documentation.contains("Example: KBK Depot\n" +
                                            "Example: Billedsamlingen. John R. Johnsen. Balletfotografier"));
    }




    private void printDocumentation() throws IOException {
        String documentation = DocumentationExtractor.getTransformed(SCHEMA2DOC, SCHEMA);
        System.out.println(documentation);
    }
}
