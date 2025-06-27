package dk.kb.license.storage;

import org.apache.hc.core5.http.NotImplementedException;

/**
 * Internal DTO used in the AUDITLOG table when adding a new audit for an object change
 * Every time a user changes any license or configuration definitions there will be generated an entry in the auditlog table.
 * The entry will have the objectId of the modified objects so all changes on that object can be extracted.
 * 
 *  The service generated AuditLogDto object is used when extracting information from the AuditLog
 * 
 */
public class AuditLogEntry {
   
    private long objectId;
    private String userName;   
    private String changeType; //skal være enum
    private String changeName;  //skal være enum
    private String changeValue;
    private String textBefore; 
    private String textAfter;
   
    
    public AuditLogEntry(long millis, String username, String changeType, String objectName, String textBefore,String textAfter)  throws Exception{
        throw new NotImplementedException();
    }
    
    public AuditLogEntry (long objectId,  String userName, String changeType, String changeName,
                    String changeValue, String textBefore, String textAfter) {        
        this.objectId = objectId;
        this.userName = userName;
        this.changeType = changeType;
        this.changeName = changeName;
        this.changeValue = changeValue;
        this.textBefore = textBefore;
        this.textAfter = textAfter;
    }

    public long getObjectId() {
        return objectId;
    }

    public String getUserName() {
        return userName;
    }


    public String getChangeType() {
        return changeType;
    }


    public String getChangeName() {
        return changeName;
    }


    public String getChangeValue() {
        return changeValue;
    }


    public String getTextBefore() {
        return textBefore;
    }


    public String getTextAfter() {
        return textAfter;
    }

    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }


    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }


    public void setChangeName(String changeName) {
        this.changeName = changeName;
    }


    public void setChangeValue(String changeValue) {
        this.changeValue = changeValue;
    }


    public void setTextBefore(String textBefore) {
        this.textBefore = textBefore;
    }


    public void setTextAfter(String textAfter) {
        this.textAfter = textAfter;
    }

    @Override
    public String toString() {
        return "AuditLog [objectId=" + objectId + ", userName="
                + userName + ", changeType=" + changeType + ", changeName=" + changeName + ", changeValue=" + changeValue + ", textBefore=" + textBefore
                + ", textAfter=" + textAfter + "]";
    }
    
    
}
