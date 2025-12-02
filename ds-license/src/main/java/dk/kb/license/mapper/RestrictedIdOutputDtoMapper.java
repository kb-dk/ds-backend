package dk.kb.license.mapper;

import dk.kb.license.model.v1.IdTypeEnumDto;
import dk.kb.license.model.v1.PlatformEnumDto;
import dk.kb.license.model.v1.RestrictedIdOutputDto;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RestrictedIdOutputDtoMapper {

    /**
     * Create a {@link RestrictedIdOutputDto} from a ResultSet
     *
     * @param rs containing values from restricted_ids table
     * @return RestrictedIdOutputDto populated with data
     * @throws SQLException
     */
    public RestrictedIdOutputDto map(ResultSet rs) throws SQLException {
        RestrictedIdOutputDto output = new RestrictedIdOutputDto();

        output.setId(rs.getLong("id"));
        output.setIdValue(rs.getString("id_value"));
        output.setIdType(IdTypeEnumDto.fromValue(rs.getString("id_type")));
        output.setPlatform(PlatformEnumDto.fromValue(rs.getString("platform")));
        output.setTitle(rs.getString("title"));
        output.setComment(rs.getString("comment"));

        return output;
    }
}
