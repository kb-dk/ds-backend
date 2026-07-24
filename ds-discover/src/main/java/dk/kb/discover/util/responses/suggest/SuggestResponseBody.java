package dk.kb.discover.util.responses.suggest;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The body of a suggest response from solr. This one uses a suggester called radiotv_title_suggest.
 */
public class SuggestResponseBody {
    @JsonProperty("radiotv_title_suggest")
    RadioTvTitleSuggest radioTvTitleSuggest;

    public RadioTvTitleSuggest getRadioTvTitleSuggest() {
        return radioTvTitleSuggest;
    }

    public void setRadioTvTitleSuggest(RadioTvTitleSuggest radioTvTitleSuggest) {
        this.radioTvTitleSuggest = radioTvTitleSuggest;
    }

    @Override
    public String toString() {
        return "SuggestResponseBody{" +
                "radioTvTitleSuggest=" + radioTvTitleSuggest +
                '}';
    }
}
