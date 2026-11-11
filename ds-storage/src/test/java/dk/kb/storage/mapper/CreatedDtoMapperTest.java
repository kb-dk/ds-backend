package dk.kb.storage.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dk.kb.storage.model.v1.CreatedDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class CreatedDtoMapperTest {

  @Test
  public void map_whenResultSet_thenReturnCreatedDto() throws SQLException {
    // Assert
    CreatedDtoMapper createdDtoMapper = new CreatedDtoMapper();

    UUID id = UUID.randomUUID();
    OffsetDateTime created = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");

    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getObject("id", UUID.class)).thenReturn(id);
    when(resultSet.getObject("created", OffsetDateTime.class)).thenReturn(created);

    // Act
    CreatedDto createdDto = createdDtoMapper.map(resultSet);

    // Assert
    assertNotNull(createdDto);
    assertEquals(id, createdDto.getId());
    assertEquals(created, createdDto.getCreated());
  }
}
