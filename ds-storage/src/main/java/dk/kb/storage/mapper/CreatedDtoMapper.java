package dk.kb.storage.mapper;

import dk.kb.storage.model.v1.CreatedDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

public class CreatedDtoMapper {

  /**
   * Create a {@link CreatedDto} from a ResultSet
   *
   * @param resultSet containing values from rerun_cluster table
   * @return createdDto populated with data
   * @throws SQLException
   */
  public CreatedDto map(ResultSet resultSet) throws SQLException {
    CreatedDto createdDto = new CreatedDto();

    createdDto.setCreated((OffsetDateTime) resultSet.getObject("latest_created"));

    return createdDto;
  }
}
