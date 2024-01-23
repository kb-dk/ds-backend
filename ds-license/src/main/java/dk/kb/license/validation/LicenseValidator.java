package dk.kb.license.validation;

import java.io.IOException;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.license.Util;
import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.CheckAccessForIdsInputDto;
import dk.kb.license.model.v1.CheckAccessForIdsOutputDto;
import dk.kb.license.model.v1.GetUserGroupsInputDto;
import dk.kb.license.model.v1.GetUserQueryInputDto;
import dk.kb.license.model.v1.GetUserQueryOutputDto;
import dk.kb.license.model.v1.GetUsersLicensesInputDto;
import dk.kb.license.model.v1.UserGroupDto;
import dk.kb.license.model.v1.UserObjAttributeDto;
import dk.kb.license.model.v1.ValidateAccessInputDto;
import dk.kb.license.solr.SolrServerClient;
import dk.kb.license.storage.*;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;


/**
 * This class is the encapsulation of most of the business logic when validating access.
 * 
 * See the OpenApi documentation for ds-license module to understand the concepts.
 *  
 */
public class LicenseValidator {

    private static final Logger log = LoggerFactory.getLogger(LicenseValidator.class);		
    public static final String LOCALE_DA = "da";
    public static final String LOCALE_EN = "en";
    public static final String NO_ACCESS = ServiceConfig.SOLR_FILTER_ID_FIELD+":NoAccess"; 


    public static final List<String> locales=Collections.unmodifiableList(Arrays.asList(new String[] {LOCALE_DA,LOCALE_EN}));

    /**
     * Extract all license that validates for the user data. Licenses must also be be active. (date to-from check).  
     *  
     * @param input The input describing all information known about the user.
     * @return List of all licenses that validates for the user  input.
     */
    public static ArrayList<License> getUsersLicenses(GetUsersLicensesInputDto input) {
        //validate
        if (input.getAttributes() == null || input.getAttributes().size() == 0){
            log.error("No attributes defined in input.");
            throw new IllegalArgumentException("No attributes defined in input");
        }
        validateLocale(input.getLocale());

        // Load all licences        
        ArrayList<License> allLicenses = LicenseCache.getAllLicense();
        //Filter by date first
        ArrayList<License> dateFilteredLicenses = filterLicenseByValidDate(allLicenses, System.currentTimeMillis());		

        //Find licenses that give access for the dateFiltered licenses.
        ArrayList<License> accessLicenses = findLicensesValidatingAccess(input.getAttributes(),  dateFilteredLicenses);

        return accessLicenses;
    }


    /**
     * Extract all groups(packages/exclusions(klausulering) that validates for the user data.
     * 
     * Different licenses can give access the same group, but a group will only be added once.
     * The solr query can be constructing by 'adding' all queries from the packages and remove exclusions.
     * 
     * @param input The input describing all information known about the user.
     * @return List of all groups that validates for the user. This will be extracted from all licenses that validates for the user input. 
     */    
    public static ArrayList<UserGroupDto> getUsersGroups(GetUserGroupsInputDto input) {
        //validate
        if (input.getAttributes() == null || input.getAttributes().size() == 0){
            log.error("No attributes defined in input.");
            throw new InvalidArgumentServiceException("No attributes defined in input");
        }

        
        validateLocale(input.getLocale());

        // First filter by valid date
        ArrayList<License> allLicenses = LicenseCache.getAllLicense();		

        ArrayList<License> dateFilteredLicenses = filterLicenseByValidDate(allLicenses, System.currentTimeMillis());		

        //Find licenses that give access (not checking groups) for the dateFiltered licenses
        ArrayList<License> accessLicenses = findLicensesValidatingAccess(input.getAttributes(),  dateFilteredLicenses);

        ArrayList<UserGroupDto> filteredGroups = filterGroupsWithPresentationtype(accessLicenses);

        LicenseValidator.fixLocale(filteredGroups, input.getLocale());       

        return filteredGroups;
    }

