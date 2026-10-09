package dk.kb.storage.storage;

import dk.kb.storage.mapper.CreatedDtoMapper;
import dk.kb.storage.mapper.RecordsCountDtoMapper;
import dk.kb.storage.mapper.RerunClusterResponseDtoMapper;
import dk.kb.storage.model.v1.CreatedDto;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.RerunClusterRequestDto;
import dk.kb.storage.model.v1.RerunClusterResponseDto;
import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RerunClusterStorage extends BaseModuleStorage {
  private static final Logger log = LoggerFactory.getLogger(RerunClusterStorage.class);

  private final static CreatedDtoMapper createdDtoMapper = new CreatedDtoMapper();
  private final static RecordsCountDtoMapper recordsCountDtoMapper = new RecordsCountDtoMapper();
  private final static RerunClusterResponseDtoMapper rerunClusterResponseDtoMapper =
      new RerunClusterResponseDtoMapper();

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
                                                                     file_id -- what row needs to be updated in ds_records
                                                             )
                                                             UPDATE
                                                                 ds_records dr
                                                             SET
                                                                 mtime = (EXTRACT(EPOCH FROM statement_timestamp()) * 1000000)::bigint -- get unix timestamp in microseconds
                                                             FROM
                                                                 insert_update_rerun_clusters iurc
                                                             WHERE
                                                                 dr.referenceid = iurc.file_id::TEXT
                                                             """;

  private static final String getRerunClusterByFileIdStatement = """
                                                                 SELECT
                                                                     rc.id,
                                                                     rc.file_id,
                                                                     rc.rerun_cluster_id,
                                                                     (SELECT COUNT(*) FROM rerun_clusters WHERE rerun_cluster_id = rc.rerun_cluster_id)::int AS rerun_cluster_id_count,
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
   * Insert row in rerun_cluster table, or update row if the fileId exists. Execute the batch and
   * get an array containing the result code for every individual statement execution.
   *
   * @param rerunClusterRequestDtoList
   * @return RecordsCountDto number of rows inserted or updated
   * @throws SQLException
   */
  public RecordsCountDto updateRerunClusters(
      List<RerunClusterRequestDto> rerunClusterRequestDtoList) throws SQLException {

    try (PreparedStatement stmt = connection.prepareStatement(updateRerunClustersStatement)) {
      for (RerunClusterRequestDto rerunClusterRequestDto : rerunClusterRequestDtoList) {
        stmt.setObject(1, rerunClusterRequestDto.getId());
        stmt.setObject(2, rerunClusterRequestDto.getFileId());
        stmt.setObject(3, rerunClusterRequestDto.getRerunClusterId());
        stmt.setObject(4, rerunClusterRequestDto.getCreated());
        stmt.setObject(5, rerunClusterRequestDto.getJobId());

        stmt.addBatch();
      }

      int[] updateCounts = stmt.executeBatch();
      // Each batch statement inserts or updates exactly one rerun_clusters row
      return recordsCountDtoMapper.map(updateCounts.length);
    } catch (BatchUpdateException e) {
      SQLException root = e.getNextException();
      if (root == null) {
        root = e;
      }
      log.error("Batch update failed, nothing was persisted: {}", root.getMessage(), e);
      throw e;
    } catch (SQLException e) {
      String message = "SQL Exception in updateRerunClusters: " + e.getMessage();
      log.error(message);
      throw new SQLException(message, e);
    }
  }

  /**
   * Return a RerunClusterResponseDto by fileId.
   *
   * @param fileId
   * @return RerunClusterResponseDto. If fileId is not found will return null
   * @throws SQLException
   */
  public RerunClusterResponseDto getRerunClusterByFileId(UUID fileId) throws SQLException {
    try (PreparedStatement stmt = connection.prepareStatement(getRerunClusterByFileIdStatement)) {
      stmt.setObject(1, fileId);
      ResultSet resultSet = stmt.executeQuery();

      if (resultSet.next()) {
        return rerunClusterResponseDtoMapper.map(resultSet);
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

      if (resultSet.next()) {
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
