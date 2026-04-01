package dk.kb.license.storage;

import dk.kb.license.mapper.AuditLogEntryOutputDtoMapper;
import dk.kb.license.model.v1.AuditLogEntryOutputDto;
import dk.kb.license.model.v1.ObjectTypeEnumDto;
import dk.kb.license.webservice.KBAuthorizationInterceptor;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for handling the audit log entries.
 */
public class AuditLogModuleStorage extends BaseModuleStorage {
    private static final Logger log = LoggerFactory.getLogger(AuditLogModuleStorage.class);

    private static final AuditLogEntryOutputDtoMapper auditLogEntryOutputDtoMapper = new AuditLogEntryOutputDtoMapper();

    //AUDITLOG
    private static final String TABLE = "AUDITLOG";
    private static final String ID_COLUMN = "ID";
    private static final String OBJECTID_COLUMN = "OBJECTID";
    private static final String MODIFIEDTIME_COLUMN = "MODIFIEDTIME";
    private static final String USERNAME_COLUMN = "USERNAME";
    private static final String CHANGETYPE_COLUMN = "CHANGETYPE";
    private static final String CHANGENAME_COLUMN = "CHANGENAME";
    private static final String IDENTIFIER_COLUMN = "IDENTIFIER";
    private static final String CHANGECOMMENT_COLUMN = "CHANGECOMMENT";
    private static final String TEXTBEFORE_COLUMN = "TEXTBEFORE";
    private static final String TEXTAFTER_COLUMN = "TEXTAFTER";
    
                                
    private final static String selectAuditLogOlderThanModifiedTimeQuery    = "SELECT * FROM " + TABLE + " WHERE " + MODIFIEDTIME_COLUMN + " < ? ORDER BY "+MODIFIEDTIME_COLUMN + " DESC LIMIT 100";
    private final static String selectAuditLogOlderThanModifiedTimeByChangeNameQuery          = "SELECT * FROM " + TABLE + " WHERE " + MODIFIEDTIME_COLUMN + " < ? AND "+ CHANGENAME_COLUMN + " = ? ORDER BY " + MODIFIEDTIME_COLUMN + " DESC LIMIT 100";
  
    private final static String selectAuditLogQueryById = "SELECT * FROM " + TABLE + " WHERE " + ID_COLUMN + " = ? ";
    private final static String selectAuditLogQueryByObjectId = "SELECT * FROM " + TABLE + " WHERE " + OBJECTID_COLUMN + " = ? " + " ORDER BY " + MODIFIEDTIME_COLUMN + " DESC";

    private final static String selectAllAuditLogQuery = "SELECT * FROM " + TABLE + " ORDER BY " + MODIFIEDTIME_COLUMN + " DESC";
    private final static String persistAuditLog = "INSERT INTO " + TABLE + " (" +
            ID_COLUMN + ", " +
            OBJECTID_COLUMN + ", " +
            MODIFIEDTIME_COLUMN + ", " +
            USERNAME_COLUMN + ", " +
            CHANGETYPE_COLUMN + ", " +
            CHANGENAME_COLUMN + ", " +
            IDENTIFIER_COLUMN + ", " +
            CHANGECOMMENT_COLUMN + ", " +
            TEXTBEFORE_COLUMN + ", " +
            TEXTAFTER_COLUMN + ") " +
            "VALUES (?,?,?,?,?,?,?,?,?,?)"; // #|?|=10

    public AuditLogModuleStorage() throws SQLException {
        super();
    }

