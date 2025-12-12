package dk.kb.license.validation;

import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class InputValidatorTest {

    @Test
    public void validateId_whenValidId_thenDoNotThrow() {
        // Arrange
        Long id = 12345678901L;
        InputValidator inputValidator = mock(InputValidator.class);
        doCallRealMethod().when(inputValidator).validateId(id);

        // Act and assert
        // validateDrProductionIdFormat() has return type void, so we can only check that it did not throw exception
        assertDoesNotThrow(() -> inputValidator.validateId(id));

        // and it only was called once
        verify(inputValidator, times(1)).validateId(id);
    }

    @Test
    public void validateId_whenTooShortId_thenThrowInvalidArgumentServiceException() {
        // Arrange
        Long id = 1234567890L;
        String expectedMessage = "'id': " + id + " should be at least 11 digits";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateId(id));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

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
            "1",
            "0bdf8656-4a96-400d-b3d8-e4695328688e",
            ":0bdf8656-4a96-400d-b3d8-e4695328688e",
            "ds.tv:oai:io0bdf8656-4a96-400d-b3d8-e4695328688e",
            "\"ds.tv:oai:io:0bdf8656-4a96-400d-b3d8-e4695328688e\""
    })
    public void validateDsId_whenInvalidDsId_thenThrowInvalidArgumentServiceException(String dsId) {
        // Arrange
        String expectedMessage = "Invalid 'dsId': " + dsId;
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateDsId(dsId));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateDsId_whenNullDsId_thenThrowInvalidArgumentServiceException() {
        // Arrange
        String expectedMessage = "'dsId' cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateDsId(null));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " "
    })
    public void validateDsId_whenEmptyOrBlankDsId_thenThrowInvalidArgumentServiceException(String dsId) {
        // Arrange
        String expectedMessage = "'dsId' cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateDsId(dsId));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateKey_whenValidKey_thenDoNotThrow() {
        // Arrange
        String key = "2.04";
        InputValidator inputValidator = mock(InputValidator.class);
        doCallRealMethod().when(inputValidator).validateKey(key);

        // Act and assert
        // validateDrProductionIdFormat() has return type void, so we can only check that it did not throw exception
        assertDoesNotThrow(() -> inputValidator.validateKey(key));

        // and it only was called once
        verify(inputValidator, times(1)).validateKey(key);
    }

    @Test
    public void validateKey_whenNullKey_thenThrowInvalidArgumentServiceException() {
        // Arrange
        String expectedMessage = "'key' cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateKey(null));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " "
    })
    public void validateKey_whenEmptyOrBlankKey_thenThrowInvalidArgumentServiceException(String key) {
        // Arrange
        String expectedMessage = "'key' cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateKey(key));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateDrProductionIdFormat_whenValidDrProductionId_thenDoNotThrow() {
        // Arrange
        String productionId = "12345678";
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
        String expectedMessage = "'drProductionId' cannot be empty";
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
        String expectedMessage = "'drProductionId' cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateDrProductionIdFormat(drProductionId));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateDrProductionIdFormat_whenTooShortDrProductionId_thenThrowInvalidArgumentServiceException() {
        // Arrange
        String drProductionId = "1234567";
        String expectedMessage = "'drProductionId': " + drProductionId + " should be at least 8 digits";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateDrProductionIdFormat(drProductionId));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateDrProductionIdFormat_whenInvalidDrProductionId_thenThrowInvalidArgumentServiceException() {
        // Arrange
        String drProductionId = "12345abcde";
        String expectedMessage = "'drProductionId': " + drProductionId + " should only contain digits";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateDrProductionIdFormat(drProductionId));

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
        String expectedMessage = "'strictTitle' cannot be empty";
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
        String expectedMessage = "'strictTitle' cannot be empty";
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
        String expectedMessage = "'ownProductionCode' cannot be empty";
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
        String expectedMessage = "'ownProductionCode' cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateOwnProductionCode(ownProductionCode));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateOwnProductionCode_whenInvalidOwnProductionCode_thenThrowInvalidArgumentServiceException() {
        // Arrange
        String invalidOwnProductionCode = "12ab";
        String expectedMessage = "'ownProductionCode': " + invalidOwnProductionCode + " should only contain digits";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateOwnProductionCode(invalidOwnProductionCode));

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
        String expectedMessage = "'title' cannot be empty";
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
        String expectedMessage = "'title' cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateTitle(title));

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
        String expectedMessage = "'comment' cannot be empty";
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
        String expectedMessage = "'comment' cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateComment(comment));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void validateChangeComment_whenValidChangeComment_thenDoNotThrow() {
        // Arrange
        String changeComment = "DR holdback ranges skal ikke længere bruges";
        InputValidator inputValidator = mock(InputValidator.class);
        doCallRealMethod().when(inputValidator).validateChangeComment(changeComment);

        // Act and assert
        // validateDrProductionIdFormat() has return type void, so we can only check that it did not throw exception
        assertDoesNotThrow(() -> inputValidator.validateChangeComment(changeComment));

        // and it only was called once
        verify(inputValidator, times(1)).validateChangeComment(changeComment);
    }

    @Test
    public void validateChangeComment_whenNullChangeComment_thenThrowInvalidArgumentServiceException() {
        // Arrange
        String expectedMessage = "'changeComment' cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateChangeComment(null));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " "
    })
    public void validateChangeComment_whenEmptyOrBlankChangeComment_thenThrowInvalidArgumentServiceException(String changeComment) {
        // Arrange
        String expectedMessage = "'changeComment' cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateChangeComment(changeComment));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }
}
