package dk.kb.image.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import dk.kb.util.Pair;
import dk.kb.util.Resolver;
import dk.kb.util.webservice.exception.InternalServiceException;

public class ImageAccessValidation {
    private static final Logger log = LoggerFactory.getLogger(ImageAccessValidation.class);
    private static DsLicenseApi licenseClient;

    public static enum ACCESS_TYPE {
        ACCESS, NO_ACCESS, ID_NON_EXISTING
    };

	/*
	 * This pattern validates the IIIF size parameter, but has the additional restriction that both height and size must be defined.    
	 * 
	 * ^       : start of string
	 * \\^?    : 0 or 1 of character '^'  (needs to be escaped)
	 * !?      : 0 or 1 of character '!'
	 * ([0-9]+): group #1 of digits, at least 1 digit
	 * ,       : (must be be present)
	 * ([0-9]+): group #2 of digits, at least 1 digit
	 * $       : end of string 
	 * 
	 */				
	public static final Pattern IIIF_SIZE_PATTERN = Pattern.compile("^\\^?!?([0-9]+),([0-9]+)$");
	     			
    
    /**     
     * Is request classified as a thumbnail or fullsize for IIP requests.
     * 
     * This implementation is very conservative and will determine if request is for a thumbnail. For thumbnail validation to succeed both width and height must be define,
     * while most other control parameters must not be defined.    
     * It is better to be conservative and later loosen up than giving too much control over thumbnail extraction.
     * 
     * Will be full size if most other parameters than FIF and CVT is defined. Also WID and HEI must be below a defined limit in the configuration or it will also be fullsize.
     * 
     * For a full description of all arguments see method:  {@link AccessApiServiceImpl#iIPImageRequest() IIP-parameters}      
     *    
     * @return true if image request is classified as thumbnail request. Else false 
     */
    public static boolean isThumbnailIIP( String FIF, Long WID, Long HEI, List<Float> RGN, Integer QLT, Float CNT, String ROT, Float GAM, String CMP, String PFL, String CTW, Boolean INV, String COL,
            List<Integer> JTL, List<Integer> PTL, String CVT) {
        
        //FIF and CVT allowed. WID and HEI checked below
        if (  (RGN != null && RGN.size() !=0) || QLT != null || CNT != null ||  (ROT != null && !"0".equals(ROT)) || GAM != null || CMP != null || PFL != null || INV != null || COL != null  || (JTL != null && JTL.size() >0 ) || (PTL != null && PTL.size() >0 )) {            
            log.debug("Fullsize for IIP request since a custom parameter was defined. Identifier={}", FIF);
            return false;
        }
        else if (WID == null || HEI == null) {
            log.debug("Fullsize for IIP request since size parameter was not defined. Identifier={}", FIF);            
            return false;
        }
        else if ( (WID != null && WID > ServiceConfig.getConfig().getInteger("thumbnail.max_width")) || (HEI != null && HEI > ServiceConfig.getConfig().getInteger("thumbnail.max_height")) ) {
            log.debug("Fullsize for IIP request since size parameter was over thumbnail size. Identifier={}, width={}, height={}", FIF, WID, HEI);
            return false;
        }
        
        return true;
    }

    
    /** Is request classified as a thumbnail or fullsize for IIIF requests.
     * 
     * This implementation is very conservative and will determine thumbnail also if most non size-parameters are defined.
     * It is better to be conservative and later loosen up than giving too much control over thumbnail extraction.
     * 
     * Will be full size if any other parameters than FIF and CVT is defined. Also WID and HEI must be below a defined limit in the configuration or it will also be fullsize.

     * For a full description of all arguments see method:  {@link AccessApiServiceImpl#ifffImageRequest() IIUF-parameters} 
     * @return true if image request is classified as thumbnail request. Else false.    
     */
    public static boolean isThumbnailIIIF(String identifier, String region, String size, String rotation, String quality, String format) {
         if (!"full".equals(region)  ||  !rotation.equals("0") || !quality.equals("default")) {                     
             log.debug("Fullsize for IIIF request since a custom parameter was defined. Identifier={}", identifier);             
             return false;                
         }
         else if (size == null) {
             log.debug("Fullsize for IIIF request since size parameter was not defined Identifier={}", identifier);
             return false;
         }                           
         
            //Only allow size as "w,h" parameter. Etc. "100,100". Value is default 'max' when requesting the full image.
        
         Matcher matcher = IIIF_SIZE_PATTERN.matcher(size);
         if (!matcher.find()) {
        	 return false;
         }
                                                      
         int width;
         int height;         
         try {
             width=Integer.parseInt(matcher.group(1));
             height=Integer.parseInt(matcher.group(2));
         }
         catch(Exception e) {
             log.debug("Fullsize for IIIF request since size parameter could not be parsed as (width,height) integers value={} , identifier={}",size,identifier);
             return false;
         }
                  
        if ( width > ServiceConfig.getConfig().getInteger("thumbnail.max_width") || height > ServiceConfig.getConfig().getInteger("thumbnail.max_height")){         
             log.debug("Fullsize for IIIF request since size parameter was over thumbnail size. Identifier={}, width={}, height={}",identifier, width,height);
             return false;
         }
         
        return true;
    }

    
        
