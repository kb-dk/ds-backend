package dk.kb.present.util;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public Map<String, PathPair<String,String>> values = new HashMap<>();
    public static final String RECORD_ID_KEY = "recordId";
    public static final String STARTTIME_KEY = "startTime";
    public static final String ENDTIME_KEY = "endTime";
    public static final String FORM_KEY = "form";
    public static final String CONTENT_KEY = "content";
    public static final String ORIGIN_COUNTRY_KEY = "originCountry";
    public static final String PURPOSE_KEY = "purpose";
    public static final String PRODUCTION_ID_KEY = "productionId";

    public ExtractedPreservicaValues(){
        values.put(RECORD_ID_KEY, new PathPair<>("id", ""));
        values.put(STARTTIME_KEY, new PathPair<>(startTimePath, ""));
        values.put(ENDTIME_KEY, new PathPair<>(endTimePath, ""));
        values.put(FORM_KEY, new PathPair<>("", ""));
        values.put(CONTENT_KEY, new PathPair<>("", ""));
        values.put(ORIGIN_COUNTRY_KEY, new PathPair<>("", ""));
        values.put(PURPOSE_KEY, new PathPair<>("", ""));
        values.put(PRODUCTION_ID_KEY, new PathPair<>("",""));
    }

    private static final String startTimePath = "/XIP/Metadata/Content/PBCoreDescriptionDocument/pbcoreInstantiation/pbcoreDateAvailable/dateAvailableStart";
    private static final String endTimePath = "/XIP/Metadata/Content/PBCoreDescriptionDocument/pbcoreInstantiation/pbcoreDateAvailable/dateAvailableEnd";

    public String getStartTime() {
        return values.get(STARTTIME_KEY).getValue();
    }

    public void setStartTime(String startTime) {
        String cleanedTime = DataCleanup.getCleanZonedDateTimeFromString(startTime).format(DateTimeFormatter.ISO_INSTANT);
        values.get(STARTTIME_KEY).setValue(cleanedTime);
    }

    public String getEndTime() {
        return values.get(ENDTIME_KEY).getValue();
    }

    public void setEndTime(String endTime) {
        String cleanedTime = DataCleanup.getCleanZonedDateTimeFromString(endTime).format(DateTimeFormatter.ISO_INSTANT);
        values.get(ENDTIME_KEY).setValue(cleanedTime);
    }

    public String getFormValue() {
        return values.get(FORM_KEY).getValue();
    }

    public void setFormValue(String formValue) {
        values.get(FORM_KEY).setValue(formValue);
    }

    public String getContent() {
        return values.get(CONTENT_KEY).getValue();
    }

    public void setContent(String content) {
        values.get(CONTENT_KEY).setValue(content);
    }

    public String getOriginCountry(){
        return values.get(ORIGIN_COUNTRY_KEY).getValue();
    }

    public void setOriginCountry(String origin){
        values.get(ORIGIN_COUNTRY_KEY).setValue(origin);
    }

    public String getId(){
        return values.get(RECORD_ID_KEY).getValue();
    }

    public void setId(String id){
        values.get(RECORD_ID_KEY).setValue(id);
    }

    public String getPurpose(){
        return values.get(PURPOSE_KEY).getValue();
    }

    public void setPurpose(String purpose){
        values.get(PURPOSE_KEY).setValue(purpose);
    }

    public String getProductionId() {
        return values.get(PRODUCTION_ID_KEY).getValue();
    }

    public void setProductionId(String formValue) {
        values.get(FORM_KEY).setValue(formValue);
    }


    public List<String> getPaths(){
        List<String> paths = new ArrayList<>();
        for (Map.Entry<String, PathPair<String, String>> entry : values.entrySet()) {
            paths.add(entry.getValue().getPath());
        }

        return paths;
    }

    @Override
    public String toString() {
        return "ExtractedPreservicaValues{" +
                "values=" + values +
                '}';
    }
}


