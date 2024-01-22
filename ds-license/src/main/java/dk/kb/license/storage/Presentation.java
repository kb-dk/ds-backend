package dk.kb.license.storage;

/**
 * This is a persistent DTO.
 * See the documentation and UML model:<br>
 * licensemodule_uml.png<br>
 * License_validation_logic.png <br>
 * 
 */
public class Presentation extends Persistent{

	private String key;
	
	
	public Presentation() {
	
	}
	
	public Presentation(String key) {
		super();
		this.key = key;
	}
	
	
	public String getKey() {
		return key;
	}

	public void setKey(String name) {
		this.key = name;
	}

    @Override
    public String toString() {
        return key;
    }

	
	
	
}
