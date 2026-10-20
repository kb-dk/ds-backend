package dk.kb.storage.api.v1.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dk.kb.storage.facade.RecordFacade;
import dk.kb.storage.model.v1.CreatedDto;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.SummaryRequestDto;
import dk.kb.storage.model.v1.SummaryResponseDto;
import dk.kb.storage.storage.RecordStorageForUnitTest;
import dk.kb.storage.storage.SummaryStorageForUnitTest;
import dk.kb.storage.util.TestcontainersUtil;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletMapping;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SummaryApiServiceImplTest extends TestcontainersUtil {

  private static RecordStorageForUnitTest recordStorage = null;
  private static SummaryStorageForUnitTest summaryStorage = null;
  SummaryApiServiceImpl summaryApiServiceImpl = new SummaryApiServiceImpl();

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
  public void updateSummaries_whenGivenListOfSummaryRequestDto_thenReturnHowManyRowsWasInsertedOrUpdated() {
    // Arrange
    String recordId = "doms.radio:id1";
    String origin = "doms.radio"; //Must be defined in YAML properties as allowed origin
    String data = "Hello";
    UUID fileId = UUID.randomUUID();
    String kalturaId = "kalturaId1";

    UUID id = UUID.randomUUID();
    String summary = "Fin beskrivelse.";
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
    RecordsCountDto returnedRecordsCountDto =
        summaryApiServiceImpl.updateSummaries(summaryRequestDtoList);

    DsRecordDto updatedRecord = RecordFacade.getRecord(recordId, false);

    // Assert
    assertNotNull(returnedRecordsCountDto);
    assertEquals(1, returnedRecordsCountDto.getCount());
    assertTrue(insertedRecord.getmTime() < updatedRecord.getmTime());
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
    RecordsCountDto recordsCountDto = summaryApiServiceImpl.updateSummaries(summaryRequestDtoList);
    SummaryResponseDto summaryResponseDto = summaryApiServiceImpl.getSummaryByFileId(fileId);

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

    // The JAX-RS container injects HttpServletRequest via @Context. In unit tests we must do it ourselves,
    // otherwise handleException -> getCallDetails() throws a NullPointerException
    HttpServletRequest httpServletRequestMock = mock(HttpServletRequest.class);
    HttpServletMapping httpServletMappingMock = mock(HttpServletMapping.class);

    when(httpServletRequestMock.getMethod()).thenReturn("GET");
    when(httpServletRequestMock.getContextPath()).thenReturn("/ds-storage");
    when(httpServletRequestMock.getHttpServletMapping()).thenReturn(httpServletMappingMock);
    when(httpServletMappingMock.getMatchValue()).thenReturn("v1");
    when(httpServletRequestMock.getPathInfo()).thenReturn("/summary");
    when(httpServletRequestMock.getParameterMap()).thenReturn(Map.of());
    when(httpServletRequestMock.getHeaders("Accept")).thenReturn(Collections.emptyEnumeration());

    // Anonymous subclass, so we can set the protected field from ImplBase without reflection
    summaryApiServiceImpl = new SummaryApiServiceImpl() {
      {
        httpServletRequest = httpServletRequestMock;
      }
    };

    // Act
    NotFoundServiceException exception = assertThrows(NotFoundServiceException.class,
        () -> summaryApiServiceImpl.getSummaryByFileId(fileId));

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
    firstSummaryRequestDto.setSummary("Fin beskrivelse.");
    firstSummaryRequestDto.setCreated(firstCreated);
    firstSummaryRequestDto.setJobId("test run 1");

    SummaryRequestDto secondSummaryRequestDto = new SummaryRequestDto();
    secondSummaryRequestDto.setId(UUID.randomUUID());
    secondSummaryRequestDto.setFileId(UUID.randomUUID());
    secondSummaryRequestDto.setSummary("Fin beskrivelse.");
    secondSummaryRequestDto.setCreated(secondCreated);
    secondSummaryRequestDto.setJobId("test run 2");

    List<SummaryRequestDto> summaryRequestDtoList =
        List.of(firstSummaryRequestDto, secondSummaryRequestDto);

    RecordsCountDto recordsCountDto = summaryApiServiceImpl.updateSummaries(summaryRequestDtoList);

    // Act
    CreatedDto createdDto = summaryApiServiceImpl.latestCreatedSummary();

    // Assert
    assertNotNull(createdDto);
    assertEquals(secondCreated, createdDto.getCreated());
  }

  @Test
  public void latestCreatedSummary_whenTableIsEmpty_thenReturnCreatedDtoWithNullCreated() {
    // Act
    CreatedDto createdDto = summaryApiServiceImpl.latestCreatedSummary();

    // Assert
    assertNotNull(createdDto);
    assertNull(createdDto.getCreated());
  }
}