   /** 
    *  Takes an input of ID's, which can be for different ID fields in solr and filter them from the query generated from the user attributes.
    *  If there are several solr servers configured this method will be called for each solr server.
    *  
    *  The steps involved are:<br>
    *  1) Extract all licenses that validates for the user <br>
    *  2) From the licenses collect all groups (packages/exclusion) that the user has access to <br>
    *  3) Generate the final solr filter-query from all groups. <br> 
    *  4) Return 3 differentlist of id's <br> 
    *  4.1) List of id's that validates access. This is the main purpose of the ds-license module <br>
    *  4.2) List of id's that the user does not have access too. It can be usefull to give this information to the user <br>
    *  4.3) List of id's that does not even exists in solr. This should not happen. <br>
    * 
    * 
    * @param input The object containing the information about the caller and list of IDs to be filtered 
    * @param solrIdField The ID field in Solr used for filtering. So far only 'id' and 'resource_id' are suitable fields
    */

    public static CheckAccessForIdsOutputDto checkAccessForIds(CheckAccessForIdsInputDto input, String solrIdField)
            throws InvalidArgumentServiceException, SolrServerException, IOException {

        if  (input.getAccessIds() == null || input.getAccessIds().size() == 0){
            throw new InvalidArgumentServiceException("No ID's in input");          
        }
        log.debug("checkAccessForResourceID callled for presentationtype:"+input.getPresentationType());

        //Get the query. This also validates the input 
        GetUserQueryInputDto inputQuery = new GetUserQueryInputDto();       
        inputQuery.setAttributes(input.getAttributes());
        inputQuery.setPresentationType(input.getPresentationType());
        GetUserQueryOutputDto query = getUserQuery(inputQuery);
        CheckAccessForIdsOutputDto output = new  CheckAccessForIdsOutputDto();
        output.setPresentationType(input.getPresentationType());
        output.setQuery(query.getQuery());

        List<SolrServerClient> servers = ServiceConfig.SOLR_SERVERS;          
        ArrayList<String> filteredIdsSet = filterIDs(input.getAccessIds(), query.getQuery(),solrIdField); 
        output.setAccessIds(filteredIdsSet);

        //Sanity check!
        if (output.getAccessIds().size() > input.getAccessIds().size()){
            log.warn("Security problem::More ID's in output than input. Input ids:"+input.getAccessIds() +" output IDs:"+output.getAccessIds());
            throw new InvalidArgumentServiceException("Security problem: More Id's in output than input. Check for query injection.");
        }

        //Set IDs that exists but with no access
        ArrayList<String> existingResourceIds = filterIDs(input.getAccessIds(), null, solrIdField);         
        existingResourceIds.removeAll(output.getAccessIds());             
        output.setNonAccessIds(existingResourceIds);

        //Set non existing ID's from input, but not found in Solr.        
        ArrayList<String> nonExistingIds = new ArrayList<String>();
        nonExistingIds.addAll(input.getAccessIds());
        nonExistingIds.removeAll(output.getAccessIds());//Remove those with access
        nonExistingIds.removeAll(existingResourceIds); //Remove those without accesss        
        output.setNonExistingIds(nonExistingIds);
          
        //This should not happen, so better log it until we know if there is a usecase that it can happen
        if (nonExistingIds.size() > 0) {
            log.warn(" Filter ID called with non existing IDs:"+nonExistingIds);
        }
        
        log.debug("#query IDs="+input.getAccessIds().size() + " returned #filtered IDs="+output.getAccessIds().size() +" using resourceId field:"+ServiceConfig.SOLR_FILTER_RESOURCE_ID_FIELD + " for input IDs:"+input.getAccessIds() +" non-access IDs:"+output.getNonAccessIds() + " non-existing IDs:"+nonExistingIds);
        return output;      
    }



