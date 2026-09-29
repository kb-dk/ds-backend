package dk.kb.kaltura.client;

import com.google.gson.JsonObject;
import com.kaltura.client.types.APIException;
import com.kaltura.client.types.ListResponse;
import com.kaltura.client.types.MediaEntry;
import com.kaltura.client.types.MediaEntryFilter;
import com.kaltura.client.utils.request.BaseRequestBuilder;
import com.kaltura.client.utils.request.MultiRequestBuilder;
import com.kaltura.client.utils.response.base.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link DsKalturaClient} that never calls Kaltura. {@code listMediaEntry} returns entries with the configured ids
 * and keeps the last filter it was called with.
 */
class ReferenceIdStubKalturaClient extends DsKalturaClient {
    private final List<String> entryIds;
    private MediaEntryFilter lastFilter;

    ReferenceIdStubKalturaClient(String... entryIds) throws APIException {
        super("http://localhost", "test@kb.dk", 0, "token", "tokenId", null, 86400, 3600, 10, 1);
        this.entryIds = List.of(entryIds);
    }

    MediaEntryFilter getLastFilter() {
        return lastFilter;
    }

    @Override
    public ListResponse<MediaEntry> listMediaEntry(MediaEntryFilter filter) throws APIException {
        lastFilter = filter;
        List<MediaEntry> entries = new ArrayList<>();
        for (String id : entryIds) {
            JsonObject json = new JsonObject();
            json.addProperty("id", id);
            entries.add(new MediaEntry(json));
        }
        ListResponse<MediaEntry> response = new ListResponse<>();
        response.setObjects(entries);
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
