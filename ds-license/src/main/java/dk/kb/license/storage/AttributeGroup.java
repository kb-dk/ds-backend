package dk.kb.license.storage;

import java.util.ArrayList;

/**
 * This is a persistent DTO.
 * See the documentation and UML model:<br>
 * licensemodule_uml.png<br>
 * License_validation_logic.png <br>
 * 
 */
public class AttributeGroup extends Persistent{

	private int number;	

	private ArrayList<Attribute> attributes = new  ArrayList<Attribute>();

	public AttributeGroup(int number){
		this.number=number;

	}

	public ArrayList<Attribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(ArrayList<Attribute> attributes) {
		this.attributes = attributes;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

}
