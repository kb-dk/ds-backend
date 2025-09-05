package dk.kb.license.validation;

import dk.kb.license.Util;
import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.*;
import dk.kb.license.solr.SolrServerClient;
import dk.kb.license.storage.*;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * This class is the encapsulation of most of the business logic when validating access.
 * <p>
 * See the OpenApi documentation for ds-license module to understand the concepts.
 */
public class LicenseValidator {
    private static final Logger log = LoggerFactory.getLogger(LicenseValidator.class);
    public static final String LOCALE_DA = "da";
    public static final String LOCALE_EN = "en";
    public static final String NO_ACCESS = ServiceConfig.SOLR_FILTER_ID_FIELD + ":NoAccess";

    public static final List<String> locales = Collections.unmodifiableList(Arrays.asList(LOCALE_DA, LOCALE_EN));

    /**
     * Extract all {@link License}s which validate for the user data. Licenses must also be active. (date to-from check).
     *
     * @param input The input describing all information known about the user.
     * @return List of all licenses that validates for the user input.
     */
    public static ArrayList<License> getUsersLicenses(GetUsersLicensesInputDto input) {
        //validate
        if (input.getAttributes() == null || input.getAttributes().size() == 0) {
            log.error("No attributes defined in input.");
            throw new IllegalArgumentException("No attributes defined in input");
        }

        validateLocale(input.getLocale());

        // Load all licences        
        ArrayList<License> allLicenses = LicenseCache.getAllLicense();
        //Filter by date first
        ArrayList<License> dateFilteredLicenses = filterLicenseByValidDate(allLicenses, System.currentTimeMillis());

        //Find licenses that give access for the dateFiltered licenses.
        ArrayList<License> accessLicenses = findLicensesValidatingAccess(input.getAttributes(), dateFilteredLicenses);

        return accessLicenses;
    }

    /**
     * Extract all {@link GroupType} and {@link PresentationType} that validates for the input that describes the user.
     * The {@link UserGroupDto} is a wrapper for {@link GroupType} that also has the  {@link PresentationType} as a list.
     * This method can be used to show a user what he has access to.
     *
     * <p>
     * Different licenses can give access to the same group, but a group will only be added once.
     * The solr query can be constructing by 'adding' all queries from the packages and remove exclusions.
     *
     * @param input The input describing all information known about the user.
     * @return List of all groups that validates for the user.
     * This will be extracted from all licenses that validates for the user input.
     */
    public static ArrayList<UserGroupDto> getUsersGroups(GetUserGroupsInputDto input) {
        //validate
        if (input.getAttributes() == null || input.getAttributes().size() == 0) {
            log.error("No attributes defined in input.");
            throw new InvalidArgumentServiceException("No attributes defined in input");
        }

        validateLocale(input.getLocale());

        // First filter by valid date
        ArrayList<License> allLicenses = LicenseCache.getAllLicense();

        ArrayList<License> dateFilteredLicenses = filterLicenseByValidDate(allLicenses, System.currentTimeMillis());

        //Find licenses that give access (not checking groups) for the dateFiltered licenses
        ArrayList<License> accessLicenses = findLicensesValidatingAccess(input.getAttributes(), dateFilteredLicenses);

        ArrayList<UserGroupDto> filteredGroups = filterGroupsWithPresentationtype(accessLicenses);

        LicenseValidator.fixLocale(filteredGroups, input.getLocale());

        return filteredGroups;
    }

