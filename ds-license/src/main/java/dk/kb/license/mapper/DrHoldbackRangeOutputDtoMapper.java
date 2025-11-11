package dk.kb.license.mapper;

import dk.kb.license.model.v1.DrHoldbackRangeOutputDto;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DrHoldbackRangeOutputDtoMapper {

    /**
     * Create a {@link DrHoldbackRangeOutputDto} from a ResultSet
     *
     * @param rs containing values from dr_holdback_ranges table
     * @return DrHoldbackRangeOutputDto populated with data
     * @throws SQLException
     */
    public DrHoldbackRangeOutputDto map(ResultSet rs) throws SQLException {
        DrHoldbackRangeOutputDto output = new DrHoldbackRangeOutputDto();

        output.setId(rs.getLong("id"));
        output.setContentRangeFrom(rs.getObject("content_range_from", Integer.class));
        output.setContentRangeTo(rs.getObject("content_range_to", Integer.class));
        output.setFormRangeFrom(rs.getObject("form_range_from", Integer.class));
        output.setFormRangeTo(rs.getObject("form_range_to", Integer.class));
        output.setDrHoldbackValue(rs.getString("dr_holdback_value"));

        return output;
    }
}
