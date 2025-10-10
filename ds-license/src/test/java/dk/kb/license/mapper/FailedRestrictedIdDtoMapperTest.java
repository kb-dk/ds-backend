package dk.kb.license.mapper;

import dk.kb.license.model.v1.FailedRestrictedIdDto;
import dk.kb.license.model.v1.IdTypeEnumDto;
import dk.kb.license.model.v1.PlatformEnumDto;
import dk.kb.license.model.v1.RestrictedIdInputDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FailedRestrictedIdDtoMapperTest {

    @Test
    public void mapFailedRestrictedIdDto_whenRestrictedIdHasFailed_thenPopulateFailedRestrictedIdDto() {
        // Arrange
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue("12345678");
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        Exception exception = new Exception("Test exception");

        FailedRestrictedIdDtoMapper failedRestrictedIdDtoMapper = new FailedRestrictedIdDtoMapper();

        // Act
        FailedRestrictedIdDto failedRestrictedIdDto = failedRestrictedIdDtoMapper.mapFailedRestrictedIdDto(restrictedIdInputDto, exception);

        // Assert
        assertEquals(restrictedIdInputDto.getIdValue(), failedRestrictedIdDto.getIdValue());
        assertEquals(restrictedIdInputDto.getIdType(), failedRestrictedIdDto.getIdType());
        assertEquals(restrictedIdInputDto.getPlatform(), failedRestrictedIdDto.getPlatform());
        assertEquals(restrictedIdInputDto.getComment(), failedRestrictedIdDto.getComment());
        assertEquals(exception.getClass().getSimpleName(), failedRestrictedIdDto.getException());
        assertEquals(exception.getMessage(), failedRestrictedIdDto.getErrorMessage());
    }
}
