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
        // validateId() has return type void, so we can only check that it did not throw exception
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
    public void validateString_whenValidString_thenDoNotThrow() {
        // Arrange
        String inputString = "2.04";
        InputValidator inputValidator = mock(InputValidator.class);
        doCallRealMethod().when(inputValidator).validateString(inputString, "inputString");

        // Act and assert
        // validateString() has return type void, so we can only check that it did not throw exception
        assertDoesNotThrow(() -> inputValidator.validateString(inputString, "inputString"));

        // and it only was called once
        verify(inputValidator, times(1)).validateString(inputString, "inputString");
    }

    @Test
    public void validateString_whenNullString_thenThrowInvalidArgumentServiceException() {
        // Arrange
        String expectedMessage = "'inputString' cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateString(null, "inputString"));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " "
    })
    public void validateString_whenEmptyOrBlankString_thenThrowInvalidArgumentServiceException(String inputString) {
        // Arrange
        String expectedMessage = "'inputString' cannot be empty";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateString(inputString, "inputString"));

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
    public void validateOwnProductionCode_whenValidOwnProductionCode_thenDoNotThrow() {
        // Arrange
        String ownProductionCode = "1234";
        InputValidator inputValidator = mock(InputValidator.class);
        doCallRealMethod().when(inputValidator).validateOwnProductionCode(ownProductionCode);

        // Act and assert
        // validateOwnProductionCode() has return type void, so we can only check that it did not throw exception
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
    public void validateInteger_whenValidInteger_thenDoNotThrow() {
        // Arrange
        Integer inputInteger = 1;
        InputValidator inputValidator = mock(InputValidator.class);
        doCallRealMethod().when(inputValidator).validateInteger(inputInteger, "inputInteger");

        // Act and assert
        // validateInteger() has return type void, so we can only check that it did not throw exception
        assertDoesNotThrow(() -> inputValidator.validateInteger(inputInteger, "inputInteger"));

        // and it only was called once
        verify(inputValidator, times(1)).validateInteger(inputInteger, "inputInteger");
    }

    @Test
    public void validateInteger_whenNullInteger_thenThrowInvalidArgumentServiceException() {
        // Arrange
        String expectedMessage = "'inputInteger' cannot be null";
        InputValidator inputValidator = new InputValidator();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> inputValidator.validateInteger(null, "inputInteger"));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }
}