    /**
    * Get the filter query-part for a given user.
    * <p>
    * The filter query is used when filtering ID's in solr.<br> 
    * The solr filtering is called with a query(list of IDS) and the filter query.<br>
    * The id field is defined in the configuration.<br>
    * Example: Query= id:(id1 OR id2 .. OR idn), Filter query: collection:dr 
    * @param input The input that defines the user.
    * @return
    */    
    public static GetUserQueryOutputDto getUserQuery(GetUserQueryInputDto input) {

        //validate
        if (input.getAttributes() == null){
            log.error("No attributes defined in input.");
            input.setAttributes(new ArrayList<UserObjAttributeDto>());

        }		

        if (input.getPresentationType() == null){
            log.error("No presentationtype defined in input.");
            throw new InvalidArgumentServiceException("No presentationtype defined in input.");
        }


        //Will throw exception if not matched		
        matchPresentationtype(input.getPresentationType());		


        // First filter by valid date
        ArrayList<License> allLicenses = LicenseCache.getAllLicense();		
        ArrayList<License> dateFilteredLicenses = filterLicenseByValidDate(allLicenses, System.currentTimeMillis());		

        //Find licenses that give access (not checking groups) for the dateFiltered licenses
        ArrayList<License> accessLicenses = findLicensesValidatingAccess(input.getAttributes(),  dateFilteredLicenses);

        ArrayList<String> types = new ArrayList<String>();
        types.add(input.getPresentationType());

        ArrayList<String> filterGroups = filterGroups(accessLicenses,  types);
        
        //Now we have to find all restriction-groups the user is missing 
        ArrayList<GroupType> configuredRestrictionLicenseGroupTypes = LicenseCache.getConfiguredRestrictionLicenseGroupTypes();
        GetUserQueryOutputDto output = new GetUserQueryOutputDto();
        output.setUserLicenseGroups(filterGroups);

        ArrayList<String> missingRestrictionGroups = new ArrayList<String>();
        //First add all restriction groups then remove those that user has access too
        for (GroupType current : configuredRestrictionLicenseGroupTypes){
            missingRestrictionGroups.add(current.getKey());
        }

        for (String current : filterGroups){
            missingRestrictionGroups.remove(current); 
        }
        output.setUserNotInDenyGroups(missingRestrictionGroups);	

        String query = generateQueryString(filterGroups, missingRestrictionGroups);

        output.setQuery(query);
        return output;
    }

