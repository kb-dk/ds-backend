package dk.kb.image.util;

import java.util.ArrayList;
import java.util.List;

import dk.kb.image.config.ServiceConfig;
import dk.kb.image.kaltura.DsKalturaClient;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

public class KalturaUtil {
          
    /**
     * 
     * Example url to the sprite with all thumbnails:
     * https://api.kaltura.nordu.net/p/380/thumbnail/entry_id/0_jym3ia3g/width/200/vid_slices/10/
     * 
     * Example url to slice #5 out of 10
     * https://api.kaltura.nordu.net/p/380/thumbnail/entry_id/0_jym3ia3g/width/200/vid_slices/10/vid_slice/5
     * 
     * @param fileId The externalId we have for the record. 
     * @param numberOfSlices Number of thumbnails. They be divided uniform over the video.
     * @param width Optional width parameter in pixels. Aspect ratio will be kept.
     * @param height Optiomal height parameter in pixels. Aspect ratio will be kept. 
     * 
     * @return List of links. The first link in the list is the sprite containing all thumbnails.
     * @throws Exception If the fileId is not found or internal server error with Kaltura
     */
    public static List<String> getThumbnails(String fileId,Integer numberOfSlices, Integer width, Integer height) throws Exception{        
        List<String> imageLinks= new ArrayList<String>();
        //Create the client
        String kalturaUrl= ServiceConfig.getConfig().getString("kaltura.url");
        String adminSecret = ServiceConfig.getConfig().getString("kaltura.adminSecret"); //Must not be shared or exposed.
        Integer partnerId = ServiceConfig.getConfig().getInteger("kaltura.partnerId");  
        String userId = ServiceConfig.getConfig().getString("kaltura.userId");
        DsKalturaClient client = new DsKalturaClient(kalturaUrl,userId,partnerId,adminSecret);
        String kalturaId = client.getKulturaInternalId(fileId);                     
        
        if (kalturaId == null) {
            throw new InvalidArgumentServiceException("FileId not found at Kaltura:"+fileId);
        }
                
        String baseUrl=kalturaUrl+"/p/"+partnerId+"/thumbnail/entry_id/"+kalturaId;
        //String baseUrl="https://api.kaltura.nordu.net/p/380/thumbnail/entry_id/"+kalturaId;
        if (width != null && width.intValue() > 0) {
            baseUrl=baseUrl+"/width/"+width;
        }
        if (height != null && height.intValue() > 0) {
            baseUrl=baseUrl+"/height/"+height;
        }
        baseUrl=baseUrl+"/vid_slices/"+numberOfSlices; //This is the sprite version will all thumbnails

        imageLinks.add(baseUrl); 
        //Add the images slices 
        for (int i=0;i<numberOfSlices;i++ ) {
           imageLinks.add(baseUrl +"/vid_slice/"+i);  //Yes, slices start with value=0
        }
        
        return imageLinks;                        
    }

}
