package dk.kb.license.api.v1.impl;

import dk.kb.license.api.v1.DsLicenseApi;
import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.*;
import dk.kb.license.storage.*;
import dk.kb.license.validation.LicenseValidator;
import dk.kb.util.webservice.ImplBase;
import org.apache.cxf.interceptor.InInterceptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;

/**
 * ds-license by the Royal Danish Library
 */
@InInterceptors(interceptors = "dk.kb.license.webservice.KBAuthorizationInterceptor")
public class DsLicenseApiServiceImpl extends ImplBase implements DsLicenseApi {
    private static final Logger log = LoggerFactory.getLogger(DsLicenseApiServiceImpl.class);

    @Override
    public CheckAccessForIdsOutputDto checkAccessForIds(@NotNull CheckAccessForIdsInputDto input) {
        log.debug("checkAccessForIds(...) called with call details: {}", getCallDetails());

        try {
            PresentationType presentationType = LicenseValidator.matchPresentationtype(input.getPresentationType());
        } catch (IllegalArgumentException e) {
            log.error("Unknown presentationtype: " + input.getPresentationType());
            CheckAccessForIdsOutputDto output = new CheckAccessForIdsOutputDto();
            output.setAccessIds(new ArrayList<String>());
            output.setPresentationType(input.getPresentationType());
            output.setQuery("(NoAccess:NoAccess)"); //query that returns nothing

            return output;
        }

        try {
            CheckAccessForIdsOutputDto output = LicenseValidator.checkAccessForIds(input, ServiceConfig.SOLR_FILTER_ID_FIELD);

            return output;
        } catch (Exception e) {
            log.error("Error in checkAccessForIds: ", e);
            throw handleException(e);
        }
    }

    @Override
    public CheckAccessForIdsOutputDto checkAccessForResourceIds(@NotNull CheckAccessForIdsInputDto input) {
        log.debug("checkAccessForResourceIds(...) called with call details: {}", getCallDetails());

        try {
            PresentationType presentationType = LicenseValidator.matchPresentationtype(input.getPresentationType());
        } catch (IllegalArgumentException e) {
            log.error("Unknown presentationtype: " + input.getPresentationType());
            CheckAccessForIdsOutputDto output = new CheckAccessForIdsOutputDto();
            output.setAccessIds(new ArrayList<String>());
            output.setPresentationType(input.getPresentationType());
            output.setQuery("(NoAccess:NoAccess)"); //query that returns nothing

            return output;
        }

        try {
            CheckAccessForIdsOutputDto output = LicenseValidator.checkAccessForIds(input, ServiceConfig.SOLR_FILTER_RESOURCE_ID_FIELD);

            return output;
        } catch (Exception e) {
            log.error("Error in checkAccessForResourceIds: ", e);
            throw handleException(e);
        }
    }