    /**
     * Takes an input of ID's, which can be for different ID fields in solr
     * and filter them from the query generated from the user attributes.
     * If there are several solr servers configured this method will be called for each solr server.
     * <p>
     * The steps involved are:
     * <ol>
     *     <li>Extract all licenses that validate for the user.</li>
     *     <li>From the licenses collect all groups (packages/exclusion) that the user has access to.</li>
     *     <li>Generate the final solr filter-query from all groups.</li>
     *     <li>Return 3 different lists of id's:
     *     <ol>
     *         <li>List of id's that validates access. This is the main purpose of the ds-license module.</li>
     *         <li>List of id's that the user does not have access too.
     *             It can be useful to give this information to the user.</li>
     *         <li>List of id's that does not exists in solr. This should not happen.</li>
     *     </ol>
     *     </li>
     * </ol>
     *
     * @param input       The object containing the information about the caller and list of IDs to be filtered.
     * @param solrIdField The ID field in Solr used for filtering. So far only 'id' and 'resource_id' are suitable fields.
     */
    public static CheckAccessForIdsOutputDto checkAccessForIds(CheckAccessForIdsInputDto input, String solrIdField) throws InvalidArgumentServiceException, SolrServerException, IOException {
        if (input.getAccessIds() == null || input.getAccessIds().size() == 0) {
            throw new InvalidArgumentServiceException("No ID's in input");
        }
        log.debug("checkAccessForResourceID callled for presentationtype:" + input.getPresentationType());

        //Get the query. This also validates the input
        GetUserQueryInputDto inputQuery = new GetUserQueryInputDto();
        inputQuery.setAttributes(input.getAttributes());
        inputQuery.setPresentationType(input.getPresentationType());
        GetUserQueryOutputDto query = getUserQuery(inputQuery);
        CheckAccessForIdsOutputDto output = new CheckAccessForIdsOutputDto();
        output.setPresentationType(input.getPresentationType());
        output.setQuery(query.getQuery());

        List<SolrServerClient> servers = ServiceConfig.getSolrServers();
        ArrayList<String> filteredIdsSet = filterIDs(input.getAccessIds(), query.getQuery(), solrIdField);
        output.setAccessIds(filteredIdsSet);

        //Sanity check!
        if (output.getAccessIds().size() > input.getAccessIds().size()) {
            log.warn("Security problem::More ID's in output than input. Input ids:" + input.getAccessIds() + " output IDs:" + output.getAccessIds());
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
            log.warn(" Filter ID called with non existing IDs:" + nonExistingIds);
        }

        log.debug("#query IDs=" + input.getAccessIds().size() + " returned #filtered IDs=" + output.getAccessIds().size() + " using resourceId field:" + ServiceConfig.SOLR_FILTER_RESOURCE_ID_FIELD + " for input IDs:" + input.getAccessIds() + " non-access IDs:" + output.getNonAccessIds() + " non-existing IDs:" + nonExistingIds);
        return output;
    }


    /**
     * Get a {@link GetUserQueryOutputDto} object that has the following information about the user.
     *   <ul>
     *     <li>The filter query used when calling Solr to filter ID's</li>
     *     <li>List of names for all {@link GroupType} of type 'package' (Pakke) that the user has access to </li>
     *     <li>List of names for all {@link GroupType} of type 'exclusion' (Klausulering) that the user does NOT have access to </li>
     *   </ul>
     *
     * <p>
     * The filter query is used when filtering ID's in solr.<br>
     * The solr filtering is called with a query (list of ID's) and the filter query.<br>
     * The id field is defined in the configuration.<br>
     * Example: Query= id:(id1 OR id2 ... OR idn), Filter query: collection:dr
     *
     * @param input The input that defines the user.
     * @return GetUserQueryOutputDto With the filterquery and the two list of names for packages and exclusions GroupTypes
     */
    public static GetUserQueryOutputDto getUserQuery(GetUserQueryInputDto input) {
        //validate
        if (input.getAttributes() == null) {
            log.error("No attributes defined in input.");
            input.setAttributes(new ArrayList<UserObjAttributeDto>());
        }

        if (input.getPresentationType() == null) {
            log.error("No presentationtype defined in input.");
            throw new InvalidArgumentServiceException("No presentationtype defined in input.");
        }

        //Will throw exception if not matched
        matchPresentationtype(input.getPresentationType());

        // First filter by valid date
        ArrayList<License> allLicenses = LicenseCache.getAllLicense();
        ArrayList<License> dateFilteredLicenses = filterLicenseByValidDate(allLicenses, System.currentTimeMillis());

        //Find licenses that give access (not checking groups) for the dateFiltered licenses
        ArrayList<License> accessLicenses = findLicensesValidatingAccess(input.getAttributes(), dateFilteredLicenses);

        ArrayList<String> types = new ArrayList<String>();
        types.add(input.getPresentationType());

        ArrayList<String> filterGroups = filterGroups(accessLicenses, types);

        //Now we have to find all restriction-groups the user is missing
        ArrayList<GroupType> configuredRestrictionLicenseGroupTypes = LicenseCache.getConfiguredRestrictionLicenseGroupTypes();
        GetUserQueryOutputDto output = new GetUserQueryOutputDto();
        output.setUserLicenseGroups(filterGroups);

        ArrayList<String> missingRestrictionGroups = new ArrayList<String>();
        //First add all restriction groups then remove those that user has access too
        for (GroupType current : configuredRestrictionLicenseGroupTypes) {
            missingRestrictionGroups.add(current.getKey());
        }

        for (String current : filterGroups) {
            missingRestrictionGroups.remove(current);
        }
        output.setUserNotInDenyGroups(missingRestrictionGroups);

        String query = generateQueryString(filterGroups, missingRestrictionGroups);

        output.setQuery(query);
        return output;
    }

