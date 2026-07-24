package dk.kb.license.mapper;

import dk.kb.license.model.v1.DrBroadcastDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DrBroadcastDtoMapperTest {

    final String drProductionId = "9213163000";

    @Test
    public void map_whenDrProductionIdIsNull_thenReturnWithRestrictedFalse() {
        // Arrange
        DrBroadcastDtoMapper DrBroadcastDtoMapper = new DrBroadcastDtoMapper();
        DrBroadcastDto drBroadcastDto = new DrBroadcastDto();

        // Act
        DrBroadcastDto actualDrBroadcastDto = DrBroadcastDtoMapper.map(drBroadcastDto, null, null);

        // Assert
        assertEquals(drBroadcastDto, actualDrBroadcastDto);
        assertNull(actualDrBroadcastDto.getDrProductionId());
        assertEquals(false, actualDrBroadcastDto.getRestricted());
        assertNull(actualDrBroadcastDto.getRestrictedComment());
    }

    @Test
    public void map_whenRestrictedIdCommentIsNull_thenReturnWithRestrictedFalse() {
        // Arrange
        DrBroadcastDtoMapper DrBroadcastDtoMapper = new DrBroadcastDtoMapper();
        DrBroadcastDto drBroadcastDto = new DrBroadcastDto();

        // Act
        DrBroadcastDto actualDrBroadcastDto = DrBroadcastDtoMapper.map(drBroadcastDto, drProductionId, null);

        // Assert
        assertEquals(drBroadcastDto, actualDrBroadcastDto);
        assertEquals(drProductionId, actualDrBroadcastDto.getDrProductionId());
        assertEquals(false, actualDrBroadcastDto.getRestricted());
        assertNull(actualDrBroadcastDto.getRestrictedComment());
    }

    @Test
    public void map_whenRestrictedIdCommentIsNotNull_thenReturnWithRestrictedTrue() {
        // Arrange
        String restrictedComment = "Brugeren har trukket deres samtykke tilbage";
        DrBroadcastDtoMapper DrBroadcastDtoMapper = new DrBroadcastDtoMapper();
        DrBroadcastDto drBroadcastDto = new DrBroadcastDto();

        // Act
        DrBroadcastDto actualDrBroadcastDto = DrBroadcastDtoMapper.map(drBroadcastDto, drProductionId, restrictedComment);

        // Assert
        assertEquals(drBroadcastDto, actualDrBroadcastDto);
        assertEquals(drProductionId, actualDrBroadcastDto.getDrProductionId());
        assertEquals(true, actualDrBroadcastDto.getRestricted());
        assertEquals(restrictedComment, actualDrBroadcastDto.getRestrictedComment());
    }
}
