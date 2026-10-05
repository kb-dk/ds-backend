package dk.kb.storage.storage;

import dk.kb.storage.model.v1.TranscriptionDto;
import dk.kb.storage.util.TestcontainersUtil;
import java.lang.invoke.MethodHandles;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TranscriptionStorageTest extends TestcontainersUtil {

    private static TranscriptionStorageForUnitTest transcriptionStorage = null;

    private final String fileId = "a3332323-3323233-333333";
    private final String fileName = "a3332323-3323233-333333.mp3";
    private final String transcription = "This is linie1. This is linie2";
    private final String transcriptionLines = "00:00 - 10:00This is linie1.\n10:00 - 20:00  This is linie2";

    @BeforeAll
    public static void beforeClass() throws Exception {
        setupDatabaseForClass(MethodHandles.lookup().lookupClass());
        transcriptionStorage = new TranscriptionStorageForUnitTest();
    }

    /**
     * Delete all records between each unittest. The clearTableRecords is only called from here.
     * The facade class is responsible for committing transactions. So clean up between unittests.
     */
    @BeforeEach
    public void beforeEach() throws SQLException {
        transcriptionStorage.clearTableRecords();
    }

    @Test
    public void createTranscription_whenNewTranscription_thenCountIsOne() throws Exception {
        // Arrange
        TranscriptionDto transcriptionDto = new TranscriptionDto();
        transcriptionDto.setFileId(fileId);
        transcriptionDto.setFileName(fileName);
        transcriptionDto.setTranscription(transcription);
        transcriptionDto.setTranscriptionLines(transcriptionLines);

        // Act
        transcriptionStorage.createTranscription(transcriptionDto);

        int count = transcriptionStorage.countTranscriptionByFileId(fileId);

        // Assert
        assertEquals(1, count);
    }

    @Test
    public void getTranscriptionByFileId_whenFileIdExists_thenReturnTranscription() throws Exception {
        // Arrange
        TranscriptionDto transcriptionDto = new TranscriptionDto();
        transcriptionDto.setFileId(fileId);
        transcriptionDto.setFileName(fileName);
        transcriptionDto.setTranscription(transcription);
        transcriptionDto.setTranscriptionLines(transcriptionLines);

        transcriptionStorage.createTranscription(transcriptionDto);

        // Act
        TranscriptionDto returnedTranscriptionDto = transcriptionStorage.getTranscriptionByFileId(fileId);

        // Assert
        assertNotNull(returnedTranscriptionDto);
        assertEquals(fileId, returnedTranscriptionDto.getFileId());
        assertEquals(fileName, returnedTranscriptionDto.getFileName());
        assertTrue(returnedTranscriptionDto.getmTime() > 0);
        assertEquals(transcription, returnedTranscriptionDto.getTranscription());
        assertEquals(transcriptionLines, returnedTranscriptionDto.getTranscriptionLines());
    }

    @Test
    public void getTranscriptionByFileId_whenFileIdDoNotExists_thenReturnTranscription() throws SQLException {
        // Act
        TranscriptionDto returnedTranscriptionDto = transcriptionStorage.getTranscriptionByFileId(fileId);

        // Assert
        assertNotNull(returnedTranscriptionDto);
        assertEquals(fileId, returnedTranscriptionDto.getFileId());
        assertNull(returnedTranscriptionDto.getFileName());
        assertNull(returnedTranscriptionDto.getmTime());
        assertNull(returnedTranscriptionDto.getTranscription());
        assertNull(returnedTranscriptionDto.getTranscriptionLines());
    }

    @Test
    public void deleteTranscriptionByFileId_whenFileIdExists_thenNoTranscriptionIsReturned() throws Exception {
        // Arrange
        TranscriptionDto transcriptionDto = new TranscriptionDto();
        transcriptionDto.setFileId(fileId);
        transcriptionDto.setFileName(fileName);
        transcriptionDto.setTranscription(transcription);
        transcriptionDto.setTranscriptionLines(transcriptionLines);

        transcriptionStorage.createTranscription(transcriptionDto);

        // Act
        transcriptionStorage.deleteTranscriptionByFileId(fileId);

        int count = transcriptionStorage.countTranscriptionByFileId(fileId);

        TranscriptionDto deletedTranscriptionDto = transcriptionStorage.getTranscriptionByFileId(fileId);

        // Assert
        assertEquals(0, count);

        assertNull(deletedTranscriptionDto.getTranscription());
    }

    @Test
    public void deleteTranscriptionByFileId_whenFileIdDoNotExists_thenNoTranscriptionIsReturned() throws Exception {
        // Act
        transcriptionStorage.deleteTranscriptionByFileId(fileId);

        int count = transcriptionStorage.countTranscriptionByFileId(fileId);

        TranscriptionDto deletedTranscriptionDto = transcriptionStorage.getTranscriptionByFileId(fileId);

        // Assert
        assertEquals(0, count);

        assertNull(deletedTranscriptionDto.getTranscription());
    }
}
