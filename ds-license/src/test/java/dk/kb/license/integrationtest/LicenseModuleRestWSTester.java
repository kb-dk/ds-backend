package dk.kb.license.integrationtest;

import java.util.ArrayList;

import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.client.WebClient;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import dk.kb.license.model.v1.CheckAccessForIdsInputDto;
import dk.kb.license.model.v1.CheckAccessForIdsOutputDto;
import dk.kb.license.model.v1.GetUserGroupsInputDto;
import dk.kb.license.model.v1.GetUserGroupsOutputDto;
import dk.kb.license.model.v1.GetUserQueryInputDto;
import dk.kb.license.model.v1.GetUsersLicensesInputDto;
import dk.kb.license.model.v1.GetUsersLicensesOutputDto;
import dk.kb.license.model.v1.UserObjAttributeDto;
import dk.kb.license.model.v1.ValidateAccessInputDto;
import dk.kb.license.model.v1.ValidateAccessOutputDto;




//This class is an integration class run manuel, change service URL to a devel service.
// Use to reproduce and find production bugs etc.
public class LicenseModuleRestWSTester {

  
  private static String serviceUrl = "http://localhost:9612/licensemodule/services";
   
  
  
	public static void main(String[] args) throws Exception {
		
		 //testValidateAccess();
		 //testGetUserLicenseQuery();
   	     //testCheckAccessForIds();
         //testGetUsersLicenses();
 		  //testGetUsersGroups();
		
		}
	
	private static WebClient getWebClient() {
	      ArrayList<Object> providers = new ArrayList<Object>();
	        providers.add( new JacksonJaxbJsonProvider() );
	        WebClient client = WebClient.create(serviceUrl,providers);
	        return client;
	}

	@SuppressWarnings("all")
	private static void testValidateAccess() throws Exception {
		// Test Validate Access
		ValidateAccessInputDto input = new ValidateAccessInputDto();

		ArrayList<UserObjAttributeDto> userObjAttributes = createTestUserObjAttributeDto();
		input.setAttributes(userObjAttributes);

		ArrayList<String> groups = new ArrayList<String>();
		groups.add("PublicDomain");		
		input.setGroups(groups);
		input.setPresentationType("Download");
				
	    WebClient client = getWebClient();
			    	   
	    ValidateAccessOutputDto post = client.path("validateAccess").type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).post(input,ValidateAccessOutputDto.class);	    
	    System.out.println(post.getAccess()); //true of false
	}

	
	
	@SuppressWarnings("all")
	private static void testGetUserLicenseQuery() throws Exception {
		GetUserQueryInputDto input = new GetUserQueryInputDto();
		input.setPresentationType("Search");
		input.setAttributes(createTestUserObjAttributeDto());				
		 WebClient client = getWebClient();
		String output = client.path("getUserLicenseQuery").type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).post(input,String.class);

		//This query is use to filter results.
		System.out.println("query:" + output);
	}

	
	@SuppressWarnings("all")
	private static void testCheckAccessForIds() throws Exception {
		CheckAccessForIdsInputDto input = new CheckAccessForIdsInputDto();
		input.setPresentationType("Search");
		input.setAttributes(createTestUserObjAttributeDto());
        ArrayList<String> ids = new ArrayList<String>();        
        ids.add("doms_radioTVCollection:uuid:371157ee-b120-4504-bfaf-364c15a4137c");//radio TV        
        ids.add("doms_radioTVCollection:uuid:c3386ed5-9b79-47a2-a648-8de53569e630");//radio TV
		ids.add("doms_reklamefilm:uuid:35a1aa76-97a1-4f1b-b5aa-ad2a246eeeec"); //reklame
		ids.add("doms_newspaperCollection:uuid:18709dea-802c-4bd7-98e6-32ca3b285774-segment_6"); //aviser		
		input.setAccessIds(ids);
				
	  WebClient client = getWebClient();
			
		CheckAccessForIdsOutputDto output = client.path("checkAccessForIds").type(MediaType.TEXT_XML).accept(MediaType.TEXT_XML).post(input,CheckAccessForIdsOutputDto.class);
		System.out.println("query:" + output.getQuery());
		System.out.println("presentationtype:" + output.getPresentationType());
		System.out.println("number of IDs:" + output.getAccessIds().size());	
	}

	
	@SuppressWarnings("all")
	private static void testGetUsersLicenses() throws Exception {
		// GetUserLicensesOutputDTO getUserLicenses

		GetUsersLicensesInputDto input = new GetUsersLicensesInputDto();
		input.setAttributes(createTestUserObjAttributeDto());
        input.setLocale("da");
		        
        WebClient client = getWebClient();
		GetUsersLicensesOutputDto output = client.path("getUserLicenses").type(MediaType.TEXT_XML).accept(MediaType.TEXT_XML).post(input,GetUsersLicensesOutputDto.class);
					
		System.out.println("output, licensenames:" + output.getLicenses());
	}
	
	

	
	
	@SuppressWarnings("all")
	private static void testGetUsersGroups() throws Exception {
		GetUserGroupsInputDto input = new GetUserGroupsInputDto();
		input.setAttributes(createTestUserObjAttributeDto());
        input.setLocale("da");        
        WebClient client = getWebClient();
		GetUserGroupsOutputDto output = client.path("getUserGroups").type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).post(input,GetUserGroupsOutputDto.class);	
		System.out.println("output, groups:" + output.getGroups());
	}

	

	
	private static ArrayList<UserObjAttributeDto> createTestUserObjAttributeDto() {
		ArrayList<UserObjAttributeDto> userObjAttributes = new ArrayList<UserObjAttributeDto>();
	
		UserObjAttributeDto newUserObjAtt = new UserObjAttributeDto();
		userObjAttributes.add(newUserObjAtt);
		newUserObjAtt.setAttribute("attribut_store.MediestreamFullAccess");
		ArrayList<String> values = new ArrayList<String>();
		values.add("true");
		values.add("yes");
		newUserObjAtt.setValues(values);
		
		
		UserObjAttributeDto newUserObjAtt1 = new UserObjAttributeDto();
        userObjAttributes.add(newUserObjAtt1);
        newUserObjAtt1.setAttribute("mail");
        ArrayList<String> values1 = new ArrayList<String>();
        values1.add("mvk@statsbiblioteket.dk");
        newUserObjAtt1.setValues(values1);
		        
        UserObjAttributeDto newUserObjAtt2 = new UserObjAttributeDto();
        userObjAttributes.add(newUserObjAtt2);
        newUserObjAtt2.setAttribute("SBIPRoleMapper");
        ArrayList<String> values2 = new ArrayList<String>();
        values2.add("aucampus");
        newUserObjAtt2.setValues(values2);
        
        
        return userObjAttributes;
		
	}
}
