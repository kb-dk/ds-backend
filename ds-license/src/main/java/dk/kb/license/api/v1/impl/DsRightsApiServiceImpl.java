package dk.kb.license.api.v1.impl;

import dk.kb.license.api.v1.DsRightsApi;
import dk.kb.license.config.ServiceConfig;
import dk.kb.license.facade.RightsModuleFacade;
import dk.kb.license.model.v1.*;
import dk.kb.util.webservice.ImplBase;
import org.apache.cxf.interceptor.InInterceptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ds-license
 *
 * <p># Ds-license(Digitale Samlinger) by the Royal Danish Library.        ## Ds-license restricts access to items in collections based on the user credential information. The primary method in Ds-license is to filter a list of IDs (recordIds) and only return the subset  ID's that the user has access to based in the user information. Access can be restricted  to a presentationtype value. (Download,Search etc.) The filtering is done against a Solr server with all information about the records. The application has a GUI interface to define all access rules based on user attributes.   ## User attributes The User attributes is key-value pairs, where the values can be a list and a user can have multiple key-value pairs. All values and keys are strings. The key-values pairs can be WAYF attributes (https://www.wayf.dk/) which is a  standard for describing users at educational institutions and will have some guarateed values always. But the GUI administration can define arbitarity key-value rules and not just WAYF attributes. All user atttribute keys must be define in  the GUI and they will be available for use when defining a license attribute-group.  ### Examples of UserAttributes (WAYF)        | key                         | values                     | Remark                                          |  | ----------------------------| ---------------------------|-------------------------------------------------| | SBIPRolemapper              | inhouse,kb                 | inhouse(local computer), kb (organisation)      | | mail                        | teg@kb.dk                  | Can be used to give individual access           | | shachHomeOrganisation       | ku.dk                      | Educational institution                         | | eduPersonPrimaryAffiliation | stud                       | Student. (staff, employee,faculty also values)  |     ## Packages (pakker) and restrictions (klausulering) The packages and restrictions are the building blocks that gives access to materials. In  the UML they are both groups, but on the GUI they will be seen as two seperate entities.  Packages gives access to materials (positive Solr filter) while restrictions forbid access (negative Solr filter).  The more packages groups a user validates will give more materials and more validated restrictions will also give more materials since this will remove the negative filter. The restrictions negative filters are always applied unless the user validates access to them and that restriction will be removed. Some materials can be locked under several different restrictions and all must validate before the user can access it.  Giving a user access   to a restriction will not mean the user can see all the restricted material. He can still only see what  the packages give access too and the restricted material within those packages. Whenever a new restriction is added to the configuration it will take immediate effect and block all access for all users. To give access to the new restriction a licence needed to be edited and configure access to that restriction.      ### Example of access with two groups Group 1 (package): lma_long:\"radio\"  Group 2 (restriction):  klausuleret:\"ja\"  If the user has acesss to both groups, the final filter query will be:   **lma_long:\"radio\"**  If the user has only access to the first group, the final filter query will be:  **lma_long:\"radio\" -klausuleret:\"ja\"**  ### Example of access with four groups The normal groups filter queries will be OR'ed.  Group 1 (package): lma_long:\"tv\"  Group 2 (package): lma_long:\"radio\"     Group 3 (restriction):  klausuleret:\"ja\"  Group 4 (restiction):  individuelt_forbud:\"ja\"  If the user has access to group 1,2 and 3 the filter query will be: **(lma_long:\"radio\" OR lma_long\"tv\") -individuelt_forbud:\"ja\"**      ## Licences Licenses are the mapping from UserAttributes to groups/packages. One license can give access to several groups.  For each group a license gives access to, the license must also specify a presentation type (or several) for that group. Presentation types are also defined in the GUI and examples of presentation types are:Stream,Search.Download,Thumbnails,Headlines.  ### Licenses structure A license must define a valid from date and valid to date and is only valid in the date interval. The format is dd-mm-yyyy. A license has to defines one or more attribute-groups. An attribute-group is mapping from UserAttributes keys to values. An attribute-group can define several mappings and every single mapping in the attribute group has to validate for the attribute-group to validate, which will then validate the whole license.So a license can have several attribute groups and just one of them has to validate for the whole license to validate. The reason you can define several attribute-groups in one license is to avoid defining many identical licences that gives  access to same material but by different conditions.  ### License validation algoritm First the validation check will limit to licenses that are valid for the date of the requests. Then for each license every  attributed group will be checked. If just one of the attribute groups validates then the license validates.  The license will give access to the groups (pakker) defined for the license, but restricted to the presentationtype (Download etc.) Every license that validate will give access to more material. All the allow groups will each expand the positive filter query used for filtering Ids. All restriction-group validated will remove the negative filter blocked by that restriction-group. See 'uml/License_validation_logic.png' for a visualisation  of the validation logic for a license.   ## UML model  The UML diagram can be found in the /uml/licensemodule_uml.png folder. The database persistence model and object layer is a  direct implementation of the UML model, except for some naming. The UML model is created using DIA and the project file is also in the folder.      ### UML model explained There are 3 general type classes that can be maintained and the values are configured are used when defining a license. It is not possible to delete a configuration value if it is used in a license.  ## Configuration of the Ds-license   ### Property: solr.servers The configuration requires at least one Solr server for the property 'solr.servers'. When filtering IDs all Solr servers will be called for filtering and each will be called with all IDs.          ### Property: solr.idField The Solr field used for filtering. Multivalued fields allowed. This field must exist in the schema for the Solr servers. Example: If the filter field is 'id' and the filter query generated by from the package is 'subject:danmark' Then the query will be (id:id1 OR id:id2 OR id:id3..) AND (subject:Denmark). Solr will return documents that is a  subset of the ID's asked for. And this is what happens in the id-filtering.   ### Properties:  url,driver,username,password  The 4 database properties  (JDBC) must be defined: url,driver,username,password      A PostGreSql database is recommended when not running locally. The database tables must be created before use with DDL file: test/resources/ddl/licensemodule_create_db.ddl   ## API    While there are many methods exposed through the API only the following two are necessary for integrating License module into the software stack.  #### checkAccessForIds This is the method that takes the UserAttributes and a list of IDs. The return value will be the IDs that was not removed in the filtering and those that the user has access to. The method is called every time a user tries to access or search materials.  #### getUserLicenses Method only takes the userattributes and returns a list of licences that the userattributes give access to. For each license the name, validFrom,ValidTo and a description will be returned. This information can be shown to the user.  Developed and maintained by the Royal Danish Library.
 *
 */
