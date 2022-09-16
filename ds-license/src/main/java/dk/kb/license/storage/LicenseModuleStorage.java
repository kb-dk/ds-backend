package dk.kb.license.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * The DB consist of the following tables:
 * 
 * 3 tables for configuration: PRESENTATIONTYPES: configured presentationtypes.
 * GROUPTYPES: configured groups ATTRIBUTETYPES: configured attributes
 * 
 * The following tables to store created licenses: LICENSE (top parent)
 * ATTRIBUTEGROUP (parent=LICENSE) ATTRIBUTE (parent = ATTRIBUTEGROUP) VALUE
 * (parent = ATTRIBUTE) LICENSECONTENT (parent = LICENSE) PRESENTATION (parent =
 * LICENSECONTENT)
 * 
 */
public class LicenseModuleStorage implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(LicenseModuleStorage.class);

    private Connection connection = null; // private
    private static BasicDataSource dataSource = null; // shared

    private long lastTimestamp = 0; // Remember last timestamp and make sure each is only used once;

    // statistics shown on monitor.jsp page
    public static Date INITDATE = null;

    // Table and column names
    private static final String LICENSEPRESENTATIONTYPES_TABLE = "PRESENTATIONTYPES";
    private static final String LICENSEGROUPTYPES_TABLE = "GROUPTYPES";
    private static final String ATTRIBUTETYPES_TABLE = "ATTRIBUTETYPES";
    private static final String LICENSE_TABLE = "LICENSE";
    private static final String ATTRIBUTEGROUP_TABLE = "ATTRIBUTEGROUP";
    private static final String ATTRIBUTE_TABLE = "ATTRIBUTE";
    private static final String VALUE_TABLE = "VALUE_ORG";
    private static final String LICENSECONTENT_TABLE = "LICENSECONTENT";
    private static final String PRESENTATION_TABLE = "PRESENTATION";
    private static final String AUDITLOG_TABLE = "AUDITLOG";
    
    private static final String VALIDTO_COLUMN = "VALIDTO";
    private static final String VALIDFROM_COLUMN = "VALIDFROM";
    private static final String NAME_COLUMN = "NAME";
    private static final String NAME_EN_COLUMN = "NAME_EN";
    private static final String DESCRIPTION_DK_COLUMN = "DESCRIPTION_DK";
    private static final String DESCRIPTION_EN_COLUMN = "DESCRIPTION_EN";
    private static final String QUERY_COLUMN = "QUERYSTRING";
    private static final String NUMBER_COLUMN = "NUMBER";
    private static final String LICENSEID_COLUMN = "LICENSEID";
    private static final String LICENSECONTENTID_COLUMN = "LICENSECONTENTID";
    private static final String ATTRIBUTEGROUPID_COLUMN = "ATTRIBUTEGROUPID";
    private static final String ATTRIBUTEID_COLUMN = "ATTRIBUTEID";
    
    private static final String ID_COLUMN = "ID"; // ID used for all tables
    private static final String KEY_COLUMN = "KEY_ID";
    private static final String VALUE_COLUMN = "VALUE_ORG";
    private static final String VALUE_DK_COLUMN = "VALUE_DK";
    private static final String VALUE_EN_COLUMN = "VALUE_EN";
    private static final String DENYGROUP_COLUMN = "DENYGROUP";

    //AUDITLOG
    private static final String MILLIS_COLUMN = "MILLIS";
    private static final String USERNAME_COLUMN = "USERNAME";
    private static final String CHANGETYPE_COLUMN = "CHANGETYPE";
    private static final String OBJECTNAME_COLUMN = "OBJECTNAME";
    private static final String TEXTBEFORE_COLUMN = "TEXTBEFORE";
    private static final String TEXTAFTER_COLUMN = "TEXTAFTER";
    
    private final static String selectLicensePresentationTypesQuery = " SELECT * FROM "
            + LICENSEPRESENTATIONTYPES_TABLE;

    private final static String selectAllLicensesQuery = " SELECT * FROM " + LICENSE_TABLE;
    private final static String selectAuditLogQuery = " SELECT * FROM " + AUDITLOG_TABLE + " WHERE millis = ? ";   
    private final static String selectLicenseQuery = " SELECT * FROM " + LICENSE_TABLE + " WHERE ID = ? ";

    private final static String persistLicensePresentationTypeQuery = "INSERT INTO "
            + LICENSEPRESENTATIONTYPES_TABLE + " (" + ID_COLUMN + "," + KEY_COLUMN + "," + VALUE_DK_COLUMN + ","
            + VALUE_EN_COLUMN +  ") VALUES (?,?,?,?)"; // #|?|=4

    private final static String persistAttributeGroupForLicenseQuery = "INSERT INTO " + ATTRIBUTEGROUP_TABLE + " ("
            + ID_COLUMN + "," + NUMBER_COLUMN + "," + LICENSEID_COLUMN + ") VALUES (?,?,?)"; // #|?|=3

    private final static String selectAttributeGroupsForLicenseQuery = " SELECT * FROM " + ATTRIBUTEGROUP_TABLE
            + " WHERE " + LICENSEID_COLUMN + "= ? ORDER BY NUMBER";

    private final static String persistAttributeForAttributeGroupQuery = "INSERT INTO " + ATTRIBUTE_TABLE + " ("
            + ID_COLUMN + "," + NAME_COLUMN + "," + ATTRIBUTEGROUPID_COLUMN + ") VALUES (?,?,?)"; // #|?|=3

    

    private final static String persistAuditLog = "INSERT INTO " + AUDITLOG_TABLE + " ("
            + MILLIS_COLUMN + "," + USERNAME_COLUMN + "," + CHANGETYPE_COLUMN + ","+OBJECTNAME_COLUMN +","+TEXTBEFORE_COLUMN +","+TEXTAFTER_COLUMN+") VALUES (?,?,?,?,?,?)"; // #|?|=6
    
    private final static String selectAttributesForAttributeGroupQuery = " SELECT * FROM " + ATTRIBUTE_TABLE + " WHERE "
            + ATTRIBUTEGROUPID_COLUMN + " = ?";

    private final static String persistValueForAttributeQuery = "INSERT INTO " + VALUE_TABLE + " (" + ID_COLUMN + ","
            + VALUE_COLUMN + "," + ATTRIBUTEID_COLUMN + ") VALUES (?,?,?)"; // #|?|=3

    private final static String selectValuesForAttributeQuery = " SELECT * FROM " + VALUE_TABLE + " WHERE "
            + ATTRIBUTEID_COLUMN + " = ?";

    private final static String selectLicenseGroupTypesQuery = " SELECT * FROM " + LICENSEGROUPTYPES_TABLE
            + " ORDER BY " + KEY_COLUMN;

    private final static String persistLicenseGroupTypeQuery = "INSERT INTO " + LICENSEGROUPTYPES_TABLE + " ("
            + ID_COLUMN + "," + KEY_COLUMN + "," + VALUE_DK_COLUMN + " ," + VALUE_EN_COLUMN + " ,"
            + DESCRIPTION_DK_COLUMN + " ," + DESCRIPTION_EN_COLUMN + " ," + QUERY_COLUMN + " ," + DENYGROUP_COLUMN
            + ") VALUES (?,?,?,?,?,?,?,?)"; // #|?|=8

    private final static String updateLicenseGroupTypeQuery = "UPDATE " + LICENSEGROUPTYPES_TABLE + " SET "
            + VALUE_DK_COLUMN + " = ? , " + VALUE_EN_COLUMN + " = ? ," + DESCRIPTION_DK_COLUMN + " = ? ,"
            + DESCRIPTION_EN_COLUMN + " = ? ," + QUERY_COLUMN + " = ? ," + DENYGROUP_COLUMN + " = ? " + "WHERE "
            + ID_COLUMN + " = ? ";

    private final static String updateLicensePresentationTypeQuery = "UPDATE " + LICENSEPRESENTATIONTYPES_TABLE
            + " SET " + VALUE_DK_COLUMN + " = ? , " + VALUE_EN_COLUMN + " = ? " + "WHERE " + ID_COLUMN + " = ? ";

    private final static String persistLicenseQuery = "INSERT INTO " + LICENSE_TABLE + " (" + ID_COLUMN + ","
            + NAME_COLUMN + "," + NAME_EN_COLUMN + "," + DESCRIPTION_DK_COLUMN + "," + DESCRIPTION_EN_COLUMN + ","
            + VALIDFROM_COLUMN + "," + VALIDTO_COLUMN + ") VALUES (?,?,?,?,?,?,?)"; // #|?|=7

    private final static String selectAttributeTypesQuery = " SELECT * FROM " + ATTRIBUTETYPES_TABLE
            + " ORDER BY " + VALUE_COLUMN;;

    private final static String deleteAttributeTypeByNameQuery = " DELETE FROM " + ATTRIBUTETYPES_TABLE
            + " WHERE " + VALUE_COLUMN + " = ?";

    private final static String deleteGroupTypeByKeyQuery = " DELETE FROM " + LICENSEGROUPTYPES_TABLE + " WHERE "
            + KEY_COLUMN + " = ?";

    private final static String deletePresentationTypeByKeyQuery = " DELETE FROM "
            + LICENSEPRESENTATIONTYPES_TABLE + " WHERE " + KEY_COLUMN + " = ?";

    private final static String persistAttributeTypeQuery = "INSERT INTO " + ATTRIBUTETYPES_TABLE + " ("
            + ID_COLUMN + "," + VALUE_COLUMN + ") VALUES (?,?)"; // #|?|=2

    private final static String selectLicenseContentForLicenseQuery = " SELECT * FROM " + LICENSECONTENT_TABLE
            + " WHERE " + LICENSEID_COLUMN + " = ? ";

    private final static String persistLicenseContentForLicenseQuery = "INSERT INTO " + LICENSECONTENT_TABLE + " ("
            + ID_COLUMN + "," + NAME_COLUMN + "," + LICENSEID_COLUMN + ") VALUES (?,?,?)"; // #|?|=3

    private final static String selectPresentationTypesForLicenseContentQuery = " SELECT * FROM " + PRESENTATION_TABLE
            + " WHERE " + LICENSECONTENTID_COLUMN + " = ? ";

    private final static String persistPresentationTypesForLicenseContentQuery = "INSERT INTO " + PRESENTATION_TABLE
            + " (" + ID_COLUMN + "," + NAME_COLUMN + "," + LICENSECONTENTID_COLUMN + ") VALUES (?,?,?)"; // #|?|=3

    // Deletes
    private final static String deletePresentationsByLicenseContentIdQuery = " DELETE FROM " + PRESENTATION_TABLE
            + " WHERE " + LICENSECONTENTID_COLUMN + " = ?";

    private final static String deleteLicenseContentsByLicenseIdQuery = " DELETE FROM " + LICENSECONTENT_TABLE
            + " WHERE " + LICENSEID_COLUMN + " = ?";

    private final static String deleteAttributesByAttributeGroupIdQuery = " DELETE FROM " + ATTRIBUTE_TABLE + " WHERE "
            + ATTRIBUTEGROUPID_COLUMN + " = ?";

    private final static String countAttributesByAttributeNameQuery = " SELECT COUNT(*) FROM " + ATTRIBUTE_TABLE
            + " WHERE " + NAME_COLUMN + " = ?";

    private final static String countGroupTypeByGroupNameQuery = " SELECT COUNT(*) FROM " + LICENSECONTENT_TABLE
            + " WHERE " + NAME_COLUMN + " = ?";

    private final static String countPresentationTypeByPresentationNameQuery = " SELECT COUNT(*) FROM "
            + PRESENTATION_TABLE + " WHERE " + NAME_COLUMN + " = ?";

    private final static String deleteValuesByAttributeIdQuery = " DELETE FROM " + VALUE_TABLE + " WHERE "
            + ATTRIBUTEID_COLUMN + " = ?";

    private final static String deleteAttributeGroupByLicenseIdQuery = " DELETE FROM " + ATTRIBUTEGROUP_TABLE
            + " WHERE " + LICENSEID_COLUMN + " = ?";

    private final static String deleteLicenseByLicenseIdQuery = " DELETE FROM " + LICENSE_TABLE + " WHERE " + ID_COLUMN
            + " = ?";

    public static void initialize(String driverName, String driverUrl, String userName, String password) {
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName(driverName);
        dataSource.setUsername(userName);
        dataSource.setPassword(password);
        dataSource.setUrl(driverUrl);

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

    public LicenseModuleStorage() throws SQLException {
        connection = dataSource.getConnection();
    }

    public void persistLicensePresentationType(String key, String value_dk, String value_en) throws Exception {
        log.info("Persisting new license presentationtype: " + key);

        validateValue(key);
        validateValue(value_dk);
        value_dk = value_dk.trim();
        key = key.trim();
        validateValue(value_en);
        value_en = value_en.trim();

        try (PreparedStatement stmt = connection.prepareStatement(persistLicensePresentationTypeQuery);) {
            stmt.setLong(1, generateUniqueID());
            stmt.setString(2, key);
            stmt.setString(3, value_dk);
            stmt.setString(4, value_en);
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in persistPresentationType:" + e.getMessage());
            throw e;
        }
       
    }
    
  
    public void persistAuditLog(AuditLog auditlog) throws Exception {
        log.info("Persisting  persistAuditLog " + auditlog.getChangeType() +" for username:"+auditlog.getUsername());
        try (PreparedStatement stmt = connection.prepareStatement(persistAuditLog);) {
            stmt.setLong(1,  auditlog.getMillis());
            stmt.setString(2,auditlog.getUsername());            
            stmt.setString(3, auditlog.getChangeType());
            stmt.setString(4, auditlog.getObjectName());
            stmt.setString(5, auditlog.getTextBefore());
            stmt.setString(6, auditlog.getTextAfter());
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in persistAuditLog:" + e.getMessage());
            throw e;
        }       
    }
    
    public ArrayList<PresentationType> getLicensePresentationTypes() throws SQLException {

        ArrayList<PresentationType> list = new ArrayList<PresentationType>();

        try (PreparedStatement stmt = connection.prepareStatement(selectLicensePresentationTypesQuery);) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Long id = rs.getLong(ID_COLUMN);
                String key = rs.getString(KEY_COLUMN);
                String value = rs.getString(VALUE_DK_COLUMN);
                String value_en = rs.getString(VALUE_EN_COLUMN);
                PresentationType item = new PresentationType(id, key, value,
                        value_en);
                list.add(item);
            }
            return list;
        } catch (SQLException e) {
            log.error("SQL Exception in getPresentationTypes:" + e.getMessage());
            throw e;
        }
    }


    public void deleteLicense(long licenseId) throws Exception {
        log.info("Deleting license with id: " + licenseId);
        License license = null;
        try {
            license = getLicense(licenseId);
        } catch (IllegalArgumentException e) {
            // No license in DB with that ID, nothing to delete
            return;
        }

        for (AttributeGroup currentAttributeGroup : license.getAttributeGroups()) {

            ArrayList<Attribute> attributes = currentAttributeGroup.getAttributes();
            for (Attribute currentAttribute : attributes) {
                deleteById(deleteValuesByAttributeIdQuery, currentAttribute.getId());
            }

            deleteById(deleteAttributesByAttributeGroupIdQuery, currentAttributeGroup.getId());
        }

        for (LicenseContent currentLicenseContent : license.getLicenseContents()) {
            deleteById(deletePresentationsByLicenseContentIdQuery, currentLicenseContent.getId());
        }

        deleteById(deleteLicenseContentsByLicenseIdQuery, licenseId);
        deleteById(deleteAttributeGroupByLicenseIdQuery, licenseId);
        deleteById(deleteLicenseByLicenseIdQuery, licenseId);

        LicenseCache.reloadCache(); // Force reload so the change will be instant in the cache
    }

    // query can be null or empty
    public void persistLicenseGroupType(String key, String value, String value_en, String description,
            String description_en, String query, boolean denyGroup) throws Exception {

        if (!StringUtils.isNotEmpty(key)) {
            throw new IllegalArgumentException("Key can not be null when creating new Group");
        }

        if (!StringUtils.isNotEmpty(value)) {
            throw new IllegalArgumentException("Value can not be null when creating new Group");
        }

        if (!StringUtils.isNotEmpty(value_en)) {
            throw new IllegalArgumentException("Value(EN) can not be null when creating new Group");
        }

        if (!StringUtils.isNotEmpty(query)) {
            throw new IllegalArgumentException("Query can not be null when creating new Group");
        }

        log.info("Persisting new  license group type: " + key);

        validateValue(value);
        value = value.trim();

        try (PreparedStatement stmt = connection.prepareStatement(persistLicenseGroupTypeQuery);) {
            stmt.setLong(1, generateUniqueID());
            stmt.setString(2, key);
            stmt.setString(3, value);
            stmt.setString(4, value_en);
            stmt.setString(5, description);
            stmt.setString(6, description_en);
            stmt.setString(7, query);
            stmt.setBoolean(8, denyGroup);
            stmt.execute();
            connection.commit();
        } catch (SQLException e) {
            log.error("SQL Exception in persistLicenseGroupType:" + e.getMessage());
            throw e;
        }
        LicenseCache.reloadCache(); // Force reload so the change will be instant in the cache
    }

    public void updateLicenseGroupType(long id, String value_dk, String value_en, String description,
            String description_en, String query, boolean denyGroup) throws Exception {

        try (PreparedStatement stmt = connection.prepareStatement(updateLicenseGroupTypeQuery);) {
            log.info("Updating Group type with id:" + id);

            // if it exists already, we do not add it.

            stmt.setString(1, value_dk);
            stmt.setString(2, value_en);
            stmt.setString(3, description);
            stmt.setString(4, description_en);
            stmt.setString(5, query);
            stmt.setBoolean(6, denyGroup);
            stmt.setLong(7, id);

            int updated = stmt.executeUpdate();
            if (updated != 1) {
                throw new SQLException("Grouptype id not found:" + id);
            }

            connection.commit();

        } catch (Exception e) {
            log.error("Exception in updateLicenseGroupType:" + e.getMessage());
            throw e;
        }
        LicenseCache.reloadCache(); // Force reload so the change will be instant in the cache
    }

    public void updatePresentationType(long id, String value_dk, String value_en) throws Exception {

        try (PreparedStatement stmt = connection.prepareStatement(updateLicensePresentationTypeQuery);) {
            log.info("Updating Presentation type with id:" + id);

            // if it exists already, we do not add it.
            stmt.setString(1, value_dk);
            stmt.setString(2, value_en);
            stmt.setLong(3, id);

            int updated = stmt.executeUpdate();
            if (updated != 1) {
                throw new SQLException("Presentationtype id not found:" + id);
            }

            connection.commit();

        } catch (Exception e) {
            log.error("Exception in updatePresentationType:" + e.getMessage());
            throw e;
        }
        LicenseCache.reloadCache(); // Force reload so the change will be instant in the cache
    }

    public void deleteLicenseGroupType(String groupName) throws Exception {

        log.info("Deleting grouptype: " + groupName);
        // First check it is not used in any license, in that case throw exception.

        try (PreparedStatement stmt = connection.prepareStatement(countGroupTypeByGroupNameQuery);) {
            stmt.setString(1, groupName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int number = rs.getInt(1);
                if (number > 0) {
                    throw new IllegalArgumentException("Can not delete group with name:" + groupName
                            + " because it is used in at least 1 license");
                }
            }

        } catch (SQLException e) {
            log.error("SQL Exception in deleteLicenseGroupType:" + e.getMessage());
            throw e;
        }

        try (PreparedStatement stmt = connection.prepareStatement(deleteGroupTypeByKeyQuery);) {
            stmt.setString(1, groupName);
            int updated = stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("SQL Exception in deleteLicenseGroupType:" + e.getMessage());
            throw e;
        }

        LicenseCache.reloadCache(); // Force reload so the change will be instant in the cache
    }

    public void deletePresentationType(String presentationName) throws Exception {

        log.info("Deleting presentation type: " + presentationName);
        // First check it is not used in any license, in that case throw exception.

        try (PreparedStatement stmt = connection.prepareStatement(countPresentationTypeByPresentationNameQuery);) {
            stmt.setString(1, presentationName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int number = rs.getInt(1);
//               System.out.println("presentation used times:"+number + " with name:"+presentationName);
                if (number > 0) {
                    throw new IllegalArgumentException("Can not delete presentationtype with name:" + presentationName
                            + " because it is used in at least 1 license");
                }
            }

        } catch (SQLException e) {
            log.error("SQL Exception in deletePresentationType:" + e.getMessage());
            throw e;
        }

        try (PreparedStatement stmt = connection.prepareStatement(deletePresentationTypeByKeyQuery);) {
            stmt.setString(1, presentationName);
            int updated = stmt.executeUpdate();
            log.info("deleted " + updated + " presentationtype with name:" + presentationName);

        } catch (SQLException e) {
            log.error("SQL Exception in deletePresentationType:" + e.getMessage());
            throw e;
        }

        LicenseCache.reloadCache(); // Force reload so the change will be instant in the cache
    }

    public void persistLicense(License license) throws Exception {

        log.info("Persisting new license: " + license.getLicenseName());

        // validate name, description, validTo,validFrom
        boolean validateMainFields = license.validateMainFields();
        boolean validateAttributesValues = license.validateAttributesAndValuesNotNull();
        if (!validateMainFields) {
            throw new IllegalArgumentException(
                    "Validation error. Name/description too short or validTo/validFrom not legal dates");
        }
        if (!validateAttributesValues) {
            throw new IllegalArgumentException("Validation error. Attributes or values can not be empty");
        }

        Long licenseId;
        if (license.getId() > 0) { // This is an existing license in the DB, delete it before updating
            licenseId = license.getId();
            // Delete old license before updating (creating new)
            log.info("Deleting license before updating");
            deleteLicense(licenseId);

        } else {
            licenseId = generateUniqueID(); // new ID.
        }
        try (PreparedStatement stmt = connection.prepareStatement(persistLicenseQuery);) {

            stmt.setLong(1, licenseId);
            stmt.setString(2, license.getLicenseName());
            stmt.setString(3, license.getLicenseName_en());
            stmt.setString(4, license.getDescription_dk());
            stmt.setString(5, license.getDescription_en());
            stmt.setString(6, license.getValidFrom());
            stmt.setString(7, license.getValidTo());
            stmt.execute();

            persistAttributeGroupsForLicense(licenseId, license.getAttributeGroups());
            persistLicenseContentsForLicense(licenseId, license.getLicenseContents());

            
        } catch (Exception e) {
            log.error("SQL Exception in persistLicense:" + e.getMessage());
            throw e;
        }
    
        
        //TODO! Important! Remove to facade!
        //LicenseCache.reloadCache(); // Force reload so the change will be instant in the cache
    }

    public ArrayList<GroupType> getLicenseGroupTypes() throws SQLException {

        ArrayList<GroupType> list = new ArrayList<GroupType>();
        try (PreparedStatement stmt = connection.prepareStatement(selectLicenseGroupTypesQuery);) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Long id = rs.getLong(ID_COLUMN);
                String key = rs.getString(KEY_COLUMN);
                String value_dk = rs.getString(VALUE_DK_COLUMN);
                String value_en = rs.getString(VALUE_EN_COLUMN);
                String description = rs.getString(DESCRIPTION_DK_COLUMN);
                String description_en = rs.getString(DESCRIPTION_EN_COLUMN);
                String query = rs.getString(QUERY_COLUMN);
                boolean denyGroup = rs.getBoolean(DENYGROUP_COLUMN);
                GroupType item = new GroupType(id, key, value_dk, value_en,description, description_en, query, denyGroup);
                list.add(item);
            }
            return list;
        } catch (SQLException e) {
            log.error("SQL Exception in getLicenseGroupTypes():" + e.getMessage());
            throw e;
        }
    }

    public void persistAttributeType(String value) throws Exception {

        log.info("Persisting new  attribute type: " + value);

        validateValue(value);
        value = value.trim();

        try (PreparedStatement stmt = connection.prepareStatement(persistAttributeTypeQuery);) {
            stmt.setLong(1, generateUniqueID());
            stmt.setString(2, value);
            stmt.execute();

        } catch (SQLException e) {
            log.error("SQL Exception in persistAttributeType:" + e.getMessage());
            throw e;
        }
        LicenseCache.reloadCache(); // Force reload so the change will be instant in the cache
    }

    public void deleteAttributeType(String attributeTypeName) throws Exception {

        log.info("Deleting attributetype: " + attributeTypeName);
        // First check it is not used in any license, in that case throw exception.

        try (PreparedStatement stmt = connection.prepareStatement(countAttributesByAttributeNameQuery);) {

            stmt.setString(1, attributeTypeName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int number = rs.getInt(1);
                if (number > 0) {
                    throw new IllegalArgumentException("Can not delete attribute with name:" + attributeTypeName
                            + " because it is used in at least 1 license");
                }
            }

        } catch (SQLException e) {
            log.error("SQL Exception in deleteAttributeType:" + e.getMessage());
            throw e;
        }

        try (PreparedStatement stmt = connection.prepareStatement(deleteAttributeTypeByNameQuery);) {
            stmt.setString(1, attributeTypeName);
            int updated = stmt.executeUpdate();
            log.info("deleted " + updated + " attributetypes with name:" + attributeTypeName);
        } catch (SQLException e) {
            log.error("SQL Exception in deleteAttributeType:" + e.getMessage());
            throw e;
        }

        LicenseCache.reloadCache(); // Force reload so the change will be instant in the cache
    }

    public ArrayList<AttributeType> getAttributeTypes() throws SQLException {

        ArrayList<AttributeType> list = new ArrayList<AttributeType>();

        try (PreparedStatement stmt = connection.prepareStatement(selectAttributeTypesQuery);) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Long id = rs.getLong(ID_COLUMN);
                String value = rs.getString(VALUE_COLUMN);
                AttributeType item = new AttributeType(id, value);
                list.add(item);
            }
            return list;
        } catch (SQLException e) {
            log.error("SQL Exception in getAttributes:" + e.getMessage());
            throw e;
        }
    }

    // this method only loads the name of the license and NOT all associations.
    public ArrayList<License> getAllLicenseNames() throws SQLException {

        ArrayList<License> list = new ArrayList<License>();
        try (PreparedStatement stmt = connection.prepareStatement(selectAllLicensesQuery);) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                License license = new License();
                Long id = rs.getLong(ID_COLUMN);
                String name = rs.getString(NAME_COLUMN);
                String description_dk = rs.getString(DESCRIPTION_DK_COLUMN);
                String description_en = rs.getString(DESCRIPTION_EN_COLUMN);
                String validFrom = rs.getString(VALIDFROM_COLUMN);
                String validTo = rs.getString(VALIDTO_COLUMN);
                license.setId(id);
                license.setLicenseName(name);
                license.setDescription_dk(description_dk);
                license.setDescription_en(description_en);
                license.setValidFrom(validFrom);// todo format
                license.setValidTo(validTo);// todo format
                list.add(license);
            }
            return list;
        } catch (SQLException e) {
            log.error("SQL Exception in getAllLicenseNames:" + e.getMessage());
            throw e;
        }
    }

    // this method a license from licenseId with all associations (complete
    // object-tree)
    public License getLicense(long licenseId) throws Exception {

        License license = new License();

        try (PreparedStatement stmt = connection.prepareStatement(selectLicenseQuery);) {
            stmt.setLong(1, licenseId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) { // maximum one due to unique/primary key constraint
                Long id = rs.getLong(ID_COLUMN);
                String name = rs.getString(NAME_COLUMN);
                String name_en = rs.getString(NAME_EN_COLUMN);
                String description_dk = rs.getString(DESCRIPTION_DK_COLUMN);
                String description_en = rs.getString(DESCRIPTION_EN_COLUMN);
                String validFrom = rs.getString(VALIDFROM_COLUMN);
                String validTo = rs.getString(VALIDTO_COLUMN);
                license.setId(id);
                license.setLicenseName(name);
                license.setLicenseName_en(name_en);
                license.setDescription_dk(description_dk);
                license.setDescription_en(description_en);
                license.setValidFrom(validFrom);
                license.setValidTo(validTo);

                license.setAttributeGroups(getAttributeGroupsForLicense(id));
                license.setLicenseContents(getLicenseContentsForLicense(id));

                return license;
            }
            throw new IllegalArgumentException("License not found for licenseId:" + licenseId);

        } catch (SQLException e) {
            log.error("SQL Exception in getLicense:" + e.getMessage());
            throw e;
        }
    }

   /**
    *  
    * @param millis ID is of the auditlog
    * @return
    * @throws Exception
    */
    
    
    public AuditLog getAuditLog(long millis) throws Exception {

        License license = new License();

        try (PreparedStatement stmt = connection.prepareStatement(selectAuditLogQuery);) {
            stmt.setLong(1, millis);
            
            ResultSet rs = stmt.executeQuery();            
            while (rs.next()) { // maximum one due to unique/primary key constraint
                String username = rs.getString(USERNAME_COLUMN);
                String changetype = rs.getString(CHANGETYPE_COLUMN);
                String objectName = rs.getString(OBJECTNAME_COLUMN);
                String textBefore = rs.getString(TEXTBEFORE_COLUMN);
                String textAfter = rs.getString(TEXTAFTER_COLUMN);
                AuditLog audit=new AuditLog(millis,username, changetype,objectName,textBefore,textAfter);
                return audit;
            }
            throw new IllegalArgumentException("AuditId not found for millis:" + millis);

        } catch (SQLException e) {
            log.error("SQL Exception in getLicense:" + e.getMessage());
            throw e;
        }
    }
    
    
    protected void validateValue(String value) {
        // sanity, must have length at least 2
        if (value == null || value.trim().length() < 2) {
            throw new IllegalArgumentException("Value empty or too short");
        }

    }

    /*
     * public StatisticsDTO getStatistics() throws SQLException{
     * 
     * StatisticsDTO output = new StatisticsDTO(); long
     * t1=System.currentTimeMillis(); PreparedStatement stmt = null; try{
     * 
     * //Extract statistics for number of items added for each day, up to 50 days
     * back. Calendar cal = Calendar.getInstance();
     * 
     * Date start=cal.getTime(); Date end=cal.getTime();; ArrayList<Integer>
     * itemsAddedDaysAgo = new ArrayList<Integer>();
     * 
     * for (int daysAgo=0;daysAgo<50;daysAgo++){ //last 50 days
     * cal.add(Calendar.DAY_OF_YEAR, -1); start=cal.getTime();
     * 
     * long startLong=start.getTime(); long endLong=end.getTime(); stmt =
     * singleDBConnection.prepareStatement("SELECT COUNT(*) FROM " +
     * ATTRIBUTESTORE_TABLE + " WHERE "+MODIFIED_COLUMN +" > ? AND "
     * +MODIFIED_COLUMN +" <= ?"); stmt.setLong(1, startLong); stmt.setLong(2,
     * endLong); ResultSet rs=stmt.executeQuery(); while (rs.next()){ int
     * inserted=rs.getInt(1); itemsAddedDaysAgo.add(new Integer(inserted)); }
     * end=cal.getTime(); }
     * 
     * stmt = singleDBConnection.prepareStatement("SELECT COUNT(*) FROM " +
     * ATTRIBUTESTORE_TABLE); ResultSet rs=stmt.executeQuery(); rs.next(); //Will
     * always have 1 row int count = rs.getInt(1);
     * 
     * output.setTotalItems(count); output.setItemsAddedDaysAgo(itemsAddedDaysAgo);
     * output.setLast100KeyValuesAdded(lastKeyValues);
     * output.setExtractTimeInMillis(System.currentTimeMillis()-t1);
     * 
     * 
     * return output; } catch (SQLException e){ e.printStackTrace();
     * log.error("SQL Exception in extractStatistics:"+e.getMessage()); throw e; }
     * finally{ closeStatement(stmt); }
     * 
     * 
     * }
     */

    protected void persistAttributeGroupsForLicense(Long licenseId, ArrayList<AttributeGroup> attributegroups)
            throws SQLException {

        if (attributegroups == null || attributegroups.size() == 0) {
            throw new IllegalArgumentException("No attributegroups defined for license");
        }

        for (AttributeGroup current : attributegroups) {
            long attributeGroupId = generateUniqueID();

            try (PreparedStatement stmt = connection.prepareStatement(persistAttributeGroupForLicenseQuery);) {

                stmt.setLong(1, attributeGroupId);
                stmt.setInt(2, current.getNumber());
                stmt.setLong(3, licenseId);
                stmt.execute();

                persistAttributesForAttributeGroup(attributeGroupId, current.getAttributes());
            } catch (SQLException e) {
                log.error("SQL Exception in persistAttributeGroupsForLicense:" + e.getMessage());
                throw e;
            }
        }
    }

    // This method delete by ID from a given tabel defined in the query
    protected void deleteById(String query, Long id) throws SQLException {

        try (PreparedStatement stmt = connection.prepareStatement(query);) {
            stmt.setLong(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            log.error("SQL Exception in deleteById for query:" + query + " Exception:" + e.getMessage());
            throw e;
        }
    }

    protected ArrayList<AttributeGroup> getAttributeGroupsForLicense(Long licenseId) throws SQLException {

        ArrayList<AttributeGroup> list = new ArrayList<AttributeGroup>();
        try (PreparedStatement stmt = connection.prepareStatement(selectAttributeGroupsForLicenseQuery);) {
            stmt.setLong(1, licenseId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Long id = rs.getLong(ID_COLUMN);
                int number = rs.getInt(NUMBER_COLUMN);
                AttributeGroup item = new AttributeGroup(number);
                item.setId(id);
                list.add(item);

                ArrayList<Attribute> attributes = getAttributesForAttributeGroup(id);
                item.setAttributes(attributes);
            }
            return list;
        } catch (SQLException e) {
            log.error("SQL Exception in getAttributeGroupsForLicense:" + e.getMessage());
            throw e;
        }
    }

    protected void persistAttributesForAttributeGroup(Long attributeGroupId, ArrayList<Attribute> attributes)
            throws SQLException {

        if (attributes == null || attributes.size() == 0) {
            throw new IllegalArgumentException("No attributes defined for attributegroup:" + attributeGroupId);
        }

        for (Attribute current : attributes) {
            try (PreparedStatement stmt = connection.prepareStatement(persistAttributeForAttributeGroupQuery);) {
                long attributeId = generateUniqueID();
                stmt.setLong(1, attributeId);
                stmt.setString(2, current.getAttributeName());
                stmt.setLong(3, attributeGroupId);
                stmt.execute();

                persistValuesForAttribute(attributeId, current.getValues());
            } catch (SQLException e) {
                log.error("SQL Exception in persistAttributesForAttributeGroup:" + e.getMessage());
                throw e;
            }
        }
    }

    protected ArrayList<LicenseContent> getLicenseContentsForLicense(Long licenseId) throws SQLException {
        ArrayList<LicenseContent> list = new ArrayList<LicenseContent>();
        try (PreparedStatement stmt = connection.prepareStatement(selectLicenseContentForLicenseQuery);) {

            stmt.setLong(1, licenseId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Long id = rs.getLong(ID_COLUMN);
                String name = rs.getString(NAME_COLUMN);
                LicenseContent item = new LicenseContent();
                item.setId(id);
                item.setName(name);
                list.add(item);

                ArrayList<Presentation> presentations = getPresentationsForLicenseContent(id);
                item.setPresentations(presentations);

            }
            return list;
        } catch (SQLException e) {
            log.error("SQL Exception in getLicenseContentsForLicense:" + e.getMessage());
            throw e;
        }

    }

    protected void persistLicenseContentsForLicense(Long licenseId, ArrayList<LicenseContent> licenseContents)
            throws SQLException {

        for (LicenseContent current : licenseContents) {

            try (PreparedStatement stmt = connection.prepareStatement(persistLicenseContentForLicenseQuery);) {
                long licenseContentId = generateUniqueID();

                stmt.setLong(1, licenseContentId);
                stmt.setString(2, current.getName());
                stmt.setLong(3, licenseId);
                stmt.execute();

                persistPresentationsForLicenseContent(licenseContentId, current.getPresentations());

            } catch (SQLException e) {
                log.error("SQL Exception in persistLicenseContentsForLicense:" + e.getMessage());
                throw e;
            }
        }
    }

    protected void persistPresentationsForLicenseContent(Long licenseContentId, ArrayList<Presentation> presentations)
            throws SQLException {

        if (presentations == null || presentations.size() == 0) {
            throw new IllegalArgumentException("No presentationtypes defined for licensecontentId:" + licenseContentId);
        }

        for (Presentation current : presentations) {
            try (PreparedStatement stmt = connection.prepareStatement(persistPresentationTypesForLicenseContentQuery);) {

                stmt.setLong(1, generateUniqueID());
                stmt.setString(2, current.getKey());
                stmt.setLong(3, licenseContentId);
                stmt.execute();
            } catch (SQLException e) {
                log.error("SQL Exception in persistPresentationsForLicenseContent:" + e.getMessage());
                throw e;
            }
        }
    }

    protected ArrayList<Presentation> getPresentationsForLicenseContent(long licenseContentId) throws SQLException {
        
        ArrayList<Presentation> list = new ArrayList<Presentation>();                
        try (PreparedStatement stmt = connection.prepareStatement(selectPresentationTypesForLicenseContentQuery);) {
        
            stmt.setLong(1, licenseContentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Long id = rs.getLong(ID_COLUMN);
                String name = rs.getString(NAME_COLUMN);
                Presentation item = new Presentation(name);
                item.setId(id);
                list.add(item);
            }
            return list;
        } catch (SQLException e) {
            log.error("SQL Exception in getPresentationsForLicenseContent:" + e.getMessage());
            throw e;
        }

    }

    protected ArrayList<Attribute> getAttributesForAttributeGroup(long attributeGroupId) throws SQLException {
        
        ArrayList<Attribute> list = new ArrayList<Attribute>();
        
        try (PreparedStatement stmt = connection.prepareStatement(selectAttributesForAttributeGroupQuery);) {
        
            stmt.setLong(1, attributeGroupId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Long id = rs.getLong(ID_COLUMN);
                String name = rs.getString(NAME_COLUMN);
                Attribute item = new Attribute();
                item.setId(id);
                item.setAttributeName(name);

                ArrayList<AttributeValue> attributeValues = getValuesForAttribute(id);
                item.setValues(attributeValues);
                list.add(item);

            }
            return list;
        } catch (SQLException e) {
            log.error("SQL Exception in getAttributesForAttributeGroup:" + e.getMessage());
            throw e;
        } 
    }

    protected void persistValuesForAttribute(Long attributeId, ArrayList<AttributeValue> values) throws SQLException {

        if (values == null || values.size() == 0) {
            throw new IllegalArgumentException("No values defined for attribute:" + attributeId);
        }

        for (AttributeValue current : values) {

            try (PreparedStatement stmt = connection.prepareStatement(persistValueForAttributeQuery);) {
                stmt.setLong(1, generateUniqueID());
                stmt.setString(2, current.getValue());
                stmt.setLong(3, attributeId);
                stmt.execute();
            }

         catch (SQLException e) {
            log.error("SQL Exception in  persistValuesForAttribute:" + e.getMessage());
            throw e;
        }
        }
    }

    protected ArrayList<AttributeValue> getValuesForAttribute(long attributeId) throws SQLException {
        
        ArrayList<AttributeValue> list = new ArrayList<AttributeValue>();
        try (PreparedStatement stmt = connection.prepareStatement(selectValuesForAttributeQuery);) {      
        
            stmt.setLong(1, attributeId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Long id = rs.getLong(ID_COLUMN);
                String value = rs.getString(VALUE_COLUMN);
                AttributeValue item = new AttributeValue(value);
                item.setId(id);
                item.setValue(value);
                list.add(item);
            }
            return list;
        } catch (SQLException e) {
            log.error("SQL Exception in getValuesForAttribute:" + e.getMessage());
            throw e;
        } 

    }

    // Just a simple way to generate unique ID's and make sure they are unique
    private synchronized long generateUniqueID() {
        long now = System.currentTimeMillis();
        if (now <= lastTimestamp) { // this timestamp has already been used. just +1 and use that
            lastTimestamp++;
            return lastTimestamp;
        } else {
            lastTimestamp = now;
            return now;
        }
    }

    // Used from unittests. Create tables DDL etc.
    protected synchronized void runDDLScript(File file) throws SQLException {
        log.info("Running DDL script:" + file.getAbsolutePath());

        if (!file.exists()) {
            log.error("DDL script not found:" + file.getAbsolutePath());
            throw new RuntimeException("DDLscript file not found:" + file.getAbsolutePath());
        }

        String scriptStatement = "RUNSCRIPT FROM '" + file.getAbsolutePath() + "'";

        connection.prepareStatement(scriptStatement).execute();        
    }

    /*
     * FOR TEST JETTY RUN ONLY!
     * 
     */
    public void createNewDatabase(String ddlFile) throws SQLException {
        connection.createStatement().execute("RUNSCRIPT FROM '" + ddlFile + "'");
    }

    /*
     * Only called from unittests, not exposed on facade class
     * 
     */
 
   

    public void commit() throws SQLException {
        connection.commit();
    }

    public void rollback() {
        try {
            connection.rollback();
        } catch (Exception e) {
            // nothing to do here
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (Exception e) {
            // nothing to do here
        }
    }

    /*
     * Only called from unittests, not exposed on facade class
     * 
     */
    
    
    public void clearTableRecords() throws SQLException {
      ArrayList<String> tables = new ArrayList<String>();
      tables.add("PRESENTATIONTYPES");
      tables.add("GROUPTYPES");
      tables.add("ATTRIBUTETYPES");
      tables.add("LICENSE");
      tables.add("ATTRIBUTEGROUP");
      tables.add("ATTRIBUTE");
      tables.add("VALUE_ORG");
      tables.add("LICENSECONTENT");    
      tables.add("PRESENTATION");
      
      
      for (String table : tables) {
          String deleteSQL="DELETE FROM " +table; 
        try (PreparedStatement stmt = connection.prepareStatement(deleteSQL);) {
            stmt.execute();
        }   
        
      }
      log.info("All tables cleared for unittest");
    }
    
    
    // This is called by from InialialziationContextListener by the Web-container
    // when server is shutdown,
    // Just to be sure the DB lock file is free.
    public static void shutdown() {
        log.info("Shutdown ds-storage");
        try {
            if (dataSource != null) {
                dataSource.close();
            }
        } catch (Exception e) {
            // ignore errors during shutdown, we cant do anything about it anyway
            log.error("shutdown failed", e);
        }
    }

}
