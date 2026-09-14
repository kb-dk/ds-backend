package dk.kb.datahandler.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dk.kb.datahandler.model.v1.CategoryDto;
import dk.kb.datahandler.model.v1.JobDto;
import dk.kb.datahandler.model.v1.JobStatusDto;
import dk.kb.datahandler.model.v1.TypeDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class JobDtoMapperTest {

  @Test
  public void map_whenResultSet_thenReturnJobDto() throws SQLException {
    // Assert
    JobDtoMapper jobDtoMapper = new JobDtoMapper();

    UUID id = UUID.randomUUID();
    String type = "DELTA";
    String category = "SOLR_INDEX";
    String source = "ds.tv";
    String createdBy = "Unit test user";
    String status = "RUNNING";
    UUID errorCorrelationId = UUID.randomUUID();
    String message = "The job has ended";
    OffsetDateTime modifiedTimeFrom = OffsetDateTime.now(ZoneOffset.UTC);
    OffsetDateTime startTime = OffsetDateTime.now(ZoneOffset.UTC);
    OffsetDateTime endTime = OffsetDateTime.now(ZoneOffset.UTC);
    Integer numberOfRecords = 1;
    OffsetDateTime restartValue = OffsetDateTime.now(ZoneOffset.UTC);

    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getObject("id", UUID.class)).thenReturn(id);
    when(resultSet.getString("type")).thenReturn(type);
    when(resultSet.getString("category")).thenReturn(category);
    when(resultSet.getString("source")).thenReturn(source);
    when(resultSet.getString("created_by")).thenReturn(createdBy);
    when(resultSet.getString("status")).thenReturn(status);
    when(resultSet.getObject("error_correlation_id", UUID.class)).thenReturn(errorCorrelationId);
    when(resultSet.getString("message")).thenReturn(message);
    when(resultSet.getObject("modified_time_from", OffsetDateTime.class)).thenReturn(
        modifiedTimeFrom);
    when(resultSet.getObject("start_time", OffsetDateTime.class)).thenReturn(startTime);
    when(resultSet.getObject("end_time", OffsetDateTime.class)).thenReturn(endTime);
    when(resultSet.getObject("number_of_records", Integer.class)).thenReturn(numberOfRecords);
    when(resultSet.getObject("restart_value", OffsetDateTime.class)).thenReturn(restartValue);

    // Act
    JobDto jobDto = jobDtoMapper.map(resultSet);

    // Assert
    assertNotNull(jobDto);
    assertEquals(id, jobDto.getId());
    assertEquals(TypeDto.DELTA, jobDto.getType());
    assertEquals(CategoryDto.SOLR_INDEX, jobDto.getCategory());
    assertEquals(source, jobDto.getSource());
    assertEquals(createdBy, jobDto.getCreatedBy());
    assertEquals(JobStatusDto.RUNNING, jobDto.getJobStatus());
    assertEquals(errorCorrelationId, jobDto.getErrorCorrelationId());
    assertEquals(message, jobDto.getMessage());
    assertEquals(modifiedTimeFrom, jobDto.getModifiedTimeFrom());
    assertEquals(startTime, jobDto.getStartTime());
    assertEquals(endTime, jobDto.getEndTime());
    assertEquals(numberOfRecords, jobDto.getNumberOfRecords());
    assertEquals(restartValue, jobDto.getRestartValue());
  }
}
