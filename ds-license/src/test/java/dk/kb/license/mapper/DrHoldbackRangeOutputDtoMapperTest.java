package dk.kb.license.mapper;

import dk.kb.license.model.v1.DrHoldbackRangeOutputDto;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DrHoldbackRangeOutputDtoMapperTest {

    @Test
    public void map_whenResultSet_thenReturnDrHoldbackRangeOutputDto() throws SQLException {
        // Assert
        DrHoldbackRangeOutputDtoMapper drHoldbackRangeOutputDtoMapper = new DrHoldbackRangeOutputDtoMapper();

        long id = 1760081561736L;
        Integer contentRangeFrom = 1000;
        Integer contentRangeTo = 1900;
        Integer formRangeFrom = 1000;
        Integer formRangeTo = 1000;
        String drHoldbackCategoryKey = "2.02";

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong("id")).thenReturn(id);
        when(resultSet.getObject("content_range_from", Integer.class)).thenReturn(contentRangeFrom);
        when(resultSet.getObject("content_range_to", Integer.class)).thenReturn(contentRangeTo);
        when(resultSet.getObject("form_range_from", Integer.class)).thenReturn(formRangeFrom);
        when(resultSet.getObject("form_range_to", Integer.class)).thenReturn(formRangeTo);
        when(resultSet.getString("dr_holdback_category_key")).thenReturn(drHoldbackCategoryKey);

        // Act
        DrHoldbackRangeOutputDto drHoldbackRangeOutputDto = drHoldbackRangeOutputDtoMapper.map(resultSet);

        // Assert
        assertEquals(id, drHoldbackRangeOutputDto.getId());
        assertEquals(contentRangeFrom, drHoldbackRangeOutputDto.getContentRangeFrom());
        assertEquals(contentRangeTo, drHoldbackRangeOutputDto.getContentRangeTo());
        assertEquals(formRangeFrom, drHoldbackRangeOutputDto.getFormRangeFrom());
        assertEquals(formRangeTo, drHoldbackRangeOutputDto.getFormRangeTo());
        assertEquals(drHoldbackCategoryKey, drHoldbackRangeOutputDto.getDrHoldbackCategoryKey());
    }
}
