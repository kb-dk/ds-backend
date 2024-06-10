package dk.kb.image.kaltura;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kaltura.client.APIOkRequestsExecutor;
import com.kaltura.client.Configuration;
import com.kaltura.client.enums.SessionType;
import com.kaltura.client.services.MediaService;
import com.kaltura.client.services.MediaService.ListMediaBuilder;
import com.kaltura.client.types.FilterPager;
import com.kaltura.client.types.ListResponse;
import com.kaltura.client.types.MediaEntryFilter;
import com.kaltura.client.types.MediaEntry;
import com.kaltura.client.Client;
import com.kaltura.client.utils.request.RequestElement;
import com.kaltura.client.utils.response.*;
import com.kaltura.client.utils.response.base.Response;

import dk.kb.util.webservice.exception.InternalServiceException;


/**
 * Kaltura client that can: 
 * 1) API lookup and map external ID to internal ID 
 */
public class DsKalturaClient {

    private com.kaltura.client.Client client = null;
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
            //KalturaConfiguration config = new KalturaConfiguration();
            Configuration config = new Configuration();
            config.setEndpoint(kalturaUrl);
             Client client = new Client(config);                          
             String ks = client.generateSession(adminSecret, userId, SessionType.ADMIN, partnerId);
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
    @SuppressWarnings("unchecked")
    public String getKulturaInternalId(String referenceId) throws IOException{
                
        MediaEntryFilter filter = new MediaEntryFilter();
        filter.setReferenceIdEqual(referenceId);
        //filter.idEqual("0_g9ys622b"); //Example to search for id
                
        FilterPager pager = new FilterPager();
        pager.setPageIndex(10);
        
        ListMediaBuilder request =  MediaService.list(filter);               
                                
        //Getting this line correct was very hard. Little documentation and has to know which object to cast to.                
        //For some documentation about the "Kaltura search" api see: https://developer.kaltura.com/api-docs/service/media/action/list        
        Response <ListResponse<MediaEntry>> response = (Response <ListResponse<MediaEntry>>) APIOkRequestsExecutor.getExecutor().execute(request.build(client));
       
        //This is not normal situation. Normally Kaltura will return empty list: ({"objects":[],"totalCount":0,"objectType":"KalturaMediaListResponse"})
        // When this happens something is wrong in kaltura and we dont know if there is results or not
        if (response.results == null) {
           log.error("Unexpected NULL response from Kaltura for referenceId:"+referenceId);
            throw new InternalServiceException("Unexpected null response from Kaltura for referenceId:"+referenceId);            
        }
        List<MediaEntry> mediaEntries = response.results.getObjects();           
        
        int numberResults = mediaEntries.size();
     
        if ( numberResults == 0) {
            log.warn("No entry found at Kaltura for referenceId:"+referenceId);// warn since method it should not happen if given a valid referenceId 
            return null;
        }
        else if (numberResults >1) { //Sanity, has not happened yet.
            log.error("More that one entry was found at Kaltura for referenceId:"+referenceId); // if this happens there is a logic error with uploading records
            throw new IOException("More than 1 entry found at Kaltura for referenceId:"+referenceId);
        }
         
        return response.results.getObjects().get(0).getId();
    
    }
    
}




