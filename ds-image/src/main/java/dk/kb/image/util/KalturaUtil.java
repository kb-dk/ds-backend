package dk.kb.image.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.image.config.ServiceConfig;
import dk.kb.image.kaltura.DsKalturaClient;
import dk.kb.image.model.v1.ThumbnailsDto;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

public class KalturaUtil {

    private static final Logger log = LoggerFactory.getLogger(KalturaUtil.class);
    private static DsKalturaClient clientInstance = null;
    private static long lastSessionStart=0;
    private static long reloadIntervalInMillis=1L*24*60*60*1000; // 1 day
          
    /**
     * 
     * Example url to the sprite with all thumbnails:<br>
     * https://api.kaltura.nordu.net/p/380/thumbnail/entry_id/0_jym3ia3g/width/200/vid_slices/10/<br>
     * <br>
     * Example url to slice #5 out of 10<br>
     * https://api.kaltura.nordu.net/p/380/thumbnail/entry_id/0_jym3ia3g/width/200/vid_slices/10/vid_slice/5<br>
     * 
     * See <a href="https://developer.kaltura.com/api-docs/Engage_and_Publish/kaltura-thumbnail-api.html">Kaltura Thumbnail API</a> 
     * 
     * 
     * @param fileId The externalId we have for the record. 
     * @param numberOfSlices Number of thumbnails. They be divided uniform over the video.
     * @param width Optional width parameter in pixels. Aspect ratio will be kept.
     * @param height Optiomal height parameter in pixels. Aspect ratio will be kept. 
     * 
     * @return ThumbnailsDto. Has a default thumbnail, a sprite and list of time sliced thumbnails.
     * @throws IOException If the fileId is not found or internal server error with Kaltura.
     */
    public static ThumbnailsDto getThumbnails(String fileId,Integer numberOfSlices, Integer width, Integer height) throws IOException{        
        ThumbnailsDto thumbnails = new ThumbnailsDto();
        String kalturaUrl= ServiceConfig.getConfig().getString("kaltura.url");
        Integer partnerId = ServiceConfig.getConfig().getInteger("kaltura.partnerId");  
        
        DsKalturaClient client = getClientInstance();
        String kalturaId = client.getKulturaInternalId(fileId);                     
        
        if (kalturaId == null) {
            throw new InvalidArgumentServiceException("FileId not found at Kaltura:"+fileId);
        }             
        log.debug("ReferenceId lookup at kaltura resolved to internalId {} -> {}",fileId,kalturaId);
        String baseUrl=kalturaUrl+"/p/"+partnerId+"/thumbnail/entry_id/"+kalturaId;
      
        if (width != null && width.intValue() > 0) {
            baseUrl=baseUrl+"/width/"+width;
        }
        if (height != null && height.intValue() > 0) {
            baseUrl=baseUrl+"/height/"+height;
        }

        thumbnails.setDefault(baseUrl); // Example: https://api.kaltura.nordu.net/p/380/thumbnail/entry_id/0_dtvciomh/width/200/

        //This is the sprite version will all thumbnails.
        //Example: https://api.kaltura.nordu.net/p/380/thumbnail/entry_id/0_dtvciomh/width/200/vid_slices/10
        baseUrl=baseUrl+"/vid_slices/"+numberOfSlices; 

        List<String> timeSliceThumbnails= new ArrayList<String>();
        thumbnails.setSprite(baseUrl); 
        //Add the images slices 
        for (int i=0;i<numberOfSlices;i++ ) {
            //example: https://api.kaltura.nordu.net/p/380/thumbnail/entry_id/0_dtvciomh/width/200/vid_slices/10/vid_slice/5
            timeSliceThumbnails.add(baseUrl +"/vid_slice/"+i);  //Yes, slices start with value=0.           
        }
        thumbnails.setThumbnails(timeSliceThumbnails);

        return thumbnails;          
    }

    
    public  synchronized static DsKalturaClient getClientInstance() throws IOException{
        if (System.currentTimeMillis()-lastSessionStart >= reloadIntervalInMillis) {            
            //Create the client
            String kalturaUrl= ServiceConfig.getConfig().getString("kaltura.url");
            String adminSecret = ServiceConfig.getConfig().getString("kaltura.adminSecret"); //Must not be shared or exposed.
            Integer partnerId = ServiceConfig.getConfig().getInteger("kaltura.partnerId");  
            String userId = ServiceConfig.getConfig().getString("kaltura.userId");
            DsKalturaClient client = new DsKalturaClient(kalturaUrl,userId,partnerId,adminSecret);             
            clientInstance=client;
            log.info("Started a new Kaltura session");
            lastSessionStart=System.currentTimeMillis(); //Reset timer           
            return clientInstance;
        }
        return clientInstance; //Reuse existing connection.
        
    }
    
}
