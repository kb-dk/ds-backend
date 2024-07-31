package dk.kb.present.transform;

import dk.kb.present.config.ServiceConfig;
import net.sf.saxon.TransformerFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class XSLTSolrFromSchemaTransformer extends XSLTTransformer{
    private static final Logger log = LoggerFactory.getLogger(XSLTTransformer.class);
    public static final String ID = "xsltsolr";

    /**
     * Placeholder XML used when transforming JSON to JSON, as the {@link javax.xml.transform.TransformerFactory} is
     * build towards XSLT 1.0, and therefore it's not possible to transform JSON without using an empty placeholder XML.
     */
    private static final String placeholderXml = "<placeholder></placeholder>";

    public static final TransformerFactory transformerFactory;

    static {
        System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
        transformerFactory = TransformerFactoryImpl.newInstance();
        /* Attributes on the transformer level can be tweaked quite a bit, however the javax
           TransformerFactory is oriented towards XSLT 1.0, which doesn't allow calling an XSLT
           without giving it XML as input. The only way to transform through this API is with either an empty placeholder
           or encapsulating the JSON structure in XML tags.
           Link to StackOverfow discussion on this: https://stackoverflow.com/a/35383155/12400491 */
        // transformerFactory.setAttribute("http://saxon.sf.net/feature/initialTemplate", "initial-template");

    }

    /**
     * Construct a transformer that uses Saxon to perform an XSLT transformation on its input.
     *
     * @param stylesheet      the stylesheet for the transformation. This can be be a file resolved relatively to the current
     *                        folder, under the user.home or on the classpath.
     * @param fixedInjections variables that should always be injected into the transformator.
     *                        These co-exists with {@code metadata} in {@link #apply(String, Map)}.
     *                        If the same key is in both {@code injections} and {@code metadata}, {@code metadata} wins.
     *                        Ignored if null.
     * @throws IOException if the stylesheet could not be resolved.
     */
    public XSLTSolrFromSchemaTransformer(String stylesheet, Map<String, String> fixedInjections) throws IOException {
        super(stylesheet, fixedInjections);
    }

    @Override
    public String apply(String s, Map<String, String> metadata) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Transformer transformer = templates.newTransformer();
            if (fixedInjections != null) {
                fixedInjections.forEach(transformer::setParameter);
            }
            transformer.setParameter("schemaorgjson", s);
            metadata.forEach(transformer::setParameter);

            if (ServiceConfig.getConfig().getInteger("transformations.threads",0) > 0) {
                semaphore.acquire();
            }
            transformer.transform(new StreamSource(new ByteArrayInputStream(placeholderXml.getBytes(StandardCharsets.UTF_8))),
                                  new StreamResult(out));

            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException | TransformerException e) {
            throw new RuntimeTransformerException(
                    "Exception transforming with stylesheet '" + stylesheet + "' and metadata '" + metadata + "'", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (ServiceConfig.getConfig().getInteger("transformations.threads",0) > 0) {
                semaphore.release();
            }
        }
    }
}
