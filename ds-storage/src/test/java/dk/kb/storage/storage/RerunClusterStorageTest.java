package dk.kb.storage.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dk.kb.storage.model.v1.CreatedDto;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.RerunClusterDto;
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

    RerunClusterDto rerunClusterDto = new RerunClusterDto();
    rerunClusterDto.setId(id);
    rerunClusterDto.setFileId(fileId);
    rerunClusterDto.setRerunClusterId(rerunClusterId);
    rerunClusterDto.setCreated(created);
    rerunClusterDto.setJobId(jobId);

    // Act
    RecordsCountDto recordsCountDto = rerunClusterStorage.updateRerunClusters(rerunClusterDto);

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

    RerunClusterDto firstRerunClusterDto = new RerunClusterDto();
    firstRerunClusterDto.setId(UUID.randomUUID());
    firstRerunClusterDto.setFileId(fileId);
    firstRerunClusterDto.setRerunClusterId(UUID.randomUUID());
    firstRerunClusterDto.setCreated(firstCreated);
    firstRerunClusterDto.setJobId(firstJobId);

    RerunClusterDto secondRerunClusterDto = new RerunClusterDto();
    secondRerunClusterDto.setId(id);
    secondRerunClusterDto.setFileId(fileId);
    secondRerunClusterDto.setRerunClusterId(rerunClusterId);
    secondRerunClusterDto.setCreated(secondCreated);
    secondRerunClusterDto.setJobId(secondJobId);

    // Insert row
    RecordsCountDto insertedRecordsCountDto =
        rerunClusterStorage.updateRerunClusters(firstRerunClusterDto);
    RerunClusterDto insertedRerunClusterDto = rerunClusterStorage.getRerunClusterByFileId(fileId);

    // Act
    RecordsCountDto updatedRecordsCountDto =
        rerunClusterStorage.updateRerunClusters(secondRerunClusterDto);
    RerunClusterDto updatedRerunClusterDto = rerunClusterStorage.getRerunClusterByFileId(fileId);

    // Assert
    assertNotNull(insertedRecordsCountDto);
    assertEquals(1, insertedRecordsCountDto.getCount());

    assertNotNull(updatedRecordsCountDto);
    assertEquals(1, updatedRecordsCountDto.getCount());

    assertEquals(id, updatedRerunClusterDto.getId());
    assertEquals(fileId, updatedRerunClusterDto.getFileId());
    assertEquals(rerunClusterId, updatedRerunClusterDto.getRerunClusterId());
    assertEquals(1, updatedRerunClusterDto.getRerunClusterIdCount());
    assertEquals(secondCreated, updatedRerunClusterDto.getCreated());
    assertEquals(secondJobId, updatedRerunClusterDto.getJobId());
    assertEquals(insertedRerunClusterDto.getInserted(), updatedRerunClusterDto.getInserted());
    assertTrue(insertedRerunClusterDto.getUpdated().isBefore(updatedRerunClusterDto.getUpdated()));
  }

  @Test
  public void getRerunClusterByFileId_whenFileIdExists_thenReturnRerunClusterDto()
      throws Exception {
    // Arrange
    UUID id = UUID.randomUUID();
    UUID fileId = UUID.randomUUID();
    UUID rerunClusterId = UUID.randomUUID();
    OffsetDateTime created = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    String jobId = "test run 1";

    RerunClusterDto rerunClusterDto = new RerunClusterDto();
    rerunClusterDto.setId(id);
    rerunClusterDto.setFileId(fileId);
    rerunClusterDto.setRerunClusterId(rerunClusterId);
    rerunClusterDto.setCreated(created);
    rerunClusterDto.setJobId(jobId);

    // Act
    RecordsCountDto recordsCountDto = rerunClusterStorage.updateRerunClusters(rerunClusterDto);
    RerunClusterDto returnedRerunClusterDto = rerunClusterStorage.getRerunClusterByFileId(fileId);

    // Assert
    assertNotNull(recordsCountDto);
    assertEquals(1, recordsCountDto.getCount());

    assertEquals(id, returnedRerunClusterDto.getId());
    assertEquals(fileId, returnedRerunClusterDto.getFileId());
    assertEquals(rerunClusterId, returnedRerunClusterDto.getRerunClusterId());
    assertEquals(1, returnedRerunClusterDto.getRerunClusterIdCount());
    assertEquals(created, returnedRerunClusterDto.getCreated());
    assertEquals(jobId, returnedRerunClusterDto.getJobId());
    assertEquals(returnedRerunClusterDto.getInserted(), returnedRerunClusterDto.getUpdated());
  }

  @Test
  public void getRerunClusterByFileId_whenFileIdDoNotExists_thenReturnEmptyRerunCluster() throws Exception {
    // Arrange
    UUID fileId = UUID.randomUUID();

    // Act
    RerunClusterDto rerunClusterDto = rerunClusterStorage.getRerunClusterByFileId(fileId);

    // Assert
    assertNotNull(rerunClusterDto);
    assertNull(rerunClusterDto.getFileId());
    assertNull(rerunClusterDto.getRerunClusterId());
    assertNull(rerunClusterDto.getRerunClusterIdCount());
    assertNull(rerunClusterDto.getCreated());
    assertNull(rerunClusterDto.getJobId());
    assertNull(rerunClusterDto.getInserted());
    assertNull(rerunClusterDto.getUpdated());
  }

  @Test
  public void latestCreated_whenTableIsPopulated_thenReturnLatestCreated() throws Exception {
    // Arrange
    OffsetDateTime firstCreated = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    OffsetDateTime secondCreated = OffsetDateTime.parse("2026-05-01T07:20:00.000Z");

    RerunClusterDto firstRerunClusterDto = new RerunClusterDto();
    firstRerunClusterDto.setId(UUID.randomUUID());
    firstRerunClusterDto.setFileId(UUID.randomUUID());
    firstRerunClusterDto.setRerunClusterId(UUID.randomUUID());
    firstRerunClusterDto.setCreated(firstCreated);
    firstRerunClusterDto.setJobId("test run 1");

    RerunClusterDto secondRerunClusterDto = new RerunClusterDto();
    secondRerunClusterDto.setId(UUID.randomUUID());
    secondRerunClusterDto.setFileId(UUID.randomUUID());
    secondRerunClusterDto.setRerunClusterId(UUID.randomUUID());
    secondRerunClusterDto.setCreated(secondCreated);
    secondRerunClusterDto.setJobId("test run 2");

    RecordsCountDto firstRecordsCountDto =
        rerunClusterStorage.updateRerunClusters(firstRerunClusterDto);
    RecordsCountDto secondRecordsCountDto =
        rerunClusterStorage.updateRerunClusters(secondRerunClusterDto);

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