    public static ACCESS_TYPE accessTypeForImage(String resource_id, boolean thumbnail) throws ApiException {

        // Add filter query from license module.
        DsLicenseApi licenseClient = getDsLicenseApiClient();
        CheckAccessForIdsInputDto licenseQueryDto = getCheckAccessForIdsInputDto(resource_id, thumbnail);
        CheckAccessForIdsOutputDto checkAccessForIds;
        try {
           checkAccessForIds = licenseClient.checkAccessForResourceIds(licenseQueryDto); // Use
        }
        catch(Exception e) {
            log.error("Error calling licensemodule with resource ID '" + resource_id + "'",e);
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

    private static CheckAccessForIdsInputDto getCheckAccessForIdsInputDto(String resource_id, boolean thumbnail) {
                
        CheckAccessForIdsInputDto idsDto = new CheckAccessForIdsInputDto();


        String presentationType  = thumbnail ?   "Thumbnails" :  "Fullsize";                             
        idsDto.setPresentationType(presentationType);                  
                
        // TODO these attributes must come from Keycloak
        UserObjAttributeDto everybodyUserAttribute = new UserObjAttributeDto();
        everybodyUserAttribute.setAttribute("everybody");
        ArrayList<String> values = new ArrayList<String>();
        values.add("yes");
        everybodyUserAttribute.setValues(values);

        List<UserObjAttributeDto> allAttributes = new ArrayList<UserObjAttributeDto>();
        allAttributes.add(everybodyUserAttribute);        
        idsDto.setAttributes(allAttributes);

        List<String> ids = new ArrayList<String>();
        ids.add(resource_id);
        idsDto.setAccessIds(ids);
        return idsDto;

    }

    /**
     * Will return a default image with property HTTP status code if there is no access to the image.
     * If there is access to the image, return null.
     * 
     * @param httpServletResponse used for setting MIME type.
     * @param identifier an identifier for an image.
     * @return a streamed image or null
     * @throws Exception in case of exception from license module client
     */

    
    public static StreamingOutput handleNoAccessOrNoImage(String resource_id , HttpServletResponse httpServletResponse, boolean  thumbnail) throws ApiException, IOException {
        ACCESS_TYPE type = accessTypeForImage( resource_id, thumbnail);

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

        String dsLicenseUrl = ServiceConfig.getConfig().getString("licensemodule.url");
        log.info("license module url:"+dsLicenseUrl);
        licenseClient = new DsLicenseClient(dsLicenseUrl);
        return licenseClient;
    }

    private static StreamingOutput getImageForbidden() throws IOException {
        String img=ServiceConfig.getConfig().getString("images.no_access");
        try (InputStream nonExisting= Resolver.resolveStream(img)){                                       
            StreamingOutput result= output -> output.write(nonExisting.readAllBytes()); 
            return result; 
        }
    }


    private static StreamingOutput getImageNotExist() throws IOException {
        String img=ServiceConfig.getConfig().getString("images.non_existing");
        try (InputStream nonExisting= Resolver.resolveStream(img)){                        
            StreamingOutput result= output -> output.write(nonExisting.readAllBytes()); 
            return result;                
        }
    }

    
    

    
    
}
