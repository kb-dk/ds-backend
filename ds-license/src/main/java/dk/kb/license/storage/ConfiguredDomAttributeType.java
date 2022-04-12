package dk.kb.license.storage;

public class ConfiguredDomAttributeType extends Persistent{

	private String value;
				
	public ConfiguredDomAttributeType(Long id, String value) {
		super();
		this.id = id;
		this.value = value;
	}

	public String getValue() {
		return value;
	}
		
}
