package dk.kb.discover;

import dk.kb.discover.config.ServiceConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;

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
}
