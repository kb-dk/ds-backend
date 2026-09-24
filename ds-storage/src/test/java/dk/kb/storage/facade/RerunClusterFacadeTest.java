package dk.kb.storage.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dk.kb.storage.model.v1.CreatedDto;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.RerunClusterRequestDto;
import dk.kb.storage.model.v1.RerunClusterResponseDto;
import dk.kb.storage.storage.RecordStorageForUnitTest;
import dk.kb.storage.storage.RerunClusterStorageForUnitTest;
import dk.kb.storage.util.TestcontainersUtil;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RerunClusterFacadeTest extends TestcontainersUtil {

  private static RecordStorageForUnitTest recordStorage = null;
  private static RerunClusterStorageForUnitTest rerunClusterStorage = null;

  @BeforeAll
  public static void beforeClass() throws Exception {
    setupDatabaseForClass(MethodHandles.lookup().lookupClass());
    recordStorage = new RecordStorageForUnitTest();
    rerunClusterStorage = new RerunClusterStorageForUnitTest();
  }

  /**
   * Delete all records between each unittest. The clearTableRecords is only called from here. The
   * facade class is responsible for committing transactions. So clean up between unittests.
   */
  @BeforeEach
  public void beforeEach() throws SQLException {
    recordStorage.clearTableRecords();
    rerunClusterStorage.clearTableRecords();
  }

  @Test
  public void updateRerunClusters_whenGivenRerunClusterRequestDtoList_thenSaveRerunClusterInTableAndRecordsTableMTimeIsUpdated() {
    // Arrange
    String recordId = "doms.radio:id1";
    String origin = "doms.radio"; //Must be defined in YAML properties as allowed origin
    String data = "Hello";
    UUID fileId = UUID.randomUUID();
    String kalturaId = "kalturaId1";

    UUID id = UUID.randomUUID();
    UUID rerunClusterId = UUID.randomUUID();
    OffsetDateTime created = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    String jobId = "test run 1";

    DsRecordDto record = new DsRecordDto();
    record.setId(recordId);
    record.setOrigin(origin);
    record.setData(data);
    record.setKalturaId(kalturaId);
    record.setReferenceId(fileId.toString());
    record.setRecordType(RecordTypeDto.MANIFESTATION);

    RerunClusterRequestDto rerunClusterRequestDto = new RerunClusterRequestDto();
    rerunClusterRequestDto.setId(id);
    rerunClusterRequestDto.setFileId(fileId);
    rerunClusterRequestDto.setRerunClusterId(rerunClusterId);
    rerunClusterRequestDto.setCreated(created);
    rerunClusterRequestDto.setJobId(jobId);

    List<RerunClusterRequestDto> rerunClusterRequestDtoList = List.of(rerunClusterRequestDto);

    RecordFacade.createOrUpdateRecord(record);
    DsRecordDto insertedRecord = RecordFacade.getRecord(recordId, false);

    // Act
    RecordsCountDto returnedRecordsCountDto =
        RerunClusterFacade.updateRerunClusters(rerunClusterRequestDtoList);

    DsRecordDto updatedRecord = RecordFacade.getRecord(recordId, false);

    // Assert
    assertNotNull(returnedRecordsCountDto);
    assertEquals(1, returnedRecordsCountDto.getCount());
    assertTrue(insertedRecord.getmTime() < updatedRecord.getmTime());
  }

  @Test
  public void updateRerunClusters_whenGivenListOfRerunClusterRequest_thenReturnHowManyRowsWasInsertedOrUpdated() {
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
    RecordsCountDto recordsCountDto = RerunClusterFacade.updateRerunClusters(rerunClusterRequestDtoList);

    // Assert
    assertNotNull(recordsCountDto);
    assertEquals(2, recordsCountDto.getCount());
  }

  @Test
  public void getRerunClusterByFileId_whenFileIdExists_thenReturnRerunClusterResponseDto() {
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
    RecordsCountDto recordsCountDto = RerunClusterFacade.updateRerunClusters(rerunClusterRequestDtoList);
    RerunClusterResponseDto rerunClusterResponseDto = RerunClusterFacade.getRerunClusterByFileId(fileId);

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
  public void getRerunClusterByFileId_whenFileIdDoNotExists_thenReturnEmptyRerunCluster() {
    // Arrange
    UUID fileId = UUID.randomUUID();

    // Act
    RerunClusterResponseDto rerunClusterResponseDto = RerunClusterFacade.getRerunClusterByFileId(fileId);

    // Assert
    assertNull(rerunClusterResponseDto.getFileId());
    assertNull(rerunClusterResponseDto.getRerunClusterId());
    assertNull(rerunClusterResponseDto.getRerunClusterIdCount());
    assertNull(rerunClusterResponseDto.getCreated());
    assertNull(rerunClusterResponseDto.getJobId());
    assertNull(rerunClusterResponseDto.getInserted());
    assertNull(rerunClusterResponseDto.getUpdated());
  }

  @Test
  public void latestCreated_whenTableIsPopulated_thenReturnLatestCreated() {
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

    RecordsCountDto recordsCountDto = RerunClusterFacade.updateRerunClusters(rerunClusterRequestDtoList);

    // Act
    CreatedDto createdDto = RerunClusterFacade.latestCreated();

    // Assert
    assertNotNull(createdDto);
    assertEquals(secondCreated, createdDto.getCreated());
  }

  @Test
  public void latestCreated_whenTableIsEmpty_thenReturnNull() {
    // Act
    CreatedDto createdDto = RerunClusterFacade.latestCreated();

    // Assert
    assertNotNull(createdDto);
    assertNull(createdDto.getCreated());
  }
}