   /**
    * Validate if a user has access to a specific presentationtype from a list of grouptypes.
    * If there are no grouptypes of restriction, just one of the grouptypes needs have a license giving access. <br>
    * If there one or more  grouptypes of restriction(klausulering) every one of them them must validate. <br>
    * The logic is that each restriction is a lock on the material. And if there are multiple locks, everyone must be opened to access the material.
    * 
    * @input Input having the specific presentationtype, groupnames and userattributes (describing the user) 
    */
    public static boolean validateAccess(ValidateAccessInputDto input) {
    	
        //validate
        if (input.getAttributes() == null || input.getAttributes().size() == 0){
            log.error("No attributes defined in input.");
            throw new InvalidArgumentServiceException("No attributes defined in input");
        }


        PresentationType presentationType = matchPresentationtype(input.getPresentationType());

        //Validate presentationType exists
        if (presentationType == null){
            log.warn("Unknown presentationtype in validateAccess:"+input.getPresentationType());			
            throw new InvalidArgumentServiceException("Unknown presentationtype in validateAccess:"+input.getPresentationType());
        }

        ArrayList<GroupType> groups = buildGroups(input.getGroups());
        //Validate groups. Same size or one was not matched.
        if (groups.size() != input.getGroups().size()){
            log.warn("At least 1 unknown group  in validateAccess:"+input.getGroups());			
            throw new InvalidArgumentServiceException("At least 1 unknown group  in validateAccess:"+input.getGroups());
        }		

        ArrayList<GroupType> restrictionGroups = filterRestrictionGroups(groups);
        if (restrictionGroups.size() > 0){
            log.debug("At least 1 restriction groups found in input, number of Restriction-groups:"+restrictionGroups.size());			
        }

        // First filter by valid date
        ArrayList<License> allLicenses = LicenseCache.getAllLicense();		
        ArrayList<License> dateFilteredLicenses = filterLicenseByValidDate(allLicenses, System.currentTimeMillis());		

        //Find licenses that give access (not checking groups) for the dateFiltered licenses
        ArrayList<License> accessLicenses = findLicensesValidatingAccess(input.getAttributes(),  dateFilteredLicenses);

        if (accessLicenses.size()==0){
            log.debug("No licenses validate access-part");
            return false;
        }

        //two situations. At least one restriction group involved, or no restriction groups.
        if (restrictionGroups.size() == 0){
            log.debug("Case: no restriction(klausulering) group");		
            //Simple situation. Just need to find 1 license having one of the groups with allowed presentationtype
            ArrayList<License> validatedLicenses = filterLicensesWithGroupNamesAndPresentationTypeNoRestrictionGroup(accessLicenses, groups, presentationType);
            return (validatedLicenses.size() >0); //OK since at least 1 license found        	           
        }
        else{
            // ALL groups+presentationtype must be in at least 1 license
            //Only Restriction groups are checked
            log.debug("Case: at least 1 restriction(klausulering) group");
            //notice only the restrictionGroups are used
            ArrayList<License> validatedLicenses = filterLicensesWithGroupNamesAndPresentationTypeRestrictionGroup(accessLicenses, restrictionGroups , presentationType);
            return (validatedLicenses.size() >0); //OK since at least 1 license found
        }
    }



    
    /**
     * From a list of licenses extract all grouptypes (with presentationtypes) that they give access to.       
     * <p>
     * Multiple licenses can give access the same grouptype, and the group type will only be added once.
     * <br>
     * Example:<br>
     * License 1 has grouptype A with presentationtype 'Stream' and 'Search' <br>
     * License 2 has grouptype A with presentationtype 'Stream' and 'Download'<br>
     * <p>
     * The result will be grouptype A with all 3 presentationtypes: 'Stream', 'Search' and 'Download' 
     * 
     * @param List of licenses 
     * @return List UserGroups. 
     */
    //Get all dom-groups and for each dom-group find the union of presentationtypes
    public static ArrayList<UserGroupDto> filterGroupsWithPresentationtype(ArrayList<License> licenses){
        TreeMap<String, UserGroupDto> groups = new TreeMap<String, UserGroupDto>();
        for (License currentLicense : licenses){
            for (LicenseContent currentGroup : currentLicense.getLicenseContents()){
                String name = currentGroup.getName();
                UserGroupDto group = groups.get(name);
                if (group == null){
                    group = new UserGroupDto();        	
                    group.setPresentationTypes(new ArrayList<String>());
                    group.setGroupName(name);
                    groups.put(name,group);
                }
                for (Presentation currentPresentation: currentGroup.getPresentations()){
                    String presentation_key = currentPresentation.getKey();
                    if (group.getPresentationTypes().contains(presentation_key)){
                        //Already added
                    }
                    else{
                        group.getPresentationTypes().add(presentation_key);
                    }
                }
            }
        }
        return new ArrayList<UserGroupDto>(groups.values());
    }



   /**
    * Helper method. <br>
    * 
    * Extract all grouptypes(name only) having a least of the presentationstypes from list of licences<br>
    * Each license can have multiple grouptypes each having 1 or more presentationtypes allowed.
    * 
    * @param licenses List of licenses
    * @param presentationTypes List of presentationtypes (name only)
    * @return List of grouptypes (name one) that has at least one of the presentationtypes allowed.
    * 
    */    
    public static ArrayList<String> filterGroups(ArrayList<License> licenses, ArrayList<String> presentationTypes) {
        HashSet<String> groups = new  HashSet<String>();
        for (License current : licenses){
            for (LicenseContent currentContent : current.getLicenseContents()){
                for (Presentation currentPresentation: currentContent.getPresentations()){
                    if (presentationTypes.contains(currentPresentation.getKey())){
                        groups.add(currentContent.getName()); 	

                    }				      
                }				
            }
        }		
        return new ArrayList<String>(groups);
    }

   /**
    * Helper method. <br>
    * 
    * Filter a list of licences and return only those valid for a give date.
    * <p>
    * Each license has a valid from and valid to date. The date given must be between those.
    * 
    * @param licenses The list of licenses to filter by the date
    * @param date the date (millis) validated against license.
    * @return The subset of licences that are valid at the give date.
    */    
    public static ArrayList<License> filterLicenseByValidDate(ArrayList<License> licenses, long date){
        ArrayList<License> filtered = new ArrayList<License>();
        for (License currentLicense : licenses){

            long validFromLong = Util.convertDateFormatToLong( currentLicense.getValidFrom());
            long validToLong = Util.convertDateFormatToLong( currentLicense.getValidTo());

            if (validFromLong <= date &&  date < validToLong ){ // interval: [start,end[ 
                filtered.add(currentLicense);	
            }

        }		
        return filtered;		
    }


