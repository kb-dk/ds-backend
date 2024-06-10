package dk.kb.image.kaltura;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.image.config.ServiceConfig;
import dk.kb.image.util.KalturaUtil;

/**
 * Unittest that will call the API search method. Search for a local refenceId to get the Kaltura internal id for the record.
 * Using Kaltura client v.19.3.3 there is no longer sporadic errors when calling the API.
 * API call from devel that can fail: http://<devel-server>:10001/ds-image/v1/kaltura/thumbnails/?fileId=9ad1d1aa-be25-4466-ada7-1e3c1e140e98&numberOfThumbnails=10
 *
 * Call 'kb init' to fetch YAML property file with server url for Kaltura
 * 
 */
@Tag("integration")
public class KalturaApiIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(KalturaApiIntegrationTest.class);
    @Test
    public void callKalturaApi() throws Exception{
        ServiceConfig.getInstance().initialize("conf/ds-image-kaltura-integration-test.yaml"); //Load Kaltura API access properties. This file is in aegis and not the project.
                
        String referenceId="bd7bbd14-4c2e-4200-9df8-721390a0e88c";
        String kalturaInternallId="0_3abv654p";
        
        int success=0;
        for (int i = 0;i<20;i++) {
            DsKalturaClient client = KalturaUtil.getClientInstance(); //This will reuse the session as it is configured in KalturaUtil.                  
            String kalturaId = client.getKulturaInternalId(referenceId);
            assertEquals(kalturaInternallId, kalturaId,"API error was reproduced after "+success+" number of calls");
            log.debug("API returned internal Kaltura id:"+kalturaId);
            success++;            
            Thread.sleep(1000L);                        
        }
        
         
        
        
        
    }
    
}
