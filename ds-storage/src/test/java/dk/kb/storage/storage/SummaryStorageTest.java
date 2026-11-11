package dk.kb.storage.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dk.kb.storage.model.v1.CreatedDto;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.SummaryRequestDto;
import dk.kb.storage.model.v1.SummaryResponseDto;
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

public class SummaryStorageTest extends TestcontainersUtil {

  private static SummaryStorageForUnitTest summaryStorage = null;

  @BeforeAll
  public static void beforeClass() throws Exception {
    setupDatabaseForClass(MethodHandles.lookup().lookupClass());
    summaryStorage = new SummaryStorageForUnitTest();
  }

  /**
   * Delete all records between each unittest. The clearTable is only called from here. The facade
   * class is responsible for committing transactions. So clean up between unittests.
   */
  @BeforeEach
  public void beforeEach() throws SQLException {
    summaryStorage.clearTable();
  }

  @Test
  public void updateSummaries_whenFileIdDoesNotExistInTable_thenInsertRow() throws Exception {
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
    RecordsCountDto recordsCountDto = summaryStorage.updateSummaries(summaryRequestDtoList);

    // Assert
    assertNotNull(recordsCountDto);
    assertEquals(1, recordsCountDto.getCount());
  }

  @Test
  public void updateSummaries_whenFileIdExistInTable_thenUpdateRow() throws Exception {
    // Arrange
    UUID id = UUID.randomUUID();
    // Having the same fileId when inserting and updating
    UUID fileId = UUID.randomUUID();
    String summary = "Fin beskrivelse 2.";

    OffsetDateTime firstCreated = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    String firstJobId = "test run 1";

    OffsetDateTime secondCreated = OffsetDateTime.parse("2026-05-01T07:20:00.000Z");
    String secondJobId = "test run 2";

    SummaryRequestDto firstSummaryRequestDto = new SummaryRequestDto();
    firstSummaryRequestDto.setId(UUID.randomUUID());
    firstSummaryRequestDto.setFileId(fileId);
    firstSummaryRequestDto.setSummary("Fin beskrivelse 1.");
    firstSummaryRequestDto.setCreated(firstCreated);
    firstSummaryRequestDto.setJobId(firstJobId);

    SummaryRequestDto secondSummaryRequestDto = new SummaryRequestDto();
    secondSummaryRequestDto.setId(id);
    secondSummaryRequestDto.setFileId(fileId);
    secondSummaryRequestDto.setSummary(summary);
    secondSummaryRequestDto.setCreated(secondCreated);
    secondSummaryRequestDto.setJobId(secondJobId);

    List<SummaryRequestDto> firstSummaryRequestDtoList = List.of(firstSummaryRequestDto);

    List<SummaryRequestDto> secondSummaryRequestDtoList = List.of(secondSummaryRequestDto);

    // Insert row
    RecordsCountDto insertedRecordsCountDto =
        summaryStorage.updateSummaries(firstSummaryRequestDtoList);
    SummaryResponseDto insertedSummaryResponseDto = summaryStorage.getSummaryByFileId(fileId);

    // Act
    RecordsCountDto updatedRecordsCountDto =
        summaryStorage.updateSummaries(secondSummaryRequestDtoList);
    SummaryResponseDto updatedSummaryResponseDto = summaryStorage.getSummaryByFileId(fileId);

    // Assert
    assertNotNull(insertedRecordsCountDto);
    assertEquals(1, insertedRecordsCountDto.getCount());

    assertNotNull(updatedRecordsCountDto);
    assertEquals(1, updatedRecordsCountDto.getCount());

    assertEquals(id, updatedSummaryResponseDto.getId());
    assertEquals(fileId, updatedSummaryResponseDto.getFileId());
    assertEquals(summary, updatedSummaryResponseDto.getSummary());
    assertEquals(secondCreated, updatedSummaryResponseDto.getCreated());
    assertEquals(secondJobId, updatedSummaryResponseDto.getJobId());
    assertEquals(insertedSummaryResponseDto.getInserted(), updatedSummaryResponseDto.getInserted());
    assertTrue(
        insertedSummaryResponseDto.getUpdated().isBefore(updatedSummaryResponseDto.getUpdated()));
  }

  @Test
  public void updateSummaries_whenGivenListOfSummaryRequest_thenReturnHowManyRowsWasInsertedOrUpdated()
      throws SQLException {
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
    RecordsCountDto recordsCountDto = summaryStorage.updateSummaries(summaryRequestDtoList);

    // Assert
    assertNotNull(recordsCountDto);
    assertEquals(2, recordsCountDto.getCount());
  }

