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

    public ExtractedPreservicaValues(){
        values.put("recordId", new PathPair<>("id", ""));
        values.put("startTime", new PathPair<>(startTimePath, ""));
        values.put("endTime", new PathPair<>(endTimePath, ""));
        values.put("form", new PathPair<>("", ""));
        values.put("content", new PathPair<>("", ""));
        values.put("originCountry", new PathPair<>("", ""));
        values.put("purpose", new PathPair<>("", ""));
    }

    private static final String startTimePath = "/XIP/Metadata/Content/PBCoreDescriptionDocument/pbcoreInstantiation/pbcoreDateAvailable/dateAvailableStart";
    private static final String endTimePath = "/XIP/Metadata/Content/PBCoreDescriptionDocument/pbcoreInstantiation/pbcoreDateAvailable/dateAvailableEnd";

    public String getStartTime() {
        return values.get("startTime").getValue();
    }

    public void setStartTime(String startTime) {
        String cleanedTime = DataCleanup.getCleanZonedDateTimeFromString(startTime).format(DateTimeFormatter.ISO_INSTANT);
        values.get("startTime").setValue(cleanedTime);
    }

    public String getEndTime() {
        return values.get("endTime").getValue();
    }

    public void setEndTime(String endTime) {
        String cleanedTime = DataCleanup.getCleanZonedDateTimeFromString(endTime).format(DateTimeFormatter.ISO_INSTANT);
        values.get("endTime").setValue(cleanedTime);
    }

    public String getFormValue() {
        return values.get("form").getValue();
    }

    public void setFormValue(String formValue) {
        values.get("form").setValue(formValue);
    }

    public String getContent() {
        return values.get("content").getValue();
    }

    public void setContent(String content) {
        values.get("content").setValue(content);
    }

    public String getOriginCountry(){
        return values.get("origin").getValue();
    }

    public void setOriginCountry(String origin){
        values.get("origin").setValue(origin);
    }

    public String getId(){
        return values.get("recordId").getValue();
    }

    public void setId(String id){
        values.get("recordId").setValue(id);
    }

    public String getPurpose(){
        return values.get("purpose").getValue();
    }

    public void setPurpose(String purpose){
        values.get("purpose").setValue(purpose);
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


