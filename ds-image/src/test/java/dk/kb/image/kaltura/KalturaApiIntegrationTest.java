package dk.kb.image.kaltura;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import dk.kb.image.config.ServiceConfig;
import dk.kb.image.util.KalturaUtil;

/**
 * Unittest that will call the API api search method. Search for a local refenceId to get the Kaltura internal id for the record.
 * This API call fails often when running in a web-container (Tomcat/Jetty) and we do not know why. It fails both when
 * reusing the same session or always creating a new session.
 * But it always works from and IDE or stand alone java program.
 *
 * API call from devel that can fail: http://<devel-server>:10001/ds-image/v1/kaltura/thumbnails/?fileId=9ad1d1aa-be25-4466-ada7-1e3c1e140e98&numberOfThumbnails=10
 *
 * Call 'kb init' to fetch YAML property file with server url for Kaltura
 * 
 */
@Tag("integration")
public class KalturaApiIntegrationTest {

    
    @Test
    public void callKalturaApi() throws Exception{
        ServiceConfig.getInstance().initialize("ds-image-integration-test.yaml"); //Load Kaltura API access properties. This file is in aegis and not the project.
        
        String referenceId="9ad1d1aa-be25-4466-ada7-1e3c1e140e98";
        String kalturaInternallId="0_g9ys622b";
 
                
        int success=0;
        for (int i = 0;i<20;i++) {
            DsKalturaClient client = KalturaUtil.getClientInstance(); //This will reuse the session as it is configured in KalturaUtil.            
            
            String kalturaId = client.getKulturaInternalId(referenceId);
            assertEquals(kalturaInternallId, kalturaId,"API was reproduced after number of calls:"+success);
            success++;
            Thread.sleep(10000L);
                System.out.println(kalturaId);
            
        }
        
         
        
        
        
    }
    
}
