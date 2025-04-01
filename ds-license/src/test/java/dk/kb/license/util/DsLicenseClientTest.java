package dk.kb.license.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dk.kb.license.model.v1.HoldbackCalculationInputDto;
import dk.kb.license.model.v1.RestrictionsCalculationInputDto;
import dk.kb.license.model.v1.RightsCalculationInputDto;
import dk.kb.license.model.v1.RightsCalculationOutputDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.CheckAccessForIdsInputDto;
import dk.kb.license.model.v1.CheckAccessForIdsOutputDto;
import dk.kb.license.model.v1.GetUserGroupsAndLicensesInputDto;
import dk.kb.license.model.v1.GetUserGroupsAndLicensesOutputDto;
import dk.kb.license.model.v1.GetUserGroupsInputDto;
import dk.kb.license.model.v1.GetUserGroupsOutputDto;
import dk.kb.license.model.v1.GetUserQueryInputDto;
import dk.kb.license.model.v1.GetUsersFilterQueryOutputDto;
import dk.kb.license.model.v1.GetUsersLicensesInputDto;
import dk.kb.license.model.v1.GetUsersLicensesOutputDto;
import dk.kb.license.model.v1.UserObjAttributeDto;
import dk.kb.license.model.v1.ValidateAccessInputDto;
import dk.kb.license.model.v1.ValidateAccessOutputDto;


/**
 * Integration test on class level, will not be run by automatic build flow.
 * Call 'kb init' to fetch YAML property file with server urls
 * 
 */
@Tag("integration")
public class DsLicenseClientTest {


    private static final Logger log =  LoggerFactory.getLogger(DsLicenseClientTest.class);

    private static DsLicenseClient remote = null;
    private static String dsLicenseDevel=null;  

    private static final String SEARCH_PRESENTATIONTYPE="Search";

    @BeforeAll
    static void setup() {
        try {
            ServiceConfig.initialize("conf/ds-license-behaviour.yaml","ds-license-integration-test.yaml"); 

            dsLicenseDevel= ServiceConfig.getConfig().getString("integration.devel.licensemodule"); 
            System.out.println(dsLicenseDevel);
            remote = new DsLicenseClient(dsLicenseDevel);
        } catch (IOException e) { 
            e.printStackTrace();
            log.error("Integration yaml 'ds-license-integration-test.yaml' file most be present. Call 'kb init'"); 
            fail();

        }
    }

    @Test
    public void testCheckAccessForIds() throws Exception {      
        ArrayList<String> ids= new ArrayList<String>();
        ids.add("does_not_exist");
        CheckAccessForIdsInputDto input = getCheckAccessForIdsInputDto(SEARCH_PRESENTATIONTYPE, ids);                         
        CheckAccessForIdsOutputDto output = remote.checkAccessForIds(input);
        assertEquals(output.getNonExistingIds().size(),1); //We have a valid response        
    }

    @Test
    public void testCheckAccessForResourceIds() throws Exception {              
        ArrayList<String> ids= new ArrayList<String>();
        ids.add("does_not_exist");
        CheckAccessForIdsInputDto input = getCheckAccessForIdsInputDto(SEARCH_PRESENTATIONTYPE, ids);                          
        CheckAccessForIdsOutputDto output = remote.checkAccessForResourceIds(input);
        System.out.println(output);  //We have a valid response

    }

    
    @Test
    public void testValidateAccess() throws Exception {       
        ValidateAccessInputDto input = getValidateAccessInputDto(new ArrayList<String>());      
        ValidateAccessOutputDto output = remote.validateAccess(input);
        assertNotNull(output); //We have a valid response;
    }


    @Test
    public void testUserGroups() throws Exception {      
        GetUserGroupsInputDto input = new GetUserGroupsInputDto();
        input.setLocale("da");                                                       
        input.setAttributes(getDefaultAttributes());          
        GetUserGroupsOutputDto output = remote.getUserGroups(input);
        assertNotNull(output); //We get a valid response          
    }
    
