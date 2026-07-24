package dk.kb.license.mapper;

import dk.kb.license.model.v1.IdTypeEnumDto;
import dk.kb.license.model.v1.PlatformEnumDto;
import dk.kb.license.model.v1.RestrictedIdOutputDto;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RestrictedIdOutputDtoMapperTest {

    @Test
    public void map_whenResultSet_thenReturnRestrictedIdOutputDto() throws SQLException {
        // Assert
        RestrictedIdOutputDtoMapper restrictedIdOutputDtoMapper = new RestrictedIdOutputDtoMapper();

        long id = 1760081561736L;
        String idValue = "12345678";
        IdTypeEnumDto idTypeEnumDto = IdTypeEnumDto.DR_PRODUCTION_ID;
        PlatformEnumDto platformEnumDto = PlatformEnumDto.DRARKIV;
        String title = "TV Avisen";
        String comment = "Brugeren har trukket deres samtykke tilbage";

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong("id")).thenReturn(id);
        when(resultSet.getString("id_value")).thenReturn(idValue);
        when(resultSet.getString("id_type")).thenReturn(idTypeEnumDto.name());
        when(resultSet.getString("platform")).thenReturn(platformEnumDto.name());
        when(resultSet.getString("title")).thenReturn(title);
        when(resultSet.getString("comment")).thenReturn(comment);

        // Act
        RestrictedIdOutputDto restrictedIdOutputDto = restrictedIdOutputDtoMapper.map(resultSet);

        // Assert
        assertEquals(id, restrictedIdOutputDto.getId());
        assertEquals(idValue, restrictedIdOutputDto.getIdValue());
        assertEquals(idTypeEnumDto, restrictedIdOutputDto.getIdType());
        assertEquals(platformEnumDto, restrictedIdOutputDto.getPlatform());
        assertEquals(title, restrictedIdOutputDto.getTitle());
        assertEquals(comment, restrictedIdOutputDto.getComment());
    }
}
