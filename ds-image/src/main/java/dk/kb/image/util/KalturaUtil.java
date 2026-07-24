package dk.kb.image.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.image.config.ServiceConfig;
import dk.kb.image.model.v1.ThumbnailsDto;

public class KalturaUtil {

    private static final Logger log = LoggerFactory.getLogger(KalturaUtil.class);

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
     * @param kalturaId The internal Kaltura id given by Kaltura on creation 
     * @param numberOfSlices Number of thumbnails. They be divided uniform over the video.
     * @param secondsStartSeek Generated thumbnails in stream from between secondsStartSeek and secondsEndSeek
     * @param secondsEndSeek Generated thumbnails in stream from between secondsStartSeek and secondsEndSeek. secondsEndSeek must be set for seeking to be active. 
     * @param width Optional width parameter in pixels. Aspect ratio will be kept.
     * @param height Optiomal height parameter in pixels. Aspect ratio will be kept. 
     *
     * @return ThumbnailsDto. Has a default thumbnail, a sprite and list of time sliced thumbnails.
     */
    public static ThumbnailsDto generateThumbnails(String  kalturaId,Integer numberOfSlices, Integer secondsStartSeek, Integer secondsEndSeek, Integer width, Integer height) {
        ThumbnailsDto thumbnails = new ThumbnailsDto();
        String kalturaUrl= ServiceConfig.getConfig().getString("kaltura.url");
        Integer partnerId = ServiceConfig.getConfig().getInteger("kaltura.partnerId");  

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

    }

}