@InInterceptors(interceptors = "dk.kb.license.webservice.KBAuthorizationInterceptor")
public class DsRightsApiServiceImpl extends ImplBase implements DsRightsApi {
    private final static Logger log = LoggerFactory.getLogger(DsRightsApiServiceImpl.class);

    @Override
    public RestrictedIdOutputDto createRestrictedId(Boolean touchRecord, RestrictedIdInputDto restrictedIdInputDto) {
        log.debug("Creating restricted id: {}", restrictedIdInputDto);
        try {
            return RightsModuleFacade.createRestrictedId(touchRecord, restrictedIdInputDto);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public void updateRestrictedIdComment(Boolean touchRecord, UpdateRestrictedIdCommentInputDto updateRestrictedIdCommentInputDto) {
        log.debug("Updating restricted id: {}", updateRestrictedIdCommentInputDto);
        try {
            RightsModuleFacade.updateRestrictedIdComment(updateRestrictedIdCommentInputDto, touchRecord);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public RecordsCountDto deleteRestrictedId(Long id, DeleteReasonDto deleteReasonDto, Boolean touchRecord) {
        log.debug("Deleting restricted id: {}", id);
        try {
            return RightsModuleFacade.deleteRestrictedId(id, touchRecord, deleteReasonDto);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public RestrictedIdOutputDto getRestrictedId(String idValue, IdTypeEnumDto idType, PlatformEnumDto platform) {
        log.debug("Get restricted id idValue: {}, idType: {}, platform: {}", idValue, idType, platform);
        try {
            return RightsModuleFacade.getRestrictedId(idValue, idType, platform);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    /**
     * Returns a DrBroadcastDto object that shows if the input dsId broadcast has a drProductionId.
     * DrBroadcastDto has a list of BroadcastDto that is populated with broadcasts sharing the same drProductionId.
     * Each BroadcastDto also shows if there is a restriction and restriction comment on that broadcast.
     *
     * @param dsId the unique id of a DR-arkiv broadcast
     * @return DrBroadcastDto with a list of BroadcastDto
     */
    @Override
    public DrBroadcastDto matchingDrProductionIdBroadcasts(String dsId) {
        try {
            return RightsModuleFacade.matchingDrProductionIdBroadcasts(dsId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public List<RestrictedIdOutputDto> getAllRestrictedIds(IdTypeEnumDto idType, PlatformEnumDto platform) {
        try {
            return RightsModuleFacade.getAllRestrictedIds(idType, platform);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public void createRestrictedIds(List<RestrictedIdInputDto> restrictedIds, Boolean touchRecord) {
        try {
            RightsModuleFacade.createRestrictedIds(restrictedIds, touchRecord);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public RightsCalculationOutputDto calculateRights(RightsCalculationInputDto rightsCalculationInputDto) {
        try {
            return RightsModuleFacade.calculateRightsForRecord(rightsCalculationInputDto);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    /**
     * Gets a DR holdback rule
     *
     * @param drHoldbackValue the drHoldbackValue of the DR holdback rule
     * @return
     */
    @Override
    public DrHoldbackRuleOutputDto getDrHoldbackRuleByDrHoldbackValue(String drHoldbackValue) {
        try {
            return RightsModuleFacade.getDrHoldbackRuleByDrHoldbackValue(drHoldbackValue);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    /**
     * Create a DR holdback rule.
     *
     * @param drHoldbackRuleDto
     */
    @Override
    public DrHoldbackRuleOutputDto createDrHoldbackRule(DrHoldbackRuleInputDto drHoldbackRuleDto) {
        try {
            return RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleDto);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

        /**
     * @param id
     * @param drHoldbackRuleInputDto
     * @return
     */
    @Override
    public DrHoldbackRuleOutputDto updateDrHoldbackRule(Long id, DrHoldbackRuleInputDto drHoldbackRuleInputDto) {
        log.debug("Updating DR holdback rule: {}, restrictedIdInputDto: {}", id, drHoldbackRuleInputDto);
        try {
            return RightsModuleFacade.updateDrHoldbackRule(id, drHoldbackRuleInputDto);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    /**
     * Delete a DR holdback rule
     *
     * @param id id of the DR holdback rule
     */
    @Override
    public RecordsCountDto deleteDrHoldbackRule(Long id, DeleteReasonDto deleteReasonDto) {
        log.debug("Deleting DR holdback rule: {}", id);
        try {
            return RightsModuleFacade.deleteDrHoldbackRule(id, deleteReasonDto);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    /**
     * get all holdback rules for DR
     *
     * @return
     */
    @Override
    public List<DrHoldbackRuleOutputDto> getDrHoldbackRules() {
        try {
            return RightsModuleFacade.getDrHoldbackRules();
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    /**
     * set the form and content range combinations for a drHoldbackValue
     * This requires the drHoldbackValue to be present in the DR holdback rule table
     *
     * @param drHoldbackRangeInputDto
     */
    @Override
    public List<DrHoldbackRangeOutputDto> createDrHoldbackRanges(DrHoldbackRangeInputDto drHoldbackRangeInputDto) {
        try {
            return RightsModuleFacade.createDrHoldbackRanges(drHoldbackRangeInputDto);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    /**
     * Deletes all form and content range combinations for a drHoldbackValue
     *
     * @param drHoldbackValue
     */
    @Override
    public RecordsCountDto deleteDrHoldbackRanges(String drHoldbackValue, DeleteReasonDto deleteReasonDto) {
        log.debug("Deleting DR holdback ranges: {}", drHoldbackValue);
        try {
            return RightsModuleFacade.deleteDrHoldbackRanges(drHoldbackValue, deleteReasonDto);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    /**
     * Gets the drHoldbackValue from a content and form metadata values.
     *
     * @param content
     * @param form
     * @return
     */
    @Override
    public String getDrHoldbackValueFromContentAndForm(Integer content, Integer form) {
        try {
            return RightsModuleFacade.getDrHoldbackValueFromContentAndFormValues(content, form);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public List<DrHoldbackRangeOutputDto> getDrHoldbackRanges(String drHoldbackValue) {
        try {
            return RightsModuleFacade.getDrHoldbackRanges(drHoldbackValue);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public List<String> getIdTypes(PlatformEnumDto platform) {
        try {
            return ServiceConfig.getRightsPlatformConfig(platform.getValue()).getList("idTypes");
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public List<Object> getPlatforms() {
        try {
            return ServiceConfig.getConfig().getList("rights.platforms");
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
