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
package dk.kb.present.util;

import dk.kb.present.config.ServiceConfig;
import dk.kb.present.invoker.v1.ApiException;
import dk.kb.present.model.v1.FormatDto;
import dk.kb.present.model.v1.OriginDto;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.util.Resolver;
import dk.kb.util.oauth2.KeycloakUtil;
import dk.kb.util.webservice.OAuthConstants;
import dk.kb.util.webservice.stream.ContinuationInputStream;
import dk.kb.util.webservice.stream.ContinuationStream;
import dk.kb.util.webservice.stream.ContinuationUtil;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.MessageImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

/**
 * Integration test, will not be run by automatic build flow.
 * 
 */

@Tag("integration")
public class DsPresentClientTest {
    private static final Logger log = LoggerFactory.getLogger(DsPresentClientTest.class);

    public static final String TEST_CONF = "internal-test-setup.yaml";
    private static String dsPresentDevel=null;  
    
    private static DsPresentClient remote = null;

    @BeforeAll
    static void setup() {
        try {
            ServiceConfig.initialize(TEST_CONF);                        
            dsPresentDevel= ServiceConfig.getConfig().getString("present.url");                        
            remote = new DsPresentClient(dsPresentDevel);
        } catch (IOException e) {          
            log.error("Integration yaml "+TEST_CONF+" file most be present. Call 'kb init'");            
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
    public void testRemoteRecordsRaw() throws IOException {      
        try (ContinuationInputStream<Long> recordsIS = remote.getRecordsJSON(
                "ds.tv", 0L, 3L, FormatDto.JSON_LD)) {
            String recordsStr = IOUtils.toString(recordsIS, StandardCharsets.UTF_8);
            assertTrue(recordsStr.contains("\"id\":\"ds.tv:oai"),
                    "At least 1 JSON block for a record should be returned");
            assertNotNull(recordsIS.getContinuationToken(),
                    "The continuation header '" + ContinuationUtil.HEADER_PAGING_CONTINUATION_TOKEN +
                            "' should be present");
            assertTrue(ContinuationUtil.getHasMore(recordsIS).isPresent(),
                       "The continuation header '" + ContinuationUtil.HEADER_PAGING_HAS_MORE + "' should be present");
        }
    }

    
    @Test
    public void testRemoteRecordsStream() throws IOException {
       
        try (ContinuationStream<DsRecordDto, Long> records = remote.getRecordsRawStream("ds.tv", 0L, 3L)) {
            List<DsRecordDto> recordList = records.collect(Collectors.toList());

            assertEquals(3L, recordList.size(), "The requested number of records should be received");
            assertNotNull(records.getContinuationToken(),
                    "The highest modification time should be present");
            log.debug("Stated highest modification time was " + records.getContinuationToken());
            assertEquals(recordList.get(recordList.size()-1).getmTime(),
                         records.getContinuationToken(),
                    "Received highest mTime should match stated highest mTime");
        }
    }

    @Test
    public void testRecordCount() throws IOException {       
        try (ContinuationInputStream<Long> records = remote.getRecordsJSON("ds.tv", 0L, 1L, FormatDto.JSON_LD)) {
            // Here we are implying that there are at least 1 record in the backing storage,
            // which are "Deliverable units", in other words, records that contain actual metadata and are delivered.
            assertEquals(1L, records.getRecordCount());
        }
    }

    @Test
    public void testSchemaTransformation() throws IOException {
        String schema = Resolver.resolveUTF8String("src/main/solr/dssolr/conf/schema.xml");

        String result = remote.transformSolrSchema(schema, "markdown");
        assertTrue(result.startsWith("# Schema documentation"));
    }
   
    
    @Test
    public void testGetOrigin() throws ApiException {
          OriginDto origin = remote.getOrigin("ds.radio"); //this should always exist 
          assertNotNull(origin);          
    }
    
    @Test
    public void testGetOrigins() throws ApiException {
        List<OriginDto> origins=remote.getOrigins(); // there will always be some        
        assertTrue(origins.size() >0);
          
    }
    
    @Test
    public void testGetRecord() throws ApiException {
     String id="ds.tv:oai:io:d0df0579-4886-41d3-9177-a2f71f62de19"; //tv-avisen
        String record = remote.getRecord(id, FormatDto.JSON_LD);        
        assertNotNull(record); 
          
    }
    
    
}
