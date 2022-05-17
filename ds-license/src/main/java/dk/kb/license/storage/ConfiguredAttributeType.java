package dk.kb.license.storage;

public class ConfiguredAttributeType extends Persistent{

	private String value;
				
	public ConfiguredAttributeType(Long id, String value) {
		super();
		this.id = id;
		this.value = value;
	}

	public String getValue() {
		return value;
	}
		
}
