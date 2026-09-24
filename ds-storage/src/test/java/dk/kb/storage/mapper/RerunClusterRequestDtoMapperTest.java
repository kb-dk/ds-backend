package dk.kb.storage.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dk.kb.storage.model.v1.RerunClusterRequestDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class RerunClusterRequestDtoMapperTest {

  @Test
  public void map_whenResultSet_thenReturnRerunClusterRequestDto() throws SQLException {
    // Assert
    RerunClusterRequestDtoMapper rerunClusterRequestDtoMapper = new RerunClusterRequestDtoMapper();

    UUID id = UUID.randomUUID();
    UUID fileId = UUID.randomUUID();
    UUID rerunClusterId = UUID.randomUUID();
    OffsetDateTime created = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    String jobId = "test run 1";

    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getObject("id", UUID.class)).thenReturn(id);
    when(resultSet.getObject("file_id", UUID.class)).thenReturn(fileId);
    when(resultSet.getObject("rerun_cluster_id", UUID.class)).thenReturn(rerunClusterId);
    when(resultSet.getObject("created", OffsetDateTime.class)).thenReturn(created);
    when(resultSet.getString("job_id")).thenReturn(jobId);

    // Act
    RerunClusterRequestDto rerunClusterRequestDto = rerunClusterRequestDtoMapper.map(resultSet);

    // Assert
    assertNotNull(rerunClusterRequestDto);
    assertEquals(id, rerunClusterRequestDto.getId());
    assertEquals(fileId, rerunClusterRequestDto.getFileId());
    assertEquals(rerunClusterId, rerunClusterRequestDto.getRerunClusterId());
    assertEquals(created, rerunClusterRequestDto.getCreated());
    assertEquals(jobId, rerunClusterRequestDto.getJobId());
  }
}