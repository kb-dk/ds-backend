package dk.kb.license.storage;

import java.util.ArrayList;


/**
 * This is a persistent DTO.
 * See the documentation and UML model:<br>
 * licensemodule_uml.png<br>
 * License_validation_logic.png <br>
 * 
 */
public class LicenseContent extends Persistent{

	private String name;
	
	private ArrayList<Presentation> presentations = new ArrayList<Presentation>();
	
	public LicenseContent(){
		
	}

	public LicenseContent(String name){
		this.name=name;
	}
	
	public ArrayList<Presentation> getPresentations() {
		return presentations;
	}

	public void setPresentations(ArrayList<Presentation> presentations) {
		this.presentations = presentations;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
	
	
}
