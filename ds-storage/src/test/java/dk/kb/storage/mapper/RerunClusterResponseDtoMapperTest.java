package dk.kb.storage.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dk.kb.storage.model.v1.RerunClusterResponseDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class RerunClusterResponseDtoMapperTest {

  @Test
  public void map_whenResultSet_thenReturnRerunClusterResponseDto() throws SQLException {
    // Assert
    RerunClusterResponseDtoMapper rerunClusterResponseDtoMapper = new RerunClusterResponseDtoMapper();

    UUID id = UUID.randomUUID();
    UUID fileId = UUID.randomUUID();
    UUID rerunClusterId = UUID.randomUUID();
    Integer rerunClusterIdCount = 1;
    OffsetDateTime created = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    String jobId = "test run 1";
    OffsetDateTime inserted = OffsetDateTime.parse("2026-05-30T12:26:57.570Z");
    OffsetDateTime updated = OffsetDateTime.parse("2026-06-30T12:26:57.570Z");

    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getObject("id", UUID.class)).thenReturn(id);
    when(resultSet.getObject("file_id", UUID.class)).thenReturn(fileId);
    when(resultSet.getObject("rerun_cluster_id", UUID.class)).thenReturn(rerunClusterId);
    when(resultSet.getObject("rerun_cluster_id_count", Integer.class)).thenReturn(rerunClusterIdCount);
    when(resultSet.getObject("created", OffsetDateTime.class)).thenReturn(created);
    when(resultSet.getString("job_id")).thenReturn(jobId);
    when(resultSet.getObject("inserted", OffsetDateTime.class)).thenReturn(inserted);
    when(resultSet.getObject("updated", OffsetDateTime.class)).thenReturn(updated);

    // Act
    RerunClusterResponseDto rerunClusterResponseDto = rerunClusterResponseDtoMapper.map(resultSet);

    // Assert
    assertNotNull(rerunClusterResponseDto);
    assertEquals(id, rerunClusterResponseDto.getId());
    assertEquals(fileId, rerunClusterResponseDto.getFileId());
    assertEquals(rerunClusterId, rerunClusterResponseDto.getRerunClusterId());
    assertEquals(rerunClusterIdCount, rerunClusterResponseDto.getRerunClusterIdCount());
    assertEquals(created, rerunClusterResponseDto.getCreated());
    assertEquals(jobId, rerunClusterResponseDto.getJobId());
    assertEquals(inserted, rerunClusterResponseDto.getInserted());
    assertEquals(updated, rerunClusterResponseDto.getUpdated());
  }
}