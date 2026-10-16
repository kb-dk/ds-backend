package dk.kb.storage.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dk.kb.storage.model.v1.TranscriptionDto;
import dk.kb.storage.storage.TranscriptionStorageForUnitTest;
import dk.kb.storage.util.TestcontainersUtil;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TranscriptionFacadeTest extends TestcontainersUtil {

  private static TranscriptionStorageForUnitTest transcriptionStorage = null;
  private final String fileId = "a3332323-3323233-333333";
  private final String fileName = "a3332323-3323233-333333.mp3";
  private final String transcription = "This is linie1. This is linie2";
  private final String transcriptionLines =
      "00:00 - 10:00This is linie1.\n10:00 - 20:00  This is linie2";

  @BeforeAll
  public static void beforeClass() throws Exception {
    setupDatabaseForClass(MethodHandles.lookup().lookupClass());
    transcriptionStorage = new TranscriptionStorageForUnitTest();
  }

  /**
   * Delete all records between each unittest. The clearTableRecords is only called from here. The
   * facade class is responsible for committing transactions. So clean up between unittests.
   */
  @BeforeEach
  public void beforeEach() throws SQLException {
    transcriptionStorage.clearTableRecords();
  }

  @Test
  public void getTranscriptionByFileId_whenFileIdExists_thenReturnTranscription() {
    // Arrange
    TranscriptionDto transcriptionDto = new TranscriptionDto();
    transcriptionDto.setFileId(fileId);
    transcriptionDto.setFileName(fileName);
    transcriptionDto.setTranscription(transcription);
    transcriptionDto.setTranscriptionLines(transcriptionLines);

    TranscriptionFacade.createOrUpdateTranscription(transcriptionDto);

    // Act
    TranscriptionDto returnedTranscriptionDto =
        TranscriptionFacade.getTranscriptionByFileId(fileId);

    // Assert
    assertNotNull(returnedTranscriptionDto);
    assertEquals(fileId, returnedTranscriptionDto.getFileId());
    assertEquals(fileName, returnedTranscriptionDto.getFileName());
    assertTrue(returnedTranscriptionDto.getmTime() > 0);
    assertEquals(transcription, returnedTranscriptionDto.getTranscription());
    assertEquals(transcriptionLines, returnedTranscriptionDto.getTranscriptionLines());
  }

  @Test
  public void getTranscriptionByFileId_whenFileIdDoNotExists_thenThrowNotFoundServiceException() {
    // Act
    NotFoundServiceException exception = assertThrows(NotFoundServiceException.class,
        () -> TranscriptionFacade.getTranscriptionByFileId(fileId));

    // Assert
    String errorMessage = "No transcription found for fileId '" + fileId + "'";
    assertEquals(errorMessage, exception.getMessage());
  }
}
