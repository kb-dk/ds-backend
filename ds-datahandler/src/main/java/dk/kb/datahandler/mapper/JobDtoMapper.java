package dk.kb.datahandler.mapper;

import dk.kb.datahandler.model.v1.CategoryDto;
import dk.kb.datahandler.model.v1.JobDto;
import dk.kb.datahandler.model.v1.JobStatusDto;
import dk.kb.datahandler.model.v1.TypeDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

public class JobDtoMapper {

  public JobDto map(ResultSet result) throws SQLException {
    JobDto jobDto = new JobDto();

    jobDto.setId(result.getObject("id", UUID.class));
    jobDto.setType(TypeDto.valueOf(result.getString("type")));
    jobDto.setCategory(CategoryDto.valueOf(result.getString("category")));
    jobDto.setSource(result.getString("source"));
    jobDto.setJobStatus(JobStatusDto.valueOf(result.getString("status")));
    jobDto.setCreatedBy(result.getString("created_by"));
    jobDto.setErrorCorrelationId(result.getObject("error_correlation_id", UUID.class));
    jobDto.setMessage(result.getString("message"));
    jobDto.setModifiedTimeFrom(result.getObject("modified_time_from", OffsetDateTime.class));
    jobDto.setStartTime(result.getObject("start_time", OffsetDateTime.class));
    jobDto.setEndTime(result.getObject("end_time", OffsetDateTime.class));
    jobDto.setNumberOfRecords(result.getObject("number_of_records", Integer.class));
    jobDto.setRestartValue(result.getObject("restart_value", OffsetDateTime.class));

    return jobDto;
  }
}