    /**
     * Filter a list of licenses and return only those that validates from the user attributes. The method can be <br>
     * used to show a user all licenses that he has access to. It does not validating against specific ID's.<br>
     * There is also no presentationtype used in the filtering<br>
     * <p>  
     * The understand the license validation step from userattributes see the documentation.    
     * 
     * @param attributes The userattribus defining the user
     * @param allLicenses The licences to validate against the user attributes
     * @return The subset of licenses that validates against the user attributes.
     */
    public static ArrayList<License> findLicensesValidatingAccess(List<UserObjAttributeDto> attributes, ArrayList<License> allLicenses){		
        ArrayList<License> licenses = new  ArrayList<License>();
        
        //Iterate all licenses and test accesss
        boolean validatedForAtLeastOneLicense = false;
        for (License currentLicense : allLicenses){
            boolean licenseAllreadyAdded=false;
            //for each license check all attributegroups
            ArrayList<AttributeGroup> groups = currentLicense.getAttributeGroups();
            for (AttributeGroup currentGroup : groups){			 
                boolean allAttributeGroupPartsMatched = true;
                for (Attribute currentAttribute : currentGroup.getAttributes()){
                    ArrayList<UserObjAttributeDto> filtered = filterUserObjAttributesToValidatedOnly(currentAttribute, attributes);
                    if (filtered.size() == 0){ //Found attributegroup-part did not validate
                        allAttributeGroupPartsMatched = false; //Could break, but finding all attributegroup-parts matches is useful for debug purpose
                    }					
                }

                if (allAttributeGroupPartsMatched && ! licenseAllreadyAdded){				
                    validatedForAtLeastOneLicense=true;
                    licenseAllreadyAdded=true;
                    licenses.add(currentLicense);
                    log.debug("For license:"+ currentLicense.getLicenseName() + " VALIDATED for attributegroup number:"+currentGroup.getNumber());					

                }
                else{
                    log.debug("For license:"+ currentLicense.getLicenseName() + " FAILED VALIDATE for attributegroup number:"+currentGroup.getNumber());
                }
            }
        }
        log.info("Validate completed, access="+validatedForAtLeastOneLicense);
        return licenses;
    }

    //Filter so only the UserObjAttribute that match the license are returned. Values that does not match are also removed.

    
    
   /**
   * Filter a list of userAttributes against a license attribute (name and list of values). Return only those userAttributes that matches.
   * 
   * <p>
   * If the return list is not empty it means that the userattributes validates against the attributes
   * 
   * @param licenseattribute An attribute with a list of valid values
   * @param userAttributes The userattributes defining the user
   * @return The sublist of UserAttributes that validates
   */
    public static ArrayList<UserObjAttributeDto> filterUserObjAttributesToValidatedOnly(Attribute licenseattribute,  List<UserObjAttributeDto> userAttributes){
        String name = licenseattribute.getAttributeName();
        ArrayList<AttributeValue> values = licenseattribute.getValues();		
        ArrayList<UserObjAttributeDto> filteredUserObjAttributes = new ArrayList<UserObjAttributeDto>();

        for (UserObjAttributeDto currentUserObjAttribute : userAttributes){
            UserObjAttributeDto newFilteredObjAttribute = new UserObjAttributeDto(); //will be added to list returned if match		     
            ArrayList<String> newFilteredObjAttributeValues = new ArrayList<String>(); 
            newFilteredObjAttribute.setValues(newFilteredObjAttributeValues);
            if (currentUserObjAttribute.getAttribute().equals(name)){ //We have an attribute match, see if any values match				 
                newFilteredObjAttribute.setAttribute(name); //Name match, but does any values also match?
                for (String currentObjAttributeValue : currentUserObjAttribute.getValues()){ 
                    if (containsName(values,currentObjAttributeValue)){//Value match, add to filtered						
                        newFilteredObjAttributeValues.add(currentObjAttributeValue);						  
                    }					 
                }            	 
            }
            if (newFilteredObjAttributeValues.size() >0){ //we actually found attribute name and at least 1 value
                filteredUserObjAttributes.add(newFilteredObjAttribute); 
            }
        }

        return filteredUserObjAttributes;		
    }



