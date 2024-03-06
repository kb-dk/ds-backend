package dk.kb.discover;

import dk.kb.discover.config.ServiceConfig;
import dk.kb.util.Resolver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import dk.kb.present.transform.XSLTTransformer;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.yaml.YAML;
import org.apache.commons.io.IOUtils;

public class DocumentationExtractor {

    private static final String SCHEMA2MARKDOWN = "schema2markdown.xsl";

    public static String transformSchema(String collection, String format) throws IOException {
        String rawSchema= getRawSchema(collection);

        switch (format){
            case "xml":
                return getRawSchema(collection);
            case "html":
                return "HTML has not been implemented yet. Sorry";
            case "markdown":
                return getTransformed(SCHEMA2MARKDOWN, rawSchema);
            default:
                throw new InvalidArgumentServiceException("The format '" + format + "' is not supported.");
        }


    }

    public static String getRawSchema(String collection) throws IOException {
        YAML conf = ServiceConfig.getConfig();
        String server = conf.getString("solr.collections[0].ds.server");
        String path = conf.getString("solr.collections[0].ds.path");
        String rawSchemaEndpoint = "admin/file/?contentType=text/xml;charset=utf-8&file=schema.xml";


        URL rawSchemaUrl = new URL(server + "/" +  path + "/" + collection + "/" + rawSchemaEndpoint);

        InputStream schema = rawSchemaUrl.openStream();

        return IOUtils.toString(schema, StandardCharsets.UTF_8);
    }


    public static String getTransformed(String xsltResource, String xmlResource) throws IOException {
        return getTransformed(xsltResource, xmlResource, null, null);
    }
    public static String getTransformed(String xsltResource, String xmlResource, Map<String,String> fixedInjections,
                                        Map<String,String> metadata) throws IOException {
        XSLTTransformer transformer = new XSLTTransformer(xsltResource, fixedInjections);
        // Ensure metadata is defined and that it is mutable
        metadata = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
        return transformer.apply(xmlResource, metadata);
    }
}
