package dk.kb.storage.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dk.kb.storage.model.v1.CreatedDto;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.RerunClusterRequestDto;
import dk.kb.storage.model.v1.RerunClusterResponseDto;
import dk.kb.storage.util.TestcontainersUtil;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.time.OffsetDateTime;
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

    // Act
    RecordsCountDto recordsCountDto = rerunClusterStorage.updateRerunClusters(rerunClusterRequestDto);

    // Assert
    assertNotNull(recordsCountDto);
    assertEquals(1, recordsCountDto.getCount());
  }

  @Test
  public void updateRerunClusters_whenFileIdExistInTable_thenUpdateRow() throws Exception {
    // Arrange
    UUID id = UUID.randomUUID();
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

    // Insert row
    RecordsCountDto insertedRecordsCountDto =
        rerunClusterStorage.updateRerunClusters(firstRerunClusterRequestDto);
    RerunClusterResponseDto insertedRerunClusterResponseDto = rerunClusterStorage.getRerunClusterByFileId(fileId);

    // Act
    RecordsCountDto updatedRecordsCountDto =
        rerunClusterStorage.updateRerunClusters(secondRerunClusterRequestDto);
    RerunClusterResponseDto updatedRerunClusterResponseDto = rerunClusterStorage.getRerunClusterByFileId(fileId);

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
    assertEquals(insertedRerunClusterResponseDto.getInserted(), updatedRerunClusterResponseDto.getInserted());
    assertTrue(insertedRerunClusterResponseDto.getUpdated().isBefore(updatedRerunClusterResponseDto.getUpdated()));
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

    // Act
    RecordsCountDto recordsCountDto = rerunClusterStorage.updateRerunClusters(rerunClusterRequestDto);
    RerunClusterResponseDto rerunClusterResponseDto = rerunClusterStorage.getRerunClusterByFileId(fileId);

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
  public void getRerunClusterByFileId_whenFileIdDoNotExists_thenReturnEmptyRerunClusterResponse() throws Exception {
    // Arrange
    UUID fileId = UUID.randomUUID();

    // Act
    RerunClusterResponseDto rerunClusterResponseDto = rerunClusterStorage.getRerunClusterByFileId(fileId);

    // Assert
    assertNotNull(rerunClusterResponseDto);
    assertNull(rerunClusterResponseDto.getFileId());
    assertNull(rerunClusterResponseDto.getRerunClusterId());
    assertNull(rerunClusterResponseDto.getRerunClusterIdCount());
    assertNull(rerunClusterResponseDto.getCreated());
    assertNull(rerunClusterResponseDto.getJobId());
    assertNull(rerunClusterResponseDto.getInserted());
    assertNull(rerunClusterResponseDto.getUpdated());
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

    RecordsCountDto firstRecordsCountDto =
        rerunClusterStorage.updateRerunClusters(firstRerunClusterRequestDto);
    RecordsCountDto secondRecordsCountDto =
        rerunClusterStorage.updateRerunClusters(secondRerunClusterRequestDto);

    // Act
    CreatedDto createdDto = rerunClusterStorage.latestCreated();

    // Assert
    assertNotNull(createdDto);
    assertEquals(secondCreated, createdDto.getCreated());
  }

  @Test
  public void latestCreated_whenTableIsEmpty_thenReturnNull() throws Exception {
    // Act
    CreatedDto createdDto = rerunClusterStorage.latestCreated();

    // Assert
    assertNotNull(createdDto);
    assertNull(createdDto.getCreated());
  }
}
