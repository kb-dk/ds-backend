package dk.kb.present.util.saxhandlers;

import dk.kb.present.util.DataCleanup;
import dk.kb.present.util.ExtractedPreservicaValues;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.time.format.DateTimeFormatter;
import java.util.Map;
/**
 * Extract multiple values from an XML stream to a {@link ExtractedPreservicaValues}-object.
 **/
public class ElementsExtractionHandler extends DefaultHandler {
    private static final String METADATA_PATH = "/XIP/Metadata";

    private static final String START_TIME_PATH = METADATA_PATH + "/Content/PBCoreDescriptionDocument/pbcoreInstantiation/pbcoreDateAvailable/dateAvailableStart";
    private static final String END_TIME_PATH = METADATA_PATH + "/Content/PBCoreDescriptionDocument/pbcoreInstantiation/pbcoreDateAvailable/dateAvailableEnd";

    private static final String TVMETER_FORM_PATH = METADATA_PATH + "/Content/record/source/tvmeter/form";
    private static final String TVMETER_CONTENT_PATH = METADATA_PATH + "/Content/record/source/tvmeter/contentsitem";
    private static final String TVMETER_ORIGIN_PATH = METADATA_PATH + "/Content/record/source/tvmeter/origin";
    private static final String TVMETER_ORIGIN_COUNTRY_PATH = METADATA_PATH + "/Content/record/source/tvmeter/productioncountry";
    private static final String TVMETER_PURPOSE_PATH = METADATA_PATH + "/Content/record/source/tvmeter/intent";
    private static final String TVMETER_PRODUCTION_ID_PATH = METADATA_PATH + "/Content/record/source/tvmeter/internalidcode";

    private static final String NIELSEN_FORM_PATH = METADATA_PATH + "/Content/record/source/nielsen/form";
    // TODO: Is this still the correct path to extract from?
    private static final String NIELSEN_CONTENT_PATH = METADATA_PATH + "/Content/record/source/nielsen/typology";
    private static final String NIELSEN_ORIGIN_PATH = METADATA_PATH + "/Content/record/source/nielsen/origin";
    private static final String NIELSEN_ORIGIN_COUNTRY_PATH = METADATA_PATH + "/Content/record/source/nielsen/origincountry";
    private static final String NIELSEN_PURPOSE_PATH = METADATA_PATH + "/Content/record/source/nielsen/purpose";
    private static final String NIELSEN_PRODUCTION_ID_PATH = METADATA_PATH + "/Content/record/source/nielsen/internalidcode";

    private static final String PBCORE_TITLE_PATH = METADATA_PATH + "/Content/PBCoreDescriptionDocument/pbcoreTitle";
    private static final String PBCORE_TITLE_VALUE_PATH = PBCORE_TITLE_PATH + "/title";
    private static final String PBCORE_TITLE_TYPE_PATH = PBCORE_TITLE_PATH + "/titleType";


    private static final Map<String,String> PBCORE_EXTRACT_PATHS = Map.of(
            START_TIME_PATH,ExtractedPreservicaValues.STARTTIME_KEY,
            END_TIME_PATH,ExtractedPreservicaValues.ENDTIME_KEY
    );

    private static final Map<String,String> NIELSEN_EXTRACT_PATHS = Map.of(
            NIELSEN_FORM_PATH,ExtractedPreservicaValues.FORM_KEY,
            NIELSEN_CONTENT_PATH,ExtractedPreservicaValues.CONTENT_KEY,
            NIELSEN_ORIGIN_PATH,ExtractedPreservicaValues.ORIGIN_KEY,
            NIELSEN_ORIGIN_COUNTRY_PATH,ExtractedPreservicaValues.ORIGIN_COUNTRY_KEY,
            NIELSEN_PURPOSE_PATH,ExtractedPreservicaValues.PURPOSE_KEY,
            NIELSEN_PRODUCTION_ID_PATH,ExtractedPreservicaValues.PRODUCTION_ID_KEY
    );

    private static final Map<String,String> TVMETER_EXTRACT_PATHS = Map.of(
            TVMETER_FORM_PATH,ExtractedPreservicaValues.FORM_KEY,
            TVMETER_CONTENT_PATH,ExtractedPreservicaValues.CONTENT_KEY,
            TVMETER_ORIGIN_PATH,ExtractedPreservicaValues.ORIGIN_KEY,
            TVMETER_ORIGIN_COUNTRY_PATH,ExtractedPreservicaValues.ORIGIN_COUNTRY_KEY,
            TVMETER_PURPOSE_PATH,ExtractedPreservicaValues.PURPOSE_KEY,
            TVMETER_PRODUCTION_ID_PATH,ExtractedPreservicaValues.PRODUCTION_ID_KEY
    );

    private boolean hasNielsenData = false;
    private boolean hasTvMetadata = false;

