package dk.kb.license.storage;


public class GroupType extends Persistent {


	private String key;
	private String value_dk;
	private String value_en;
	private String description_dk;
	private String description_en;
	private String query;
	private boolean restriction;
	
	public GroupType(Long id, String key,  String value_dk ,String value_en, String description_dk,String description_en, String query, boolean restriction) {
		super();
		this.id = id;
		this.key=key;
		this.value_dk = value_dk;
		this.value_en = value_en;
		this.description_dk= description_dk;
		this.description_en= description_en;
	    this.query = query;
		this.restriction=restriction;
	}
	
	public String getDescription_dk() {
		return description_dk;
	}

	public void setDescription_dk(String description_dk) {
		this.description_dk = description_dk;
	}

	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getQuery() {		 
		return query;
	}

	public void setValue_dk(String value_dk) {
		this.value_dk = value_dk;
	}


	public String getValue_dk() {
		return value_dk;
	}

 public boolean isRestrictionGroup() {
		return restriction;
	}
		
	public String getValue_en() {
		return value_en;
	}
	
	public String getDescription_en() {
		return description_en;
	}

	public String toString(){
		return key;
	}


    public String toPresentationtString() {
        return "GroupType [key=" + key + ", value_dk=" + value_dk + ", value_en=" + value_en + ", description_dk="
                + description_dk + ", description_en=" + description_en + ", query=" + query + ", restriction="
                + restriction + "]";
    }
}