package dk.kb.discover.util.responses.suggest;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.Map;

/**
 * A JSON wrapper for the inner part of a solr suggest response looking like this:
 * <pre>
 *     "radiotv_title_suggest":{
 *       "test":{
 *         "numFound":5,
 *         "suggestions":[{
 *           "term":"Testen: Det usikre internet",
 *           "weight":10,
 *           "payload":""
 *         },{
 *           "term":"Testen: De ulovlige slagtere",
 *           "weight":10,
 *           "payload":""
 *         }, ... ]
 *        }
 *       }
 * </pre>
 *
 * where @{code radiotv_title_suggest} is the name of the suggest implementation in use and @{code test} is the query, which the response is delivered to.
 */
public class RadioTvTitleSuggest {
    /**
     * The keys in this map is the suggestResponseBody.q param from solr. For RadioTvTitleSuggest, the map should only contain one entry.
     */
    @JsonUnwrapped
    @JsonAnySetter
    Map<String, SuggestionObjectList> suggestQueryObject;


    @JsonAnyGetter
    public Map<String, SuggestionObjectList> getSuggestQueryObject() {
        return suggestQueryObject;
    }


    @JsonAnySetter
    public void setSuggestQueryObject(Map<String, SuggestionObjectList> suggestQueryObject) {
        this.suggestQueryObject = suggestQueryObject;
    }

    @Override
    public String toString() {
        return "RadioTvTitleSuggest{" +
                "suggestQueryObject=" + suggestQueryObject +
                '}';
    }
}