    /**
     * Validate if a user has access to a specific {@link PresentationType} from a list of {@link GroupType}s.
     * If there are no GroupTypes of restriction, just one of the GroupTypes needs to have a license giving access.<br>
     * If there are one or more GroupTypes of restrictions(klausulering) every one of them must validate.<br>
     * <p>
     * The logic is that each restriction can be seen as a padlock on the material.
     * If there are multiple locks, each padlock must be opened to access the material.
     *
     * @param input containing the specific presentationtype, groupnames and userattributes describing the user.
     */
    public static boolean validateAccess(ValidateAccessInputDto input) {
        //validate
        if (input.getAttributes() == null || input.getAttributes().size() == 0) {
            log.error("No attributes defined in input.");
            throw new InvalidArgumentServiceException("No attributes defined in input");
        }

        PresentationType presentationType = matchPresentationtype(input.getPresentationType());

        //Validate presentationType exists
        if (presentationType == null) {
            log.warn("Unknown presentationtype in validateAccess:" + input.getPresentationType());
            throw new InvalidArgumentServiceException("Unknown presentationtype in validateAccess:" + input.getPresentationType());
        }

        ArrayList<GroupType> groups = buildGroups(input.getGroups());
        //Validate groups. Same size or one was not matched.
        if (groups.size() != input.getGroups().size()) {
            log.warn("At least 1 unknown group  in validateAccess:" + input.getGroups());
            throw new InvalidArgumentServiceException("At least 1 unknown group  in validateAccess:" + input.getGroups());
        }

        ArrayList<GroupType> restrictionGroups = filterRestrictionGroups(groups);
        if (restrictionGroups.size() > 0) {
            log.debug("At least 1 restriction groups found in input, number of Restriction-groups:" + restrictionGroups.size());
        }

        // First filter by valid date
        ArrayList<License> allLicenses = LicenseCache.getAllLicense();
        ArrayList<License> dateFilteredLicenses = filterLicenseByValidDate(allLicenses, System.currentTimeMillis());

        //Find licenses that give access (not checking groups) for the dateFiltered licenses
        ArrayList<License> accessLicenses = findLicensesValidatingAccess(input.getAttributes(), dateFilteredLicenses);

        if (accessLicenses.size() == 0) {
            log.debug("No licenses validate access-part");
            return false;
        }

        //two situations. At least one restriction group involved, or no restriction groups.
        if (restrictionGroups.size() == 0) {
            log.debug("Case: no restriction(klausulering) group");
            //Simple situation. Just need to find 1 license having one of the groups with allowed presentationtype
            ArrayList<License> validatedLicenses = filterLicensesWithGroupNamesAndPresentationTypeNoRestrictionGroup(accessLicenses, groups, presentationType);
            return (validatedLicenses.size() > 0); //OK since at least 1 license found
        } else {
            // ALL groups+presentationtype must be in at least 1 license
            //Only Restriction groups are checked
            log.debug("Case: at least 1 restriction(klausulering) group");
            //notice only the restrictionGroups are used
            ArrayList<License> validatedLicenses = filterLicensesWithGroupNamesAndPresentationTypeRestrictionGroup(accessLicenses, restrictionGroups, presentationType);
            return (validatedLicenses.size() > 0); //OK since at least 1 license found
        }
    }

