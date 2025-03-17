/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.discover.util;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.MessageImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.discover.config.ServiceConfig;
import dk.kb.util.oauth2.KeycloakUtil;
import dk.kb.util.webservice.OAuthConstants;

/**
 * Simple verification of client code generation.
 */

@Tag("integration")
public class DsDiscoverClientTest {
    private static final Logger log = LoggerFactory.getLogger(DsDiscoverClientTest.class);
   
    private static DsDiscoverClient remote = null;
    private static String dsDiscoverDevel=null;  
    
    @BeforeAll
    static void setUp() throws Exception{
        try {
            ServiceConfig.initialize("ds-discover-integration-test.yaml"); 
            dsDiscoverDevel= ServiceConfig.getConfig().getString("discover.url");
            remote = new DsDiscoverClient(dsDiscoverDevel);
        } catch (IOException e) { 
            e.printStackTrace();
            log.error("Integration yaml 'ds-discover-integration-test.yaml' file most be present. Call 'kb init'"); 
            fail();
        }
        
        try {            
            String keyCloakRealmUrl= ServiceConfig.getConfig().getString("integration.devel.keycloak.realmUrl");            
            String clientId=ServiceConfig.getConfig().getString("integration.devel.keycloak.clientId");
            String clientSecret=ServiceConfig.getConfig().getString("integration.devel.keycloak.clientSecret");                
            String token=KeycloakUtil.getKeycloakAccessToken(keyCloakRealmUrl, clientId, clientSecret);           
            log.info("Retrieved keycloak access token:"+token);            
            
            //Mock that we have a JaxRS session with an Oauth token as seen from within a service call.
            MessageImpl message = new MessageImpl();                            
            message.put(OAuthConstants.ACCESS_TOKEN_STRING,token);            
            MockedStatic<JAXRSUtils> mocked = mockStatic(JAXRSUtils.class);           
            mocked.when(JAXRSUtils::getCurrentMessage).thenReturn(message);
                                                                         
        }
        catch(Exception e) {
            log.warn("Could not retrieve keycloak access token. Service will be called without Bearer access token");            
            e.printStackTrace();
        }                        
    }

    @Test
    public void test() throws IOException {
    //Must be one unit test to test the setup method is working 
    }
         
         
    
}
