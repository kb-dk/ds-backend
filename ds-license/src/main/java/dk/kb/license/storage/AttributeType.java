package dk.kb.license.storage;

public class AttributeType extends Persistent{

	private String value;
				
	public AttributeType(Long id, String value) {
		super();
		this.id = id;
		this.value = value;
	}

	public String getValue() {
		return value;
	}
		
}
