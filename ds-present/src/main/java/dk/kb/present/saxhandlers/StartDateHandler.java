package dk.kb.present.saxhandlers;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class StartDateHandler extends DefaultHandler {
    private StringBuilder currentValue = new StringBuilder();
    private String currentPath = "";
    private boolean captureValue = false;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        // Update the current path
        currentPath += "/" + qName;

        // Check if the current path matches the target path
        if ("/XIP/Metadata/Content/ns2:PBCoreDescriptionDocument/ns2:pbcoreInstantiation/ns2:pbcoreDateAvailable/ns2:dateAvailableStart".equals(currentPath)) {
            captureValue = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        // Check if we are at the end of the target element
        if ("/XIP/Metadata/Content/ns2:PBCoreDescriptionDocument/ns2:pbcoreInstantiation/ns2:pbcoreDateAvailable/ns2:dateAvailableStart".equals(currentPath)) {
            captureValue = false;
        }

        // Update the current path
        currentPath = currentPath.substring(0, currentPath.lastIndexOf('/'));
        // currentValue.setLength(0); // Clear the current value
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
}
