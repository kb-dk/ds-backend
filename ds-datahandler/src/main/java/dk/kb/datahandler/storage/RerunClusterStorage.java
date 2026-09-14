package dk.kb.datahandler.storage;

import dk.kb.datahandler.mapper.RerunClusterDtoMapper;
import dk.kb.storage.model.v1.RerunClusterDto;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RerunClusterStorage extends BaseModuleStorage {
  private static final Logger log = LoggerFactory.getLogger(RerunClusterStorage.class);

  private final static RerunClusterDtoMapper rerunClusterDtoMapper = new RerunClusterDtoMapper();

  private static final String getRerunClustersStatement =
    """
    SELECT DISTINCT ON (c.file_id) -- there can be multiple of the same file_id (history) and we want the newest inserted file_id
        c.id,
        c.file_id,
        c.rerun_cluster_id,
        c.created,
        c.job_id
    FROM
        cluster c
    WHERE
        (
            CAST(? AS TIMESTAMP WITH TIME ZONE) IS NULL -- takes care if the rerun_clusters table is empty
            OR c.created > ?
        )
    ORDER BY
        c.file_id ASC,
        c.created DESC
    """;

  public RerunClusterStorage() throws SQLException {
    super();
  }

  /**
   * Return new rows from remote p3rerun database in table clusters table.
   *
   * @param created latest created time in our database
   * @return List<RerunClusterDto> of rows
   * @throws Exception
   */
  public List<RerunClusterDto> getRerunClusters(OffsetDateTime created) throws Exception {
    List<RerunClusterDto> rerunClusterDtoList = new ArrayList<>();

    try (PreparedStatement stmt = connection.prepareStatement(getRerunClustersStatement)) {
      stmt.setObject(1, created);
      stmt.setObject(2, created);
      ResultSet resultSet = stmt.executeQuery();

      while (resultSet.next()) {
        RerunClusterDto rerunClusterDto = rerunClusterDtoMapper.map(resultSet);
        rerunClusterDtoList.add(rerunClusterDto);
      }

      return rerunClusterDtoList;
    } catch (SQLException e) {
      String message = "SQL Exception in getRerunClusters: " + e.getMessage();
      log.error(message);
      throw new SQLException(message, e);
    }
  }
}