  @Test
  public void updateSummaries_whenOneStatementInBatchFails_thenThrowBatchUpdateExceptionAndNoRowsArePersisted()
      throws SQLException {
    // Arrange
    UUID firstFileId = UUID.randomUUID();
    UUID thirdFileId = UUID.randomUUID();
    OffsetDateTime created = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    String jobId = "test run 1";

    SummaryRequestDto firstSummaryRequestDto = new SummaryRequestDto();
    firstSummaryRequestDto.setId(UUID.randomUUID());
    firstSummaryRequestDto.setFileId(firstFileId);
    firstSummaryRequestDto.setSummary("Fin beskrivelse 1.");
    firstSummaryRequestDto.setCreated(created);
    firstSummaryRequestDto.setJobId(jobId);

    // Invalid row in the middle, so there is a valid row before and after the bad ome
    SummaryRequestDto invalidSummaryRequestDto = new SummaryRequestDto();
    invalidSummaryRequestDto.setId(UUID.randomUUID());
    // fileId = null violates NOT NULL on file_id
    invalidSummaryRequestDto.setFileId(null);
    invalidSummaryRequestDto.setSummary("Invalid");
    invalidSummaryRequestDto.setCreated(created);
    invalidSummaryRequestDto.setJobId(jobId);

    SummaryRequestDto thirdSummaryRequestDto = new SummaryRequestDto();
    thirdSummaryRequestDto.setId(UUID.randomUUID());
    thirdSummaryRequestDto.setFileId(thirdFileId);
    thirdSummaryRequestDto.setSummary("Fin beskrivelse 3.");
    thirdSummaryRequestDto.setCreated(created);
    thirdSummaryRequestDto.setJobId(jobId);

    List<SummaryRequestDto> summaryRequestDtoList =
        List.of(firstSummaryRequestDto, invalidSummaryRequestDto, thirdSummaryRequestDto);
    // Act
    BatchUpdateException exception = assertThrows(BatchUpdateException.class,
        () -> summaryStorage.updateSummaries(summaryRequestDtoList));

    // The transaction is aborted after the failure and must be rolled back before it can be used again
    summaryStorage.rollback();

    // Assert
    SQLException rootCause = exception.getNextException();
    assertNotNull(rootCause);
    String errorMessage = """
                          ERROR: null value in column "file_id" of relation "summaries" violates not-null constraint
                          """;
    assertTrue(rootCause.getMessage().startsWith(errorMessage));

    // The SQLState code Postgres returns for a NOT NULL violation.
    String notNullViolation = "23502";
    assertEquals(notNullViolation, rootCause.getSQLState());

    // The row before and after the bad one are rolled back (not persisted)
    assertNull(summaryStorage.getSummaryByFileId(firstFileId));
    assertNull(summaryStorage.getSummaryByFileId(thirdFileId));
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
        summaryStorage.updateSummaries(List.of(firstSummaryRequestDto));
    summaryStorage.commit(); // make the baseline row survive the later rollback

    // Baseline we want to keep
    SummaryResponseDto insertedSummaryResponseDto = summaryStorage.getSummaryByFileId(fileId);

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
    BatchUpdateException exception = assertThrows(BatchUpdateException.class,
        () -> summaryStorage.updateSummaries(summaryRequestDtoList));

    // The transaction is aborted after the failure and must be rolled back before it can be used again
    summaryStorage.rollback();

    // Assert
    assertTrue(exception.getMessage().contains("""
                                               null value in column "file_id" of relation "summaries" violates not-null constraint
                                               """));

    SummaryResponseDto notChangedSummaryResponseDto = summaryStorage.getSummaryByFileId(fileId);

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
  public void getSummaryByFileId_whenFileIdExists_thenReturnSummaryResponseDto() throws Exception {
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
    RecordsCountDto recordsCountDto = summaryStorage.updateSummaries(summaryRequestDtoList);
    SummaryResponseDto summaryResponseDto = summaryStorage.getSummaryByFileId(fileId);

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
  public void getSummaryByFileId_whenFileIdDoNotExists_thenReturnNull() throws Exception {
    // Arrange
    UUID fileId = UUID.randomUUID();

    // Act
    SummaryResponseDto summaryResponseDto = summaryStorage.getSummaryByFileId(fileId);

    // Assert
    assertNull(summaryResponseDto);
  }

  @Test
  public void latestCreatedSummary_whenTableIsPopulated_thenReturnLatestCreated() throws Exception {
    // Arrange
    UUID secondId = UUID.randomUUID();
    OffsetDateTime firstCreated = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
    OffsetDateTime secondCreated = OffsetDateTime.parse("2026-05-01T07:20:00.000Z");

    SummaryRequestDto firstSummaryRequestDto = new SummaryRequestDto();
    firstSummaryRequestDto.setId(UUID.randomUUID());
    firstSummaryRequestDto.setFileId(UUID.randomUUID());
    firstSummaryRequestDto.setSummary("Fin beskrivelse 1.");
    firstSummaryRequestDto.setCreated(firstCreated);
    firstSummaryRequestDto.setJobId("test run 1");

    SummaryRequestDto secondSummaryRequestDto = new SummaryRequestDto();
    secondSummaryRequestDto.setId(secondId);
    secondSummaryRequestDto.setFileId(UUID.randomUUID());
    secondSummaryRequestDto.setSummary("Fin beskrivelse 2.");
    secondSummaryRequestDto.setCreated(secondCreated);
    secondSummaryRequestDto.setJobId("test run 2");

    List<SummaryRequestDto> summaryRequestDtoList =
        List.of(firstSummaryRequestDto, secondSummaryRequestDto);

    // Act
    RecordsCountDto recordsCountDto = summaryStorage.updateSummaries(summaryRequestDtoList);

    // Act
    CreatedDto createdDto = summaryStorage.latestCreatedSummary();

    // Assert
    assertNotNull(recordsCountDto);
    assertEquals(2, recordsCountDto.getCount());

    assertNotNull(createdDto);
    assertEquals(secondId, createdDto.getId());
    assertEquals(secondCreated, createdDto.getCreated());
  }

  @Test
  public void latestCreatedSummary_whenTableIsEmpty_thenReturnNull()
      throws Exception {
    // Act
    CreatedDto createdDto = summaryStorage.latestCreatedSummary();

    // Assert
    assertNull(createdDto);
  }
}
