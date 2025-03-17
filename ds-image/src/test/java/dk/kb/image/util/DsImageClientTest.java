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
package dk.kb.image.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mockStatic;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.StreamingOutput;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.MessageImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.image.config.ConfigAdjuster;
import dk.kb.image.config.ServiceConfig;
import dk.kb.util.Resolver;
import dk.kb.util.oauth2.KeycloakUtil;
import dk.kb.util.webservice.OAuthConstants;

/**
 * Simple verification of client code generation.
 */
@Tag("integration")
public class DsImageClientTest {
    private static final Logger log = LoggerFactory.getLogger(DsImageClientTest.class);

    private static DsImageClient remote = null;
    private static String dsImageDevel=null;  

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



    @Test
    public void testPlaceholderImageStreamingOutput() throws IOException {
        //This is not a DsImageClient test,  but logic will call ds-license service.          
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        if (Resolver.getPathFromClasspath("ds-image-integration-test.yaml") == null){
            fail("Internal test config is not present. 'testPlaceholderImageStreamingOutput' is therefore not run. Please update aegis and do 'kb init' to make this run.");
        }

        try (ConfigAdjuster configAdjuster = new ConfigAdjuster("ds-image-integration-test.yaml")){
            // Get test image
            ByteArrayOutputStream testImageByteArray = getImageAsByteArrayOS("nonExisting.jpg");
            // Get image through ImageAccessValidation
            StreamingOutput streamingImage = ImageAccessValidation.handleNoAccessOrNoImage("notExistingImage", response, false);

            // Convert image to ByteArrayOutputStream for comparison
            ByteArrayOutputStream streamedImageByteArray = new ByteArrayOutputStream();
            streamingImage.write(streamedImageByteArray);

            assertEquals(testImageByteArray.size(), streamedImageByteArray.size());
        }
    }



    private static ByteArrayOutputStream getImageAsByteArrayOS(String imgName) throws IOException {
        String imgPath = Resolver.getPathFromClasspath(imgName).toString();

        BufferedImage trueImage = ImageIO.read(new File(imgPath));
        ByteArrayOutputStream trueImageByteArray = new ByteArrayOutputStream();

        // Write the BufferedImage to the ByteArrayOutputStream in the specified format
        ImageIO.write(trueImage, "jpg", trueImageByteArray);
        trueImageByteArray.flush();
        return trueImageByteArray;
    }

}
