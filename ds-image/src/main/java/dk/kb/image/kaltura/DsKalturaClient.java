package dk.kb.image.kaltura;

import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kaltura.client.KalturaApiException;
import com.kaltura.client.KalturaClient;
import com.kaltura.client.KalturaConfiguration;
import com.kaltura.client.enums.KalturaSessionType;
import com.kaltura.client.types.KalturaFilterPager;
import com.kaltura.client.types.KalturaMediaEntry;
import com.kaltura.client.types.KalturaMediaEntryFilter;
import com.kaltura.client.types.KalturaMediaListResponse;


/**
 * Kaltura client that can: 
 * 1) API lookup and map external ID to internal ID 
 */
public class DsKalturaClient {

    private KalturaClient client = null;
    private static final Logger log = LoggerFactory.getLogger(DsKalturaClient.class);

    /**
     * Instantiate a session to Kaltura that can be used to upload files. The session can be be reused for multiple uploads etc.
     * 
     * @param kalturaUrl The Kaltura API url. Using the baseUrl will automatic append the API service part to the URL. 
     * @param userId The userId that must be defined in the kaltura, userId is email xxx@kb.dk in our kaltura
     * @param partnerId The partner id for kaltura. Kind of a collectionId. 
     * @param adminSecret The admin secret that must not be shared that gives access to API
     * @throws IOException  If session could not be created at Kaltura
     */
    public DsKalturaClient(String kalturaUrl, String userId, int partnerId, String adminSecret) throws IOException {
        try {
            KalturaConfiguration config = new KalturaConfiguration();
            config.setEndpoint(kalturaUrl);
            KalturaClient client = new KalturaClient(config);
            String ks = client.generateSession(adminSecret, userId, KalturaSessionType.ADMIN, partnerId);
            client.setKs(ks);
            this.client=client;
        }
        catch (Exception e) {
            log.warn("Connecting to Kaltura failed. KalturaUrl={},error={}",kalturaUrl,e.getMessage());
            throw new IOException (e);
        }
    }
    
    
    /**
     * Search Kaltura for a referenceId. The referenceId was given to Kaltura when uploading the record.<br>
     * We use filenames (file_id) as refereceIds. Example: b16bc5cb-1ea9-48d4-8e3c-2a94abae501b <br>
     * <br> 
     * The Kaltura response contains a lot more information that is required, so it is not a light weight call against Kaltura.
     *  
     * @param referenceId External reference ID given when uploading the entry to Kaltura.
     * @return The Kaltura id (internal id). Return null if the refId is not found.
     * @throws IOException if Kaltura called failed, or more than 1 entry was found with the referenceId.
     */
    public String getKulturaInternalId(String referenceId) throws IOException{

        KalturaFilterPager pager = new KalturaFilterPager();
        pager.pageSize = 10;
                
        KalturaMediaEntryFilter filter = new KalturaMediaEntryFilter();                
        //filter.searchTextMatchAnd="\""+referenceId+"\""; // Can also find with freetext search (quotes), but this can give false matches.
        filter.referenceIdEqual=referenceId;
        filter.
        KalturaMediaListResponse list=null;
        try {
          list = client.getMediaService().list(filter,pager);               
        }
        catch(KalturaApiException e) {            
           throw new IOException("Error searching kaltura for referenceId:"+referenceId);
        }
        
        if (list.totalCount == 0) {
            log.warn("No entry found at Kaltura for referenceId:"+referenceId);// warn since method it should not happen if given a valid referenceId 
            return null;
        }
        if (list.totalCount >1) { //Sanity, has not happened yet.
            log.error("More that one entry was found at Kaltura for referenceId:"+referenceId); // if this happens there is a logic error with uploading records
            throw new IOException("More than 1 entry found at Kaltura for referenceId:"+referenceId);
        }
        
        ArrayList<KalturaMediaEntry> objects = list.objects;
        KalturaMediaEntry entry = objects.get(0); //There is all meta information here if needed.
        return entry.id;        
    }
    
}




