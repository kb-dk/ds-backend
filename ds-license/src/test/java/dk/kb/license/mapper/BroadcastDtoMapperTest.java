package dk.kb.license.mapper;

import dk.kb.license.model.v1.BroadcastDto;
import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class BroadcastDtoMapperTest {

    // The format date Solr client from dependency returns
    String parseDateFormat = "EEEE MMM dd HH:mm:ss z yyyy";

    String dsId = "ds.tv:oai:io:d5ec7b20-c1f2-491e-a2cb-f143683a40f8";
    String title = "P2 Radioavis";

    String startTime = "Thu Apr 05 08:00:00 CEST 2018";
    Date startTimeDate = null;

    String endTime = "Thu Apr 05 08:06:00 CEST 2018";
    Date endTimeDate = null;

    SolrDocument solrDocument;

    @BeforeEach
    public void setUp() throws ParseException {
        startTimeDate = new SimpleDateFormat(parseDateFormat, Locale.ROOT).parse(startTime);
        endTimeDate = new SimpleDateFormat(parseDateFormat, Locale.ROOT).parse(endTime);

        solrDocument = new SolrDocument();
        solrDocument.put("id", dsId);
        solrDocument.put("title", title);
        solrDocument.put("startTime", startTimeDate);
        solrDocument.put("endTime", endTimeDate);
    }

    @Test
    public void map_whenSolrDocumentAndRestrictedIdComment_thenReturnWithRestrictedTrue() {
        // Arrange
        String restrictedComment = "Brugeren har trukket deres samtykke tilbage";
        BroadcastDtoMapper broadcastDtoMapper = new BroadcastDtoMapper();

        // Act
        BroadcastDto actualBroadcastDto = broadcastDtoMapper.map(solrDocument, restrictedComment);

        // Assert
        assertEquals(dsId, actualBroadcastDto.getDsId());
        assertEquals(title, actualBroadcastDto.getTitle());

        assertNotNull(actualBroadcastDto.getStartTime());
        assertEquals(OffsetDateTime.class, actualBroadcastDto.getStartTime().getClass());
        assertEquals(OffsetDateTime.parse("2018-04-05T06:00Z"), actualBroadcastDto.getStartTime());

        assertNotNull(actualBroadcastDto.getEndTime());
        assertEquals(OffsetDateTime.class, actualBroadcastDto.getEndTime().getClass());
        assertEquals(OffsetDateTime.parse("2018-04-05T06:06Z"), actualBroadcastDto.getEndTime());

        assertEquals(true, actualBroadcastDto.getRestricted());
        assertNotNull(actualBroadcastDto.getRestrictedComment());
        assertEquals(restrictedComment, actualBroadcastDto.getRestrictedComment());
    }

    @Test
    public void map_whenSolrDocumentAndNullRestrictedIdComment_thenReturnWithRestrictedFalse() {
        // Arrange
        BroadcastDtoMapper broadcastDtoMapper = new BroadcastDtoMapper();

        // Act
        BroadcastDto actualBroadcastDto = broadcastDtoMapper.map(solrDocument, null);

        // Assert
        assertEquals(dsId, actualBroadcastDto.getDsId());
        assertEquals(title, actualBroadcastDto.getTitle());

        assertNotNull(actualBroadcastDto.getStartTime());
        assertEquals(OffsetDateTime.class, actualBroadcastDto.getStartTime().getClass());
        assertEquals(OffsetDateTime.parse("2018-04-05T06:00Z"), actualBroadcastDto.getStartTime());

        assertNotNull(actualBroadcastDto.getEndTime());
        assertEquals(OffsetDateTime.class, actualBroadcastDto.getEndTime().getClass());
        assertEquals(OffsetDateTime.parse("2018-04-05T06:06Z"), actualBroadcastDto.getEndTime());

        assertEquals(false, actualBroadcastDto.getRestricted());
        assertNull(actualBroadcastDto.getRestrictedComment());
    }
}
