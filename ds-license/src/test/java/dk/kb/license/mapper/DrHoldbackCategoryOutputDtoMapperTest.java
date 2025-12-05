package dk.kb.license.mapper;

import dk.kb.license.model.v1.DrHoldbackCategoryOutputDto;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DrHoldbackCategoryOutputDtoMapperTest {

    @Test
    public void map_whenResultSet_thenReturnDrHoldbackCategoryOutputDto() throws SQLException {
        // Assert
        DrHoldbackCategoryOutputDtoMapper drHoldbackCategoryOutputDtoMapper = new DrHoldbackCategoryOutputDtoMapper();

        long id = 1760081561736L;
        String key = "2.02";
        String name = "Aktualitet og Debat";
        Integer days = 100;

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong("id")).thenReturn(id);
        when(resultSet.getString("key")).thenReturn(key);
        when(resultSet.getString("name")).thenReturn(name);
        when(resultSet.getObject("days", Integer.class)).thenReturn(days);

        // Act
        DrHoldbackCategoryOutputDto drHoldbackCategoryOutputDto = drHoldbackCategoryOutputDtoMapper.map(resultSet);

        // Assert
        assertEquals(id, drHoldbackCategoryOutputDto.getId());
        assertEquals(key, drHoldbackCategoryOutputDto.getKey());
        assertEquals(name, drHoldbackCategoryOutputDto.getName());
        assertEquals(days, drHoldbackCategoryOutputDto.getDays());
    }
}
