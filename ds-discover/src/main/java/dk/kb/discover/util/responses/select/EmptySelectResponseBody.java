package dk.kb.discover.util.responses.select;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Collections;
import java.util.List;

@JsonPropertyOrder({"numFound", "start", "numFoundExact", "docs"})
public class EmptySelectResponseBody {
    @JsonProperty("numFound")
    Long numFound;
    @JsonProperty("start")
    Long start;
    @JsonProperty("numFoundExact")
    boolean numFoundExact;
    @JsonProperty("docs")
    List<Object> docs = Collections.emptyList();

    public Long getNumFound() {
        return numFound;
    }

    public void setNumFound(Long numFound) {
        this.numFound = numFound;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public boolean isNumFoundExact() {
        return numFoundExact;
    }

    public void setNumFoundExact(boolean numFoundExact) {
        this.numFoundExact = numFoundExact;
    }

    public List<Object> getDocs() {
        return docs;
    }

    public void setDocs(List<Object> docs) {
        this.docs = docs;
    }

    @Override
    public String toString() {
        return "EmptySelectResponseBody{" +
                "numFound=" + numFound +
                ", start=" + start +
                ", numFoundExact=" + numFoundExact +
                ", docs=" + docs +
                '}';
    }
}