    /**
     * This method is called with a list of licenses. Each license has information about access to {@link GroupType} and
     * the allowed {@link PresentationType}s for that {@link GroupType}.
     * Get all dom-groups and for each dom-group find the union of PresentationTypes
     * <p>
     * From the licences extract all {@link GroupType}s with the {@link PresentationType}s which they give access to.
     * <p>
     * Multiple licenses can give access the same GroupType, and the group type will only be added once.
     * <br>
     * Example:<br>
     * License 1 has GroupType A with PresentationType 'Stream' and 'Search' <br>
     * License 2 has GroupType A with PresentationType 'Stream' and 'Download'<br>
     * <p>
     * The result will be GroupType A with all 3 PresentationTypes: 'Stream', 'Search' and 'Download'
     *
     * @param licenses of licenses
     * @return List UserGroups.
     */
    public static ArrayList<UserGroupDto> filterGroupsWithPresentationtype(ArrayList<License> licenses) {
        TreeMap<String, UserGroupDto> groups = new TreeMap<String, UserGroupDto>();
        for (License currentLicense : licenses) {
            for (LicenseContent currentGroup : currentLicense.getLicenseContents()) {
                String name = currentGroup.getName();
                UserGroupDto group = groups.get(name);
                if (group == null) {
                    group = new UserGroupDto();
                    group.setPresentationTypes(new ArrayList<String>());
                    group.setGroupName(name);
                    groups.put(name, group);
                }
                for (Presentation currentPresentation : currentGroup.getPresentations()) {
                    String presentation_key = currentPresentation.getKey();
                    if (group.getPresentationTypes().contains(presentation_key)) {
                        //Already added
                    } else {
                        group.getPresentationTypes().add(presentation_key);
                    }
                }
            }
        }
        return new ArrayList<UserGroupDto>(groups.values());
    }

    /**
     * Helper method.
     * <p>
     * Extract all GroupTypes(name only) having at least one of the PresentationsTypes from list of licences
     * Each license can have multiple GroupTypes each having 1 or more PresentationTypes allowed.
     * <p>
     *
     * @param licenses          List of licenses.
     * @param presentationTypes List of PresentationTypes (name only)
     * @return List of GroupTypes (names only) that has at least one of the PresentationTypes allowed.
     */
    public static ArrayList<String> filterGroups(ArrayList<License> licenses, ArrayList<String> presentationTypes) {
        HashSet<String> groups = new HashSet<String>();
        for (License current : licenses) {
            for (LicenseContent currentContent : current.getLicenseContents()) {
                for (Presentation currentPresentation : currentContent.getPresentations()) {
                    if (presentationTypes.contains(currentPresentation.getKey())) {
                        groups.add(currentContent.getName());

                    }
                }
            }
        }
        return new ArrayList<String>(groups);
    }

    /**
     * Helper method. <br>
     * <p>
     * Filter a list of licences and return those valid for a give date.
     * <p>
     * Each license has a valid from and valid to date. The date given must be between those values.
     *
     * @param licenses The list of licenses to apply date filtering for.
     * @param date     the date (milliseconds) validated against license.
     * @return The subset of licenses that are valid at the give date.
     */
    public static ArrayList<License> filterLicenseByValidDate(ArrayList<License> licenses, long date) {
        ArrayList<License> filtered = new ArrayList<License>();
        for (License currentLicense : licenses) {

            long validFromLong = Util.convertDateFormatToLong(currentLicense.getValidFrom());
            long validToLong = Util.convertDateFormatToLong(currentLicense.getValidTo());

            if (validFromLong <= date && date < validToLong) { // interval: [start,end[
                filtered.add(currentLicense);
            }

        }
        return filtered;
    }

