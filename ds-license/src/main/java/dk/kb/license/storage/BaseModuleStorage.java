package dk.kb.license.storage;

import dk.kb.license.model.v1.AuditEntryOutputDto;
import dk.kb.license.model.v1.ChangeTypeEnumDto;
import dk.kb.license.model.v1.ObjectTypeEnumDto;
import dk.kb.license.webservice.KBAuthorizationInterceptor;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

/**
 * The BaseModuleStorage, which sets up the connection to the database which is then used by  {@link LicenseModuleStorage} and {@link RightsModuleStorage}.
 * This class only sets up the connection, while the other two are responsible for implementing the interactions with the database.
 */
public abstract class BaseModuleStorage implements AutoCloseable  {
    private static final Logger log = LoggerFactory.getLogger(BaseModuleStorage.class);
          
    //AUDITLOG
    private static final String AUDITLOG_TABLE = "AUDITLOG";
    private static final String AUDITLOG_ID_COLUMN = "ID";
    private static final String AUDITLOG_OBJECTID_COLUMN = "OBJECTID";
    private static final String AUDITLOG_MODIFIEDTIME_COLUMN = "MODIFIEDTIME";     
    private static final String AUDITLOG_USERNAME_COLUMN = "USERNAME";
    private static final String AUDITLOG_CHANGETYPE_COLUMN = "CHANGETYPE";
    private static final String AUDITLOG_CHANGENAME_COLUMN = "CHANGENAME";
    private static final String AUDITLOG_CHANGECOMMENT_COLUMN = "CHANGECOMMENT";  
    private static final String AUDITLOG_TEXTBEFORE_COLUMN = "TEXTBEFORE";
    private static final String AUDITLOG_TEXTAFTER_COLUMN = "TEXTAFTER";

    private final static String selectAuditLogQueryById = "SELECT * FROM " + AUDITLOG_TABLE + " WHERE " + AUDITLOG_ID_COLUMN + " = ? ";
    private final static String selectAuditLogQueryByObjectId = "SELECT * FROM " + AUDITLOG_TABLE + " WHERE " + AUDITLOG_OBJECTID_COLUMN + " = ? " + " ORDER BY " + AUDITLOG_MODIFIEDTIME_COLUMN + " DESC";
    
    private final static String selectAllAuditLogQuery = "SELECT * FROM " + AUDITLOG_TABLE + " ORDER BY " + AUDITLOG_MODIFIEDTIME_COLUMN + " DESC";
    private final static String persistAuditLog = "INSERT INTO " + AUDITLOG_TABLE + " (" +
                                                                   AUDITLOG_ID_COLUMN + ", " + 
                                                                   AUDITLOG_OBJECTID_COLUMN + ", " +
                                                                   AUDITLOG_MODIFIEDTIME_COLUMN + ", " +
                                                                   AUDITLOG_USERNAME_COLUMN + ", "+
                                                                   AUDITLOG_CHANGETYPE_COLUMN + ", " +
                                                                   AUDITLOG_CHANGENAME_COLUMN + ", " +
                                                                   AUDITLOG_CHANGECOMMENT_COLUMN + ", " +
                                                                   AUDITLOG_TEXTBEFORE_COLUMN + ", " +
                                                                   AUDITLOG_TEXTAFTER_COLUMN + ") " +
                                                                   "VALUES (?,?,?,?,?,?,?,?,?)"; // #|?|=9

    // statistics shown on monitor.jsp page
    public static Date INITDATE = null;

    protected Connection connection = null; // private
    protected static BasicDataSource dataSource = null; // shared

    private static long lastTimestamp = 0; // Remember last timestamp and make sure each is only used once;

    public BaseModuleStorage() throws SQLException {
        connection = dataSource.getConnection();
    }

