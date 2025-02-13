package dk.kb.license.storage;

public class RestrictedId {
    private String id;
    private String idType;
    private String system;
    private String comment;
    private String modified_by;
    private long modified_time;

    public RestrictedId(String id, String idType, String system, String comment, String modified_by, long modified_time) {
        this.id = id;
        this.idType = idType;
        this.system = system;
        this.comment = comment;
        this.modified_by = modified_by;
        this.modified_time = modified_time;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getModified_by() {
        return modified_by;
    }

    public void setModified_by(String modified_by) {
        this.modified_by = modified_by;
    }

    public long getModified_time() {
        return modified_time;
    }

    public void setModified_time(long modified_time) {
        this.modified_time = modified_time;
    }
}