    /**
     * Filter a list of licenses and return only those that validates from the user attributes. The method can be <br>
     * used to show a user all licenses that he/she has access to.<br>
     * There is also no PresentationType used in the filtering.<br>
     * <p>
     * For a better understanding of license validation, please see either the OpenAPI spefication or POM documentation.
     *
     * @param attributes  The user attributes defining the user.
     * @param allLicenses The licences to validate against the user attributes.
     * @return The subset of licenses that validates against the user attributes.
     */
    public static ArrayList<License> findLicensesValidatingAccess(List<UserObjAttributeDto> attributes, ArrayList<License> allLicenses) {
        ArrayList<License> licenses = new ArrayList<License>();

        //Iterate all licenses and test accesss
        boolean validatedForAtLeastOneLicense = false;
        for (License currentLicense : allLicenses) {
            boolean licenseAllreadyAdded = false;
            //for each license check all attributegroups
            ArrayList<AttributeGroup> groups = currentLicense.getAttributeGroups();
            for (AttributeGroup currentGroup : groups) {
                boolean allAttributeGroupPartsMatched = true;
                for (Attribute currentAttribute : currentGroup.getAttributes()) {
                    ArrayList<UserObjAttributeDto> filtered = filterUserObjAttributesToValidatedOnly(currentAttribute, attributes);
                    if (filtered.size() == 0) { //Found attributegroup-part did not validate
                        allAttributeGroupPartsMatched = false; //Could break, but finding all attributegroup-parts matches is useful for debug purpose
                    }
                }

                if (allAttributeGroupPartsMatched && !licenseAllreadyAdded) {
                    validatedForAtLeastOneLicense = true;
                    licenseAllreadyAdded = true;
                    licenses.add(currentLicense);
                    log.debug("For license:" + currentLicense.getLicenseName() + " VALIDATED for attributegroup number:" + currentGroup.getNumber());

                } else {
                    log.debug("For license:" + currentLicense.getLicenseName() + " FAILED VALIDATE for attributegroup number:" + currentGroup.getNumber());
                }
            }
        }
        log.debug("Validate completed, access=" + validatedForAtLeastOneLicense + " for userdata:" + attributes);
        return licenses;
    }

    /**
     * Filter a list of userAttributes against a license attribute (name and list of values).
     * Return only those userAttributes that matches.
     * <p>
     * If the return list is not empty it means that the user attributes validates against the license attributes.
     *
     * @param licenseAttribute An attribute with a list of valid values.
     * @param userAttributes   The userAttributes defining the user.
     * @return The sublist of UserAttributes that validates.
     */
    public static ArrayList<UserObjAttributeDto> filterUserObjAttributesToValidatedOnly(Attribute licenseAttribute, List<UserObjAttributeDto> userAttributes) {
        String name = licenseAttribute.getAttributeName();
        ArrayList<AttributeValue> values = licenseAttribute.getValues();
        ArrayList<UserObjAttributeDto> filteredUserObjAttributes = new ArrayList<UserObjAttributeDto>();

        for (UserObjAttributeDto currentUserObjAttribute : userAttributes) {
            UserObjAttributeDto newFilteredObjAttribute = new UserObjAttributeDto(); //will be added to list returned if match
            ArrayList<String> newFilteredObjAttributeValues = new ArrayList<String>();
            newFilteredObjAttribute.setValues(newFilteredObjAttributeValues);
            if (currentUserObjAttribute.getAttribute().equals(name)) { //We have an attribute match, see if any values match
                newFilteredObjAttribute.setAttribute(name); //Name match, but does any values also match?
                for (String currentObjAttributeValue : currentUserObjAttribute.getValues()) {
                    if (containsName(values, currentObjAttributeValue)) {//Value match, add to filtered
                        newFilteredObjAttributeValues.add(currentObjAttributeValue);
                    }
                }
            }
            if (newFilteredObjAttributeValues.size() > 0) { //we actually found attribute name and at least 1 value
                filteredUserObjAttributes.add(newFilteredObjAttribute);
            }
        }

        return filteredUserObjAttributes;
    }

