package dk.kb.present.util;

import dk.kb.license.model.v1.HoldbackCalculationInputDto;
import dk.kb.license.model.v1.PlatformEnumDto;
import dk.kb.license.model.v1.RestrictionsCalculationInputDto;
import dk.kb.license.model.v1.RightsCalculationInputDto;
import dk.kb.present.util.saxhandlers.ElementsExtractionHandler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * An object containing all values from a single Preservica record, that are to be handled in either a special context or in multiple contexts. The main component of this object is
 * the {@link #values}-map. This map contains keys, representing the name of each entry, and values of type {@link PathPair}, containing the path to the extracted value as
 * {@link PathPair#path} and the value extracted from the specified path at {@link PathPair#value}. This value can then be retrieved by getters in {@link ExtractedPreservicaValues}.
 * <br/>
 * Can be used in conjunction with {@link dk.kb.present.util.saxhandlers.ElementsExtractionHandler} to parse an XML stream for multiple values.
 * <br/>
 * Values for form, content and origin can come from either tvmeter or nielsen metadata fragments, but never from both of them at the same time.
 */
public class ExtractedPreservicaValues {

    public Map<String, String> values = new HashMap<>();
    public static final String RECORD_ID_KEY = "recordId";
    public static final String STARTTIME_KEY = "startTime";
    public static final String ENDTIME_KEY = "endTime";
    public static final String FORM_KEY = "form";
    public static final String CONTENT_KEY = "content";
    public static final String ORIGIN_KEY = "origin";
    public static final String ORIGIN_COUNTRY_KEY = "originCountry";
    public static final String PURPOSE_KEY = "purpose";
    public static final String PRODUCTION_ID_KEY = "productionId";
    public static final String TITLE_KEY = "title";
    public static final String ORIGINAL_TITLE_KEY = "originalTitle";
    public static final String HOLDBACK_CATEGORY_KEY = "holdbackCategory";

    public ExtractedPreservicaValues(){
    }

    /**
     * Extract all needed values from a preservica record. These values are either tricky values such as dates, where we know that extra parsing is needed or values that are
     * used in multiple parts of the processing of the record.
     * @param content of the record. i.e. the XML data.
     * @param recordId of the processed record. Used for logging and debugging.
     * @return a {@link ExtractedPreservicaValues}-object containing the extracted values.
     */
    public static ExtractedPreservicaValues extractValuesFromPreservicaContent(String content, String recordId) throws ParserConfigurationException, SAXException, IOException {
        try (InputStream xml = IOUtils.toInputStream(content, StandardCharsets.UTF_8)) {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(false);
            SAXParser saxParser = factory.newSAXParser();

            ElementsExtractionHandler handler = new ElementsExtractionHandler(recordId);
            saxParser.parse(xml, handler);

            return handler.getDataValues();
        }
    }

    public String getStartTime() {
        return values.get(STARTTIME_KEY);
    }
    public String getEndTime() {
        return values.get(ENDTIME_KEY);
    }
    public String getFormValue() {
        return values.get(FORM_KEY);
    }
    public String getContent() {
        return values.get(CONTENT_KEY);
    }
    public String getOriginCountry(){
        return values.get(ORIGIN_COUNTRY_KEY);
    }
    public String getOrigin(){
        return values.get(ORIGIN_KEY);
    }
    public String getId(){
        return values.get(RECORD_ID_KEY);
    }
    public String getPurpose(){
        return values.get(PURPOSE_KEY);
    }
    public String getProductionId() {
        return values.get(PRODUCTION_ID_KEY);
    }
    public String getTitle(){
        return values.get(TITLE_KEY);
    }
    public String getOriginalTitle() {
        return values.get(ORIGINAL_TITLE_KEY);
    }
    public String getHoldbackCategory() {
        return values.get(HOLDBACK_CATEGORY_KEY);
    }

    public void setValue(String key, String value) {
        values.put(key, value);
    }

    /**
     * Converts the current object's data into a {@link RightsCalculationInputDto} instance.
     * <p>
     * This method initializes a new {@link RightsCalculationInputDto} and populates it with
     * data from the current object, including holdback and restrictions information.
     * The method sets various properties of the holdback and restrictions DTOs based on
     * the current object's state and the provided platform.
     *<p/>
     * @param platform the platform for which the rights calculation input DTO is being created.
     * @param dsOrigin the origin of the record in the DS system.
     * @return a {@link RightsCalculationInputDto} populated with the current object's data.
     */
    public RightsCalculationInputDto asRightsCalculationInputDto(PlatformEnumDto platform, String dsOrigin) {
        RightsCalculationInputDto rightsInputDto = new RightsCalculationInputDto();

        HoldbackCalculationInputDto holdbackInputDto = new HoldbackCalculationInputDto();
        RestrictionsCalculationInputDto restrictionsDto = new RestrictionsCalculationInputDto();

        // If holdbackCategory is not null in dr_archive_supplementary_rights_metadata we only need to populate holdbackCategory
        if (!StringUtils.isBlank(getHoldbackCategory())) {
            holdbackInputDto.setHoldbackCategory(getHoldbackCategory());
        } else {
            holdbackInputDto.setHensigt(convertStringToInteger(getPurpose()));
            holdbackInputDto.setForm(convertStringToInteger(getFormValue()));
            holdbackInputDto.setIndhold(convertStringToInteger(getContent()));
            holdbackInputDto.setProductionCountry(convertStringToInteger(getOriginCountry()));
        }

        holdbackInputDto.setOrigin(dsOrigin);

        restrictionsDto.setRecordId(getId());
        restrictionsDto.setDrProductionId(getProductionId());
        restrictionsDto.setProductionCode(getOrigin());
        restrictionsDto.setTitle(getTitle());

        rightsInputDto.setStartTime(getStartTime());
        rightsInputDto.setRecordId(getId());
        rightsInputDto.setPlatform(platform);
        rightsInputDto.setHoldbackInput(holdbackInputDto);
        rightsInputDto.setRestrictionsInput(restrictionsDto);
        return rightsInputDto;
    }

    private Integer convertStringToInteger(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        return Integer.parseInt(value);
    }

    @Override
    public String toString() {
        return "ExtractedPreservicaValues{" +
                "values=" + values +
                '}';
    }
}


