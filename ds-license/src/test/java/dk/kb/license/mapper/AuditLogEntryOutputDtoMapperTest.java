package dk.kb.license.mapper;

import dk.kb.license.model.v1.AuditLogEntryOutputDto;
import dk.kb.license.model.v1.ChangeTypeEnumDto;
import dk.kb.license.model.v1.ObjectTypeEnumDto;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuditLogEntryOutputDtoMapperTest {

    @Test
    public void map_whenCreateAuditLogEntry_thenReturnAuditLogEntryOutputDto() throws SQLException {
        // Arrange
        AuditLogEntryOutputDtoMapper auditLogEntryOutputDtoMapper = new AuditLogEntryOutputDtoMapper();
        long id = 1760081561736L;
        long objectId = 1760081561717L;
        long modifiedTime = 1760081561736L;
        String userName = "Unknown";
        String changetype = "CREATE";
        String changename = "DS_ID";
        String identifier = "ds.tv:oai:io:4cf2d7a0-a1a2-445e-8e2e-b2637680dc65";
        String changeComment = null;
        String textBefore = "class RestrictedIdInputDto {\n" +
                "idValue: ds.tv:oai:io:4cf2d7a0-a1a2-445e-8e2e-b2637680dc65\n" +
                "idType: DS_ID\n" +
                "platform: DRARKIV\n" +
                "comment: pabr_test\n" +
                "}";
        String textAfter = null;

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong("id")).thenReturn(id);
        when(resultSet.getLong("objectId")).thenReturn(objectId);
        when(resultSet.getLong("modifiedTime")).thenReturn(modifiedTime);
        when(resultSet.getString("userName")).thenReturn(userName);
        when(resultSet.getString("changetype")).thenReturn(changetype);
        when(resultSet.getString("changename")).thenReturn(changename);
        when(resultSet.getString("identifier")).thenReturn(identifier);
        when(resultSet.getString("changeComment")).thenReturn(changeComment);
        when(resultSet.getString("textBefore")).thenReturn(textBefore);
        when(resultSet.getString("textAfter")).thenReturn(textAfter);

        // Act
        AuditLogEntryOutputDto auditLogEntryOutputDto = auditLogEntryOutputDtoMapper.map(resultSet);

        // Assert
        assertEquals(id, auditLogEntryOutputDto.getId());
        assertEquals(objectId, auditLogEntryOutputDto.getObjectId());
        assertEquals(modifiedTime, auditLogEntryOutputDto.getModifiedTime());
        assertEquals(userName, auditLogEntryOutputDto.getUserName());
        assertEquals(ChangeTypeEnumDto.CREATE, auditLogEntryOutputDto.getChangeType());
        assertEquals(ObjectTypeEnumDto.DS_ID, auditLogEntryOutputDto.getChangeName());
        assertEquals(identifier, auditLogEntryOutputDto.getIdentifier());
        assertNull(auditLogEntryOutputDto.getChangeComment());
        assertEquals(textBefore, auditLogEntryOutputDto.getTextBefore());
        assertNull(auditLogEntryOutputDto.getTextAfter());
    }

    @Test
    public void map_whenPossibleValuesAreNull_thenReturnAuditLogEntryOutputDto() throws SQLException {
        // Arrange
        AuditLogEntryOutputDtoMapper auditLogEntryOutputDtoMapper = new AuditLogEntryOutputDtoMapper();
        long id = 1760081561736L;
        long objectId = 1760081561717L;
        long modifiedTime = 1760081561736L;
        String userName = "Unknown";
        String changetype = "CREATE";
        String changename = "DS_ID";
        String identifier = null;
        String changeComment = null;
        String textBefore = null;
        String textAfter = null;

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong("id")).thenReturn(id);
        when(resultSet.getLong("objectId")).thenReturn(objectId);
        when(resultSet.getLong("modifiedTime")).thenReturn(modifiedTime);
        when(resultSet.getString("userName")).thenReturn(userName);
        when(resultSet.getString("changetype")).thenReturn(changetype);
        when(resultSet.getString("changename")).thenReturn(changename);
        when(resultSet.getString("identifier")).thenReturn(identifier);
        when(resultSet.getString("changeComment")).thenReturn(changeComment);
        when(resultSet.getString("textBefore")).thenReturn(textBefore);
        when(resultSet.getString("textAfter")).thenReturn(textAfter);

        // Act
        AuditLogEntryOutputDto auditLogEntryOutputDto = auditLogEntryOutputDtoMapper.map(resultSet);

        // Assert
        assertEquals(id, auditLogEntryOutputDto.getId());
        assertEquals(objectId, auditLogEntryOutputDto.getObjectId());
        assertEquals(modifiedTime, auditLogEntryOutputDto.getModifiedTime());
        assertEquals(userName, auditLogEntryOutputDto.getUserName());
        assertEquals(ChangeTypeEnumDto.CREATE, auditLogEntryOutputDto.getChangeType());
        assertEquals(ObjectTypeEnumDto.DS_ID, auditLogEntryOutputDto.getChangeName());
        assertNull(auditLogEntryOutputDto.getIdentifier());
        assertNull(auditLogEntryOutputDto.getChangeComment());
        assertNull(auditLogEntryOutputDto.getTextBefore());
        assertNull(auditLogEntryOutputDto.getTextAfter());
    }
}
