package dk.kb.license.storage;

/**
 * This is a persistent DTO.
 * See the documentation and UML model:
 * licensemodule_uml.png
 * License_validation_logic.png
 */
public class PresentationType extends Persistent {
	
	private String key;
	private String value_dk;
	private String value_en;
		
	public PresentationType( String key, String value_dk, String value_en) {
		super();
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
