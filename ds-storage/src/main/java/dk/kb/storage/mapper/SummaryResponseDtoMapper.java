package dk.kb.storage.mapper;

import dk.kb.storage.model.v1.SummaryResponseDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

public class SummaryResponseDtoMapper {

  /**
   * Create a {@link SummaryResponseDto} from a ResultSet
   *
   * @param resultSet containing values from summaries table
   * @return SummaryResponseDto populated with data
   * @throws SQLException
   */
  public SummaryResponseDto map(ResultSet resultSet) throws SQLException {
    SummaryResponseDto output = new SummaryResponseDto();

    output.setId(resultSet.getObject("id", UUID.class));
    output.setFileId(resultSet.getObject("file_id", UUID.class));
    output.setSummary(resultSet.getString("summary"));
    output.setCreated(resultSet.getObject("created", OffsetDateTime.class));
    output.setJobId(resultSet.getString("job_id"));
    output.setInserted(resultSet.getObject("inserted", OffsetDateTime.class));
    output.setUpdated(resultSet.getObject("updated", OffsetDateTime.class));

    return output;
  }
}
