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
import dk.kb.storage.model.v1.SummaryRequestDto;
import dk.kb.storage.model.v1.SummaryResponseDto;
import dk.kb.storage.storage.RecordStorageForUnitTest;
import dk.kb.storage.storage.SummaryStorageForUnitTest;
import dk.kb.storage.util.TestcontainersUtil;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SummaryFacadeTest extends TestcontainersUtil {

  private static RecordStorageForUnitTest recordStorage = null;
  private static SummaryStorageForUnitTest summaryStorage = null;

  @BeforeAll
  public static void beforeClass() throws Exception {
    setupDatabaseForClass(MethodHandles.lookup().lookupClass());
    recordStorage = new RecordStorageForUnitTest();
    summaryStorage = new SummaryStorageForUnitTest();
  }

  /**
   * Delete all records between each unittest. The clearTable is only called from here. The facade
   * class is responsible for committing transactions. So clean up between unittests.
   */
  @BeforeEach
  public void beforeEach() throws SQLException {
    recordStorage.clearTable();
    summaryStorage.clearTable();
  }

  @Test
  public void updateSummaries_whenGivenSummaryRequestDtoList_thenSaveSummaryInTableAndRecordsTableMTimeIsUpdated() {
    // Arrange
    String recordId = "doms.radio:id1";
    String origin = "doms.radio"; //Must be defined in YAML properties as allowed origin
    String data = "Hello";
    UUID fileId = UUID.randomUUID();
    String kalturaId = "kalturaId1";

    UUID id = UUID.randomUUID();
    String summary = "Fin beskrivelse 1.";
    OffsetDateTime created = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    String jobId = "test run 1";

    DsRecordDto record = new DsRecordDto();
    record.setId(recordId);
    record.setOrigin(origin);
    record.setData(data);
    record.setKalturaId(kalturaId);
    record.setReferenceId(fileId.toString());
    record.setRecordType(RecordTypeDto.MANIFESTATION);

    SummaryRequestDto summaryRequestDto = new SummaryRequestDto();
    summaryRequestDto.setId(id);
    summaryRequestDto.setFileId(fileId);
    summaryRequestDto.setSummary(summary);
    summaryRequestDto.setCreated(created);
    summaryRequestDto.setJobId(jobId);

    List<SummaryRequestDto> summaryRequestDtoList = List.of(summaryRequestDto);

    RecordFacade.createOrUpdateRecord(record);
    DsRecordDto insertedRecord = RecordFacade.getRecord(recordId, false);

    // Act
    RecordsCountDto returnedRecordsCountDto = SummaryFacade.updateSummaries(summaryRequestDtoList);

    DsRecordDto updatedRecord = RecordFacade.getRecord(recordId, false);

    // Assert
    assertNotNull(returnedRecordsCountDto);
    assertEquals(1, returnedRecordsCountDto.getCount());
    assertTrue(insertedRecord.getmTime() < updatedRecord.getmTime());
  }

  @Test
  public void updateSummaries_whenGivenListOfSummaryRequest_thenReturnHowManyRowsWasInsertedOrUpdated() {
    // Arrange
    OffsetDateTime firstCreated = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    OffsetDateTime secondCreated = OffsetDateTime.parse("2026-05-01T07:20:00.000Z");

    SummaryRequestDto firstSummaryRequestDto = new SummaryRequestDto();
    firstSummaryRequestDto.setId(UUID.randomUUID());
    firstSummaryRequestDto.setFileId(UUID.randomUUID());
    firstSummaryRequestDto.setSummary("Fin beskrivelse 1.");
    firstSummaryRequestDto.setCreated(firstCreated);
    firstSummaryRequestDto.setJobId("test run 1");

    SummaryRequestDto secondSummaryRequestDto = new SummaryRequestDto();
    secondSummaryRequestDto.setId(UUID.randomUUID());
    secondSummaryRequestDto.setFileId(UUID.randomUUID());
    secondSummaryRequestDto.setSummary("Fin beskrivelse 2.");
    secondSummaryRequestDto.setCreated(secondCreated);
    secondSummaryRequestDto.setJobId("test run 2");

    List<SummaryRequestDto> summaryRequestDtoList =
        List.of(firstSummaryRequestDto, secondSummaryRequestDto);

    // Act
    RecordsCountDto recordsCountDto = SummaryFacade.updateSummaries(summaryRequestDtoList);

    // Assert
    assertNotNull(recordsCountDto);
    assertEquals(2, recordsCountDto.getCount());
  }

  @Test
  public void updateSummaries_whenBatchFails_thenExistingRowIsNotChanged() throws Exception {
    // Arrange
    UUID fileId = UUID.randomUUID();
    String firstJobId = "test run 1";
    String secondJobId = "test run 2";
    OffsetDateTime firstCreated = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    OffsetDateTime secondCreated = OffsetDateTime.parse("2026-05-01T07:20:00.000Z");

    SummaryRequestDto firstSummaryRequestDto = new SummaryRequestDto();
    firstSummaryRequestDto.setId(UUID.randomUUID());
    firstSummaryRequestDto.setFileId(fileId);
    firstSummaryRequestDto.setSummary("Fin beskrivelse 1.");
    firstSummaryRequestDto.setCreated(firstCreated);
    firstSummaryRequestDto.setJobId(firstJobId);

    // Insert a row first
    RecordsCountDto recordsCountDto =
        SummaryFacade.updateSummaries(List.of(firstSummaryRequestDto));

    // Baseline we want to keep
    SummaryResponseDto insertedSummaryResponseDto = SummaryFacade.getSummaryByFileId(fileId);

    SummaryRequestDto updateInsertedSummaryRequestDto = new SummaryRequestDto();
    updateInsertedSummaryRequestDto.setId(UUID.randomUUID());
    updateInsertedSummaryRequestDto.setFileId(fileId);
    updateInsertedSummaryRequestDto.setSummary("Fin beskrivelse 2.");
    updateInsertedSummaryRequestDto.setCreated(secondCreated);
    updateInsertedSummaryRequestDto.setJobId(secondJobId);

    SummaryRequestDto invalidSummaryRequestDto = new SummaryRequestDto();
    invalidSummaryRequestDto.setId(UUID.randomUUID());
    invalidSummaryRequestDto.setFileId(null);
    invalidSummaryRequestDto.setSummary("Invalid");
    invalidSummaryRequestDto.setCreated(secondCreated);
    invalidSummaryRequestDto.setJobId(secondJobId);

    List<SummaryRequestDto> summaryRequestDtoList =
        List.of(updateInsertedSummaryRequestDto, invalidSummaryRequestDto);

    // Act
    Exception exception = assertThrows(InternalServiceException.class,
        () -> SummaryFacade.updateSummaries(summaryRequestDtoList));

    // Assert
    assertTrue(exception.getMessage().contains("""
                                               null value in column "file_id" of relation "summaries" violates not-null constraint
                                               """));

    SummaryResponseDto notChangedSummaryResponseDto = SummaryFacade.getSummaryByFileId(fileId);

    assertNotNull(notChangedSummaryResponseDto);
    assertEquals(insertedSummaryResponseDto.getId(), notChangedSummaryResponseDto.getId());
    assertEquals(fileId, notChangedSummaryResponseDto.getFileId());
    assertEquals(insertedSummaryResponseDto.getSummary(),
        notChangedSummaryResponseDto.getSummary());
    assertEquals(firstCreated, notChangedSummaryResponseDto.getCreated());
    assertEquals(firstJobId, notChangedSummaryResponseDto.getJobId());
    assertEquals(notChangedSummaryResponseDto.getInserted(),
        notChangedSummaryResponseDto.getUpdated());
  }

  @Test
  public void getSummaryByFileId_whenFileIdExists_thenReturnSummaryResponseDto() {
    // Arrange
    UUID id = UUID.randomUUID();
    UUID fileId = UUID.randomUUID();
    String summary = "Fin beskrivelse.";
    OffsetDateTime created = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    String jobId = "test run 1";

    SummaryRequestDto summaryRequestDto = new SummaryRequestDto();
    summaryRequestDto.setId(id);
    summaryRequestDto.setFileId(fileId);
    summaryRequestDto.setSummary(summary);
    summaryRequestDto.setCreated(created);
    summaryRequestDto.setJobId(jobId);

    List<SummaryRequestDto> summaryRequestDtoList = List.of(summaryRequestDto);

    // Act
    RecordsCountDto recordsCountDto = SummaryFacade.updateSummaries(summaryRequestDtoList);
    SummaryResponseDto summaryResponseDto = SummaryFacade.getSummaryByFileId(fileId);

    // Assert
    assertNotNull(recordsCountDto);
    assertEquals(1, recordsCountDto.getCount());

    assertEquals(id, summaryResponseDto.getId());
    assertEquals(fileId, summaryResponseDto.getFileId());
    assertEquals(summary, summaryResponseDto.getSummary());
    assertEquals(created, summaryResponseDto.getCreated());
    assertEquals(jobId, summaryResponseDto.getJobId());
    assertEquals(summaryResponseDto.getInserted(), summaryResponseDto.getUpdated());
  }

  @Test
  public void getSummaryByFileId_whenFileIdDoNotExists_thenThrowNotFoundServiceException() {
    // Arrange
    UUID fileId = UUID.randomUUID();

    // Act
    NotFoundServiceException exception = assertThrows(NotFoundServiceException.class,
        () -> SummaryFacade.getSummaryByFileId(fileId));

    // Assert
    String errorMessage = "No summary found for fileId '" + fileId + "'";
    assertEquals(errorMessage, exception.getMessage());
  }

  @Test
  public void latestCreatedSummary_whenTableIsPopulated_thenReturnLatestCreated() {
    // Arrange
    OffsetDateTime firstCreated = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    OffsetDateTime secondCreated = OffsetDateTime.parse("2026-05-01T07:20:00.000Z");

    SummaryRequestDto firstSummaryRequestDto = new SummaryRequestDto();
    firstSummaryRequestDto.setId(UUID.randomUUID());
    firstSummaryRequestDto.setFileId(UUID.randomUUID());
    firstSummaryRequestDto.setSummary("Fin beskrivelse 1.");
    firstSummaryRequestDto.setCreated(firstCreated);
    firstSummaryRequestDto.setJobId("test run 1");

    SummaryRequestDto secondSummaryRequestDto = new SummaryRequestDto();
    secondSummaryRequestDto.setId(UUID.randomUUID());
    secondSummaryRequestDto.setFileId(UUID.randomUUID());
    secondSummaryRequestDto.setSummary("Fin beskrivelse 2.");
    secondSummaryRequestDto.setCreated(secondCreated);
    secondSummaryRequestDto.setJobId("test run 2");

    List<SummaryRequestDto> summaryRequestDtoList =
        List.of(firstSummaryRequestDto, secondSummaryRequestDto);

    RecordsCountDto recordsCountDto = SummaryFacade.updateSummaries(summaryRequestDtoList);

    // Act
    CreatedDto createdDto = SummaryFacade.latestCreatedSummary();

    // Assert
    assertNotNull(createdDto);
    assertEquals(secondCreated, createdDto.getCreated());
  }

  @Test
  public void latestCreatedSummary_whenTableIsEmpty_thenReturnCreatedDtoWithNullCreated() {
    // Act
    CreatedDto createdDto = SummaryFacade.latestCreatedSummary();

    // Assert
    assertNotNull(createdDto);
    assertNull(createdDto.getCreated());
  }
}
