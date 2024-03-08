package dk.kb.discover;

import dk.kb.discover.config.ServiceConfig;
import dk.kb.util.Resolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("integration")
public class DocumentationExtractorTest {

    @BeforeAll
    public static void setup() {
        try {
            ServiceConfig.initialize("ds-discover-integration-test.yaml");
        } catch (IOException e) {
            fail("Integration test setup not present. Try running the command kb init");
        }
    }

    private static final String SCHEMA2DOC = "schema2markdown.xsl";
    private static final String SOLR_TEST_SCHEMA_XML = "solr-test-schema.xml";

    @Test
    public void testExtractionOfProcessingInstruction() throws IOException {

        String documentation = DocumentationExtractor.getTransformed(SCHEMA2DOC, resolveTestSchema());
        assertTrue(documentation.contains("Fields in this schema should be described with two metatags. " +
                                            "?Description should contain a description of the field"));
    }

    @Test
    public void testMultipleExamples() throws IOException {
        String documentation = DocumentationExtractor.getTransformed(SCHEMA2DOC, resolveTestSchema());
        assertTrue(documentation.contains("Example: KBK Depot\n" +
                                            "Example: Billedsamlingen. John R. Johnsen. Balletfotografier"));
    }

    @Test
    public void testXmlSchema() throws IOException {
        String xmlSchema = DocumentationExtractor.transformSchema("ds", "xml");
        assertTrue(xmlSchema.contains("<?summary "));
    }
    @Test
    public void testMarkdownSchemaTransformation() throws IOException {
        String markdownSchema = DocumentationExtractor.transformSchema("ds", "markdown");
        assertTrue(markdownSchema.contains("# Summary"));
    }

    @Test
    public void testHtmlTransformation() throws IOException {
        String htmlSchema = DocumentationExtractor.transformSchema("ds", "html");
        assertTrue(htmlSchema.contains("<h2>Summary</h2>"));
    }

    private String resolveTestSchema() throws IOException {
        return Resolver.resolveString(SOLR_TEST_SCHEMA_XML, StandardCharsets.UTF_8);
    }
}
