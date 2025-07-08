package dk.kb.license.storage;


/**
 * This is a persistent DTO.
 * See the documentation and UML model:<br>
 * licensemodule_uml.png<br>
 * License_validation_logic.png <br>
 * 
 */
public class AttributeType extends Persistent{

	private String value;
				
	public AttributeType(String value) {
		super();
		this.value = value;
	}

	public String getValue() {
		return value;
	}
		
}
