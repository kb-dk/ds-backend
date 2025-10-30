package dk.kb.license.mapper;

import dk.kb.license.model.v1.BroadcastDto;
import org.apache.solr.common.SolrDocument;

import java.time.OffsetDateTime;
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

        broadcastDto.setStartTime(parseDateToOffsetDateTime((Date) solrDocument.getFieldValue("startTime")));
        broadcastDto.setEndTime(parseDateToOffsetDateTime((Date) solrDocument.getFieldValue("endTime")));

        if (restrictedIdComment == null) {
            broadcastDto.setRestricted(false);
            broadcastDto.setRestrictedComment(null);
        } else {
            broadcastDto.setRestricted(true);
            broadcastDto.setRestrictedComment(restrictedIdComment);
        }

        return broadcastDto;
    }

    /**
     * Parses Solr Date data type to OffsetDateTime.
     * Solr use UTC
     *
     * @param date solr date
     * @return OffsetDateTime
     */
    private OffsetDateTime parseDateToOffsetDateTime(Date date) {
        return date.toInstant().atOffset(ZoneOffset.UTC);
    }
}
