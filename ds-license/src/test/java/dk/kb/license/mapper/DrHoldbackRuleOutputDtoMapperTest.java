package dk.kb.license.mapper;

import dk.kb.license.model.v1.DrHoldbackRuleOutputDto;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DrHoldbackRuleOutputDtoMapperTest {

    @Test
    public void map_whenResultSet_thenReturnDrHoldbackRuleOutputDto() throws SQLException {
        // Assert
        DrHoldbackRuleOutputDtoMapper drHoldbackRuleOutputDtoMapper = new DrHoldbackRuleOutputDtoMapper();

        long id = 1760081561736L;
        String drHoldBackValue = "2.02";
        String drHoldBackName = "Aktualitet og Debat";
        Integer days = 100;

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong("id")).thenReturn(id);
        when(resultSet.getString("dr_holdback_value")).thenReturn(drHoldBackValue);
        when(resultSet.getString("name")).thenReturn(drHoldBackName);
        when(resultSet.getObject("days", Integer.class)).thenReturn(days);

        // Act
        DrHoldbackRuleOutputDto drHoldbackRuleOutputDto = drHoldbackRuleOutputDtoMapper.map(resultSet);

        // Assert
        assertEquals(id, drHoldbackRuleOutputDto.getId());
        assertEquals(drHoldBackValue, drHoldbackRuleOutputDto.getDrHoldbackValue());
        assertEquals(drHoldBackName, drHoldbackRuleOutputDto.getName());
        assertEquals(days, drHoldbackRuleOutputDto.getDays());
    }
}