    //
    /**
     * Filter a list of licenses and return only those that has at least one of the grouptypes defined with the presentationtype.
     * 
     * @param licenses List of licenses to be filtered
     * @param groups List groups where one must match with the presentationtype
     * @param The presentationtype that will be used in the matching.
     * 
     * @return List of filtered licenses 
     */    
    public static ArrayList<License> filterLicensesWithGroupNamesAndPresentationTypeRestrictionGroup(ArrayList<License> licenses,
            ArrayList<GroupType> groups, PresentationType presentationType){

        //Iterator over groups first, since each must be found
        HashSet<License> filteredSet = new HashSet<License>(); 
        int groupsFound = 0;
        for (GroupType currentGroup : groups){
            String groupKey = currentGroup.getKey();

            for (License currentLicense : licenses){
                boolean found = Util.groupsContainsGroupWithLicense(currentLicense.getLicenseContents(), groupKey, presentationType.getKey());                 
                if (found){ 
                    groupsFound++; //Can only happen once for each group due to the break below from inner loop
                    filteredSet.add(currentLicense); 				
                    break; // Group found, break inner loop			   
                }
            }			
        }
        if (groupsFound == groups.size()){ //All groups was matched
            return new ArrayList<License>(filteredSet);
        }

        return new ArrayList<License>(); //Empty list.

    }


    
    /**
     * Validate if at least licenses that match one of the grouptypes and the presentationtype in the case there is only packages (no klausulations).  
     * <p>
     * For no restriction groups access is given if just one license validates.
     * 
     * @param licenses List licenses to filter
     * @param groups List of grouptypes
     * @param presentationType The presentationtype that must match
     * @return List that will empty or always contain one license.
     */
    public static ArrayList<License> filterLicensesWithGroupNamesAndPresentationTypeNoRestrictionGroup(ArrayList<License> licenses,
            ArrayList<GroupType> groups, PresentationType presentationType){
        ArrayList<License> filtered= new  ArrayList<License>();
        for (License currentLicense : licenses){		
            for (GroupType currentGroup : groups){
                String groupKey = currentGroup.getKey();
                if (Util.groupsContainsGroupWithLicense(currentLicense.getLicenseContents(), groupKey, presentationType.getKey())){			     			        	 
                    filtered.add(currentLicense);

                    return filtered;
                } 
            }			 
        }
        return filtered; //Will be empty list
    }

 
    /**
     * Helper method <br>
     * Filter a list of grouptype and only keep restrictions(klasulering)
     * 
     * @param groups List of groyptypes to filter
     * @return List of grouptypes that are all restrictions
     */
    public static ArrayList<GroupType> filterRestrictionGroups(ArrayList<GroupType> groups){
        ArrayList<GroupType> filteredGroups = new ArrayList<GroupType>();

        for (GroupType currentGroup : groups){
            //TODO performence tuning, use cachedMap of GroupTypes.		
            if ( currentGroup.isRestrictionGroup() ){
                filteredGroups.add(currentGroup);
            }				   
        }			
        return filteredGroups;
    }

