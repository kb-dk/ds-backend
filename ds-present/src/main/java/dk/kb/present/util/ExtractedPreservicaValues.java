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
 */
public class ExtractedPreservicaValues {

    public Map<String, PathPair<String,String>> values = new HashMap<>();

    public ExtractedPreservicaValues(){
        values.put("startTime", new PathPair<>(startTimePath, ""));
        values.put("endTime", new PathPair<>(endTimePath, ""));
        values.put("form", new PathPair<>(formPath, ""));
        values.put("contentsItem", new PathPair<>(contentsItemPath, ""));
        values.put("origin", new PathPair<>(originPath, ""));
    }

    private static final String startTimePath = "/XIP/Metadata/Content/PBCoreDescriptionDocument/pbcoreInstantiation/pbcoreDateAvailable/dateAvailableStart";
    private static final String endTimePath = "/XIP/Metadata/Content/PBCoreDescriptionDocument/pbcoreInstantiation/pbcoreDateAvailable/dateAvailableEnd";
    private static final String formPath = "/XIP/Metadata/Content/record/source/tvmeter/form";
    private static final String contentsItemPath = "/XIP/Metadata/Content/record/source/tvmeter/contentsitem";
    private static final String originPath = "/XIP/Metadata/Content/record/source/tvmeter/origin";

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

    public String getContentsItem() {
        return values.get("contentsItem").getValue();
    }

    public void setContentsItem(String contentsItem) {
        values.get("contentsItem").setValue(contentsItem);
    }

    public String getOrigin(){
        return values.get("origin").getValue();
    }

    public void setOrigin(String origin){
        values.get("origin").setValue(origin);
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


