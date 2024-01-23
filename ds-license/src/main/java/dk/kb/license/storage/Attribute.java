package dk.kb.license.storage;

import java.util.ArrayList;

/**
 * This is a persistent DTO.
 * See the documentation and UML model:<br>
 * licensemodule_uml.png<br>
 * License_validation_logic.png <br>
 * 
 */
public class Attribute extends Persistent{

	
	private String attributeName;   
	private ArrayList<AttributeValue> values = new ArrayList<AttributeValue>();

	public Attribute(){	      
	}

	public Attribute(String name){
		this.attributeName=name;	   
	}


	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public ArrayList<AttributeValue> getValues() {
		return values;
	}

	public void setValues(ArrayList<AttributeValue> values) {
		this.values = values;
	}



}
