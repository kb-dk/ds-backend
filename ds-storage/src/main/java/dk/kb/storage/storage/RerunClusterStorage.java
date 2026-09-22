package dk.kb.storage.storage;

import dk.kb.storage.mapper.CreatedDtoMapper;
import dk.kb.storage.mapper.RecordsCountDtoMapper;
import dk.kb.storage.mapper.RerunClusterDtoMapper;
import dk.kb.storage.model.v1.CreatedDto;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.RerunClusterDto;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RerunClusterStorage extends BaseModuleStorage {
  private static final Logger log = LoggerFactory.getLogger(RerunClusterStorage.class);

  private final static CreatedDtoMapper createdDtoMapper = new CreatedDtoMapper();
  private final static RecordsCountDtoMapper recordsCountDtoMapper = new RecordsCountDtoMapper();
  private final static RerunClusterDtoMapper rerunClusterDtoMapper = new RerunClusterDtoMapper();

  private static final String updateRerunClustersStatement = """
    WITH insert_update_rerun_clusters AS (
        INSERT INTO rerun_clusters (
            id,
            file_id,
            rerun_cluster_id,
            created,
            job_id,
            inserted,
            updated
        )
        VALUES (
            ?,
            ?,
            ?,
            ?,
            ?,
            transaction_timestamp(),
            transaction_timestamp()
        )
        ON CONFLICT (file_id) DO UPDATE SET
            id = EXCLUDED.id,
            rerun_cluster_id = EXCLUDED.rerun_cluster_id,
            created = EXCLUDED.created,
            job_id = EXCLUDED.job_id,
            updated = statement_timestamp()
        RETURNING
            file_id -- used in count(*)
    ),
    inserted_updated_rerun_clusters AS (
        SELECT
            count(*) AS rerun_clusters_count
        FROM
            insert_update_rerun_clusters
    )
    SELECT
        iurc.rerun_clusters_count
    FROM
        inserted_updated_rerun_clusters iurc
    CROSS JOIN
        inserted_updated_rerun_clusters udr
    """;

  private static final String getRerunClusterByFileIdStatement = """
    SELECT
        rc.id,
        rc.file_id,
        rc.rerun_cluster_id,
        (SELECT COUNT(*) FROM rerun_clusters WHERE rerun_cluster_id = rc.rerun_cluster_id) AS rerun_cluster_id_count,
        rc.created,
        rc.job_id,
        rc.inserted,
        rc.updated
    FROM
        rerun_clusters rc
    WHERE
        file_id = ?
    """;

  private static final String latestCreatedStatement = """
    SELECT
        max(rc.created) AS latest_created -- find the latest created datetime
    FROM
        rerun_clusters rc
    """;

  public RerunClusterStorage() throws SQLException {
    super();
  }

  /**
   * Insert row in rerun_cluster table, or update row if the fileId exists.
   *
   * @param rerunClusterDto
   * @return RecordsCountDto number of rows inserted or updated
   * @throws SQLException
   */
  public RecordsCountDto updateRerunClusters(RerunClusterDto rerunClusterDto) throws SQLException {
    try (PreparedStatement stmt = connection.prepareStatement(updateRerunClustersStatement)) {
      stmt.setObject(1, rerunClusterDto.getId());
      stmt.setObject(2, rerunClusterDto.getFileId());
      stmt.setObject(3, rerunClusterDto.getRerunClusterId());
      stmt.setObject(4, rerunClusterDto.getCreated());
      stmt.setObject(5, rerunClusterDto.getJobId());

      ResultSet resultSet = stmt.executeQuery();

      while (resultSet.next()) {
        return recordsCountDtoMapper.map(resultSet.getInt("rerun_clusters_count"));
      }

      return null;
    } catch (SQLException e) {
      String message = "SQL Exception in updateRerunClusters: " + e.getMessage();
      log.error(message);
      throw new SQLException(message, e);
    }
  }

  /**
   * Return a RerunCluster by fileId.
   *
   * @param fileId
   * @return RerunClusterDto
   * @throws SQLException
   */
  public RerunClusterDto getRerunClusterByFileId(UUID fileId) throws SQLException {
    try (PreparedStatement stmt = connection.prepareStatement(getRerunClusterByFileIdStatement)) {
      stmt.setObject(1, fileId);
      ResultSet resultSet = stmt.executeQuery();

      while (resultSet.next()) {
        return rerunClusterDtoMapper.map(resultSet);
      }

      return null;
    } catch (SQLException e) {
      String message =
          "SQL Exception in getRerunClusterByFileId with fileId:'" + fileId + "' error: " +
              e.getMessage();
      log.error(message);
      throw new SQLException(message, e);
    }
  }

  /**
   * Return latest created datetime from rerun_clusters table. Can be null.
   *
   * @return CreatedDto with latest created datetime
   * @throws SQLException
   */
  public CreatedDto latestCreated() throws SQLException {
    try (PreparedStatement stmt = connection.prepareStatement(latestCreatedStatement)) {
      ResultSet resultSet = stmt.executeQuery();

      while (resultSet.next()) {
        return createdDtoMapper.map(resultSet);
      }

      return null;
    } catch (SQLException e) {
      String message = "SQL Exception in latestCreated. Error: " + e.getMessage();
      log.error(message);
      throw new SQLException(message, e);
    }
  }
}