    //Maps the groups(String names) to the configured objects. 
    /**
     * Return a list of grouptypes (DTO) from a list of groupnames (String)
     * 
     * @param groups The list of names
     * @return List of GroupTypes
     * 
     * @throws InvalidArgumentServiceException if a grouptype  was found for a name in the list 
     */        
    public static ArrayList<GroupType> buildGroups(List<String> groups){
        ArrayList<GroupType> filteredGroups = new ArrayList<GroupType>();
        ArrayList<GroupType> configuredGroups = LicenseCache.getConfiguredLicenseGroupTypes();

        HashMap<String,GroupType> configuredGroupsNamesMap = new HashMap<String, GroupType>();
        for (GroupType current : configuredGroups){
            configuredGroupsNamesMap.put(current.getKey(), current);  
        }				   

        for (String currentGroup : groups){
            if (configuredGroupsNamesMap.containsKey(currentGroup)){
                filteredGroups.add(configuredGroupsNamesMap.get(currentGroup));
            }
            else{
                log.error("Group not found in Group configuration:"+currentGroup);
                throw new InvalidArgumentServiceException("Unknown group:"+currentGroup);
            }


        }		
        return filteredGroups;
    }

    
    /**
     * Returns a given presentationtype the name of the presentation.
     * 
     * @param presentationTypeName String value of the nake
     * @return The PresentationType DTO for the name
     * 
     * @throws InvalidArgumentServiceException if a no presentationtype with the name was not found 
     */
    public static PresentationType matchPresentationtype(String presentationTypeName){

        ArrayList<PresentationType> configuredTypes = LicenseCache.getConfiguredLicenseTypes();
        for (PresentationType currentType : configuredTypes){
            if (currentType.getKey().equals(presentationTypeName)){
                return currentType;
            }

        }				
        throw new InvalidArgumentServiceException("Unknown presentationType:"+presentationTypeName);		
    }



    /**
     * Generate the query that will be used when filtering ID's for access.<br> 
     * <p>
     * This is one of the fundamental purposes of the license module.
     * Each of the access groups will contribute to giving more access by adding a positive term.
     * Each of the missing Restrictiongroups will contribute ty restricting access by adding a negative term.
     * 
     * Simple Example:<br>
     * 2 accessGroups can contribute to the query part: (collection:dr OR collection:images) <br>
     * 2 missing restrictiongroups can contribute to the query part: -id:test123 -channel:dr5 <br>
     * The query that is appended when filtering ID's will be: <br>
     * <br>
     *  (collection:dr OR collection:images) -id:test666 -channel:dr5<br>
     *  <br>
     *  And the full query with a single ID will be:
     *  id:test111 AND  (collection:dr OR collection:images) -id:test666 -channel:dr5  <br>
     *  
     * @param accessGroups
     * @param missingRestrictionGroups
     * @return
     */

    public static String generateQueryString(ArrayList<String> accessGroups, ArrayList<String> missingRestrictionGroups){
        if (accessGroups.size() == 0){
            log.info("User does not have access to any group");
            return NO_ACCESS;
        }

        ArrayList<GroupType> accessGroupsType = buildGroups(accessGroups);
        ArrayList<GroupType> missingRestrictionGroupsType = buildGroups(missingRestrictionGroups);

        StringBuilder query = new StringBuilder(); 


        query.append("(("); //Outer around everything
        for (int i = 0; i<accessGroupsType.size(); i++){			
            String queryPart = accessGroupsType.get(i).getQuery();
            if (StringUtils.isBlank(queryPart)){ //Hack to allow empty queries.
                continue; //Skip
            }

            if (i >0){
                query.append("OR ");
            }

            query.append("(");			
            query.append(queryPart);

            query.append(")");
            if (i <accessGroupsType.size()-1){
                query.append(" ");
            }
        }  
        query.append(")");


        if (missingRestrictionGroupsType.size() == 0){
            return query.toString()+")"; //closing outer
        }

        for (int i = 0; i<missingRestrictionGroupsType.size(); i++){
            String queryPart = missingRestrictionGroupsType.get(i).getQuery();
            if (StringUtils.isBlank(queryPart)){ //Hack to allow empty queries.
                continue; //Skip
            }

            query.append(" -(");
            query.append(queryPart);
            query.append(")");
        }

        query.append(")");//closing outer

        return query.toString();
    }

    
    /**
     * Get the names of all presentationtypes in danish or english.
     * 
     * @param locale  On only 'da' and 'en' names are defined      
     * @return List of names of all presentationtype name in the requested language
     * 
     */
    public static ArrayList<String> getAllPresentationtypeNames(String locale){
        ArrayList<String> allTypes = new ArrayList<String>(); 

        ArrayList<PresentationType> configuredTypes = LicenseCache.getConfiguredLicenseTypes();

        for (PresentationType current : configuredTypes){
            if (LOCALE_EN.equals(locale)){
                allTypes.add(current.getValue_en());				
            }
            else if (LOCALE_DA.equals(locale)){
                allTypes.add(current.getValue_dk());
            }			
        }		
        return allTypes;
    }

    
    /**
     * Get the names of all grouptypes in danish or english.
     * 
     * @param locale  On only 'da' and 'en' names are defined      
     * @return List of names of all grouptype names in the requested language
     * 
     */
    public static ArrayList<String> getAllGroupeNames(String locale){
        ArrayList<String> allGroups = new ArrayList<String>(); 

        ArrayList<GroupType> configuredGroups = LicenseCache.getConfiguredLicenseGroupTypes();

        for (GroupType current : configuredGroups){
            if (LOCALE_EN.equals(locale)){
                allGroups.add(current.getValue_en());				
            }
            else if (LOCALE_DA.equals(locale)){
                allGroups.add(current.getValue_dk());
            }			
        }		
        return allGroups;
    }
    
    
   /**
   * Validate a locale (language) exists
   * 
   * @param locale
   * 
   * @throws InvalidArgumentServiceException if the locale is unknown. 
   */
    public static void validateLocale(String locale){
        if (locale == null){
            return; // okie
        }
        if (!locales.contains(locale)){
            throw new InvalidArgumentServiceException("Unknown locale:"+locale);
        }		
    }


