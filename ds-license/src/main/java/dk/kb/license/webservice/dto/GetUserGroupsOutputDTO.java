package dk.kb.license.webservice.dto;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

public class GetUserGroupsOutputDTO {

	ArrayList<UserGroupDTO> groups = new ArrayList<UserGroupDTO>(); 
		
	public GetUserGroupsOutputDTO(){		
	}

	public ArrayList<UserGroupDTO> getGroups() {
		return groups;
	}

	public void setGroups(ArrayList<UserGroupDTO> groups) {
		this.groups = groups;
	}
	
	
}
