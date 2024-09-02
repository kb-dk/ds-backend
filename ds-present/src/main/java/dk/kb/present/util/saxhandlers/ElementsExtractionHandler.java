package dk.kb.present.util.saxhandlers;

import dk.kb.present.util.ExtractedPreservicaValues;
import dk.kb.present.util.DataCleanup;
import dk.kb.present.util.PathPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Extract multiple values from an XML stream to a {@link ExtractedPreservicaValues}-object.
 */
public class ElementsExtractionHandler extends DefaultHandler {
    private static final Logger log = LoggerFactory.getLogger(ElementsExtractionHandler.class);

    private ExtractedPreservicaValues extractedPreservicaValues = new ExtractedPreservicaValues();
    private String currentPath = "";
    private boolean captureValue = false;
    private String captureValueKey = "";

    private static final String START_TIME_PATH = "/XIP/Metadata/Content/PBCoreDescriptionDocument/pbcoreInstantiation/pbcoreDateAvailable/dateAvailableStart";
    private static final String END_TIME_PATH = "/XIP/Metadata/Content/PBCoreDescriptionDocument/pbcoreInstantiation/pbcoreDateAvailable/dateAvailableEnd";
    private static final String TVMETER_PATH = "/XIP/Metadata/Content/record/source/tvmeter";
    private static final String NIELSEN_PATH = "/XIP/Metadata/Content/record/source/nielsen";
    private static boolean containsNielsen = false;
    private static boolean containsTvMeter = false;

    private static String formPath = "";
    private static String content = "";
    private static  String originPath = "";
    private static String purpose = "";


    public ElementsExtractionHandler(String recordId){
        extractedPreservicaValues.values.get("recordId").setValue(recordId);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        String elementName = stripPrefix(qName);
        // Update the current path
        currentPath += "/" + elementName;

        // Path for timestamps are alike in all records.
        handleTimestampPaths();

        // Check if the current path is either Tvmeter or Nielsen extracted metadata.
        handleTvmeterAndNielsenPaths();

        if (containsNielsen || containsTvMeter){
            // Check if the current path matches any target path
            for (Map.Entry<String, PathPair<String, String>> entry : extractedPreservicaValues.values.entrySet()) {
                if (currentPath.equals(entry.getValue().getPath())){
                    captureValue = true;
                    captureValueKey = entry.getKey();
                }
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (currentPath.equals(START_TIME_PATH) || currentPath.equals(END_TIME_PATH)){
            captureValue = false;
        }

        if (containsNielsen || containsTvMeter) {
            // Check if we are at the end of the target element
            for (Map.Entry<String, PathPair<String, String>> entry : extractedPreservicaValues.values.entrySet()) {
                if (currentPath.equals(entry.getValue().getPath())) {
                    captureValue = false;
                    break;
                }
            }
        }

        // Update the current path to continue traversal
        currentPath = currentPath.substring(0, currentPath.lastIndexOf('/'));
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        StringBuilder currentValue = new StringBuilder();
        // Capture characters if we are within the target element
        if (captureValue) {
            currentValue.append(new String(ch, start, length));

            if (captureValueKey.equals("startTime") || captureValueKey.equals("endTime")){
                String cleanedTime = DataCleanup.getCleanZonedDateTimeFromString(currentValue.toString()).format(DateTimeFormatter.ISO_INSTANT);
                extractedPreservicaValues.values.get(captureValueKey).setValue(cleanedTime);
            } else {
                extractedPreservicaValues.values.get(captureValueKey).setValue(currentValue.toString());
            }
        }
    }

    /**
     * Update all variables extracted from either Tvmeter or Nielsen with their correct path values and sets the respective boolean to true and the other to false.
     */
    private void handleTvmeterAndNielsenPaths() {
        if (currentPath.equals(TVMETER_PATH)) {
            containsTvMeter = true;
            containsNielsen = false;

            formPath = "/XIP/Metadata/Content/record/source/tvmeter/form";
            content = "/XIP/Metadata/Content/record/source/tvmeter/contentsitem";
            originPath = "/XIP/Metadata/Content/record/source/tvmeter/origin";
            purpose = "/XIP/Metadata/Content/record/source/tvmeter/intent";
            updatePathValues();
        }

        if (currentPath.equals(NIELSEN_PATH)){
            containsNielsen = true;
            containsTvMeter = false;

            formPath = "/XIP/Metadata/Content/record/source/nielsen/form";
            // TODO: Is this the correct path to extract from?
            content = "/XIP/Metadata/Content/record/source/nielsen/typology";
            originPath = "/XIP/Metadata/Content/record/source/nielsen/origin";
            purpose = "/XIP/Metadata/Content/record/source/nielsen/purpose";
            updatePathValues();
        }
    }

    private void handleTimestampPaths() {
        if (currentPath.equals(START_TIME_PATH)){
            captureValue = true;
            captureValueKey = "startTime";
        }

        if (currentPath.equals(END_TIME_PATH)){
            captureValue = true;
            captureValueKey = "endTime";
        }
    }

    public ExtractedPreservicaValues getDataValues() {
        if (!containsNielsen && !containsTvMeter){
            log.info("Record with id: '{}' does not contain Nielsen or TVMeter metadata fragments.",
                    extractedPreservicaValues.getId());
        }
        return extractedPreservicaValues;
    }

    /**
     * Update paths for values form, contentsItem and origin
     */
    private void updatePathValues() {
        extractedPreservicaValues.values.replace("form", new PathPair<>(formPath, ""));
        extractedPreservicaValues.values.replace("content", new PathPair<>(content, ""));
        extractedPreservicaValues.values.replace("origin", new PathPair<>(originPath, ""));
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
