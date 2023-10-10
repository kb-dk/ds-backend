package dk.kb.image.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.StreamingOutput;

import dk.kb.license.invoker.v1.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.image.api.v1.impl.AccessApiServiceImpl;
import dk.kb.image.config.ServiceConfig;
import dk.kb.license.client.v1.DsLicenseApi;
import dk.kb.license.model.v1.CheckAccessForIdsInputDto;
import dk.kb.license.model.v1.CheckAccessForIdsOutputDto;
import dk.kb.license.model.v1.UserObjAttributeDto;
import dk.kb.license.util.DsLicenseClient;
import dk.kb.util.Resolver;
import dk.kb.util.webservice.exception.InternalServiceException;

public class ImageAccessValidation {
    private static final Logger log = LoggerFactory.getLogger(ImageAccessValidation.class);
    private static DsLicenseApi licenseClient;

    public static enum ACCESS_TYPE {
        ACCESS, NO_ACCESS, ID_NON_EXISTING
    };

        
    public static ACCESS_TYPE accessTypeForImage(String resource_id) throws ApiException {

        // Add filter query from license module.
        DsLicenseApi licenseClient = getDsLicenseApiClient();
        CheckAccessForIdsInputDto licenseQueryDto = getCheckAccessForIdsInputDto(resource_id);
        CheckAccessForIdsOutputDto checkAccessForIds;
        try {
           checkAccessForIds = licenseClient.checkAccessForResourceIds(licenseQueryDto); // Use
        }
        catch(Exception e) {
            log.error("Error calling licensemodule",e);
            throw new InternalServiceException("Error calling licensemodule");            
        }
        
        List<String> access_ids = checkAccessForIds.getAccessIds();
        List<String> non_access_ids = checkAccessForIds.getNonAccessIds();
        List<String> non_existing_ids = checkAccessForIds.getNonExistingIds();

        // Check in other that happens most frequent.
        if (access_ids.contains(resource_id)) {
            return ACCESS_TYPE.ACCESS;
        } else if (non_access_ids.contains(resource_id)) {
            log.info("no access to resource_id:" + resource_id);
            return ACCESS_TYPE.NO_ACCESS;
        } else if (non_existing_ids.contains(resource_id)) {
            log.info("none existing image resource_id:" + resource_id);
            return ACCESS_TYPE.ID_NON_EXISTING;
        } else { // Sanity check, should not happen
            log.error("Could not match resource_id to access,no_access,non_existing: " + resource_id);
            throw new InternalServiceException(
                    "Could not match resource_id to access,no_access,non_existing: " + resource_id);
        }

    }

    private static CheckAccessForIdsInputDto getCheckAccessForIdsInputDto(String resource_id) {

        CheckAccessForIdsInputDto idsDto = new CheckAccessForIdsInputDto();
        // TODO these attributes must come from Keycloak
        UserObjAttributeDto everybodyUserAttribute = new UserObjAttributeDto();
        everybodyUserAttribute.setAttribute("everybody");
        ArrayList<String> values = new ArrayList<String>();
        values.add("yes");
        everybodyUserAttribute.setValues(values);

        List<UserObjAttributeDto> allAttributes = new ArrayList<UserObjAttributeDto>();
        allAttributes.add(everybodyUserAttribute);
        idsDto.setPresentationType("Thumbnails"); //TODO when we know more about architecture (and OaUth)
        idsDto.setAttributes(allAttributes);

        List<String> ids = new ArrayList<String>();
        ids.add(resource_id);
        idsDto.setAccessIds(ids);
        return idsDto;

    }

    /**
     * Will return a default image with propery HTTP status code if there is no access to the image.
     * If there is access to the image, return null.
     * 
     * @param httpServletResponse used for setting MIME type.
     * @param identifier an identifier for an image.
     * @return a streamed image or null
     * @throws Exception in case of exception from license module client
     */

    
    public static StreamingOutput handleNoAccessOrNoImage(String resource_id , HttpServletResponse httpServletResponse) throws ApiException, IOException {
        ACCESS_TYPE type = accessTypeForImage( resource_id);

        log.debug("Access type:" + type + " for resource_id:" +  resource_id);

        switch (type) {
        case NO_ACCESS:                        
            httpServletResponse.setStatus(403);
            httpServletResponse.setContentType(AccessApiServiceImpl.getMIME("jpg"));
            return getImageForbidden();
        case ID_NON_EXISTING:
            httpServletResponse.setStatus(404);
            httpServletResponse.setContentType(AccessApiServiceImpl.getMIME("jpg"));
            return getImageNotExist();
        case ACCESS:           
            return null; //this is the contract if no image is returned                
        default :
            throw new UnsupportedOperationException("Unknown Access type:"+type);
        }            

    }

    private static DsLicenseApi getDsLicenseApiClient() {
        if (licenseClient != null) {
            return licenseClient;
        }

        String dsLicenseUrl = ServiceConfig.getConfig().getString("config.licensemodule.url");
        log.info("license module url:"+dsLicenseUrl);
        licenseClient = new DsLicenseClient(dsLicenseUrl);
        return licenseClient;
    }

    private static StreamingOutput getImageForbidden() throws IOException {
        String img=ServiceConfig.getConfig().getString("config.images.no_access");       
        try (InputStream nonExisting= Resolver.resolveStream(img)){                                       
            StreamingOutput result= output -> output.write(nonExisting.readAllBytes()); 
            return result; 
        }
    }


    private static StreamingOutput getImageNotExist() throws IOException {
        String img=ServiceConfig.getConfig().getString("config.images.non_existing");
        try (InputStream nonExisting= Resolver.resolveStream(img)){                        
            StreamingOutput result= output -> output.write(nonExisting.readAllBytes()); 
            return result;                
        }
    }


}
