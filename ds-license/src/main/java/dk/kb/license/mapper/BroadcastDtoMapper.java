package dk.kb.license.mapper;

import dk.kb.license.model.v1.BroadcastDto;
import org.apache.solr.common.SolrDocument;

import java.time.ZoneOffset;
import java.util.Date;

public class BroadcastDtoMapper {

    /**
     * Map SolrDocument to BroadcastDto object
     *
     * @param solrDocument
     * @param restrictedIdComment
     * @return BroadcastDto object
     */
    public BroadcastDto mapBroadcastDto(SolrDocument solrDocument, String restrictedIdComment) {
        BroadcastDto broadcastDto = new BroadcastDto();

        broadcastDto.setDsId(solrDocument.getFieldValue("id").toString());
        broadcastDto.setTitle(solrDocument.getFieldValue("title").toString());

        // Solr uses Date data type and UTC, so we need to parse it to OffsetDateTime.
        Date startTimeDate = (Date) solrDocument.getFieldValue("startTime");
        broadcastDto.setStartTime(startTimeDate.toInstant().atOffset(ZoneOffset.UTC));

        // Solr uses Date data type and UTC, so we need to parse it to OffsetDateTime.
        Date endTimeDate = (Date) solrDocument.getFieldValue("endTime");
        broadcastDto.setEndTime(endTimeDate.toInstant().atOffset(ZoneOffset.UTC));

        if (restrictedIdComment == null) {
            broadcastDto.setRestricted(false);
            broadcastDto.setRestrictedComment(null);
        } else {
            broadcastDto.setRestricted(true);
            broadcastDto.setRestrictedComment(restrictedIdComment);
        }

        return broadcastDto;
    }
}
