package dk.kb.storage.storage;

import dk.kb.storage.mapper.CreatedDtoMapper;
import dk.kb.storage.mapper.RecordsCountDtoMapper;
import dk.kb.storage.mapper.SummaryResponseDtoMapper;
import dk.kb.storage.model.v1.CreatedDto;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.SummaryRequestDto;
import dk.kb.storage.model.v1.SummaryResponseDto;
import dk.kb.storage.util.UniqueTimestampGenerator;
import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SummaryStorage extends BaseModuleStorage {
  private static final Logger log = LoggerFactory.getLogger(SummaryStorage.class);

  private final static CreatedDtoMapper createdDtoMapper = new CreatedDtoMapper();
  private final static RecordsCountDtoMapper recordsCountDtoMapper = new RecordsCountDtoMapper();
  private final static SummaryResponseDtoMapper summaryResponseDtoMapper =
      new SummaryResponseDtoMapper();

  private static final String updateSummariesStatement = """
                                                         WITH insert_update_summaries AS (
                                                             INSERT INTO summaries (
                                                                 id,
                                                                 file_id,
                                                                 summary,
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
                                                                 summary = EXCLUDED.summary,
                                                                 created = EXCLUDED.created,
                                                                 job_id = EXCLUDED.job_id,
                                                                 updated = statement_timestamp()
                                                             RETURNING
                                                                 file_id -- what row needs to be updated in ds_records
                                                         )
                                                         UPDATE
                                                             ds_records dr
                                                         SET
                                                             mtime = ?
                                                         FROM
                                                             insert_update_summaries ius
                                                         WHERE
                                                             dr.referenceid = ius.file_id::TEXT
                                                         """;

  private static final String getSummaryByFileIdStatement = """
                                                            SELECT
                                                                s.id,
                                                                s.file_id,
                                                                s.summary,
                                                                s.created,
                                                                s.job_id,
                                                                s.inserted,
                                                                s.updated
                                                            FROM
                                                                summaries s
                                                            WHERE
                                                                file_id = ?
                                                            """;

  private static final String latestCreatedSummaryStatement = """
                                                              SELECT
                                                                  max(s.created) AS latest_created -- find the latest created datetime
                                                              FROM
                                                                  summaries s
                                                              """;

  public SummaryStorage() throws SQLException {
    super();
  }

  /**
   * Insert row in summaries table, or update row if the fileId exists. Execute the batch and get an
   * array containing the result code for every individual statement execution.
   *
   * @param summaryRequestDtoList
   * @return RecordsCountDto number of rows inserted or updated
   * @throws SQLException
   */
  public RecordsCountDto updateSummaries(List<SummaryRequestDto> summaryRequestDtoList)
      throws SQLException {

    try (PreparedStatement stmt = connection.prepareStatement(updateSummariesStatement)) {
      for (SummaryRequestDto summaryRequestDto : summaryRequestDtoList) {
        long uniqueTimestamp = UniqueTimestampGenerator.next();

        stmt.setObject(1, summaryRequestDto.getId());
        stmt.setObject(2, summaryRequestDto.getFileId());
        stmt.setString(3, summaryRequestDto.getSummary());
        stmt.setObject(4, summaryRequestDto.getCreated());
        stmt.setString(5, summaryRequestDto.getJobId());
        stmt.setLong(6, uniqueTimestamp);

        stmt.addBatch();
      }

      int[] updateCounts = stmt.executeBatch();
      // Each batch statement inserts or updates exactly one summary row
      return recordsCountDtoMapper.map(updateCounts.length);
    } catch (BatchUpdateException e) {
      SQLException root = e.getNextException();
      if (root == null) {
        root = e;
      }
      log.error("Batch update failed, nothing was persisted: {}", root.getMessage(), e);
      throw e;
    } catch (SQLException e) {
      String message = "SQL Exception in updateSummaries: " + e.getMessage();
      log.error(message);
      throw new SQLException(message, e);
    }
  }

  /**
   * Return a SummaryResponseDto by fileId.
   *
   * @param fileId
   * @return SummaryResponseDto. If fileId is not found will return null
   * @throws SQLException
   */
  public SummaryResponseDto getSummaryByFileId(UUID fileId) throws SQLException {
    try (PreparedStatement stmt = connection.prepareStatement(getSummaryByFileIdStatement)) {
      stmt.setObject(1, fileId);
      ResultSet resultSet = stmt.executeQuery();

      if (resultSet.next()) {
        return summaryResponseDtoMapper.map(resultSet);
      }

      return null;
    } catch (SQLException e) {
      String message = "SQL Exception in getSummaryByFileId with fileId:'" + fileId + "' error: " +
          e.getMessage();
      log.error(message);
      throw new SQLException(message, e);
    }
  }

  /**
   * Return latest created datetime from summaries table. Can be null.
   *
   * @return CreatedDto with latest created datetime
   * @throws SQLException
   */
  public CreatedDto latestCreatedSummary() throws SQLException {
    try (PreparedStatement stmt = connection.prepareStatement(latestCreatedSummaryStatement)) {
      ResultSet resultSet = stmt.executeQuery();

      if (resultSet.next()) {
        return createdDtoMapper.map(resultSet);
      }

      return null;
    } catch (SQLException e) {
      String message = "SQL Exception in latestCreatedSummary. Error: " + e.getMessage();
      log.error(message);
      throw new SQLException(message, e);
    }
  }
}