    @Test
    public void testUserGroupsAndLicenses() throws Exception {      
        GetUserGroupsAndLicensesInputDto input = new GetUserGroupsAndLicensesInputDto();
        input.setLocale("da");                                                       
        input.setAttributes(getDefaultAttributes());
        GetUserGroupsAndLicensesOutputDto output = remote.getUserGroupsAndLicenses(input);        
        assertTrue(output.getLicenses().size() >0); //There should be a least 1 license.                  
    }
    
    @Test
    public void testGetUserLicenseQuery() throws Exception {      
        GetUserQueryInputDto input = new GetUserQueryInputDto();                                                             
        input.setAttributes(getDefaultAttributes());
        input.setPresentationType(SEARCH_PRESENTATIONTYPE);                
        GetUsersFilterQueryOutputDto output = remote.getUserLicenseQuery(input);               
        assertTrue(output.getFilterQuery().length() >0); //There will always be a query                  
    }

    
    @Test
    public void testGetUserLicenses() throws Exception {      
        GetUsersLicensesInputDto input=new GetUsersLicensesInputDto();                                                             
        input.setAttributes(getDefaultAttributes());
        input.setLocale("da");                
        GetUsersLicensesOutputDto output = remote.getUserLicenses(input);                       
        assertTrue(output.getLicenses().size() >0); //There should be at least 1 license                  
    }

    @Test
    public void calculateRights() {
        RightsCalculationInputDto inputDto = new RightsCalculationInputDto();
        HoldbackCalculationInputDto holdbackDto = new HoldbackCalculationInputDto();
        RestrictionsCalculationInputDto restrictionsDto = new RestrictionsCalculationInputDto();

        holdbackDto.setIndhold(1000);
        holdbackDto.setForm(1000);
        holdbackDto.setHensigt(0);
        holdbackDto.setProductionCountry(1000);
        holdbackDto.setOrigin("ds.tv");

        restrictionsDto.setProductionCode("1000");
        restrictionsDto.setTitle("Almindelig udsendelses test");
        restrictionsDto.setDrProductionId("9393871973197");
        restrictionsDto.setRecordId("test id");

        inputDto.setPlatform(RightsCalculationInputDto.PlatformEnum.DRARKIV);
        inputDto.setRestrictionsInput(restrictionsDto);
        inputDto.setHoldbackInput(holdbackDto);

        inputDto.setRecordId("test ID");
        inputDto.setStartTime("2016-02-02T00:00:00Z");

        RightsCalculationOutputDto outputDto = remote.calculateRights(inputDto);

        assertFalse(outputDto.getDr().getTitleRestricted());
    }
        
    
    
    private ValidateAccessInputDto getValidateAccessInputDto(ArrayList<String> groups) {
        ValidateAccessInputDto input = new ValidateAccessInputDto();             
        input.setAttributes(getDefaultAttributes());                       
        input.setPresentationType(SEARCH_PRESENTATIONTYPE);
        input.setGroups(groups);
        return input;
    }


    /**
     * This is the default user with no additional information about the user.
     */
    private  List<UserObjAttributeDto>  getDefaultAttributes(){
        UserObjAttributeDto everybodyUserAttribute = new UserObjAttributeDto();
        everybodyUserAttribute.setAttribute("everybody");
        ArrayList<String> values = new ArrayList<>();
        values.add("yes");
        everybodyUserAttribute.setValues(values);     
        List<UserObjAttributeDto> allAttributes = new ArrayList<>();               
        allAttributes.add(everybodyUserAttribute);                               
        return allAttributes;     
    }

    private CheckAccessForIdsInputDto getCheckAccessForIdsInputDto(String presentationType, ArrayList<String> ids) {        
        CheckAccessForIdsInputDto input = new CheckAccessForIdsInputDto();
        input.setPresentationType(presentationType);


        UserObjAttributeDto everybodyUserAttribute = new UserObjAttributeDto();
        everybodyUserAttribute.setAttribute("everybody");
        ArrayList<String> values = new ArrayList<>();
        values.add("yes");
        everybodyUserAttribute.setValues(values);

        List<UserObjAttributeDto> allAttributes = new ArrayList<>();
        allAttributes.add(everybodyUserAttribute);        
        input.setAttributes(allAttributes);               
        input.setAccessIds(ids);        

        return input;
    }

}
