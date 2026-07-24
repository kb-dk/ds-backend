package dk.kb.license.mapper;

import dk.kb.license.model.v1.AuditLogEntryOutputDto;
import dk.kb.license.model.v1.ChangeTypeEnumDto;
import dk.kb.license.model.v1.ObjectTypeEnumDto;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AuditLogEntryOutputDtoMapper {

    /**
     * Create a {@link AuditLogEntryOutputDto} from a ResultSet
     *
     * @param rs containing values from auditlog table
     * @return AuditLogEntryOutputDto populated with data
     * @throws SQLException
     */
    public AuditLogEntryOutputDto map(ResultSet rs) throws SQLException {
        AuditLogEntryOutputDto auditEntry = new AuditLogEntryOutputDto();

        auditEntry.setId(rs.getLong("id"));
        auditEntry.setObjectId(rs.getLong("objectId"));
        auditEntry.setModifiedTime(rs.getLong("modifiedTime"));
        auditEntry.setUserName(rs.getString("userName"));

        String changeType = rs.getString("changetype");
        auditEntry.setChangeType(ChangeTypeEnumDto.valueOf(changeType));

        String changeName = rs.getString("changename");
        auditEntry.setChangeName(ObjectTypeEnumDto.valueOf(changeName));

        auditEntry.setIdentifier(rs.getString("identifier"));
        auditEntry.setChangeComment(rs.getString("changeComment"));
        auditEntry.setTextBefore(rs.getString("textBefore"));
        auditEntry.setTextAfter(rs.getString("textAfter"));

        return auditEntry;
    }
}
