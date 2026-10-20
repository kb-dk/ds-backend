package dk.kb.datahandler.mapper;

import dk.kb.storage.model.v1.SummaryRequestDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

public class SummaryRequestDtoMapper {

  /**
   * Create a {@link SummaryRequestDto} from a ResultSet
   *
   * @param resultSet containing values from summaries table
   * @return SummaryRequestDto populated with data
   * @throws SQLException
   */
  public SummaryRequestDto map(ResultSet resultSet) throws SQLException {
    SummaryRequestDto output = new SummaryRequestDto();

    output.setId(resultSet.getObject("id", UUID.class));
    output.setFileId(resultSet.getObject("file_id", UUID.class));
    output.setSummary(resultSet.getString("summary"));
    output.setCreated(resultSet.getObject("created", OffsetDateTime.class));
    output.setJobId(resultSet.getString("job_id"));

    return output;
  }
}
