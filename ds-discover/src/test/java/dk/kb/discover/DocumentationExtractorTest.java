package dk.kb.discover;

import dk.kb.discover.config.ServiceConfig;
import dk.kb.util.Resolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DocumentationExtractorTest {

    // TODO: Use best practise aegis setup
    @BeforeAll
    public static void setup() throws IOException {
        ServiceConfig.initialize("conf/ds-discover-local.yaml");
    }

    private static final String SCHEMA2DOC = "schema2markdown.xsl";
    private static final String SCHEMA2HTML = "schema2html.xsl";
    private static final String SCHEMA = "solr-test-schema.xml";

    @Test
    public void testExtractionOfProcessingInstruction() throws IOException {

        String documentation = DocumentationExtractor.getTransformed(SCHEMA2DOC, resolveTestSchema());
        assertTrue(documentation.contains("Fields in this schema should be described with two metatags. " +
                                            "?Description should contain a description of the field"));
    }

    @Test
    public void testMultipleExamples() throws IOException {
        printDocumentation();
        String documentation = DocumentationExtractor.getTransformed(SCHEMA2DOC, resolveTestSchema());
        assertTrue(documentation.contains("Example: KBK Depot\n" +
                                            "Example: Billedsamlingen. John R. Johnsen. Balletfotografier"));
    }


    @Test
    public void testSchemaTransformation() throws IOException {
        // TODO: Create actual test, making use of test setup
        System.out.println(DocumentationExtractor.transformSchema("ds", "xml"));
    }


    // TOOD: Create individual tests for markdown, xml and html output
    @Test
    public void testHtmlTransformation() throws IOException {
        System.out.println(DocumentationExtractor.transformSchema("ds", "html"));

    }

    private void printDocumentation() throws IOException {
        String documentation = DocumentationExtractor.getTransformed(SCHEMA2DOC, resolveTestSchema());
        System.out.println(documentation);
    }

    private String resolveTestSchema() throws IOException {
        return Resolver.resolveString(SCHEMA, StandardCharsets.UTF_8);
    }
}
