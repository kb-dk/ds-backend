package dk.kb.license.storage;

/**
 * This is a persistent DTO.
 * See the documentation and UML model:
 * licensemodule_uml.png
 * License_validation_logic.png
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
    public void setValue(String value) {
    this.value = value;
  }
}
