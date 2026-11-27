package dk.kb.datahandler.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dk.kb.storage.model.v1.SummaryRequestDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class SummaryRequestDtoMapperTest {

  @Test
  public void map_whenResultSet_thenReturnSummaryRequestDto() throws SQLException {
    // Assert
    SummaryRequestDtoMapper summaryRequestDtoMapper = new SummaryRequestDtoMapper();

    UUID id = UUID.randomUUID();
    UUID fileId = UUID.randomUUID();
    String summary = "Fin beskrivelse.";
    OffsetDateTime created = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    String jobId = "test run 1";

    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getObject("id", UUID.class)).thenReturn(id);
    when(resultSet.getObject("file_id", UUID.class)).thenReturn(fileId);
    when(resultSet.getString("summary")).thenReturn(summary);
    when(resultSet.getObject("created", OffsetDateTime.class)).thenReturn(created);
    when(resultSet.getString("job_id")).thenReturn(jobId);

    // Act
    SummaryRequestDto summaryRequestDto = summaryRequestDtoMapper.map(resultSet);

    // Assert
    assertNotNull(summaryRequestDto);
    assertEquals(id, summaryRequestDto.getId());
    assertEquals(fileId, summaryRequestDto.getFileId());
    assertEquals(summary, summaryRequestDto.getSummary());
    assertEquals(created, summaryRequestDto.getCreated());
    assertEquals(jobId, summaryRequestDto.getJobId());
  }
}
