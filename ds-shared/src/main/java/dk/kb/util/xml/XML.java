package dk.kb.util.xml;

import dk.kb.util.Resolver;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class XML {
    
    /**
     * Serialises the given Document as a (human-readable) String with indents and linebreaks
     * @param dom the dom
     * @return the doc in string form
     * @throws TransformerException if the transformation failed
     * @see #domToString(Node, boolean) for a more compact machine-readable version
     */
    public static String domToString(Node dom) throws TransformerException {
        return domToString(dom, true);
    }

    /**
     * Serialiseses the given Document as a String, with optional indent.
     * @param dom the dom
     * @param indent if true, the output will be indented. If false, output will be a single line.
     * @return the doc in string form
     * @throws TransformerException if the transformation failed
     * @see #domToString(Node) for an indented version of the same string
     */
    public static String domToString(Node dom, boolean indent) throws TransformerException {
        Transformer transformer;
        if (indent) {
            transformer = getTransformer(null);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        } else {
            transformer = getTransformer("trim-whitespace.xslt");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
        }
        
        /* Transformer */
        try (StringWriter sw = new StringWriter()) {
            transformer.transform(new DOMSource(dom), new StreamResult(sw));
            // After transformation, the only newlines are in text elements where they can be replaced
            // with the newline entity
            return indent ? sw.toString() : sw.toString().replace("\n", "&#10;");
        } catch (IOException e) {
            throw new TransformerException(e);
        }
    }
    
    private static Transformer getTransformer(String xsltResource) throws TransformerConfigurationException {
        Transformer transformer;
        if (xsltResource == null) {
            transformer = TransformerFactory.newInstance().newTransformer();
        } else {
            try (InputStream xsltStream = Resolver.resolveStream(xsltResource)) {
                transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(xsltStream));
            } catch (IOException e) {
                throw new TransformerConfigurationException(
                        "Unable to retrieve and compile XSLT resource '" + xsltResource + "'", e);
            }
        }
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        return transformer;
    }
    
    
    /**
     * Marshall the given object as xml
     * @param object the object to convert to xml
     * @param <T> the type of object
     * @return the object serialised as xml (UTF-8)
     * @throws JAXBException if something failed
     */
    public static <T> String marshall(T object) throws JAXBException {
        //TODO does this work?
        JAXBContext jc = JAXBContext.newInstance(object.getClass());
        
        
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            marshaller.marshal(object, out);
            out.flush();
            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
    }
    
    /**
     * Unmarshal the given xml back to a java object
     * @param xml the xml string
     * @param type the class of object to create
     * @param <T> the type of object
     * @return an instance of Type
     * @throws RuntimeException if anything failed
     */
    @SuppressWarnings("unchecked")
    public static <T> T unmarshall(String xml, Class<T> type) {
        try {
            JAXBContext jc = JAXBContext.newInstance(type);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            try (ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
                if (type.isAnnotationPresent(XmlRootElement.class)) {
                    return (T) unmarshaller.unmarshal(in);
                } else {
                    return unmarshaller.unmarshal(new StreamSource(in), type).getValue();
                }
            }
        } catch (JAXBException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    
    
    /**
     * Parses an XML document from a String to a DOM.
     *
     * @param xmlString      a String containing an XML document.
     * @param namespaceAware if {@code true} the parsed DOM will reflect any
     *                       XML namespaces declared in the document
     * @return The document in a DOM
     * @throws ParserConfigurationException if creating an Xml parser failed (should not happen)
     * @throws IOException If reading the xml string failed (should not happen)
     * @throws SAXException if the xml parsing failed
     */
    public static Document fromXML(String xmlString,
                                   boolean namespaceAware)
            throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilderFactory dbFact = DocumentBuilderFactory.newInstance();
        dbFact.setNamespaceAware(namespaceAware);

        InputSource in = new InputSource();
        try (StringReader characterStream = new StringReader(xmlString);) {
            in.setCharacterStream(characterStream);
            return dbFact.newDocumentBuilder().parse(in);
        }
    }
    
    /**
     * Parses a XML document from a stream to a DOM. The Stream will NOT be closed.
     *
     * @param xmlStream      a stream containing an XML document.
     * @param namespaceAware if {@code true} the constructed DOM will reflect
     *                       the namespaces declared in the XML document
     * @return The document in a DOM
     * @throws ParserConfigurationException if creating an Xml parser failed (should not happen)
     * @throws IOException If reading the xml string failed (should not happen)
     * @throws SAXException if the xml parsing failed
     */
    public static Document fromXML(InputStream xmlStream,
                                   boolean namespaceAware)
            throws ParserConfigurationException, IOException, SAXException {
        
        DocumentBuilderFactory dbFact = DocumentBuilderFactory.newInstance();
        dbFact.setNamespaceAware(namespaceAware);
        
        return dbFact.newDocumentBuilder().parse(xmlStream);
        
    }
}
