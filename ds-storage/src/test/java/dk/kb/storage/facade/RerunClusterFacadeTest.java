package dk.kb.storage.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dk.kb.storage.model.v1.CreatedDto;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.RerunClusterDto;
import dk.kb.storage.storage.RecordStorageForUnitTest;
import dk.kb.storage.storage.RerunClusterStorageForUnitTest;
import dk.kb.storage.util.TestcontainersUtil;
import dk.kb.util.webservice.exception.InternalServiceException;
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
  public void updateRerunClusters_whenGivenRerunClusterDtoList_thenRecordsTableMTimeIsUpdated() {
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

    RerunClusterDto rerunClusterDto = new RerunClusterDto();
    rerunClusterDto.setId(id);
    rerunClusterDto.setFileId(fileId);
    rerunClusterDto.setRerunClusterId(rerunClusterId);
    rerunClusterDto.setCreated(created);
    rerunClusterDto.setJobId(jobId);

    List<RerunClusterDto> rerunClusterDtoList = List.of(rerunClusterDto);

    RecordFacade.createOrUpdateRecord(record);
    DsRecordDto insertedRecord = RecordFacade.getRecord(recordId, false);

    // Act
    RecordsCountDto returnedRecordsCountDto =
        RerunClusterFacade.updateRerunClusters(rerunClusterDtoList);

    DsRecordDto updatedRecord = RecordFacade.getRecord(recordId, false);

    // Assert
    assertNotNull(returnedRecordsCountDto);
    assertEquals(1, returnedRecordsCountDto.getCount());
    assertTrue(insertedRecord.getmTime() < updatedRecord.getmTime());
  }

  @Test
  public void updateRerunClusters_whenGivenListOfRerunCluster_thenReturnHowManyRowsWasInsertedOrUpdated() {
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

    List<RerunClusterDto> rerunClusterDtoList =
        List.of(firstRerunClusterDto, secondRerunClusterDto);

    // Act
    RecordsCountDto recordsCountDto = RerunClusterFacade.updateRerunClusters(rerunClusterDtoList);

    // Assert
    assertNotNull(recordsCountDto);
    assertEquals(2, recordsCountDto.getCount());
  }

  @Test
  public void getRerunClusterByFileId_whenFileIdExists_thenReturnRerunClusterDto() {
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

    List<RerunClusterDto> rerunClusterDtoList = List.of(rerunClusterDto);

    // Act
    RecordsCountDto recordsCountDto = RerunClusterFacade.updateRerunClusters(rerunClusterDtoList);
    RerunClusterDto returnedRerunClusterDto = RerunClusterFacade.getRerunClusterByFileId(fileId);

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
  public void getRerunClusterByFileId_whenFileIdDoNotExists_thenThrowNotFoundException() {
    // Arrange
    UUID fileId = UUID.randomUUID();

    String expectedMessage =
        "dk.kb.util.webservice.exception.InternalServiceException: javax.ws.rs.NotFoundException: rerunCluster fileId='" +
            fileId + "' not found";

    // Act
    Exception exception = assertThrows(InternalServiceException.class,
        () -> RerunClusterFacade.getRerunClusterByFileId(fileId));

    // Assert
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  public void latestCreated_whenTableIsPopulated_thenReturnLatestCreated() {
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

    List<RerunClusterDto> rerunClusterDtoList =
        List.of(firstRerunClusterDto, secondRerunClusterDto);

    RecordsCountDto recordsCountDto = RerunClusterFacade.updateRerunClusters(rerunClusterDtoList);

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