    private boolean inMetadata = false;
    private String metadataType;

    private boolean inPbCoreTitle = false;
    private String pbCoreTitleValue;
    private String pbCoreTitleType;

    private String currentPath = "";
    private StringBuilder capturedCharacters = new StringBuilder();

    private ExtractedPreservicaValues extractedPreservicaValues;

    public ElementsExtractionHandler(String recordId) {
        extractedPreservicaValues = new ExtractedPreservicaValues();
        extractedPreservicaValues.setValue(ExtractedPreservicaValues.RECORD_ID_KEY, recordId);
    }

    /**
     * Get all values extracted from the input XML as a {@link ExtractedPreservicaValues}-object.
     */
    public ExtractedPreservicaValues getDataValues() {
        return extractedPreservicaValues;
    }


    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        String elementName = stripPrefix(qName);
        // Update the current path
        currentPath += "/" + elementName;
        capturedCharacters.setLength(0);

        if (!inMetadata && METADATA_PATH.equals(currentPath)) {
            inMetadata = true;
            metadataType = attributes.getValue("schemaUri");
        }

        if (PBCORE_TITLE_PATH.equals(currentPath)) {
            inPbCoreTitle = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        if (inMetadata) {
            if ("http://www.pbcore.org/PBCore/PBCoreNamespace.html".equals(metadataType)) {
                if (PBCORE_EXTRACT_PATHS.containsKey(currentPath)) {
                    String key = PBCORE_EXTRACT_PATHS.get(currentPath);
                    if (ExtractedPreservicaValues.STARTTIME_KEY.equals(key) ||
                            ExtractedPreservicaValues.ENDTIME_KEY.equals(key)) {
                        String cleanedTime = DataCleanup.getCleanZonedDateTimeFromString(capturedCharacters.toString().trim()).format(DateTimeFormatter.ISO_INSTANT);
                        extractedPreservicaValues.setValue(key, cleanedTime);
                    } else {
                        extractedPreservicaValues.setValue(key, capturedCharacters.toString().trim());
                    }
                }
                if (PBCORE_TITLE_TYPE_PATH.equals(currentPath)) {
                    pbCoreTitleType = capturedCharacters.toString().trim();
                }
                if (PBCORE_TITLE_VALUE_PATH.equals(currentPath)) {
                    pbCoreTitleValue = capturedCharacters.toString().trim();
                }
            }
            if ("http://id.kb.dk/schemas/supplementary_tvmeter_metadata".equals(metadataType) && TVMETER_EXTRACT_PATHS.containsKey(currentPath)) {
                if (!hasNielsenData) {
                    hasTvMetadata = true;
                    String key = TVMETER_EXTRACT_PATHS.get(currentPath);
                    extractedPreservicaValues.setValue(key, capturedCharacters.toString().trim());
                }
            }
            if ("http://id.kb.dk/schemas/supplementary_nielsen_metadata".equals(metadataType) && NIELSEN_EXTRACT_PATHS.containsKey(currentPath)) {
                if (!hasTvMetadata) {
                    hasNielsenData = true;
                    String key = NIELSEN_EXTRACT_PATHS.get(currentPath);
                    extractedPreservicaValues.setValue(key, capturedCharacters.toString().trim());
                }
            }

            if (PBCORE_TITLE_PATH.equals(currentPath)) {
                switch (pbCoreTitleType) {
                    case "titel" :
                        extractedPreservicaValues.setValue(ExtractedPreservicaValues.TITLE_KEY, pbCoreTitleValue);
                    case "originaltitle" :
                        extractedPreservicaValues.setValue(ExtractedPreservicaValues.ORIGINAL_TITLE_KEY, pbCoreTitleValue);
                }
                inPbCoreTitle = false;
            }

            if (METADATA_PATH.equals(currentPath)) {
                inMetadata = false;
                metadataType = null;
            }
        }

        // Update the current path to continue traversal
        currentPath = currentPath.substring(0, currentPath.lastIndexOf('/'));
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (shouldValueBeCaptured()) {
            capturedCharacters.append(new String(ch, start, length));
        }
    }

    private boolean shouldValueBeCaptured() {
        if (inMetadata) {
            switch (metadataType) {
                case "http://www.pbcore.org/PBCore/PBCoreNamespace.html":
                    return PBCORE_EXTRACT_PATHS.containsKey(currentPath) ||
                                    PBCORE_TITLE_TYPE_PATH.equals(currentPath) ||
                                    PBCORE_TITLE_VALUE_PATH.equals(currentPath);
                case "http://id.kb.dk/schemas/supplementary_tvmeter_metadata":
                    return TVMETER_EXTRACT_PATHS.containsKey(currentPath);
                case "http://id.kb.dk/schemas/supplementary_nielsen_metadata":
                    return true;
            }
        }
        return false;
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
