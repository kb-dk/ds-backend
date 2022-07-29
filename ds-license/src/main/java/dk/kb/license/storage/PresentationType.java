package dk.kb.license.storage;

public class PresentationType extends Persistent {
	
	private String key;
	private String value_dk;
	private String value_en;
		
	public PresentationType(long id, String key, String value_dk, String value_en) {
		super();
		this.id = id;
		this.key=key;
		this.value_dk = value_dk;
		this.value_en= value_en;
	}
	
	public String getValue_dk() {
		return value_dk;
	}

	public String getValue_en() {
		return value_en;
	}

	public String getKey() {
		return key;
	}
	
}
