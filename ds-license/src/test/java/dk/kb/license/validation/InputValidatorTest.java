package dk.kb.license.validation;

import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class InputValidatorTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "ds.tv:oai:io:",
            "ds.radio:oai:io:",
            "ds.tv:oai:io:0bdf8656-4a96-400d-b3d8-e4695328688",
            "ds.tv:oai:io:0bdf8656-4a96-400d-b3d8-e4695328688e"
    })
    public void validateDsId_WhenGivenValidDsId_DoNotThrow(String dsId) {
        // Arrange
        InputValidator inputValidator = mock(InputValidator.class);
        doCallRealMethod().when(inputValidator).validateDsId(dsId);

        // Act and assert
        // validateDsId() has return type void, so we can only check that it did not throw exception
        assertDoesNotThrow(() -> inputValidator.validateDsId(dsId));

        // and it only was called once
        verify(inputValidator, times(1)).validateDsId(dsId);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "1",
            "0bdf8656-4a96-400d-b3d8-e4695328688e",
            ":0bdf8656-4a96-400d-b3d8-e4695328688e",
            "ds.tv:oai:io0bdf8656-4a96-400d-b3d8-e4695328688e",
            "\"ds.tv:oai:io:0bdf8656-4a96-400d-b3d8-e4695328688e\""
    })
    public void validateDsId_WhenGivenInvalidDsId_DoThrow(String dsId) {
        // Arrange
        String expectedMessage = "Invalid dsId";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateDsId(dsId));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateDsId_WhenGivenNull_DoThrow() {
        // Arrange
        String expectedMessage = "Invalid dsId";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateDsId(null));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateCommentLength_WhenGivenNull_DoNotThrow() {
        // Arrange
        InputValidator inputValidator = mock(InputValidator.class);
        doCallRealMethod().when(inputValidator).validateCommentLength(null);

        // Act and assert
        // validateCommentLength() has return type void, so we can only check that it did not throw exception
        assertDoesNotThrow(() -> inputValidator.validateCommentLength(null));

        // and it only was called once
        verify(inputValidator, times(1)).validateCommentLength(null);
    }

    @Test
    public void validateCommentLength_WhenGivenEmpty_DoNotThrow() {
        // Arrange
        String comment = "";
        InputValidator inputValidator = mock(InputValidator.class);
        doCallRealMethod().when(inputValidator).validateCommentLength(comment);

        // Act and assert
        // validateCommentLength() has return type void, so we can only check that it did not throw exception
        assertDoesNotThrow(() -> inputValidator.validateCommentLength(comment));

        // and it only was called once
        verify(inputValidator, times(1)).validateCommentLength(comment);
    }

    @Test
    public void validateCommentLength_WhenGivenValidComment_DoNotThrow() {
        // Arrange
        String comment = "Here is some text";
        InputValidator inputValidator = mock(InputValidator.class);
        doCallRealMethod().when(inputValidator).validateCommentLength(comment);

        // Act and assert
        // validateCommentLength() has return type void, so we can only check that it did not throw exception
        assertDoesNotThrow(() -> inputValidator.validateCommentLength(comment));

        // and it only was called once
        verify(inputValidator, times(1)).validateCommentLength(comment);
    }

    @Test
    public void validateCommentLength_WhenGivenTooLongComment_DoThrow() {
        // Arrange
        // Use String repeat() method to create a long comment
        String longComment = "x".repeat(16349);
        String expectedMessage = "Comment was too long and cannot be added to rights module. Only 16348 characters are allowed.";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateCommentLength(longComment));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateDrProductionIdFormat_WhenGivenValidProductionId_DoNotThrow() {
        // Arrange
        String  productionId = "1234567890";
        InputValidator inputValidator = mock(InputValidator.class);
        doCallRealMethod().when(inputValidator).validateDrProductionIdFormat(productionId);

        // Act and assert
        // validateDrProductionIdFormat() has return type void, so we can only check that it did not throw exception
        assertDoesNotThrow(() -> inputValidator.validateDrProductionIdFormat(productionId));

        // and it only was called once
        verify(inputValidator, times(1)).validateDrProductionIdFormat(productionId);
    }

    @Test
    public void validateDrProductionIdFormat_WhenGivenNull_DoThrow() {
        // Arrange
        String expectedMessage = "The input production ID must not be null";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateDrProductionIdFormat(null));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateDrProductionIdFormat_WhenGivenEmpty_DoThrow() {
        // Arrange
        String productionId = "";
        String expectedMessage = "The input production ID: '' should only contain digits";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateDrProductionIdFormat(productionId));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateDrProductionIdFormat_WhenGivenToShortProductionId_DoThrow() {
        // Arrange
        String productionId = "12345";
        String expectedMessage = "The input production ID: '12345' should be at least 8 digits";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateDrProductionIdFormat(productionId));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateDrProductionIdFormat_WhenGivenToInvalidProductionId_DoThrow() {
        // Arrange
        String productionId = "12345abcde";
        String expectedMessage = "The input production ID: '12345abcde' should only contain digits";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateDrProductionIdFormat(productionId));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }
}
