package dk.kb.discover.util.responses.suggest;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single suggestion inside a {@link SuggestionObjectList}. The specific suggestion can be extracted as {@link #term}.
 */
public class SuggestionObject {
    @JsonProperty("term")
    String term;

    @JsonProperty("weight")
    int weight;

    @JsonProperty("payload")
    String payload;

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "SuggestionObject{" +
                "term='" + term + '\'' +
                ", weight=" + weight +
                ", payload='" + payload + '\'' +
                '}';
    }
}
