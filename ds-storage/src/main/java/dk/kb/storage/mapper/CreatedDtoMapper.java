package dk.kb.storage.mapper;

import dk.kb.storage.model.v1.CreatedDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

public class CreatedDtoMapper {

  /**
   * Create a {@link CreatedDto} from a ResultSet
   *
   * @param resultSet containing values from table
   * @return createdDto populated with data
   * @throws SQLException
   */
  public CreatedDto map(ResultSet resultSet) throws SQLException {
    CreatedDto createdDto = new CreatedDto();

    createdDto.setId(resultSet.getObject("id", UUID.class));
    createdDto.setCreated(resultSet.getObject("created", OffsetDateTime.class));

    return createdDto;
  }
}
