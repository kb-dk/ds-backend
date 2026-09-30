package dk.kb.kaltura.client;

import com.google.gson.JsonObject;
import com.kaltura.client.enums.EntryStatus;
import com.kaltura.client.types.APIException;
import com.kaltura.client.types.FilterPager;
import com.kaltura.client.types.ListResponse;
import com.kaltura.client.types.MediaEntry;
import com.kaltura.client.types.MediaEntryFilter;
import com.kaltura.client.utils.request.BaseRequestBuilder;
import com.kaltura.client.utils.request.MultiRequestBuilder;
import com.kaltura.client.utils.response.base.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link DsKalturaClient} that never calls Kaltura. {@code listMediaEntry} returns the configured entries and keeps
 * the last filter and pager it was called with.
 */
class EntryStatusStubKalturaClient extends DsKalturaClient {
    private final Map<String, EntryStatus> entries;
    private MediaEntryFilter lastFilter;
    private FilterPager lastPager;

    EntryStatusStubKalturaClient(Map<String, EntryStatus> entries) throws APIException {
        super("http://localhost", "test@kb.dk", 0, "token", "tokenId", null, 86400, 3600, 10, 1);
        this.entries = entries;
    }

    MediaEntryFilter getLastFilter() {
        return lastFilter;
    }

    FilterPager getLastPager() {
        return lastPager;
    }

    @Override
    public ListResponse<MediaEntry> listMediaEntry(MediaEntryFilter filter, FilterPager pager) throws APIException {
        lastFilter = filter;
        lastPager = pager;
        List<MediaEntry> result = new ArrayList<>();
        for (Map.Entry<String, EntryStatus> entry : entries.entrySet()) {
            JsonObject json = new JsonObject();
            json.addProperty("id", entry.getKey());
            json.addProperty("status", entry.getValue().getValue());
            result.add(new MediaEntry(json));
        }
        ListResponse<MediaEntry> response = new ListResponse<>();
        response.setObjects(result);
        return response;
    }

    @Override
    protected <ReturnedType, SelfType extends BaseRequestBuilder<ReturnedType, SelfType>>
    Response<?> buildAndExecute(SelfType requestBuilder, boolean refreshSession) throws APIException {
        // The conversion queue lookup runs from the super constructor
        if (MultiRequestBuilder.MULTIREQUEST_ACTION.equals(requestBuilder.getTag())) {
            return new Response<>(List.of(0, 0), null);
        }
        throw new IllegalStateException("Unexpected request: " + requestBuilder.getTag());
    }
}
