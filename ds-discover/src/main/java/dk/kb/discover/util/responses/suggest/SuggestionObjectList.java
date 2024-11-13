package dk.kb.discover.util.responses.suggest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of suggestions for a given query.
 */
@JsonPropertyOrder({"numFound", "suggestions"})
public class SuggestionObjectList {
    @JsonProperty("numFound")
    long numFound;

    @JsonProperty("suggestions")
    List<SuggestionObject> suggestionObjects = new ArrayList<>();

    public long getNumFound() {
        return numFound;
    }

    public void setNumFound(long numFound) {
        this.numFound = numFound;
    }

    public List<SuggestionObject> getSuggestions() {
        return suggestionObjects;
    }

    public void setSuggestions(List<SuggestionObject> suggestionObjects) {
        this.suggestionObjects = suggestionObjects;
    }

    public void addSuggestion(SuggestionObject suggestionObject) {
        suggestionObjects.add(suggestionObject);
    }

    @Override
    public String toString() {
        return "SuggestionObjectList{" +
                "numFound=" + numFound +
                ", suggestionObjects=" + suggestionObjects +
                '}';
    }
}
