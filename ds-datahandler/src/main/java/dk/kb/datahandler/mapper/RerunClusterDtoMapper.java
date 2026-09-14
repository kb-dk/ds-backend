package dk.kb.datahandler.mapper;

import dk.kb.storage.model.v1.RerunClusterDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

public class RerunClusterDtoMapper {

  /**
   * Create a {@link RerunClusterDto} from a ResultSet.
   *
   * @param resultSet containing values from remote p3rerun database in table clusters table
   * @return RerunClusterDto populated with data
   * @throws SQLException
   */
  public RerunClusterDto map(ResultSet resultSet) throws SQLException {
    RerunClusterDto output = new RerunClusterDto();

    output.setId(resultSet.getObject("id", UUID.class));
    output.setFileId(resultSet.getObject("file_id", UUID.class));
    output.setRerunClusterId(resultSet.getObject("rerun_cluster_id", UUID.class));
    output.setCreated(resultSet.getObject("created", OffsetDateTime.class));
    output.setJobId(resultSet.getString("job_id"));

    return output;
  }
}
