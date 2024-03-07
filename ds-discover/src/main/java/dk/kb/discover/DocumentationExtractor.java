package dk.kb.discover;

import dk.kb.discover.config.ServiceConfig;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import dk.kb.present.transform.XSLTTransformer;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.yaml.YAML;
import org.apache.commons.io.IOUtils;

/**
 * Delivers documentation from the backing solr. The primary functionality is to deliver the solr schema in a
 * human-readable way, which includes comments and documentation in processing instructions ({@code <?instruction ?>}-tags).
 * This is done through the method {@link #transformSchema(String collection, String format)},
 * which can deliver the content from the solr schema in the following formats:
 * <ul>
 *     <li>XML</li>
 *     <li>HTML</li>
 *     <li>MarkDown</li>
 * </ul>
 */
public class DocumentationExtractor {

    private static final String SCHEMA2MARKDOWN = "schema2markdown.xsl";
    private static final String SCHEMA2HTML = "schema2html.xsl";

    /**
     * Get and transform the schema for the input {@code collection}.
     * This method includes comments and documentation in processing instructions ({@code <?instruction ?>}-tags).
     * For the internal solr schema retrieval see: {@link SolrService#schema(String)}.
     * @param collection the name of the solr collection to extract the schema from.
     * @param format of the returned file. Supports: {@code XML}, {@code HTML} and {@code MARKDOWN}.
     * @return the solr schema in the requested format.
     */
    public static String transformSchema(String collection, String format) throws IOException {
        String rawSchema = getRawSchema(collection);

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
     * Get the raw solr schema for the queried colletion.
     * @param collection to extract schema for.
     * @return the raw solr schema in the original XML format.
     */
    private static String getRawSchema(String collection) throws IOException {
        YAML conf = ServiceConfig.getConfig();
        String server = conf.getString("solr.collections[collection=" + collection + "].server");
        String path = conf.getString("solr.collections[collection=" + collection +"].path");
        String rawSchemaEndpoint = "admin/file/?contentType=text/xml;charset=utf-8&file=schema.xml";

        StringJoiner urlJoiner = new StringJoiner("/");
        urlJoiner.add(server)
                .add(path)
                .add(collection)
                .add(rawSchemaEndpoint);

        URL rawSchemaUrl = new URL(urlJoiner.toString());
        InputStream schema = rawSchemaUrl.openStream();

        return IOUtils.toString(schema, StandardCharsets.UTF_8);
    }

    /**
     * Transform an XML resource by the specified XSLT.
     * @param xsltResource used for transformation.
     * @param xmlResource used for transformation.
     * @return the transformed document
     */
     static String getTransformed(String xsltResource, String xmlResource) throws IOException {
        Map<String, String> metadata = new HashMap<>();
        XSLTTransformer transformer = new XSLTTransformer(xsltResource, metadata);
        return transformer.apply(xmlResource, metadata);
    }
}
