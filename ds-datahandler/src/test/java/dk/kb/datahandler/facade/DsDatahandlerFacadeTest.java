package dk.kb.datahandler.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.model.v1.CategoryDto;
import dk.kb.datahandler.model.v1.JobDto;
import dk.kb.datahandler.model.v1.JobStatusDto;
import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.model.v1.RecordsCountDto;
import dk.kb.datahandler.model.v1.TypeDto;
import dk.kb.datahandler.storage.BaseModuleStorage;
import dk.kb.datahandler.storage.JobStorage;
import dk.kb.datahandler.storage.JobStorageForUnitTests;
import dk.kb.datahandler.storage.RerunClusterStorage;
import dk.kb.datahandler.util.TestcontainersUtil;
import dk.kb.datahandler.webservice.KBAuthorizationInterceptor;
import dk.kb.storage.model.v1.RerunClusterRequestDto;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.exception.InternalServiceException;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.MessageImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.AccessToken;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class DsDatahandlerFacadeTest extends TestcontainersUtil {
  private static JobStorageForUnitTests jobStorage = null;

  @BeforeAll
  public static void beforeClass() throws Exception {
    setupDatabaseForClass(MethodHandles.lookup().lookupClass());
    jobStorage = new JobStorageForUnitTests();
  }

  @BeforeEach
  public void beforeEach() throws SQLException {
    jobStorage.clearTables();
  }

  @Test
  public void updateRerunClusters_whenGivenRerunClusterRequestDtoList_thenReturnHowManyRowsWasInsertedOrUpdated() {
    // Arrange
    dk.kb.storage.model.v1.CreatedDto createdDto = new dk.kb.storage.model.v1.CreatedDto();
    createdDto.setCreated(OffsetDateTime.parse("2026-03-20T00:00:00.001Z"));

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

    String username = "unittest";
    Integer count = 1;
    dk.kb.storage.model.v1.RecordsCountDto recordsCountDto =
        new dk.kb.storage.model.v1.RecordsCountDto();
    recordsCountDto.setCount(count);

    DsStorageClient dsStorageClient = Mockito.mock(DsStorageClient.class);

    MessageImpl message = new MessageImpl();
    AccessToken mockedToken = mock(AccessToken.class);
    when(mockedToken.getName()).thenReturn(username);
    message.put(KBAuthorizationInterceptor.ACCESS_TOKEN, mockedToken);

    try (MockedStatic<JAXRSUtils> mockedJAXRSUtils = mockStatic(JAXRSUtils.class)) {
      mockedJAXRSUtils.when(JAXRSUtils::getCurrentMessage).thenReturn(message);
      try (MockedStatic<DsDatahandlerFacade> mockedDsDatahandlerFacade = mockStatic(
          DsDatahandlerFacade.class, Mockito.CALLS_REAL_METHODS)) {
        mockedDsDatahandlerFacade.when(DsDatahandlerFacade::getDsStorageApiClient)
            .thenReturn(dsStorageClient);

        try (MockedConstruction<RerunClusterStorage> mockedConstruction = Mockito.mockConstruction(
                RerunClusterStorage.class, (mock, context) ->
                    Mockito.when(mock.getRerunClusters(any()))
                        .thenReturn(rerunClusterRequestDtoList))) {

          Mockito.when(dsStorageClient.latestCreated()).thenReturn(createdDto);
          Mockito.when(dsStorageClient.updateRerunClusters(rerunClusterRequestDtoList))
              .thenReturn(recordsCountDto);

          // Act
          RecordsCountDto returnedRecordsCountDto = DsDatahandlerFacade.getRerunClusters();

          // Assert
          assertNotNull(returnedRecordsCountDto);
          assertEquals(count, returnedRecordsCountDto.getCount());

          List<JobDto> actualJobDtoList = DsDatahandlerFacade.getJobs(null, null);
          assertEquals(1, actualJobDtoList.size());

          JobDto returnedJobDto = actualJobDtoList.get(0);

          assertNotNull(returnedJobDto.getId());
          assertEquals(TypeDto.DELTA, returnedJobDto.getType());
          assertEquals(CategoryDto.RERUN_CLUSTERS, returnedJobDto.getCategory());
          assertNull(returnedJobDto.getSource());
          assertEquals(username, returnedJobDto.getCreatedBy());
          assertEquals(JobStatusDto.COMPLETED, returnedJobDto.getJobStatus());
          assertNull(returnedJobDto.getErrorCorrelationId());
          assertNull(returnedJobDto.getMessage());
          assertNull(returnedJobDto.getModifiedTimeFrom());

          assertNotNull(returnedJobDto.getStartTime());
          assertEquals(OffsetDateTime.class, returnedJobDto.getStartTime().getClass());
          assertNotNull(returnedJobDto.getEndTime());
          assertEquals(OffsetDateTime.class, returnedJobDto.getEndTime().getClass());
          assertEquals(count, returnedJobDto.getNumberOfRecords());
          assertNull(returnedJobDto.getRestartValue());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  @Test
  public void getJobs_whenJobExistInTable_thenReturnListOfAllJobs() {
    // Arrange
    OaiTargetDto oaiTarget = ServiceConfig.getOaiTargets().get("ds.radiotv");

    JobDto jobDto = new JobDto();
    jobDto.setType(TypeDto.DELTA);
    jobDto.category(CategoryDto.OAI_HARVEST);
    jobDto.setSource(oaiTarget.getName());
    jobDto.setCreatedBy("Unit test");
    jobDto.setJobStatus(JobStatusDto.RUNNING);
    jobDto.setStartTime(OffsetDateTime.now(ZoneOffset.UTC));

    BaseModuleStorage.performStorageAction("Create job for OAITest", JobStorage.class, storage -> {
      ((JobStorage) storage).createJob(jobDto);
      return null;
    });

    // Act
    // List of all jobs saved in database
    List<JobDto> actualJobDtoList = DsDatahandlerFacade.getJobs(null, null);

    // Assert
    assertEquals(1, actualJobDtoList.size());

    JobDto returnedJobDto = actualJobDtoList.get(0);

    assertNotNull(returnedJobDto.getId());
    assertEquals(jobDto.getType(), returnedJobDto.getType());
    assertEquals(jobDto.getCategory(), returnedJobDto.getCategory());
    assertEquals(jobDto.getSource(), returnedJobDto.getSource());
    assertEquals(jobDto.getCreatedBy(), returnedJobDto.getCreatedBy());
    assertEquals(jobDto.getJobStatus(), returnedJobDto.getJobStatus());
    assertNull(returnedJobDto.getErrorCorrelationId());
    assertNull(returnedJobDto.getMessage());
    assertNull(returnedJobDto.getModifiedTimeFrom());

    assertNotNull(returnedJobDto.getStartTime());
    // Assert that result is 'close enough'
    assertTrue(
        Duration.between(jobDto.getStartTime(), returnedJobDto.getStartTime()).toSeconds() <= 0);

    assertNull(returnedJobDto.getEndTime());
    assertNull(returnedJobDto.getNumberOfRecords());
    assertNull(returnedJobDto.getRestartValue());
  }

  /**
   * Can only have one job with the same name running at the same time even if one is a delta job
   * and the other is a full job
   */
  @Test
  public void startJob_whenThereIsAlreadyRunningJobWithTheSameName_thenThrowInvalidArgumentServiceException() {
    // Arrange
    String user = "Unit test user";
    JobDto jobDto = new JobDto();

    jobDto.setType(TypeDto.DELTA);
    jobDto.category(CategoryDto.KALTURA_UPLOAD);
    jobDto.setSource(null);
    jobDto.setCreatedBy(user);
    jobDto.setJobStatus(JobStatusDto.RUNNING);
    jobDto.setStartTime(OffsetDateTime.now(ZoneOffset.UTC));

    String expectedMessage =
        "dk.kb.util.webservice.exception.InvalidArgumentServiceException: dk.kb.util.webservice.exception.InvalidArgumentServiceException: There is already a/an kaltura upload job running";

    BaseModuleStorage.performStorageAction("Create job for kaltura upload test", JobStorage.class,
        storage -> {
          ((JobStorage) storage).createJob(jobDto);
          return null;
        });

    // Act/Assert
    Exception exception = Assertions.assertThrows(
        InternalServiceException.class,
        () -> DsDatahandlerFacade.kalturaDeltaUpload()
    );

    Assertions.assertEquals(expectedMessage, exception.getMessage());
  }
}