    /**
     * From a list of licenses return only those that has at least one of the GroupTypes with given PresentationType.
     * <p>
     * Example: If a license does not have the GroupType it not be included. If a license does have the GroupType,
     * but not the PresentationType it will also not be included.
     *
     * <p>
     *
     * @param licenses         List of licenses to be filtered.
     * @param groups           List groups where one must match with the PresentationType.
     * @param presentationType The PresentationType that will be used in the matching.
     * @return List of filtered licenses.
     */
    public static ArrayList<License> filterLicensesWithGroupNamesAndPresentationTypeRestrictionGroup(ArrayList<License> licenses, ArrayList<GroupType> groups, PresentationType presentationType) {

        //Iterator over groups first, since each must be found
        HashSet<License> filteredSet = new HashSet<License>();
        int groupsFound = 0;
        for (GroupType currentGroup : groups) {
            String groupKey = currentGroup.getKey();

            for (License currentLicense : licenses) {
                boolean found = Util.groupsContainsGroupWithLicense(currentLicense.getLicenseContents(), groupKey, presentationType.getKey());
                if (found) {
                    groupsFound++; //Can only happen once for each group due to the break below from inner loop
                    filteredSet.add(currentLicense);
                    break; // Group found, break inner loop
                }
            }
        }
        if (groupsFound == groups.size()) { //All groups was matched
            return new ArrayList<License>(filteredSet);
        }

        return new ArrayList<License>(); //Empty list.
    }

    /**
     * Validate if at least licenses that match one of the GroupTypes and the PresentationType
     * in the case there are only packages (no restrictions).
     * <p>
     * For no restriction groups access is given if just one license validates.
     *
     * @param licenses         List of licenses to filter
     * @param groups           List of GroupTypes.
     * @param presentationType The PresentationType that must match
     * @return List of licenses that will be empty or always contain one license.
     */
    public static ArrayList<License> filterLicensesWithGroupNamesAndPresentationTypeNoRestrictionGroup(ArrayList<License> licenses, ArrayList<GroupType> groups, PresentationType presentationType) {
        ArrayList<License> filtered = new ArrayList<License>();
        for (License currentLicense : licenses) {
            for (GroupType currentGroup : groups) {
                String groupKey = currentGroup.getKey();
                if (Util.groupsContainsGroupWithLicense(currentLicense.getLicenseContents(), groupKey, presentationType.getKey())) {
                    filtered.add(currentLicense);

                    return filtered;
                }
            }
        }
        return filtered; //Will be empty list
    }

    /**
     * Helper method <br>
     * Filter a list of GroupType and only keep restrictions(klasulering).
     *
     * @param groups List of GroupTypes to filter.
     * @return Sublist of input GroupTypes that are all restrictions.
     */
    public static ArrayList<GroupType> filterRestrictionGroups(ArrayList<GroupType> groups) {
        ArrayList<GroupType> filteredGroups = new ArrayList<GroupType>();

        for (GroupType currentGroup : groups) {
            //TODO performence tuning, use cachedMap of GroupTypes.
            if (currentGroup.isRestrictionGroup()) {
                filteredGroups.add(currentGroup);
            }
        }
        return filteredGroups;
    }

    /**
     * Return a list of {@link GroupType}s (DTO) from a list of group names (String).
     *
     * @param groups The list of names that are mapped to {@link GroupType} DTOs.
     * @return List of GroupTypes created from input group names.
     * @throws InvalidArgumentServiceException if a GroupType was not found for a name in the list
     */
    public static ArrayList<GroupType> buildGroups(List<String> groups) {
        ArrayList<GroupType> filteredGroups = new ArrayList<GroupType>();
        ArrayList<GroupType> configuredGroups = LicenseCache.getConfiguredLicenseGroupTypes();

        HashMap<String, GroupType> configuredGroupsNamesMap = new HashMap<String, GroupType>();
        for (GroupType current : configuredGroups) {
            configuredGroupsNamesMap.put(current.getKey(), current);
        }

        for (String currentGroup : groups) {
            if (configuredGroupsNamesMap.containsKey(currentGroup)) {
                filteredGroups.add(configuredGroupsNamesMap.get(currentGroup));
            } else {
                log.error("Group not found in Group configuration:" + currentGroup);
                throw new InvalidArgumentServiceException("Unknown group:" + currentGroup);
            }
        }
        return filteredGroups;
    }

