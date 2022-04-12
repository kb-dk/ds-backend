package dk.kb.license.webservice.dto;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;


public class GetUserGroupsInputDTO {
	private ArrayList<UserObjAttributeDTO> attributes = new ArrayList<UserObjAttributeDTO>();
	private String locale;
	
	public GetUserGroupsInputDTO(){			
	}

	public ArrayList<UserObjAttributeDTO> getAttributes() {
		return attributes;
	}

	public void setAttributes(ArrayList<UserObjAttributeDTO> attributes) {
		this.attributes = attributes;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}	

}
