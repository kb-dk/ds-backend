package dk.kb.license.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InputValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "ds.tv:oai:io:",
            "ds.radio:oai:io:",
            "ds.tv:oai:io:0bdf8656-4a96-400d-b3d8-e4695328688",
            "ds.tv:oai:io:0bdf8656-4a96-400d-b3d8-e4695328688e",
    })
    public void validateDsId_WhenGivenValidDsId_ReturnTrue(String dsId) {
        InputValidator validator = new InputValidator();

        boolean actualValidatedDsId = validator.validateDsId(dsId);
        assertTrue(actualValidatedDsId);
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
    public void validateDsId_WhenGivenInvalidDsId_ReturnFalse(String dsId) {
        InputValidator validator = new InputValidator();

        boolean actualValidatedDsId = validator.validateDsId(dsId);
        assertFalse(actualValidatedDsId);
    }

    @Test
    public void validateDsId_WhenGivenNull_ReturnFalse() {
        InputValidator validator = new InputValidator();

        boolean actualValidatedDsId = validator.validateDsId(null);
        assertFalse(actualValidatedDsId);
    }
}
