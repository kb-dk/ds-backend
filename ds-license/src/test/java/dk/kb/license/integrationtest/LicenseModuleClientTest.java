package dk.kb.license.integrationtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.CheckAccessForIdsInputDto;
import dk.kb.license.model.v1.CheckAccessForIdsOutputDto;
import dk.kb.license.model.v1.GetUserGroupsInputDto;
import dk.kb.license.model.v1.GetUserGroupsOutputDto;
import dk.kb.license.model.v1.GetUserQueryInputDto;
import dk.kb.license.model.v1.GetUsersFilterQueryOutputDto;
import dk.kb.license.model.v1.GetUsersLicensesInputDto;
import dk.kb.license.model.v1.GetUsersLicensesOutputDto;
import dk.kb.license.model.v1.UserObjAttributeDto;
import dk.kb.license.model.v1.ValidateAccessInputDto;
import dk.kb.license.model.v1.ValidateAccessOutputDto;
import dk.kb.license.util.DsLicenseClient;

/**
 *  Integration test on class level, will not be run by automatic build flow.
 *  Notice data in Solr may change over time. This unittest must be updated if the default minimum license module rules change drastic
 */
 
@Tag("integration")
public class LicenseModuleClientTest {
    private static final Logger log = LoggerFactory.getLogger( LicenseModuleClientTest.class);
        
    private static String dsLicenseUrl=null;
    
    @BeforeAll
    static void setup() {
        try {
            ServiceConfig.initialize("conf/ds-license-behaviour.yaml","ds-license-integration-test.yaml");                        
            dsLicenseUrl= ServiceConfig.getConfig().getString("integration.devel.licensemodule");
        } catch (IOException e) {          
            log.error("Integration yaml 'ds-license-integration-test.yaml' file most be present. Call 'kb init'");            
            fail();
        }
    }
    
    
    @Test
    public void testValidateAccess() throws Exception {        
        DsLicenseClient dsLicenseClient = getDsLicenseClient();
                        
        ValidateAccessInputDto input = new ValidateAccessInputDto();

        ArrayList<UserObjAttributeDto> userObjAttributes = createTestUserObjAttributeDto();
        input.setAttributes(userObjAttributes);

        ArrayList<String> groups = new ArrayList<String>();
        groups.add("TV");		
        input.setGroups(groups);
        input.setPresentationType("Search");

        ValidateAccessOutputDto output= dsLicenseClient.validateAccess(input);                	    
        assertTrue(output.getAccess());
    }



    @Test
    public void testGetUserLicenseQuery() throws Exception {
        DsLicenseClient dsLicenseClient = getDsLicenseClient();
        
        GetUserQueryInputDto input = new GetUserQueryInputDto();
        input.setPresentationType("Search");
        input.setAttributes(createTestUserObjAttributeDto());				
        
        GetUsersFilterQueryOutputDto output = dsLicenseClient.getUserLicenseQuery(input);
        
        //Test the filterquery makees some sense.
        //For "Samlingsbilleder" this is well defined field that require special access        
        assertTrue(output.getFilterQuery().indexOf("origin:\"ds.tv\"") >0);
    }

    
    /*  Samlings billeder no longer in corpus
    @Test
    public void testCheckAccessForIds() throws Exception {
        DsLicenseClient dsLicenseClient = getDsLicenseClient();
        
        CheckAccessForIdsInputDto input = new CheckAccessForIdsInputDto();
        input.setPresentationType("Search");
        input.setAttributes(createTestUserObjAttributeDto());
        ArrayList<String> ids = new ArrayList<String>();        
        String idVideo="ds.tv:oai:du:3006e2f8-3f73-477a-a504-4d7cb1ae1e1c";
        String idRadio="ds.radio:oai:du:e683b0b8-425b-45aa-be86-78ac2b4ef0ca"; 
        ids.add(idVideo);// Video
        ids.add(idRadio); //radio
        ids.add("none_existing_id"); //does not exist        
        input.setAccessIds(ids);
       
        CheckAccessForIdsOutputDto output = dsLicenseClient.checkAccessForIds(input);
        assertEquals("Search",output.getPresentationType());
        assertTrue(output.getQuery().indexOf("-(access_blokeret:true)") >0); //For "Samlingsbilleder" this is well defined field that require special access     
        assertEquals(output.getAccessIds().size(), 2); //both the radio and tv id 
        assertEquals(output.getNonExistingIds().size(),1);                     	
    }

*/

    @Test
    public void testGetUsersLicenses() throws Exception {
        DsLicenseClient dsLicenseClient = getDsLicenseClient();

        GetUsersLicensesInputDto input = new GetUsersLicensesInputDto();
        input.setAttributes(createTestUserObjAttributeDto());
        input.setLocale("da");

        GetUsersLicensesOutputDto output =dsLicenseClient.getUserLicenses(input);
        assertTrue(output.getLicenses().size() >0); //Just there there is a least one license. Everyone will get the default license and probably more.        
    }

    @Test
     public void testGetUsersGroups() throws Exception {
        DsLicenseClient dsLicenseClient = getDsLicenseClient();
        GetUserGroupsInputDto input = new GetUserGroupsInputDto();
        input.setAttributes(createTestUserObjAttributeDto());
        input.setLocale("da");        
        	
        GetUserGroupsOutputDto output = dsLicenseClient.getUserGroups(input);        
        assertTrue(output.getGroups().size() > 1);        
    }



    /**
     * This is the user with minimum access(everybody=yes)  
     *
     */
    private static ArrayList<UserObjAttributeDto> createTestUserObjAttributeDto() {
        ArrayList<UserObjAttributeDto> userObjAttributes = new ArrayList<UserObjAttributeDto>();

        UserObjAttributeDto newUserObjAtt = new UserObjAttributeDto();
        userObjAttributes.add(newUserObjAtt);
        newUserObjAtt.setAttribute("everybody");
        ArrayList<String> values = new ArrayList<String>();        
        values.add("yes");
        newUserObjAtt.setValues(values);
        return userObjAttributes;

    }

    private DsLicenseClient getDsLicenseClient() {
        DsLicenseClient client  = new DsLicenseClient(dsLicenseUrl,100,60000L);                
        return client;
    }

}
