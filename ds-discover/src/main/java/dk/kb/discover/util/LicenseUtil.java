package dk.kb.discover.util;

import dk.kb.discover.config.ServiceConfig;
import dk.kb.license.client.v1.DsLicenseApi;
import dk.kb.license.model.v1.GetUserQueryInputDto;
import dk.kb.license.model.v1.UserObjAttributeDto;
import dk.kb.license.util.DsLicenseClient;

import java.util.ArrayList;
import java.util.List;

public class LicenseUtil {
    private static DsLicenseApi licenseClient;


    public static DsLicenseApi getDsLicenseApiClient() {

        if (licenseClient!= null) {
            return licenseClient;
        }

        String dsLicenseUrl = ServiceConfig.getConfig().getString("licensemodule.url");
        licenseClient = new DsLicenseClient(dsLicenseUrl);
        return licenseClient;
    }

    public static GetUserQueryInputDto getLicenseQueryDto() {
        GetUserQueryInputDto getQueryDto = new GetUserQueryInputDto();

        getQueryDto.setPresentationType("Search"); // Important. Must be defined in Licensemodule with same name

        //"everybody=true" is a value everyone will (from keycloak?)
        UserObjAttributeDto everybodyUserAttribute=new UserObjAttributeDto();
        everybodyUserAttribute.setAttribute("everybody");
        ArrayList<String> values = new ArrayList<String>();
        values.add("yes");
        everybodyUserAttribute.setValues(values);

        List<UserObjAttributeDto> allAttributes = new ArrayList<UserObjAttributeDto>();
        allAttributes.add(everybodyUserAttribute);

        getQueryDto.setAttributes(allAttributes);
        return getQueryDto;


    }


}
