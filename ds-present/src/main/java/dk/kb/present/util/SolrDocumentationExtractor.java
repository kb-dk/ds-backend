package dk.kb.present.util;

import dk.kb.present.transform.XSLTTransformer;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SolrDocumentationExtractor {

    private static final String SCHEMA2MARKDOWN = "xslt/schema2markdown.xsl";
    private static final String SCHEMA2HTML = "xslt/schema2html.xsl";

    /**
     * Converts a raw solr schema to a human-readable version.
     * @param rawSchema the schema to convert.
     * @param format the format which it gets converted to.
     * @return the transformed solr schema in the specified format.
     */
    public static String transformSchema(String rawSchema, String format) throws IOException {
        switch (format){
            case "xml":
                return rawSchema;
            case "html":
                return getTransformed(SCHEMA2HTML, rawSchema);
            case "markdown":
                return getTransformed(SCHEMA2MARKDOWN, rawSchema);
            default:
                throw new InvalidArgumentServiceException("The format '" + format + "' is not supported.");
        }
    }

    /**
     * Transform an XML resource by the specified XSLT.
     * @param xsltResource used for transformation.
     * @param xmlResource used for transformation.
     * @return the transformed document
     */
    private static String getTransformed(String xsltResource, String xmlResource) throws IOException {
        Map<String, String> metadata = new HashMap<>();
        XSLTTransformer transformer = new XSLTTransformer(xsltResource, metadata);
        return transformer.apply(xmlResource, metadata);
    }
}
