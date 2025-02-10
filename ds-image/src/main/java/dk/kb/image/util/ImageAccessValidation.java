package dk.kb.image.util;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
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
    }

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
     * <p>
     * This implementation is very conservative and will determine if request is for a thumbnail. For thumbnail validation to succeed both width and height must be define,
     * while most other control parameters must not be defined.    
     * It is better to be conservative and later loosen up than giving too much control over thumbnail extraction.
     * <p>
     * Will be full size if most other parameters than FIF and CVT is defined. Also WID and HEI must be below a defined limit in the configuration or it will also be fullsize.
     * <p>
     * For a full description of all arguments see method:  {@link AccessApiServiceImpl#iIPImageRequest(String, Long, Long, List, Integer, Float, String, Float, String, String, String, Boolean, String, List, List, String)}IIP-parameters}
     *    
     * @return true if image request is classified as thumbnail request. Else false 
     */
    public static boolean isThumbnailIIP( String FIF, Long WID, Long HEI, List<Float> RGN, Integer QLT, Float CNT, String ROT, Float GAM, String CMP, String PFL, String CTW, Boolean INV, String COL,
            List<Integer> JTL, List<Integer> PTL, String CVT) {
        
        //FIF and CVT allowed. WID and HEI checked below
        if (  (RGN != null && !RGN.isEmpty()) || QLT != null || CNT != null ||  (ROT != null && !"0".equals(ROT)) || GAM != null || CMP != null || PFL != null || INV != null || COL != null  || (JTL != null && !JTL.isEmpty()) || (PTL != null && !PTL.isEmpty())) {
            log.debug("Fullsize for IIP request since a custom parameter was defined. Identifier={}", FIF);
            return false;
        }
        else if (WID == null || HEI == null) {
            log.debug("Fullsize for IIP request since size parameter was not defined. Identifier={}", FIF);            
            return false;
        }
        else if ( (WID != null && WID > ServiceConfig.getConfig().getInteger("thumbnail.max_width")) || (HEI != null && HEI > ServiceConfig.getConfig().getInteger("thumbnail.height.max")) ) {
            log.debug("Fullsize for IIP request since size parameter was over thumbnail size. Identifier={}, width={}, height={}", FIF, WID, HEI);
            return false;
        }
        
        return true;
    }

    
    /** Is request classified as a thumbnail or fullsize for IIIF requests.
     * <p>
     * This implementation is very conservative and will determine thumbnail also if most non size-parameters are defined.
     * It is better to be conservative and later loosen up than giving too much control over thumbnail extraction.
     * <p>
     * Will be full size if any other parameters than FIF and CVT is defined. Also, WID and HEI must be below a defined limit in the configuration, or it will also be full size.

     * For a full description of all arguments see method:  {@link AccessApiServiceImpl#iIIFImageRequest(String, String, String, String, String, String)} IIUF-parameters}
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
             log.debug("Fullsize for IIIF request since size parameter did was not of accepted form: '{}'.", size);
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
                  
        if ( width > ServiceConfig.getConfig().getInteger("thumbnail.max_width") || height > ServiceConfig.getConfig().getInteger("thumbnail.height.max")){
             log.debug("Fullsize for IIIF request since size parameter was over thumbnail size. Identifier={}, width={}, height={}",identifier, width,height);
             return false;
         }
         
        return true;
    }

    
        
    @SuppressWarnings("DataFlowIssue") // licenseClient.checkAccessForResourceIds always sets all 3 lists
    public static ACCESS_TYPE accessTypeForImage(String resourceID, boolean thumbnail) {

        // Add filter query from license module.
        DsLicenseApi licenseClient = getDsLicenseApiClient();
        CheckAccessForIdsInputDto licenseQueryDto = getCheckAccessForIdsInputDto(resourceID, thumbnail);
        CheckAccessForIdsOutputDto accessResponse;
        try {
           accessResponse = licenseClient.checkAccessForResourceIds(licenseQueryDto); // Use
        }
        catch(Exception e) {
            String message = String.format(Locale.ROOT,
                    "Error calling licensemodule with resource ID '%s'", resourceID);
            log.error(message, e);
            throw new InternalServiceException(message);
        }
        
        List<String> accessIDs = accessResponse.getAccessIds();
        List<String> nonAccessIDs = accessResponse.getNonAccessIds();
        List<String> nonExistingIDs = accessResponse.getNonExistingIds();

        // Check in other that happens most frequent.
        if (accessIDs.contains(resourceID)) {
            return ACCESS_TYPE.ACCESS;
        } else if (nonAccessIDs.contains(resourceID)) {
            log.debug("No access to resource ID '{}'", resourceID);
            return ACCESS_TYPE.NO_ACCESS;
        } else if (nonExistingIDs.contains(resourceID)) {
            log.debug("Non-existing image resource ID '{}'", resourceID);
            return ACCESS_TYPE.ID_NON_EXISTING;
        } else { // Sanity check, should not happen
            String message = String.format(Locale.ROOT, "Could not match resource ID '%s' to any access type %s",
                    resourceID, Arrays.toString(ACCESS_TYPE.values()));
            log.warn(message);
            throw new InternalServiceException(message);
        }
    }

    private static CheckAccessForIdsInputDto getCheckAccessForIdsInputDto(String resource_id, boolean thumbnail) {
        CheckAccessForIdsInputDto idsDto = new CheckAccessForIdsInputDto();

        String presentationType  = thumbnail ?   "Thumbnails" :  "Fullsize";                             
        idsDto.setPresentationType(presentationType);                  
                
        // TODO these attributes must come from Keycloak
        UserObjAttributeDto everybodyUserAttribute = new UserObjAttributeDto();
        everybodyUserAttribute.setAttribute("everybody");
        ArrayList<String> values = new ArrayList<>();
        values.add("yes");
        everybodyUserAttribute.setValues(values);

        List<UserObjAttributeDto> allAttributes = new ArrayList<>();
        allAttributes.add(everybodyUserAttribute);        
        idsDto.setAttributes(allAttributes);

        List<String> ids = new ArrayList<>();
        ids.add(resource_id);
        idsDto.setAccessIds(ids);
        return idsDto;

    }

    /**
     * Return a default image with property HTTP status code if there is no access to the image.
     * If there is access to the image, return null.
     *
     * @param resourceID an identifier for an image.
     * @param httpServletResponse used for setting MIME type.
     * @param thumbnail if the request has been determined to be a thumbnail request.
     * @return a streamed image or null.
     * @throws IOException in case of exception from license module client
     */
    public static StreamingOutput handleNoAccessOrNoImage(
            String resourceID, HttpServletResponse httpServletResponse, boolean  thumbnail) throws IOException {
        ACCESS_TYPE type = accessTypeForImage( resourceID, thumbnail);

        log.debug("Access type {} for resource ID '{}'", type, resourceID);

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
            throw new UnsupportedOperationException("Unknown Access type '"+type + "'");
        }            

    }

    private static DsLicenseApi getDsLicenseApiClient() {
        if (licenseClient == null) {
            licenseClient = new DsLicenseClient(ServiceConfig.getConfig());
        }

        return licenseClient;
    }

    private static StreamingOutput getImageForbidden() throws IOException {
        String noAccessImageName = ServiceConfig.getConfig().getString("images.noAccess");
        return writeImgToStreamingOutput(noAccessImageName);
    }


    private static StreamingOutput getImageNotExist() throws IOException {
        String nonExistingImageName = ServiceConfig.getConfig().getString("images.nonExisting");
        return writeImgToStreamingOutput(nonExistingImageName);
    }

    @NotNull
    private static StreamingOutput writeImgToStreamingOutput(String imgName) throws IOException {
        String imgPath = Resolver.getPathFromClasspath(imgName).toString();
        BufferedImage image = ImageIO.read(new File(imgPath));
        return output -> ImageIO.write(image, "jpg", output);
    }
}
