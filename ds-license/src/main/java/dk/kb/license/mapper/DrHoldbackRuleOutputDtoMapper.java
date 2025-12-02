package dk.kb.license.mapper;

import dk.kb.license.model.v1.DrHoldbackRuleOutputDto;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DrHoldbackRuleOutputDtoMapper {

    /**
     * Create a {@link DrHoldbackRuleOutputDto} from a ResultSet
     *
     * @param rs containing values from dr_holdback_rules table
     * @return DrHoldbackRuleOutputDto populated with data
     * @throws SQLException
     */
    public DrHoldbackRuleOutputDto map(ResultSet rs) throws SQLException {
        DrHoldbackRuleOutputDto output = new DrHoldbackRuleOutputDto();

        output.setId(rs.getLong("id"));
        output.setDrHoldbackValue(rs.getString("dr_holdback_value"));
        output.setName(rs.getString("name"));
        output.setDays(rs.getObject("days", Integer.class));

        return output;
    }
}