    /**
     *
     * @param id for the auditlog (not the objectid for the value changed)
     * @return AuditLog object
     * @throws Exception
     */
    public AuditLogEntryOutputDto getAuditLogById(long id) throws IllegalArgumentException, SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(selectAuditLogQueryById);) {
            stmt.setLong(1, id);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) { // maximum one due to unique/primary key constraint
                return auditLogEntryOutputDtoMapper.map(rs);
            }
            throw new IllegalArgumentException("Audit not found for id: " + id);

        } catch (SQLException e) {
            log.error("SQL Exception in getAuditLog: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves List of {@link AuditLogEntry} . Maximum elements is 100. Use modifiedTimeStart for pagination.
     * AuditLogEntries in the list are sorted by modifiedtime desc, so latest will come first in the list.     
     *      
     * @param modifiedTimeStart Will extract AuditLogEntries with modifiedtime less than this value
     * 
     * @return List<AuditLogEntryOutputDto> with a maximum of 100 elements in the list. 
     */
   public List<AuditLogEntryOutputDto> getAuditLogOlderThanModifiedTimeListAll(Long modifiedTimeStart) throws IllegalArgumentException, SQLException {
       log.debug("getAuditLogOlderThanModifiedTimeListAll called for modifiedTimeStart='{}', type='{}'", modifiedTimeStart);
       
       List<AuditLogEntryOutputDto> auditLogList = new ArrayList<AuditLogEntryOutputDto>();
                       
       try (PreparedStatement stmt = connection.prepareStatement(selectAuditLogOlderThanModifiedTimeQuery);) {
           stmt.setLong(1, modifiedTimeStart);                     
           ResultSet rs = stmt.executeQuery();
           while (rs.next()) { // maximum one due to unique/primary key constraint              
                AuditLogEntryOutputDto audit = auditLogEntryOutputDtoMapper.map(rs);
                auditLogList.add(audit);
           }       
           return auditLogList;
       } catch (SQLException e) {
           log.error("SQL Exception in getAuditLogOlderThanModifiedTimeListAll " + e.getMessage());
           throw e;
       }
   }
   
   /**
    * Retrieves List of {@link AuditLogEntry} . Maximum elements is 100. Use modifiedTimeStart for pagination.
    * AuditLogEntries in the list are sorted by modifiedtime desc, so latest will come first in the list.     
    *      
    * @param modifiedTimeStart Will extract AuditLogEntries with modifiedtime less than this value
    * @param type Optional, list only AuditLogEntries of this type. Will list all types if type is null
    * 
    * @return List<AuditLogEntryOutputDto> with a maximum of 100 elements in the list. 
    */
  public List<AuditLogEntryOutputDto> getAuditLogOlderThanModifiedTimeListByType(Long modifiedTimeStart, ObjectTypeEnumDto changeName) throws IllegalArgumentException, SQLException {
      if (changeName == null) {
          throw new InvalidArgumentServiceException("ObjectTypeEnum must not be null");
      }
      
      log.debug("getAuditLogOlderThanModifiedTimeListByType called for modifiedTimeStart='{}', changeName='{}'",modifiedTimeStart ,changeName);
      
      List<AuditLogEntryOutputDto> auditLogList = new ArrayList<AuditLogEntryOutputDto>();              
      
      try (PreparedStatement stmt = connection.prepareStatement(selectAuditLogOlderThanModifiedTimeByChangeNameQuery);) {
          stmt.setLong(1, modifiedTimeStart);        
          stmt.setString(2,changeName.getValue());                   
          ResultSet rs = stmt.executeQuery();
          while (rs.next()) { // maximum one due to unique/primary key constraint              
               AuditLogEntryOutputDto audit = auditLogEntryOutputDtoMapper.map(rs);
               auditLogList.add(audit);
          }       
          return auditLogList;
      } catch (SQLException e) {
          log.error("SQL Exception in getAuditLogOlderThanModifiedTimeListByType: " + e.getMessage());
          throw e;
      }
  }

   
    
    /**
     * @param objectId The ID for the object extract audit log.
     * @return List of AuditLog objects. Will return empty list if objectId is not found.
     * @throws Exception
     */
    public ArrayList<AuditLogEntryOutputDto> getAuditLogByObjectId(long objectId) throws IllegalArgumentException, SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(selectAuditLogQueryByObjectId);) {
            stmt.setLong(1, objectId);

            ArrayList<AuditLogEntryOutputDto> entries = new ArrayList<AuditLogEntryOutputDto>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                entries.add(auditLogEntryOutputDtoMapper.map(rs));
            }
            return entries;

        } catch (SQLException e) {
            log.error("SQL Exception in g getAuditLogByObjectId: " + e.getMessage());
            throw e;
        }
    }

    public ArrayList<AuditLogEntryOutputDto> getAllAudit() throws SQLException {
        ArrayList<AuditLogEntryOutputDto> entryList = new ArrayList<AuditLogEntryOutputDto>();

        try (PreparedStatement stmt = connection.prepareStatement(selectAllAuditLogQuery);) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) { // maximum one due to unique/primary key constraint
                AuditLogEntryOutputDto auditLog = auditLogEntryOutputDtoMapper.map(rs);
                entryList.add(auditLog);
            }
            return entryList;
        } catch (SQLException e) {
            log.error("SQL Exception in getAllAudit: " + e.getMessage());
            throw e;
        }
    }

    /**
     *
     *
     * @return databaseID for the new AuditLog entry
     */
    public long persistAuditLog(AuditLogEntry auditLog) throws SQLException {
        log.info("Persisting persistAuditLog changetype='{}' and changeName='{}' for user='{}'", auditLog.getChangeType(), auditLog.getChangeName(), getCurrentUsername(auditLog.getUserName()));

        Long id = generateUniqueID();

        try (PreparedStatement stmt = connection.prepareStatement(persistAuditLog);) {
            log.info("generating id: " + id);
            log.info("persisting auditLog: " + auditLog);
            log.info("identifier: " + auditLog.getIdentifier());
            stmt.setLong(1, id);
            stmt.setLong(2, auditLog.getObjectId());
            stmt.setLong(3, System.currentTimeMillis());
            stmt.setString(4, getCurrentUsername(auditLog.getUserName()));
            stmt.setString(5, auditLog.getChangeType().getValue());
            stmt.setString(6, auditLog.getChangeName().getValue());
            stmt.setString(7, auditLog.getIdentifier());
            stmt.setString(8, auditLog.getChangeComment());
            stmt.setString(9, auditLog.getTextBefore());
            stmt.setString(10, auditLog.getTextAfter());
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in persistAuditLog: " + e.getMessage());
            throw e;
        }
        return id;
    }

    /**
     * Gets the name of the current user from the OAuth token.
     *
     * @return
     */
    private static String getCurrentUsername(String username) {
        Message message = JAXRSUtils.getCurrentMessage();
        if (message == null) {
            if (username == null) {
                throw new IllegalArgumentException("No username or valid message provided");
            }
            return username;
        }
        AccessToken token = (AccessToken) message.get(KBAuthorizationInterceptor.ACCESS_TOKEN);
        if (token != null && token.getName() != null) {
            return token.getName();
        }
        throw new IllegalArgumentException("Invalid or no token provided");
    }
}
