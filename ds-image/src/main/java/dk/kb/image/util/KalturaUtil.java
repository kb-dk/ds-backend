package dk.kb.image.util;

import java.util.ArrayList;
import java.util.List;

import dk.kb.image.config.ServiceConfig;
import dk.kb.image.kaltura.DsKalturaClient;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

public class KalturaUtil {
    
    public static void main(String[] args) throws Exception {
       String fileId="09139243-9a7a-422f-b597-7aa59b06ab7d";
     List<String> links=   getThumbnails(fileId, 10,200, null);
     System.out.println(links);
    }
   
    
    /*
     * 
     * Example url: //https://api.kaltura.nordu.net/p/380/thumbnail/entry_id/0_jym3ia3g/width/200/vid_slices/10/
     * 
     */
    public static List<String> getThumbnails(String fileId,Integer numberOfSlices, Integer width, Integer height) throws Exception{        
        List<String> imageLinks= new ArrayList<String>();
        //Create the client
        String kalturaUrl= ServiceConfig.getConfig().getString("kaltura.url");
        String adminSecret = ServiceConfig.getConfig().getString("kaltura.adminSecret"); 
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