    @Override
    public ValidateAccessOutputDto validateAccess(@Valid ValidateAccessInputDto input) {
        log.debug("validateAccess(...) called with call details: {}", getCallDetails());
        try {
            boolean access = LicenseValidator.validateAccess(input);
            ValidateAccessOutputDto output = new ValidateAccessOutputDto();
            output.setAccess(access);

            return output;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public GetUsersLicensesOutputDto getUserLicenses(@NotNull GetUsersLicensesInputDto input) {
        log.debug("getUserLicenses(...) called with call details: {}", getCallDetails());

        ArrayList<LicenseOverviewDto> list = new ArrayList<LicenseOverviewDto>();
        GetUsersLicensesOutputDto output = new GetUsersLicensesOutputDto();
        output.setLicenses(list);
        try {
            ArrayList<License> licenses = LicenseValidator.getUsersLicenses(input);

            for (License current : licenses) {
                LicenseOverviewDto item = new LicenseOverviewDto();
                if ("en".equals(input.getLocale())) {
                    item.setName(current.getLicenseName_en());
                    item.setDescription(current.getDescription_en());
                } else {
                    item.setName(current.getLicenseName());
                    item.setDescription(current.getDescription_dk());
                }

                item.setValidFrom(current.getValidFrom());
                item.setValidTo(current.getValidTo());
                list.add(item);
            }

            return output;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    public GetUsersFilterQueryOutputDto getUserLicenseQuery(@NotNull GetUserQueryInputDto input) {
        log.debug("getUserLicenseQuery(...) called with call details: {}", getCallDetails());

        try {
            GetUserQueryOutputDto output = LicenseValidator.getUserQuery(input);

            log.debug("-------------------getUserLicenseQuery----------------");
            log.debug("input (presentationtype): " + input.getPresentationType());
            log.debug("input (attributes): " + input.getAttributes());
            log.debug("output (User license query): " + output.getQuery());
            log.debug("output (groups) " + output.getUserLicenseGroups());

            GetUsersFilterQueryOutputDto filterQuery = new GetUsersFilterQueryOutputDto();
            filterQuery.setFilterQuery(output.getQuery());

            return filterQuery;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public GetUserGroupsOutputDto getUserGroups(GetUserGroupsInputDto input) {
        log.debug("getUserGroups(...) called with call details: {}", getCallDetails());
        try {
            ArrayList<UserGroupDto> groups = LicenseValidator.getUsersGroups(input);
            GetUserGroupsOutputDto output = new GetUserGroupsOutputDto();
            output.setGroups(groups);

            return output;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public GetUserGroupsAndLicensesOutputDto getUserGroupsAndLicenses(GetUserGroupsAndLicensesInputDto input) {
        log.debug("getUserGroupsAndLicenses(...) called with call details: {}", getCallDetails());

        GetUserGroupsInputDto input1 = new GetUserGroupsInputDto();
        input1.setAttributes(input.getAttributes());
        input1.setLocale(input.getLocale());

        GetUsersLicensesInputDto input2 = new GetUsersLicensesInputDto();
        input2.setAttributes(input.getAttributes());
        input2.setLocale(input.getLocale());

        GetUserGroupsOutputDto userGroups = getUserGroups(input1);

        GetUsersLicensesOutputDto userLicenses = getUserLicenses(input2);
        GetUserGroupsAndLicensesOutputDto output = new GetUserGroupsAndLicensesOutputDto();
        output.setAllPresentationTypes(LicenseValidator.getAllPresentationtypeNames(input.getLocale()));
        output.setAllGroups(LicenseValidator.getAllGroupeNames(input.getLocale()));
        output.setGroups(userGroups.getGroups());
        output.setLicenses(userLicenses.getLicenses());

        return output;
    }

    private  AttributeDto convertAttributeToAttributeDto(Attribute attribute) {
        AttributeDto attributeDto = new AttributeDto();
        attributeDto.setId(attribute.getId());
        attributeDto.setAttributeName(attribute.getAttributeName());

        ArrayList<AttributeValueDto> attributeValueDtos = new ArrayList<>();
        for ( AttributeValue value : attribute.getValues() ){
            AttributeValueDto attributeValueDto = new AttributeValueDto();
            attributeValueDto.setId(value.getId());
            attributeValueDto.setValue(value.getValue());
            attributeValueDtos.add(attributeValueDto);
        }

        attributeDto.setValues(attributeValueDtos);

        return attributeDto;

    }

    private AttributeGroupDto convertAttributeGroupToDto(AttributeGroup attributeGroup){
        AttributeGroupDto attributeGroupDto = new AttributeGroupDto();
        attributeGroupDto.setId(attributeGroup.getId());
        attributeGroupDto.setNumber(attributeGroup.getNumber());

        ArrayList<AttributeDto> attributeDtos = new ArrayList<>();
        attributeGroup.getAttributes().forEach(attribute -> attributeDtos.add(convertAttributeToAttributeDto(attribute)));

        attributeGroupDto.setAttributes(attributeDtos);

        return attributeGroupDto;

    }

    private LicenseContentDto convertLicenseContentToDto(LicenseContent licenseContent) {
        LicenseContentDto licenseContentDto = new LicenseContentDto();
        licenseContentDto.setId(licenseContent.getId());
        licenseContentDto.setName(licenseContent.getName());

        ArrayList<PresentationDto> presentationDtos = new ArrayList<>();

        for ( Presentation presentation : licenseContent.getPresentations() ) {
            PresentationDto presentationDto = new PresentationDto();
            presentationDto.setId(presentation.getId());
            presentationDto.setKey(presentation.getKey());

            presentationDtos.add(presentationDto);

        }

        licenseContentDto.setPresentations(presentationDtos);

        return licenseContentDto;

    }

    @Override
    public LicenseDto getLicenseById(Long id) {
        License license = BaseModuleStorage.performStorageAction("getLicense(" + id + ")", LicenseModuleStorage.class, storage -> {
            return ((LicenseModuleStorage) storage).getLicense(id);
        });

        LicenseDto licenseDto = new LicenseDto();
        licenseDto.setId(license.getId());
        licenseDto.setDescriptionDk(license.getDescription_dk());
        licenseDto.setLicenseName(license.getLicenseName());
        licenseDto.setDescriptionEn(license.getDescription_en());
        licenseDto.setLicenseNameEn(license.getLicenseName_en());
        licenseDto.setValidFrom(license.getValidFrom());
        licenseDto.setValidTo(license.getValidTo());

        ArrayList<AttributeGroup> attributeGroups = license.getAttributeGroups();
        ArrayList<AttributeGroupDto> attributeGroupsDto = new ArrayList<>();

        attributeGroups.forEach(attributeGroup -> attributeGroupsDto.add(convertAttributeGroupToDto(attributeGroup)));
        licenseDto.setAttributeGroups(attributeGroupsDto);

        ArrayList<LicenseContent> licenseContents = license.getLicenseContents();
        ArrayList<LicenseContentDto> licenseContentDtos = new ArrayList<>();

        licenseContents.forEach(licenseContent -> licenseContentDtos.add(convertLicenseContentToDto(licenseContent)));

        licenseDto.setLicenseContents(licenseContentDtos);

        return licenseDto;
    }
}