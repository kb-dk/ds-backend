package dk.kb.license.mapper;

import dk.kb.license.model.v1.DrBroadcastDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DrBroadcastDtoMapperTest {

    final String drProductionId = "9213163000";

    @Test
    public void mapDrBroadcastDto_whenDrProductionIdIsNull_thenReturnDrBroadcastDtoWithRestrictedFalse() {
        // Arrange
        DrBroadcastDtoMapper DrBroadcastDtoMapper = new DrBroadcastDtoMapper();
        DrBroadcastDto drBroadcastDto = new DrBroadcastDto();

        // Act
        DrBroadcastDto actualDrBroadcastDto = DrBroadcastDtoMapper.mapDrBroadcastDto(drBroadcastDto, null, null);

        // Assert
        assertEquals(drBroadcastDto, actualDrBroadcastDto);
        assertNull(actualDrBroadcastDto.getDrProductionId());
        assertEquals(false, actualDrBroadcastDto.getRestricted());
        assertNull(actualDrBroadcastDto.getRestrictedComment());
    }

    @Test
    public void mapDrBroadcastDto_whenRestrictedIdCommentIsNull_thenReturnDrBroadcastDtoWithRestrictedFalse() {
        // Arrange
        DrBroadcastDtoMapper DrBroadcastDtoMapper = new DrBroadcastDtoMapper();
        DrBroadcastDto drBroadcastDto = new DrBroadcastDto();

        // Act
        DrBroadcastDto actualDrBroadcastDto = DrBroadcastDtoMapper.mapDrBroadcastDto(drBroadcastDto, drProductionId, null);

        // Assert
        assertEquals(drBroadcastDto, actualDrBroadcastDto);
        assertEquals(drProductionId, actualDrBroadcastDto.getDrProductionId());
        assertEquals(false, actualDrBroadcastDto.getRestricted());
        assertNull(actualDrBroadcastDto.getRestrictedComment());
    }

    @Test
    public void mapDrBroadcastDto_whenRestrictedIdCommentIsNotNull_thenReturnDrBroadcastDtoWithRestrictedTrue() {
        // Arrange
        String restrictedComment = "Brugeren har trukket deres samtykke tilbage";
        DrBroadcastDtoMapper DrBroadcastDtoMapper = new DrBroadcastDtoMapper();
        DrBroadcastDto drBroadcastDto = new DrBroadcastDto();

        // Act
        DrBroadcastDto actualDrBroadcastDto = DrBroadcastDtoMapper.mapDrBroadcastDto(drBroadcastDto, drProductionId, restrictedComment);

        // Assert
        assertEquals(drBroadcastDto, actualDrBroadcastDto);
        assertEquals(drProductionId, actualDrBroadcastDto.getDrProductionId());
        assertEquals(true, actualDrBroadcastDto.getRestricted());
        assertEquals(restrictedComment, actualDrBroadcastDto.getRestrictedComment());
    }
}
