package dk.kb.discover.util.responses.select;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class FacetCounts {

    @JsonProperty("facet_queries")
    Object facetQueries;

    @JsonProperty("facet_fields")
    Map<String, List<Object>> facetFields;

    @JsonProperty("facet_ranges")
    Object facetRanges;

    @JsonProperty("facet_intervals")
    Object facetIntervals;

    @JsonProperty("facet_heatmaps")
    Object facetHeatmaps;

    public Object getFacetQueries() {
        return facetQueries;
    }

    public void setFacetQueries(Object facetQueries) {
        this.facetQueries = facetQueries;
    }

    public Map<String, List<Object>> getFacetFields() {
        return facetFields;
    }

    public void setFacetFields(Map<String, List<Object>> facetFields) {
        this.facetFields = facetFields;
    }

    public Object getFacetRanges() {
        return facetRanges;
    }

    public void setFacetRanges(Object facetRanges) {
        this.facetRanges = facetRanges;
    }

    public Object getFacetIntervals() {
        return facetIntervals;
    }

    public void setFacetIntervals(Object facetIntervals) {
        this.facetIntervals = facetIntervals;
    }

    public Object getFacetHeatmaps() {
        return facetHeatmaps;
    }

    public void setFacetHeatmaps(Object facetHeatmaps) {
        this.facetHeatmaps = facetHeatmaps;
    }

    @Override
    public String toString() {
        return "FacetCounts{" +
                "facetQueries=" + facetQueries +
                ", facetFields=" + facetFields +
                ", facetRanges=" + facetRanges +
                ", facetIntervals=" + facetIntervals +
                ", facetHeatmaps=" + facetHeatmaps +
                '}';
    }
}
