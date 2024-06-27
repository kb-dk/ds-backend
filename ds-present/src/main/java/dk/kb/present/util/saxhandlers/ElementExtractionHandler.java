package dk.kb.present.util.saxhandlers;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ElementExtractionHandler extends DefaultHandler {
    private StringBuilder currentValue = new StringBuilder();
    private String wantedPath;
    private String currentPath = "";
    private boolean captureValue = false;


    /**
     * Create an ElementExtractionHandler, which returns values for the input path. Ignoring namespace prefixes.
     * @param path
     */
    public ElementExtractionHandler(String path){
        wantedPath = path;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        String elementName = stripPrefix(qName);
        // Update the current path
        currentPath += "/" + elementName;

        // Check if the current path matches the target path
        if (wantedPath.equals(currentPath)) {
            captureValue = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        // Check if we are at the end of the target element
        if (wantedPath.equals(currentPath)) {
            captureValue = false;
        }

        // Update the current path
        currentPath = currentPath.substring(0, currentPath.lastIndexOf('/'));
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        // Capture characters if we are within the target element
        if (captureValue) {
            currentValue.append(new String(ch, start, length));
        }
    }

    public String getCurrentValue(){
        return currentValue.toString();
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
