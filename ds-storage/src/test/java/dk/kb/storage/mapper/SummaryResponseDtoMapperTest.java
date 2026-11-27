package dk.kb.storage.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dk.kb.storage.model.v1.SummaryResponseDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class SummaryResponseDtoMapperTest {

  @Test
  public void map_whenResultSet_thenReturnSummaryResponseDto() throws SQLException {
    // Assert
    SummaryResponseDtoMapper summaryResponseDtoMapper = new SummaryResponseDtoMapper();

    UUID id = UUID.randomUUID();
    UUID fileId = UUID.randomUUID();
    String summary = "Fin beskrivelse.";
    OffsetDateTime created = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    String jobId = "test run 1";
    OffsetDateTime inserted = OffsetDateTime.parse("2026-05-30T12:26:57.570Z");
    OffsetDateTime updated = OffsetDateTime.parse("2026-06-30T12:26:57.570Z");

    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getObject("id", UUID.class)).thenReturn(id);
    when(resultSet.getObject("file_id", UUID.class)).thenReturn(fileId);
    when(resultSet.getString("summary")).thenReturn(summary);
    when(resultSet.getObject("created", OffsetDateTime.class)).thenReturn(created);
    when(resultSet.getString("job_id")).thenReturn(jobId);
    when(resultSet.getObject("inserted", OffsetDateTime.class)).thenReturn(inserted);
    when(resultSet.getObject("updated", OffsetDateTime.class)).thenReturn(updated);

    // Act
    SummaryResponseDto summaryResponseDto = summaryResponseDtoMapper.map(resultSet);

    // Assert
    assertNotNull(summaryResponseDto);
    assertEquals(id, summaryResponseDto.getId());
    assertEquals(fileId, summaryResponseDto.getFileId());
    assertEquals(summary, summaryResponseDto.getSummary());
    assertEquals(created, summaryResponseDto.getCreated());
    assertEquals(jobId, summaryResponseDto.getJobId());
    assertEquals(inserted, summaryResponseDto.getInserted());
    assertEquals(updated, summaryResponseDto.getUpdated());
  }
}