    /**
     * Close the connection to the database. You should probably perform a commit or rollback before closing the connection.
     */
    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            // nothing to do here
        }
    }

    /**
     * Commit all changes made since the last rollback or commit
     */
    public void commit() throws SQLException {
        connection.commit();
    }

    /**
     * Rollback all changes in the current transaction.
     */
    public void rollback() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            // nothing to do here
        }
    }

    /**
     * Initialize the connection to a database
     * @param driverName name of the database driver
     * @param dbUrl url of the database
     * @param username database username
     * @param password password for the user
     */
    public static void initialize(String driverName, String dbUrl, String username, String password) {
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName(driverName);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setUrl(dbUrl);

        dataSource.setDefaultReadOnly(false);
        dataSource.setDefaultAutoCommit(false);

        // TODO maybe set some datasource options.
        // enable detection and logging of connection leaks
        /*
         * dataSource.setRemoveAbandonedOnBorrow(
         * AlmaPickupNumbersPropertiesHolder.PICKUPNUMBERS_DATABASE_TIME_BEFORE_RECLAIM
         * > 0); dataSource.setRemoveAbandonedOnMaintenance(
         * AlmaPickupNumbersPropertiesHolder.PICKUPNUMBERS_DATABASE_TIME_BEFORE_RECLAIM
         * > 0); dataSource.setRemoveAbandonedTimeout(AlmaPickupNumbersPropertiesHolder.
         * PICKUPNUMBERS_DATABASE_TIME_BEFORE_RECLAIM); //1 hour
         * dataSource.setLogAbandoned(AlmaPickupNumbersPropertiesHolder.
         * PICKUPNUMBERS_DATABASE_TIME_BEFORE_RECLAIM > 0);
         * dataSource.setMaxWaitMillis(AlmaPickupNumbersPropertiesHolder.
         * PICKUPNUMBERS_DATABASE_POOL_CONNECT_TIMEOUT);
         */
        dataSource.setMaxTotal(10); //

        INITDATE = new Date();

        log.info("DsLicence storage initialized");
    }

    // Used from unittests. Create tables DDL etc.
    protected synchronized void runDDLScript(File file) throws SQLException {
        log.info("Running DDL script: " + file.getAbsolutePath());

        if (!file.exists()) {
            log.error("DDL script not found: " + file.getAbsolutePath());
            throw new RuntimeException("DDLscript file not found: " + file.getAbsolutePath());
        }

        String scriptStatement = "RUNSCRIPT FROM '" + file.getAbsolutePath() + "'";

        connection.prepareStatement(scriptStatement).execute();
    }

    // This is called by from InialialziationContextListener by the Web-container
    // when server is shutdown,
    // Just to be sure the DB lock file is free.
    public static void shutdown() {
        log.info("Shutdown ds-license");
        try {
            if (dataSource != null) {
                dataSource.close();
            }
        } catch (SQLException e) {
            // ignore errors during shutdown, we cant do anything about it anyway
            log.error("shutdown failed", e);
        }
    }

    /**
     * Start a storage transaction and performs the given action on it, returning the result from the action.
     * <p>
     * If the action throws an exception, a {@link LicenseModuleStorage#rollback()} is performed.
     * If the action passes without exceptions, a {@link LicenseModuleStorage#commit()} is performed.
     * @param actionID a debug-oriented ID for the action, typically the name of the calling method.
     * @param storageClass
     * @param action the action to perform on the storage.
     * @return return value from the action.
     * @throws InternalServiceException if anything goes wrong.
     */
    public static <T> T performStorageAction(String actionID,  Class<? extends BaseModuleStorage> storageClass, BaseModuleStorage.StorageAction<T> action) {
        try (BaseModuleStorage storage = storageClass.getDeclaredConstructor().newInstance()) {
            T result;
            try {
                result = action.process(storage);
            }
            catch(InvalidArgumentServiceException e) {
                log.warn("Exception performing action '{}'. Initiating rollback", actionID, e.getMessage());
                storage.rollback();
                throw new InvalidArgumentServiceException(e);
            }
            catch (Exception e) {
                log.warn("Exception performing action '{}'. Initiating rollback", actionID, e);
                storage.rollback();
                throw new InternalServiceException(e);
            }

            try {
                storage.commit();
            } catch (SQLException e) {
                log.error("Exception committing after action '{}'", actionID, e);
                throw new InternalServiceException(e);
            }

            return result;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            log.error("Exception performing action '{}'", actionID, e);
            throw new InternalServiceException(e);
        } catch (Exception e) {
            log.error("Exception performing action '{}'", actionID, e);
            throw new InternalServiceException(e);
        }
    }

    // Just a simple way to generate unique ID's and make sure they are unique
    protected synchronized long generateUniqueID() {
        long now = System.currentTimeMillis();
        if (now <= lastTimestamp) { // this timestamp has already been used. just +1 and use that
            lastTimestamp++;
            return lastTimestamp;
        } else {
            lastTimestamp = now;
            return now;
        }
    }

    /**
     * Callback used with {@link #performStorageAction(String, Class, StorageAction)}.
     * @param <T> the object returned from the {@link BaseModuleStorage.StorageAction#process(BaseModuleStorage)} method.
     */
    @FunctionalInterface
    public interface StorageAction<T> {
        /**
         * Access or modify the given storage inside of a transaction.
         * If the method throws an exception, it will be logged, a {@link LicenseModuleStorage#rollback()} will be performed and
         * a wrapping {@link dk.kb.util.webservice.exception.InternalServiceException} will be thrown.
         * @param storage a storage ready for requests and updates.
         * @return custom return value.
         * @throws Exception if something went wrong.
         */
        T process(BaseModuleStorage storage) throws Exception;
    }

    /**
     *
     * @param id for the auditlog (not the objectid for the value changed)
     * @return AuditLog object 
     * @throws Exception
     */
    public AuditEntryOutputDto getAuditLogById(long id) throws IllegalArgumentException, SQLException {

        try (PreparedStatement stmt = connection.prepareStatement(selectAuditLogQueryById);) {
            stmt.setLong(1, id);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) { // maximum one due to unique/primary key constraint
            return convertRsToAuditLog(rs);                                             
            }
            throw new IllegalArgumentException("Audit not found for id: " + id);

        } catch (SQLException e) {
            log.error("SQL Exception in getAuditLog: " + e.getMessage());
            throw e;
        }
    }
    
    /**   
    * @param objectId The ID for the object extract audit log. 
    * @return List of AuditLog objects. Will return empty list if objectId is not found. 
    * @throws Exception
    */
   public ArrayList<AuditEntryOutputDto> getAuditLogByObjectId(long objectId) throws IllegalArgumentException, SQLException {

       try (PreparedStatement stmt = connection.prepareStatement(selectAuditLogQueryByObjectId);) {
           stmt.setLong(1, objectId);

           ArrayList<AuditEntryOutputDto> entries = new ArrayList<AuditEntryOutputDto>(); 
           ResultSet rs = stmt.executeQuery();
           while (rs.next()) { 
             entries.add(convertRsToAuditLog(rs));                                                         
           }             
           return entries;
           
       } catch (SQLException e) {
           log.error("SQL Exception in g getAuditLogByObjectId: " + e.getMessage());
           throw e;
       }
   }
    
    public ArrayList<AuditEntryOutputDto> getAllAudit() throws SQLException {

        ArrayList<AuditEntryOutputDto> entryList = new ArrayList<AuditEntryOutputDto>();
        try (PreparedStatement stmt = connection.prepareStatement(selectAllAuditLogQuery);) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) { // maximum one due to unique/primary key constraint
                AuditEntryOutputDto auditLog = convertRsToAuditLog(rs);
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
            stmt.setLong(1, id);     
            stmt.setLong(2, auditLog.getObjectId());
            stmt.setLong(3, System.currentTimeMillis());                         
            stmt.setString(4, getCurrentUsername(auditLog.getUserName()));
            stmt.setString(5, auditLog.getChangeType().getValue());
            stmt.setString(6, auditLog.getChangeName().getValue());
            stmt.setString(7, auditLog.getChangeComment());               
            stmt.setString(8, auditLog.getTextBefore());
            stmt.setString(9, auditLog.getTextAfter());
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in persistAuditLog: " + e.getMessage());
            throw e;
        }
        return id;
    }

    private AuditEntryOutputDto convertRsToAuditLog( ResultSet rs) throws SQLException {
        long auditLogId = rs.getLong(AUDITLOG_ID_COLUMN);
        long objectId = rs.getLong(AUDITLOG_OBJECTID_COLUMN);
        long modifiedTime = rs.getLong(AUDITLOG_MODIFIEDTIME_COLUMN);
        String userName= rs.getString(AUDITLOG_USERNAME_COLUMN);
        String changeType= rs.getString(AUDITLOG_CHANGETYPE_COLUMN);
        String changeName= rs.getString(AUDITLOG_CHANGENAME_COLUMN);
        String changeComment= rs.getString(AUDITLOG_CHANGECOMMENT_COLUMN);
        String textBefore= rs.getString(AUDITLOG_TEXTBEFORE_COLUMN);
        String textAfter = rs.getString(AUDITLOG_TEXTAFTER_COLUMN);

        AuditEntryOutputDto auditEntry= new AuditEntryOutputDto();
        auditEntry.setId(auditLogId);
        auditEntry.setObjectId(objectId);
        auditEntry.setModifiedTime(modifiedTime);
        auditEntry.setUserName(userName);
        auditEntry.setChangeType(ChangeTypeEnumDto.valueOf(changeType));
        auditEntry.setChangeName(ObjectTypeEnumDto.valueOf(changeName));
        auditEntry.setChangeComment(changeComment);
        auditEntry.setTextAfter(textAfter);
        auditEntry.setTextBefore(textBefore);
        return auditEntry;
    }

    /**
     * Gets the name of the current user from the OAuth token.
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