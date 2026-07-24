package dk.kb.license.mapper;

import dk.kb.license.model.v1.DrHoldbackCategoryOutputDto;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DrHoldbackCategoryOutputDtoMapper {

    /**
     * Create a {@link DrHoldbackCategoryOutputDto} from a ResultSet
     *
     * @param rs containing values from dr_holdback_categories table
     * @return DrHoldbackCategoryOutputDto populated with data
     * @throws SQLException
     */
    public DrHoldbackCategoryOutputDto map(ResultSet rs) throws SQLException {
        DrHoldbackCategoryOutputDto output = new DrHoldbackCategoryOutputDto();

        output.setId(rs.getLong("id"));
        output.setKey(rs.getString("key"));
        output.setName(rs.getString("name"));
        output.setDays(rs.getObject("days", Integer.class));

        return output;
    }
}
