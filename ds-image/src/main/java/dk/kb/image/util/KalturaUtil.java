package dk.kb.image.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dk.kb.util.webservice.exception.NotFoundServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.image.config.ServiceConfig;
import dk.kb.image.model.v1.ThumbnailsDto;
import dk.kb.kaltura.client.DsKalturaClient;

public class KalturaUtil {

    private static final Logger log = LoggerFactory.getLogger(KalturaUtil.class);
    private static DsKalturaClient kalturaClientInstance;
    /**
     *
     * Example url to the sprite with all thumbnails:<br>
     * {@code https://api.kaltura.nordu.net/p/380/thumbnail/entry_id/0_jym3ia3g/width/200/vid_slices/10/}<br>
     * <br>
     * Example url to slice #5 out of 10<br>
     * {@code https://api.kaltura.nordu.net/p/380/thumbnail/entry_id/0_jym3ia3g/width/200/vid_slices/10/vid_slice/5}<br>
     * 
     * See <a href="https://developer.kaltura.com/api-docs/Engage_and_Publish/kaltura-thumbnail-api.html">Kaltura Thumbnail API</a> 
     *
     *
     * @param fileId The externalId we have for the record. 
     * @param numberOfSlices Number of thumbnails. They be divided uniform over the video.
     * @param secondsStartSeek Generated thumbnails in stream from between secondsStartSeek and secondsEndSeek
     * @param secondsEndSeek Generated thumbnails in stream from between secondsStartSeek and secondsEndSeek. secondsEndSeek must be set for seeking to be active. 
     * @param width Optional width parameter in pixels. Aspect ratio will be kept.
     * @param height Optiomal height parameter in pixels. Aspect ratio will be kept. 
     *
     * @return ThumbnailsDto. Has a default thumbnail, a sprite and list of time sliced thumbnails.
     * @throws IOException If the fileId is not found or internal server error with Kaltura.
     */
    public static ThumbnailsDto getThumbnails(String fileId,Integer numberOfSlices, Integer secondsStartSeek, Integer secondsEndSeek, Integer width, Integer height) throws ServiceException {
        ThumbnailsDto thumbnails = new ThumbnailsDto();
        String kalturaUrl= ServiceConfig.getConfig().getString("kaltura.url");
        Integer partnerId = ServiceConfig.getConfig().getInteger("kaltura.partnerId");  

        try {
            DsKalturaClient client = getKalturaClient();
            String kalturaId = client.getKulturaInternalId(fileId);

            if (kalturaId == null) {
                throw new NotFoundServiceException("FileId not found at Kaltura: '" + fileId + "'.");
            }
            log.debug("ReferenceId lookup at kaltura resolved to internalId {} -> {}",fileId,kalturaId);
            String baseUrl=kalturaUrl+"/p/"+partnerId+"/thumbnail/entry_id/"+kalturaId;
            if (width != null && width > 0) {
                baseUrl=baseUrl+"/width/"+width;
            }
            if (height != null && height > 0) {
                baseUrl=baseUrl+"/height/"+height;
            }

            String seek=""; //if seek not defined, this empty string will just be added
            if ((secondsEndSeek != null && secondsEndSeek>=0)  || (secondsStartSeek != null && secondsStartSeek>=0)) {
                seek="?start_sec="+secondsStartSeek+"&end_sec="+secondsEndSeek; 
            }
            
            thumbnails.setDefault(baseUrl); // Example: https://api.kaltura.nordu.net/p/380/thumbnail/entry_id/0_dtvciomh/width/200/

            //This is the sprite version will all thumbnails.
            //Example: https://api.kaltura.nordu.net/p/380/thumbnail/entry_id/0_dtvciomh/width/200/vid_slices/10
            baseUrl=baseUrl+"/vid_slices/"+numberOfSlices;

            List<String> timeSliceThumbnails= new ArrayList<>();
            thumbnails.setSprite(baseUrl);
            //Add the images slices
            for (int i=0;i<numberOfSlices;i++ ) {
                //example: https://api.kaltura.nordu.net/p/380/thumbnail/entry_id/0_dtvciomh/width/200/vid_slices/10/vid_slice/5
                timeSliceThumbnails.add(baseUrl +"/vid_slice/"+i+seek);  //Yes, slices start with value=0.
            }
            thumbnails.setThumbnails(timeSliceThumbnails);

            return thumbnails;
        } catch (IOException e) {
            throw new NotFoundServiceException(e);
        }
    }

    private static synchronized DsKalturaClient getKalturaClient() throws IOException {

        if (kalturaClientInstance != null) {
            return kalturaClientInstance;
        }

        String kalturaUrl= ServiceConfig.getConfig().getString("kaltura.url");
        String adminSecret = ServiceConfig.getConfig().getString("kaltura.adminSecret"); //Must not be shared or exposed. Use token,tokenId.
        Integer partnerId = ServiceConfig.getConfig().getInteger("kaltura.partnerId");  
        String userId = ServiceConfig.getConfig().getString("kaltura.userId");                               
        String token= ServiceConfig.getConfig().getString("kaltura.token");
        String tokenId= ServiceConfig.getConfig().getString("kaltura.tokenId");

        long sessionKeepAliveSeconds=3600L; //1 hour
        log.info("Creating kaltura client for partnerID: '{}'.", partnerId);
        DsKalturaClient kalturaClient = new DsKalturaClient(kalturaUrl,userId,partnerId,token,tokenId,adminSecret,sessionKeepAliveSeconds);
        kalturaClientInstance=kalturaClient;
        return kalturaClient;    
    }

}
