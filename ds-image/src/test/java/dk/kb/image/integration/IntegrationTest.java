package dk.kb.image.integration;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.MessageImpl;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.image.config.ServiceConfig;
import dk.kb.image.util.DsImageClient;
import dk.kb.util.oauth2.KeycloakUtil;
import dk.kb.util.webservice.OAuthConstants;


/**
 * Abstract IntegrationTest class that will add Oauth2 token to service calls.
 * Configuration for the external server urls are defined in the aegis file: 'ds-image-integration-test.yaml'  
 */
public abstract class IntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(IntegrationTest.class);

    static DsImageClient remote = null;
    static String dsImageDevel=null;  

    @BeforeAll
    static void setUp() throws Exception{
        try {
            ServiceConfig.getInstance().initialize("ds-image-integration-test.yaml");            
            dsImageDevel= ServiceConfig.getConfig().getString("image.url");
            remote = new DsImageClient(dsImageDevel);
        } catch (IOException e) { 
            e.printStackTrace();
            log.error("Integration yaml 'ds-image-integration-test.yaml' file most be present. Call 'kb init'"); 
            fail();
        }

        try {            
            String keyCloakRealmUrl= ServiceConfig.getConfig().getString("integration.devel.keycloak.realmUrl");            
            String clientId=ServiceConfig.getConfig().getString("integration.devel.keycloak.clientId");
            String clientSecret=ServiceConfig.getConfig().getString("integration.devel.keycloak.clientSecret");                
            String token=KeycloakUtil.getKeycloakAccessToken(keyCloakRealmUrl, clientId, clientSecret);           
            log.info("Retrieved keycloak access token:"+token);            

            if (JAXRSUtils.getCurrentMessage() == null) { //only mock if not already done by another unittest           
               //Mock that we have a JaxRS session with an Oauth token as seen from within a service call.
               MessageImpl message = new MessageImpl();                            
               message.put(OAuthConstants.ACCESS_TOKEN_STRING,token);            
               MockedStatic<JAXRSUtils> mocked = mockStatic(JAXRSUtils.class);           
               mocked.when(JAXRSUtils::getCurrentMessage).thenReturn(message);
            }

        }
        catch(Exception e) {
            log.warn("Could not retrieve keycloak access token. Service will be called without Bearer access token");            
            e.printStackTrace();
        }                        
    }
    
}
