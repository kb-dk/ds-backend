package dk.kb.present.util.saxhandlers;

import dk.kb.present.RecordValues;
import dk.kb.present.util.PathPair;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.List;
import java.util.Map;

/**
 * Extract multiple values from an XML stream to a {@link RecordValues}-object.
 */
public class ElementsExtractionHandler extends DefaultHandler {

    private final RecordValues recordValues = new RecordValues();
    private String currentPath = "";
    private boolean captureValue = false;
    private String captureValueKey = "";

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        String elementName = stripPrefix(qName);
        // Update the current path
        currentPath += "/" + elementName;

        // Check if the current path matches any target path
        for (Map.Entry<String, PathPair<String, String>> entry : recordValues.values.entrySet()) {
            if (currentPath.equals(entry.getValue().getPath())){
                captureValue = true;
                captureValueKey = entry.getKey();
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        // Check if we are at the end of the target element
        for (Map.Entry<String, PathPair<String, String>> entry : recordValues.values.entrySet()) {
            if (currentPath.equals(entry.getValue().getPath())){
                captureValue = false;
                break;
            }
        }

        // Update the current path
        currentPath = currentPath.substring(0, currentPath.lastIndexOf('/'));
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        StringBuilder currentValue = new StringBuilder();
        // Capture characters if we are within the target element
        if (captureValue) {
            currentValue.append(new String(ch, start, length));
            recordValues.values.get(captureValueKey).setValue(currentValue.toString());
        }
    }

    public RecordValues getDataValues() {
        return recordValues;
    }

    /**
     * Ignore namespace prefixes while traversing.
     */
    private String stripPrefix(String qName) {
        int colonIndex = qName.indexOf(':');
        if (colonIndex != -1) {
            return qName.substring(colonIndex + 1);
        }
        return qName;
    }
}
