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
    public void validateDsId_whenValidDsId_thenDoNotThrow(String dsId) {
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
            " ",
            "1",
            "0bdf8656-4a96-400d-b3d8-e4695328688e",
            ":0bdf8656-4a96-400d-b3d8-e4695328688e",
            "ds.tv:oai:io0bdf8656-4a96-400d-b3d8-e4695328688e",
            "\"ds.tv:oai:io:0bdf8656-4a96-400d-b3d8-e4695328688e\""
    })
    public void validateDsId_whenInvalidDsId_thenThrowInvalidArgumentServiceException(String dsId) {
        // Arrange
        String expectedMessage = "Invalid dsId: " + dsId;
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateDsId(dsId));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateDsId_whenNullDsId_thenThrowInvalidArgumentServiceException() {
        // Arrange
        String expectedMessage = "Invalid dsId: " + null;
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateDsId(null));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateDrProductionIdFormat_whenValidDrProductionId_thenDoNotThrow() {
        // Arrange
        String productionId = "1234567890";
        InputValidator inputValidator = mock(InputValidator.class);
        doCallRealMethod().when(inputValidator).validateDrProductionIdFormat(productionId);

        // Act and assert
        // validateDrProductionIdFormat() has return type void, so we can only check that it did not throw exception
        assertDoesNotThrow(() -> inputValidator.validateDrProductionIdFormat(productionId));

        // and it only was called once
        verify(inputValidator, times(1)).validateDrProductionIdFormat(productionId);
    }

    @Test
    public void validateDrProductionIdFormat_whenNullDrProductionId_thenThrowInvalidArgumentServiceException() {
        // Arrange
        String drProductionId = null;
        String expectedMessage = "The drProductionId cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateDrProductionIdFormat(drProductionId));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " "
    })
    public void validateDrProductionIdFormat_whenEmptyOrBlankDrProductionId_thenThrowInvalidArgumentServiceException(String drProductionId) {
        // Arrange
        String expectedMessage = "The drProductionId cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateDrProductionIdFormat(drProductionId));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateDrProductionIdFormat_whenTooShortDrProductionId_thenThrowInvalidArgumentServiceException() {
        // Arrange
        String drProductionId = "12345";
        String expectedMessage = "The drProductionId: 12345 should be at least 8 digits";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateDrProductionIdFormat(drProductionId));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateDrProductionIdFormat_whenInvalidDrProductionId_thenThrowInvalidArgumentServiceException() {
        // Arrange
        String productionId = "12345abcde";
        String expectedMessage = "The drProductionId: 12345abcde should only contain digits";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateDrProductionIdFormat(productionId));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateStrictTitle_whenValidStrictTitle_thenDoNotThrow() {
        // Arrange
        String strictTitle = "Indefra";
        InputValidator inputValidator = mock(InputValidator.class);
        doCallRealMethod().when(inputValidator).validateStrictTitle(strictTitle);

        // Act and assert
        // validateStrictTitle() has return type void, so we can only check that it did not throw exception
        assertDoesNotThrow(() -> inputValidator.validateStrictTitle(strictTitle));

        // and it only was called once
        verify(inputValidator, times(1)).validateStrictTitle(strictTitle);
    }

    @Test
    public void validateStrictTitle_whenNullStrictTitle_thenThrowInvalidArgumentServiceException() {
        // Arrange
        String expectedMessage = "The strictTitle cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateStrictTitle(null));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " "
    })
    public void validateStrictTitle_whenEmptyOrBlankStrictTitle_thenThrowInvalidArgumentServiceException(String strictTitle) {
        // Arrange
        String expectedMessage = "The strictTitle cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateStrictTitle(strictTitle));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateOwnProductionCode_whenValidOwnProductionCode_thenDoNotThrow() {
        // Arrange
        String ownProductionCode = "1234";
        InputValidator inputValidator = mock(InputValidator.class);
        doCallRealMethod().when(inputValidator).validateOwnProductionCode(ownProductionCode);

        // Act and assert
        // validateStrictTitle() has return type void, so we can only check that it did not throw exception
        assertDoesNotThrow(() -> inputValidator.validateOwnProductionCode(ownProductionCode));

        // and it only was called once
        verify(inputValidator, times(1)).validateOwnProductionCode(ownProductionCode);
    }

    @Test
    public void validateOwnProductionCode_whenNullOwnProductionCode_thenThrowInvalidArgumentServiceException() {
        // Arrange
        String expectedMessage = "The ownProductionCode cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateOwnProductionCode(null));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " "
    })
    public void validateOwnProductionCode_whenEmptyOrBlankOwnProductionCode_thenThrowInvalidArgumentServiceException(String ownProductionCode) {
        // Arrange
        String expectedMessage = "The ownProductionCode cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateOwnProductionCode(ownProductionCode));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateOwnProductionCode_whenInvalidOwnProductionCode_thenThrowInvalidArgumentServiceException() {
        // Arrange
        String productionId = "12ab";
        String expectedMessage = "The ownProductionCode: 12ab should only contain digits";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateOwnProductionCode(productionId));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateIdValueLength_whenTooLongIdValue_thenThrowInvalidArgumentServiceException() {
        // Arrange
        // Use String repeat() method to create a long comment
        String longComment = "x".repeat(257);
        String expectedMessage = "idValue was too long and cannot be added to rights module. Only 256 characters are allowed";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateIdValueLength(longComment));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateTitle_whenValidTitle_thenDoNotThrow() {
        // Arrange
        String title = "Test title";
        InputValidator inputValidator = mock(InputValidator.class);
        doCallRealMethod().when(inputValidator).validateTitle(title);

        // Act and assert
        // validateComment() has return type void, so we can only check that it did not throw exception
        assertDoesNotThrow(() -> inputValidator.validateTitle(title));

        // and it only was called once
        verify(inputValidator, times(1)).validateTitle(title);
    }

    @Test
    public void validateTitle_whenNullTitle_thenThrowInvalidArgumentServiceException() {
        // Arrange
        String expectedMessage = "Title cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateTitle(null));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " "
    })
    public void validateTitle_whenEmptyOrBlankTitle_thenThrowInvalidArgumentServiceException(String title) {
        // Arrange
        String expectedMessage = "Title cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateTitle(title));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateTitle_whenTooLongTitle_thenThrowInvalidArgumentServiceException() {
        // Arrange
        // Use String repeat() method to create a long comment
        String longTitle = "x".repeat(4097);
        String expectedMessage = "Title was too long and cannot be added to rights module. Only 4096 characters are allowed";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateTitle(longTitle));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateComment_whenValidComment_thenDoNotThrow() {
        // Arrange
        String comment = "Here is some text";
        InputValidator inputValidator = mock(InputValidator.class);
        doCallRealMethod().when(inputValidator).validateComment(comment);

        // Act and assert
        // validateComment() has return type void, so we can only check that it did not throw exception
        assertDoesNotThrow(() -> inputValidator.validateComment(comment));

        // and it only was called once
        verify(inputValidator, times(1)).validateComment(comment);
    }

    @Test
    public void validateComment_whenNullComment_thenThrowInvalidArgumentServiceException() {
        // Arrange
        String expectedMessage = "Comment cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateComment(null));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " "
    })
    public void validateComment_whenEmptyOrBlankComment_thenThrowInvalidArgumentServiceException(String comment) {
        // Arrange
        String expectedMessage = "Comment cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateComment(comment));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateComment_whenTooLongComment_thenThrowInvalidArgumentServiceException() {
        // Arrange
        // Use String repeat() method to create a long comment
        String longComment = "x".repeat(16385);
        String expectedMessage = "Comment was too long and cannot be added to rights module. Only 16384 characters are allowed";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateComment(longComment));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }
}
