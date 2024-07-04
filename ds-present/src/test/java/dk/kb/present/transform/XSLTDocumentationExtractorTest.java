package dk.kb.present.transform;

import dk.kb.present.PresentFacade;
import dk.kb.present.TestUtil;
import dk.kb.present.config.ServiceConfig;
import dk.kb.util.Resolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class XSLTDocumentationExtractorTest {

    public static final String SCHEMA2DOC = "xslt/schema2markdown.xsl";
    public static final String SCHEMA = "target/solr/dssolr/conf/schema.xml";

    @BeforeAll
    public static void fixConfiguration() throws IOException {
        String CONFIG = Resolver.resolveGlob("conf/ds-present-behaviour.yaml").get(0).toString();
        if ("[]".equals(CONFIG)) {
            throw new IllegalStateException("Unable to locate config");
        }

        ServiceConfig.initialize(CONFIG);
    }

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

    @Test
    public void testXmlSchema() throws IOException {
        String xmlSchema = PresentFacade.transformSolrSchema(resolveTestSchema(), "xml");
        assertTrue(xmlSchema.contains("<?summary "));
    }
    @Test
    public void testMarkdownSchemaTransformation() throws IOException {
        String markdownSchema = PresentFacade.transformSolrSchema(resolveTestSchema(), "markdown");
        assertTrue(markdownSchema.contains("# Summary"));
    }

    @Test
    public void testHtmlTransformation() throws IOException {
        String htmlSchema = PresentFacade.transformSolrSchema(resolveTestSchema(), "html");
        assertTrue(htmlSchema.contains("<h2>Summary</h2>"));
    }

    private String resolveTestSchema() throws IOException {
        return Resolver.resolveString(SCHEMA, StandardCharsets.UTF_8);
    }
}
