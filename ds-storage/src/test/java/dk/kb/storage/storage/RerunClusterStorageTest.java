package dk.kb.storage.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dk.kb.storage.model.v1.CreatedDto;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.RerunClusterRequestDto;
import dk.kb.storage.model.v1.RerunClusterResponseDto;
import dk.kb.storage.util.TestcontainersUtil;
import java.lang.invoke.MethodHandles;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RerunClusterStorageTest extends TestcontainersUtil {

  private static RerunClusterStorageForUnitTest rerunClusterStorage = null;

  @BeforeAll
  public static void beforeClass() throws Exception {
    setupDatabaseForClass(MethodHandles.lookup().lookupClass());
    rerunClusterStorage = new RerunClusterStorageForUnitTest();
  }

  /**
   * Delete all records between each unittest. The clearTableRecords is only called from here. The
   * facade class is responsible for committing transactions. So clean up between unittests.
   */
  @BeforeEach
  public void beforeEach() throws SQLException {
    rerunClusterStorage.clearTableRecords();
  }

  @Test
  public void updateRerunClusters_whenFileIdDoesNotExistInTable_thenInsertRow() throws Exception {
    // Arrange
    UUID id = UUID.randomUUID();
    UUID fileId = UUID.randomUUID();
    UUID rerunClusterId = UUID.randomUUID();
    OffsetDateTime created = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    String jobId = "test run 1";

    RerunClusterRequestDto rerunClusterRequestDto = new RerunClusterRequestDto();
    rerunClusterRequestDto.setId(id);
    rerunClusterRequestDto.setFileId(fileId);
    rerunClusterRequestDto.setRerunClusterId(rerunClusterId);
    rerunClusterRequestDto.setCreated(created);
    rerunClusterRequestDto.setJobId(jobId);

    List<RerunClusterRequestDto> rerunClusterRequestDtoList = List.of(rerunClusterRequestDto);

    // Act
    RecordsCountDto recordsCountDto =
        rerunClusterStorage.updateRerunClusters(rerunClusterRequestDtoList);

    // Assert
    assertNotNull(recordsCountDto);
    assertEquals(1, recordsCountDto.getCount());
  }

  @Test
  public void updateRerunClusters_whenFileIdExistInTable_thenUpdateRow() throws Exception {
    // Arrange
    UUID id = UUID.randomUUID();
    // Having the same fileId when inserting and updating
    UUID fileId = UUID.randomUUID();
    UUID rerunClusterId = UUID.randomUUID();

    OffsetDateTime firstCreated = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    String firstJobId = "test run 1";

    OffsetDateTime secondCreated = OffsetDateTime.parse("2026-05-01T07:20:00.000Z");
    String secondJobId = "test run 2";

    RerunClusterRequestDto firstRerunClusterRequestDto = new RerunClusterRequestDto();
    firstRerunClusterRequestDto.setId(UUID.randomUUID());
    firstRerunClusterRequestDto.setFileId(fileId);
    firstRerunClusterRequestDto.setRerunClusterId(UUID.randomUUID());
    firstRerunClusterRequestDto.setCreated(firstCreated);
    firstRerunClusterRequestDto.setJobId(firstJobId);

    RerunClusterRequestDto secondRerunClusterRequestDto = new RerunClusterRequestDto();
    secondRerunClusterRequestDto.setId(id);
    secondRerunClusterRequestDto.setFileId(fileId);
    secondRerunClusterRequestDto.setRerunClusterId(rerunClusterId);
    secondRerunClusterRequestDto.setCreated(secondCreated);
    secondRerunClusterRequestDto.setJobId(secondJobId);

    List<RerunClusterRequestDto> firstRerunClusterRequestDtoList =
        List.of(firstRerunClusterRequestDto);

    List<RerunClusterRequestDto> secondRerunClusterRequestDtoList =
        List.of(secondRerunClusterRequestDto);

    // Insert row
    RecordsCountDto insertedRecordsCountDto =
        rerunClusterStorage.updateRerunClusters(firstRerunClusterRequestDtoList);
    RerunClusterResponseDto insertedRerunClusterResponseDto =
        rerunClusterStorage.getRerunClusterByFileId(fileId);

    // Act
    RecordsCountDto updatedRecordsCountDto =
        rerunClusterStorage.updateRerunClusters(secondRerunClusterRequestDtoList);
    RerunClusterResponseDto updatedRerunClusterResponseDto =
        rerunClusterStorage.getRerunClusterByFileId(fileId);

    // Assert
    assertNotNull(insertedRecordsCountDto);
    assertEquals(1, insertedRecordsCountDto.getCount());

    assertNotNull(updatedRecordsCountDto);
    assertEquals(1, updatedRecordsCountDto.getCount());

    assertEquals(id, updatedRerunClusterResponseDto.getId());
    assertEquals(fileId, updatedRerunClusterResponseDto.getFileId());
    assertEquals(rerunClusterId, updatedRerunClusterResponseDto.getRerunClusterId());
    assertEquals(1, updatedRerunClusterResponseDto.getRerunClusterIdCount());
    assertEquals(secondCreated, updatedRerunClusterResponseDto.getCreated());
    assertEquals(secondJobId, updatedRerunClusterResponseDto.getJobId());
    assertEquals(insertedRerunClusterResponseDto.getInserted(),
        updatedRerunClusterResponseDto.getInserted());
    assertTrue(insertedRerunClusterResponseDto.getUpdated()
        .isBefore(updatedRerunClusterResponseDto.getUpdated()));
  }

  @Test
  public void updateRerunClusters_whenGivenListOfRerunClusterRequest_thenReturnHowManyRowsWasInsertedOrUpdated()
      throws SQLException {
    // Arrange
    OffsetDateTime firstCreated = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    OffsetDateTime secondCreated = OffsetDateTime.parse("2026-05-01T07:20:00.000Z");

    RerunClusterRequestDto firstRerunClusterRequestDto = new RerunClusterRequestDto();
    firstRerunClusterRequestDto.setId(UUID.randomUUID());
    firstRerunClusterRequestDto.setFileId(UUID.randomUUID());
    firstRerunClusterRequestDto.setRerunClusterId(UUID.randomUUID());
    firstRerunClusterRequestDto.setCreated(firstCreated);
    firstRerunClusterRequestDto.setJobId("test run 1");

    RerunClusterRequestDto secondRerunClusterRequestDto = new RerunClusterRequestDto();
    secondRerunClusterRequestDto.setId(UUID.randomUUID());
    secondRerunClusterRequestDto.setFileId(UUID.randomUUID());
    secondRerunClusterRequestDto.setRerunClusterId(UUID.randomUUID());
    secondRerunClusterRequestDto.setCreated(secondCreated);
    secondRerunClusterRequestDto.setJobId("test run 2");

    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        List.of(firstRerunClusterRequestDto, secondRerunClusterRequestDto);

    // Act
    RecordsCountDto recordsCountDto =
        rerunClusterStorage.updateRerunClusters(rerunClusterRequestDtoList);

    // Assert
    assertNotNull(recordsCountDto);
    assertEquals(2, recordsCountDto.getCount());
  }

  @Test
  public void updateRerunClusters_whenOneStatementInBatchFails_thenThrowBatchUpdateExceptionAndNoRowsArePersisted()
      throws SQLException {
    // Arrange
    UUID firstFileId = UUID.randomUUID();
    UUID thirdFileId = UUID.randomUUID();
    OffsetDateTime created = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    String jobId = "test run 1";

    RerunClusterRequestDto firstRerunClusterRequestDto = new RerunClusterRequestDto();
    firstRerunClusterRequestDto.setId(UUID.randomUUID());
    firstRerunClusterRequestDto.setFileId(firstFileId);
    firstRerunClusterRequestDto.setRerunClusterId(UUID.randomUUID());
    firstRerunClusterRequestDto.setCreated(created);
    firstRerunClusterRequestDto.setJobId(jobId);

    // Invalid row in the middle, so there is a valid row before and after the bad ome
    RerunClusterRequestDto invalidRerunClusterRequestDto = new RerunClusterRequestDto();
    invalidRerunClusterRequestDto.setId(UUID.randomUUID());
    // fileId = null violates NOT NULL on file_id
    invalidRerunClusterRequestDto.setFileId(null);
    invalidRerunClusterRequestDto.setRerunClusterId(UUID.randomUUID());
    invalidRerunClusterRequestDto.setCreated(created);
    invalidRerunClusterRequestDto.setJobId(jobId);

    RerunClusterRequestDto thirdRerunClusterRequestDto = new RerunClusterRequestDto();
    thirdRerunClusterRequestDto.setId(UUID.randomUUID());
    thirdRerunClusterRequestDto.setFileId(thirdFileId);
    thirdRerunClusterRequestDto.setRerunClusterId(UUID.randomUUID());
    thirdRerunClusterRequestDto.setCreated(created);
    thirdRerunClusterRequestDto.setJobId(jobId);

    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        List.of(firstRerunClusterRequestDto, invalidRerunClusterRequestDto,
            thirdRerunClusterRequestDto);
    // Act
    BatchUpdateException exception = assertThrows(BatchUpdateException.class,
        () -> rerunClusterStorage.updateRerunClusters(rerunClusterRequestDtoList));

    // The transaction is aborted after the failure and must be rolled back before it can be used again
    rerunClusterStorage.rollback();

    // Assert
    SQLException rootCause = exception.getNextException();
    assertNotNull(rootCause);
    String errorMessage = """
                          ERROR: null value in column "file_id" of relation "rerun_clusters" violates not-null constraint
                          """;
    assertTrue(rootCause.getMessage().startsWith(errorMessage));

    // The SQLState code Postgres returns for a NOT NULL violation.
    String notNullViolation = "23502";
    assertEquals(notNullViolation, rootCause.getSQLState());

    // The row before and after the bad one are rolled back (not persisted)
    assertNull(rerunClusterStorage.getRerunClusterByFileId(firstFileId));
    assertNull(rerunClusterStorage.getRerunClusterByFileId(thirdFileId));
  }

  @Test
  public void updateRerunClusters_whenBatchFails_thenExistingRowIsNotChanged() throws Exception {
    // Arrange
    UUID fileId = UUID.randomUUID();
    String firstJobId = "test run 1";
    String secondJobId = "test run 2";
    OffsetDateTime firstCreated = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    OffsetDateTime secondCreated = OffsetDateTime.parse("2026-05-01T07:20:00.000Z");

    RerunClusterRequestDto firstRerunClusterRequestDto = new RerunClusterRequestDto();
    firstRerunClusterRequestDto.setId(UUID.randomUUID());
    firstRerunClusterRequestDto.setFileId(fileId);
    firstRerunClusterRequestDto.setRerunClusterId(UUID.randomUUID());
    firstRerunClusterRequestDto.setCreated(firstCreated);
    firstRerunClusterRequestDto.setJobId(firstJobId);

    // Insert a row first
    RecordsCountDto recordsCountDto =
        rerunClusterStorage.updateRerunClusters(List.of(firstRerunClusterRequestDto));
    rerunClusterStorage.commit(); // make the baseline row survive the later rollback

    // Baseline we want to keep
    RerunClusterResponseDto insertedRerunClusterResponseDto =
        rerunClusterStorage.getRerunClusterByFileId(fileId);

    RerunClusterRequestDto updateInsertedRerunClusterRequestDto = new RerunClusterRequestDto();
    updateInsertedRerunClusterRequestDto.setId(UUID.randomUUID());
    updateInsertedRerunClusterRequestDto.setFileId(fileId);
    updateInsertedRerunClusterRequestDto.setRerunClusterId(UUID.randomUUID());
    updateInsertedRerunClusterRequestDto.setCreated(secondCreated);
    updateInsertedRerunClusterRequestDto.setJobId(secondJobId);

    RerunClusterRequestDto invalidRerunClusterRequestDto = new RerunClusterRequestDto();
    invalidRerunClusterRequestDto.setId(UUID.randomUUID());
    invalidRerunClusterRequestDto.setFileId(null);
    invalidRerunClusterRequestDto.setRerunClusterId(UUID.randomUUID());
    invalidRerunClusterRequestDto.setCreated(secondCreated);
    invalidRerunClusterRequestDto.setJobId(secondJobId);

    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        List.of(updateInsertedRerunClusterRequestDto, invalidRerunClusterRequestDto);

    // Act
    BatchUpdateException exception = assertThrows(BatchUpdateException.class,
        () -> rerunClusterStorage.updateRerunClusters(rerunClusterRequestDtoList));

    // The transaction is aborted after the failure and must be rolled back before it can be used again
    rerunClusterStorage.rollback();

    // Assert
    RerunClusterResponseDto notChangedRerunClusterResponseDto =
        rerunClusterStorage.getRerunClusterByFileId(fileId);

    assertNotNull(notChangedRerunClusterResponseDto);
    assertEquals(insertedRerunClusterResponseDto.getId(),
        notChangedRerunClusterResponseDto.getId());
    assertEquals(fileId, notChangedRerunClusterResponseDto.getFileId());
    assertEquals(insertedRerunClusterResponseDto.getRerunClusterId(),
        notChangedRerunClusterResponseDto.getRerunClusterId());
    assertEquals(1, notChangedRerunClusterResponseDto.getRerunClusterIdCount());
    assertEquals(firstCreated, notChangedRerunClusterResponseDto.getCreated());
    assertEquals(firstJobId, notChangedRerunClusterResponseDto.getJobId());
    assertEquals(notChangedRerunClusterResponseDto.getInserted(),
        notChangedRerunClusterResponseDto.getUpdated());
  }

  @Test
  public void getRerunClusterByFileId_whenFileIdExists_thenReturnRerunClusterResponseDto()
      throws Exception {
    // Arrange
    UUID id = UUID.randomUUID();
    UUID fileId = UUID.randomUUID();
    UUID rerunClusterId = UUID.randomUUID();
    OffsetDateTime created = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    String jobId = "test run 1";

    RerunClusterRequestDto rerunClusterRequestDto = new RerunClusterRequestDto();
    rerunClusterRequestDto.setId(id);
    rerunClusterRequestDto.setFileId(fileId);
    rerunClusterRequestDto.setRerunClusterId(rerunClusterId);
    rerunClusterRequestDto.setCreated(created);
    rerunClusterRequestDto.setJobId(jobId);

    List<RerunClusterRequestDto> rerunClusterRequestDtoList = List.of(rerunClusterRequestDto);

    // Act
    RecordsCountDto recordsCountDto =
        rerunClusterStorage.updateRerunClusters(rerunClusterRequestDtoList);
    RerunClusterResponseDto rerunClusterResponseDto =
        rerunClusterStorage.getRerunClusterByFileId(fileId);

    // Assert
    assertNotNull(recordsCountDto);
    assertEquals(1, recordsCountDto.getCount());

    assertEquals(id, rerunClusterResponseDto.getId());
    assertEquals(fileId, rerunClusterResponseDto.getFileId());
    assertEquals(rerunClusterId, rerunClusterResponseDto.getRerunClusterId());
    assertEquals(1, rerunClusterResponseDto.getRerunClusterIdCount());
    assertEquals(created, rerunClusterResponseDto.getCreated());
    assertEquals(jobId, rerunClusterResponseDto.getJobId());
    assertEquals(rerunClusterResponseDto.getInserted(), rerunClusterResponseDto.getUpdated());
  }

  @Test
  public void getRerunClusterByFileId_whenFileIdDoNotExists_thenReturnNull()
      throws Exception {
    // Arrange
    UUID fileId = UUID.randomUUID();

    // Act
    RerunClusterResponseDto rerunClusterResponseDto =
        rerunClusterStorage.getRerunClusterByFileId(fileId);

    // Assert
    assertNull(rerunClusterResponseDto);
  }

  @Test
  public void latestCreated_whenTableIsPopulated_thenReturnLatestCreated() throws Exception {
    // Arrange
    OffsetDateTime firstCreated = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    OffsetDateTime secondCreated = OffsetDateTime.parse("2026-05-01T07:20:00.000Z");

    RerunClusterRequestDto firstRerunClusterRequestDto = new RerunClusterRequestDto();
    firstRerunClusterRequestDto.setId(UUID.randomUUID());
    firstRerunClusterRequestDto.setFileId(UUID.randomUUID());
    firstRerunClusterRequestDto.setRerunClusterId(UUID.randomUUID());
    firstRerunClusterRequestDto.setCreated(firstCreated);
    firstRerunClusterRequestDto.setJobId("test run 1");

    RerunClusterRequestDto secondRerunClusterRequestDto = new RerunClusterRequestDto();
    secondRerunClusterRequestDto.setId(UUID.randomUUID());
    secondRerunClusterRequestDto.setFileId(UUID.randomUUID());
    secondRerunClusterRequestDto.setRerunClusterId(UUID.randomUUID());
    secondRerunClusterRequestDto.setCreated(secondCreated);
    secondRerunClusterRequestDto.setJobId("test run 2");

    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        List.of(firstRerunClusterRequestDto, secondRerunClusterRequestDto);

    // Act
    RecordsCountDto recordsCountDto =
        rerunClusterStorage.updateRerunClusters(rerunClusterRequestDtoList);

    // Act
    CreatedDto createdDto = rerunClusterStorage.latestCreated();

    // Assert
    assertNotNull(recordsCountDto);
    assertEquals(2, recordsCountDto.getCount());

    assertNotNull(createdDto);
    assertEquals(secondCreated, createdDto.getCreated());
  }

  @Test
  public void latestCreated_whenTableIsEmpty_thenReturnCreatedDtoWithNullCreated() throws Exception {
    // Act
    CreatedDto createdDto = rerunClusterStorage.latestCreated();

    // Assert
    assertNotNull(createdDto);
    assertNull(createdDto.getCreated());
  }
}
