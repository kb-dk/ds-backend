package dk.kb.kaltura.client;

import com.google.gson.JsonObject;
import com.kaltura.client.enums.EntryStatus;
import com.kaltura.client.types.*;
import com.kaltura.client.utils.request.BaseRequestBuilder;
import com.kaltura.client.utils.request.MultiRequestBuilder;
import com.kaltura.client.utils.response.base.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link DsKalturaClient} that never calls Kaltura. {@code listMediaEntry} returns the configured entries and keeps
 * the last filter and pager it was called with. {@code media.get} can not see the requested id, so it returns the
 * first configured entry, the Kaltura not found error if there are none, or the error set with {@link #setGetError}.
 */
class EntryStatusStubKalturaClient extends DsKalturaClient {
    private final Map<String, EntryStatus> entries;
    private MediaEntryFilter lastFilter;
    private FilterPager lastPager;
    private APIException getError;

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

    void setGetError(APIException getError) {
        this.getError = getError;
    }

    @Override
    public ListResponse<MediaEntry> listMediaEntry(MediaEntryFilter filter, FilterPager pager) throws APIException {
        lastFilter = filter;
        lastPager = pager;
        List<MediaEntry> result = new ArrayList<>();
        for (Map.Entry<String, EntryStatus> entry : entries.entrySet()) {
            result.add(mediaEntry(entry.getKey(), entry.getValue()));
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
        if ("media.get".equals(requestBuilder.getTag())) {
            if (getError != null) {
                return new Response<>(null, getError);
            }
            if (entries.isEmpty()) {
                return new Response<>(null, new APIException(APIException.FailureStep.OnResponse,
                        "Entry id not found", DsKalturaClient.ENTRY_ID_NOT_FOUND));
            }
            Map.Entry<String, EntryStatus> entry = entries.entrySet().iterator().next();
            return new Response<>(mediaEntry(entry.getKey(), entry.getValue()), null);
        }
        throw new IllegalStateException("Unexpected request: " + requestBuilder.getTag());
    }

    private static MediaEntry mediaEntry(String id, EntryStatus status) throws APIException {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("status", status.getValue());
        return new MediaEntry(json);
    }
}
