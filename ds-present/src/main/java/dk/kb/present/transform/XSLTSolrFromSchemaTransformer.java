package dk.kb.present.transform;

import net.sf.saxon.dom.DocumentBuilderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class XSLTSolrFromSchemaTransformer extends XSLTTransformer{
    private static final Logger log = LoggerFactory.getLogger(XSLTTransformer.class);
    public static final String ID = "xsltsolr";

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
    public synchronized String apply(String s, Map<String, String> metadata) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            transformer.clearParameters();
            if (fixedInjections != null) {
                fixedInjections.forEach(transformer::setParameter);
            }
            transformer.setParameter("schemaorgjson", s);
            metadata.forEach(transformer::setParameter);
            transformer.transform(getPlaceholderSource(), new StreamResult(out));

            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException | TransformerException e) {
            throw new RuntimeTransformerException(
                    "Exception transforming with stylesheet '" + stylesheet + "' and metadata '" + metadata + "'", e);
        }
    }

    /**
     * Create a small DOM document with only one element named placeholder.
     * This document is used as a placeholder for the transformation from schema.org JSON to JSON solr documents.
     * @return a DOM representation of the placeholder XML.
     */
    private Source getPlaceholderSource(){
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder =  dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            Element rootElement = doc.createElement("placeholder");
            doc.appendChild(rootElement);
            return new DOMSource(doc);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

    }
}
