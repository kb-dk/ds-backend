package dk.kb.license.mapper;

import dk.kb.license.model.v1.IdTypeEnumDto;
import dk.kb.license.model.v1.PlatformEnumDto;
import dk.kb.license.model.v1.RestrictedIdOutputDto;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RestrictedIdOutputDtoMapper {

    /**
     * Create a {@link RestrictedIdOutputDto} from a ResultSet, which should contain all needed values for the DTO
     *
     * @param resultSet containing values from the backing Rights database
     */
    public RestrictedIdOutputDto map(ResultSet resultSet) throws SQLException {
        RestrictedIdOutputDto output = new RestrictedIdOutputDto();

        output.setId(resultSet.getLong("id"));
        output.setIdValue(resultSet.getString("id_value"));
        output.setIdType(IdTypeEnumDto.fromValue(resultSet.getString("id_type")));
        output.setPlatform(PlatformEnumDto.fromValue(resultSet.getString("platform")));
        output.setTitle(resultSet.getString("title"));
        output.setComment(resultSet.getString("comment"));

        return output;
    }
}
