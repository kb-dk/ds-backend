package dk.kb.license.storage;

/**
 * Internal DTO used in the AUDITLOG table.
 * 
 * Every time a user changes any license or configuration definitions there will be generated an entry in the auditlog table.
 * 
 * The log-line will have a textual diff of the change in the license.
 * 
 */
public class AuditLog {

    private long millis;
    private String username;
    private String changeType;
    private String objectName;
    private String textBefore;
    private String textAfter;
    
    
    public AuditLog(long millis, String username, String changeType, String objectName, String textBefore,String textAfter) {
        super();
        this.millis = millis;
        this.username = username;
        this.changeType = changeType;
        this.objectName = objectName;
        this.textBefore = textBefore;
        this.textAfter = textAfter;
    }


    public long getMillis() {
        return millis;
    }


    public String getUsername() {
        return username;
    }


    public String getChangeType() {
        return changeType;
    }


    public String getObjectName() {
        return objectName;
    }


    public String getTextBefore() {
        return textBefore;
    }


    public String getTextAfter() {
        return textAfter;
    }


    @Override
    public String toString() {
        return "AuditLog [millis=" + millis + ", username=" + username + ", changeType=" + changeType + ", objectName="
                + objectName + ", textBefore=" + textBefore + ", textAfter=" + textAfter + "]";
    }
    
    
}