    //recursive fix group name and presentationtype names to the locale
   
    /**
     * For a given list of UserGroups rename all names to a locale (danish or english)
     * 
     * 
     * @param input List of UserGroups
     * @param locale The locale. If locale is null it will default to 'da'
     */
    public static void fixLocale( ArrayList<UserGroupDto> input, String locale){
        if (locale == null){
            locale = LOCALE_DA;
        }

        for (UserGroupDto current : input){
            current.setGroupName(LicenseCache.getGroupName(current.getGroupName(), locale));
            ArrayList<String> presentationTypesNames = new ArrayList<String>();
            for (String name :  current.getPresentationTypes()){
                presentationTypesNames.add(LicenseCache.getPresentationtypeName(name, locale));
            }
            current.setPresentationTypes(presentationTypesNames);			 
        }		  
    }

    
    /**
    * Filter a list of ID's with a filter query.<br>
    * <p>
    * The filtering method to determine access to IDs. The filter query is generated by the licensemodule from the userattributes for that user.<br>  
    * If the filterQuery is empty it will return all ID's that does exists in Solr. This case is only used to find the noAccessId, so a user can be <br>
    * informed that id does exist, but you do not have access to it.
    * 
    * @param ids The resource id's to filter. The filter field is define in the configuration.
    * @param filterQuery FilterQuery. If null there is no filter query and this is used to if ID's does exist
    * @param solrIdField The resource_id field defined in the configuration used for id filtering in Solr.
    * @return
    * @throws org.apache.solr.client.solrj.SolrServerException If communication with solr fails. Should not happen
    * @throws java.io.IOException If there are IO errors. Should not happen
    */
    
    private static ArrayList<String> filterIDs(List<String> ids, String filterQuery, String solrIdField) throws org.apache.solr.client.solrj.SolrServerException, java.io.IOException {
        List<SolrServerClient> servers = ServiceConfig.SOLR_SERVERS;

        Set<String> accessfilteredIdsSet = new HashSet<String>();
        if (ids.size() >0) {
            for (SolrServerClient server: servers){
                //If filterquery is null, it will return the ID's that does exist in solr.
                List<String> accessfilteredIds =server.filterIds(ids, filterQuery, solrIdField);
                //log.info("#filtered id for server ("+input.getPresentationType()+") "+ server.getServerUrl() +" : "+filteredIds.size());
                accessfilteredIdsSet.addAll(accessfilteredIds);
            }
        }

        ArrayList<String> noAccessfilteredIdsList = new ArrayList<String>();
        noAccessfilteredIdsList.addAll(accessfilteredIdsSet);        
        return noAccessfilteredIdsList;

    }
    
    
    /*
    * Return true of any of the AttributeValues in the list has value =valueToFind
    */
   private static boolean containsName( ArrayList<AttributeValue> values, String valueToFind){

       for (AttributeValue current : values){
           if (current.getValue().equals(valueToFind)){
               return true;
           }
       }
       return false;
   }

}