    /**
     * Returns a given {@link PresentationType} from the name of the PresentationType.
     *
     * @param presentationTypeName String value of the name.
     * @return The PresentationType DTO for the name.
     * @throws InvalidArgumentServiceException if no PresentationType with the name was found.
     */
    public static PresentationType matchPresentationtype(String presentationTypeName) {

        ArrayList<PresentationType> configuredTypes = LicenseCache.getConfiguredLicenseTypes();
        for (PresentationType currentType : configuredTypes) {
            if (currentType.getKey().equals(presentationTypeName)) {
                return currentType;
            }
        }
        throw new InvalidArgumentServiceException("Unknown presentationType:" + presentationTypeName);
    }

    /**
     * Generate the query that will be used when filtering ID's for access.<br>
     * <p>
     * This is one of the fundamental purposes of the license module.
     * Each of the access groups will contribute to giving more access by adding a positive term.
     * Each of the missing Restriction groups will contribute to restricting access by adding a negative term.
     * <p>
     * Simple Example:<br>
     * Two accessGroups can contribute to the query part: (collection:dr OR collection:images) <br>
     * Two missing restrictiongroups can contribute to the query part: -id:test123 -channel:dr5 <br>
     * The query that is appended when filtering ID's will be: <br>
     * <br>
     * (collection:dr OR collection:images) -id:test666 -channel:dr5<br>
     * <br>
     * And the full query with a single ID will be:
     * id:test111 AND  (collection:dr OR collection:images) -id:test666 -channel:dr5  <br>
     *
     * @param accessGroups
     * @param missingRestrictionGroups
     * @return The filter query string will be used to filter ID's by calling Solr.
     */
    public static String generateQueryString(ArrayList<String> accessGroups, ArrayList<String> missingRestrictionGroups) {
        if (accessGroups.size() == 0) {
            log.info("User does not have access to any group");
            return NO_ACCESS;
        }

        ArrayList<GroupType> accessGroupsType = buildGroups(accessGroups);
        ArrayList<GroupType> missingRestrictionGroupsType = buildGroups(missingRestrictionGroups);

        StringBuilder query = new StringBuilder();

        query.append("(("); //Outer around everything
        for (int i = 0; i < accessGroupsType.size(); i++) {
            String queryPart = accessGroupsType.get(i).getQuery();
            if (StringUtils.isBlank(queryPart)) { //Hack to allow empty queries.
                continue; //Skip
            }

            if (i > 0) {
                query.append("OR ");
            }

            query.append("(");
            query.append(queryPart);

            query.append(")");
            if (i < accessGroupsType.size() - 1) {
                query.append(" ");
            }
        }
        query.append(")");

        if (missingRestrictionGroupsType.size() == 0) {
            return query + ")"; //closing outer
        }

        for (int i = 0; i < missingRestrictionGroupsType.size(); i++) {
            String queryPart = missingRestrictionGroupsType.get(i).getQuery();
            if (StringUtils.isBlank(queryPart)) { //Hack to allow empty queries.
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
     * Get the names of all {@link PresentationType}s in danish or english.
     * <p>
     *
     * @param locale Language for the strings. Only 'da' and 'en' names are defined.
     * @return List of names of all PresentationType name in the requested language.
     */
    public static ArrayList<String> getAllPresentationtypeNames(String locale) {
        ArrayList<String> allTypes = new ArrayList<String>();

        ArrayList<PresentationType> configuredTypes = LicenseCache.getConfiguredLicenseTypes();

        for (PresentationType current : configuredTypes) {
            if (LOCALE_EN.equals(locale)) {
                allTypes.add(current.getValue_en());
            } else if (LOCALE_DA.equals(locale)) {
                allTypes.add(current.getValue_dk());
            }
        }
        return allTypes;
    }

    /**
     * Get the names of all {@link GroupType}s in danish or english.
     *
     * @param locale Language for the strings. Only 'da' and 'en' names are defined.
     * @return List of all GroupType names in the requested language.
     *
     */
    public static ArrayList<String> getAllGroupeNames(String locale) {
        ArrayList<String> allGroups = new ArrayList<String>();

        ArrayList<GroupType> configuredGroups = LicenseCache.getConfiguredLicenseGroupTypes();

        for (GroupType current : configuredGroups) {
            if (LOCALE_EN.equals(locale)) {
                allGroups.add(current.getValue_en());
            } else if (LOCALE_DA.equals(locale)) {
                allGroups.add(current.getValue_dk());
            }
        }
        return allGroups;
    }

    /**
     * Validate that a locale (language) exists.
     *
     * @param locale string containing the locale to lookup.
     * @throws InvalidArgumentServiceException if the locale is unknown.
     */
    public static void validateLocale(String locale) {
        if (locale == null) {
            return; // okie
        }
        if (!locales.contains(locale)) {
            throw new InvalidArgumentServiceException("Unknown locale:" + locale);
        }
    }

    /**
     * For a given list of UserGroups rename all names to a locale (danish or english)
     *
     * @param input  List of UserGroups to rename.
     * @param locale The locale. If locale is null it will default to 'da'.
     */
    public static void fixLocale(ArrayList<UserGroupDto> input, String locale) {
        if (locale == null) {
            locale = LOCALE_DA;
        }

        for (UserGroupDto current : input) {
            current.setGroupName(LicenseCache.getGroupName(current.getGroupName(), locale));
            ArrayList<String> presentationTypesNames = new ArrayList<String>();
            for (String name : current.getPresentationTypes()) {
                presentationTypesNames.add(LicenseCache.getPresentationtypeName(name, locale));
            }
            current.setPresentationTypes(presentationTypesNames);
        }
    }

    /**
     * Filter a list of ID's with a filter query.<br>
     * <p>
     * The filtering method to determine access to IDs.
     * The filter query is generated by the licensemodule from the userattributes for a given user.<br>
     * If the filterQuery is empty it will return all ID's that exists in Solr.
     * This case is only used to find the noAccessId, so a user can be
     * informed that am id does exist, but there is access to it through the users attributes.
     *
     * @param ids         The resource id's to filter. The filter field is define in the configuration.
     * @param filterQuery FilterQuery. If null there is no filter query and this is used to if ID's does exist
     * @param solrIdField The resource_id field defined in the configuration used for id filtering in Solr.
     * @return The subset of ID's from the input that validated against the filter by calling Solr.
     * @throws org.apache.solr.client.solrj.SolrServerException If communication with solr fails. Should not happen
     * @throws java.io.IOException                              If there are IO errors. Should not happen
     */
    private static ArrayList<String> filterIDs(List<String> ids, String filterQuery, String solrIdField) throws org.apache.solr.client.solrj.SolrServerException, java.io.IOException {
        List<SolrServerClient> servers = ServiceConfig.getSolrServers();

        Set<String> accessfilteredIdsSet = new HashSet<String>();
        if (ids.size() > 0) {
            for (SolrServerClient server : servers) {
                //If filterquery is null, it will return the ID's that does exist in solr.
                List<String> accessfilteredIds = server.filterIds(ids, filterQuery, solrIdField);
                //log.info("#filtered id for server ("+input.getPresentationType()+") "+ server.getServerUrl() +" : "+filteredIds.size());
                accessfilteredIdsSet.addAll(accessfilteredIds);
            }
        }

        ArrayList<String> noAccessfilteredIdsList = new ArrayList<String>();
        noAccessfilteredIdsList.addAll(accessfilteredIdsSet);
        return noAccessfilteredIdsList;
    }

    /**
     * Return true if any of the AttributeValues in the list has value =valueToFind
     *
     * @param values
     * @param valueToFind
     * @return
     */
    private static boolean containsName(ArrayList<AttributeValue> values, String valueToFind) {

        for (AttributeValue current : values) {
            if (current.getValue().equals(valueToFind)) {
                return true;
            }
        }
        return false;
    }
}
