package dk.kb.license.storage;

/**
 * This is a persistent DTO.
 * See the documentation and UML model:<br>
 * licensemodule_uml.png<br>
 * License_validation_logic.png <br>
 * 
 */
public class AttributeValue extends Persistent {

	private String value;  

	public AttributeValue(String value){
		this.value=value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

    @Override
    public String toString() {
        return value;
    }


}
