package dk.kb.license.integrationtest;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.facade.RightsModuleFacade;
import dk.kb.license.model.v1.*;
import dk.kb.license.storage.BaseModuleStorage;
import dk.kb.license.storage.UnitTestUtil;
import dk.kb.license.storage.RightsModuleStorage;
import dk.kb.license.util.H2DbUtil;
import dk.kb.util.oauth2.KeycloakUtil;
import dk.kb.util.webservice.OAuthConstants;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.MessageImpl;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mockStatic;

public class RightsModuleIntegrationTest extends UnitTestUtil {
    private static final Logger log = LoggerFactory.getLogger( RightsModuleIntegrationTest.class);

    private static RightsModuleStorage storage;

    @BeforeAll
    static void setup() {
        try {
            ServiceConfig.initialize("conf/ds-license-behaviour.yaml","ds-license-integration-test.yaml");

            BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
            H2DbUtil.createEmptyH2DBFromDDL(URL,DRIVER,USERNAME,PASSWORD, List.of("ddl/rightsmodule_create_h2_unittest.ddl"));

            // Instantiate the RightsModuleStorage without it being able to touch records in a backing DS-storage
            storage = new RightsModuleStorage();
        } catch (IOException | SQLException e) {
            log.error("Integration yaml 'ds-license-integration-test.yaml' file most be present. Call 'kb init'");
            fail();
        }

        try {
            String keyCloakRealmUrl = ServiceConfig.getConfig().getString("integration.devel.keycloak.realmUrl");
            String clientId = ServiceConfig.getConfig().getString("integration.devel.keycloak.clientId");
            String clientSecret = ServiceConfig.getConfig().getString("integration.devel.keycloak.clientSecret");
            String token = KeycloakUtil.getKeycloakAccessToken(keyCloakRealmUrl, clientId, clientSecret);
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
    @Tag("integration")
    public void testQueryLookupForProductionId() throws SolrServerException, IOException {
        int touchedRecords = RightsModuleFacade.touchRelatedStorageRecords("9213145700", IdTypeEnumDto.DR_PRODUCTION_ID);

        // Currently there are five records for this productionId in solr
        assertTrue(touchedRecords > 4);
    }

    @Test
    @Tag("integration")
    public void testQueryLookupForId() throws SolrServerException, IOException {
        int touchedRecords = RightsModuleFacade.touchRelatedStorageRecords("ds.tv:oai:io:5a888d7d-3c0d-4375-9e67-343d88d1dbd9", IdTypeEnumDto.DS_ID);

        assertEquals(1, touchedRecords);
    }

    @Test
    @Tag("integration")
    public void testQueryLookupForTitle() throws SolrServerException, IOException {
        int touchedRecords = RightsModuleFacade.touchRelatedStorageRecords("Øen", IdTypeEnumDto.STRICT_TITLE);
        log.info("touched '{}' records", touchedRecords);


        assertTrue(touchedRecords > 20);
    }

    @Test
    @Tag("slow")
    @Tag("integration")
    public void testQueryLookupForProductionCode() throws SolrServerException, IOException {
        int touchedRecords = RightsModuleFacade.touchRelatedStorageRecords("3200", IdTypeEnumDto.OWNPRODUCTION_CODE);

        assertTrue(touchedRecords > 2500);
    }

    @Test
    @Tag("integration")
    public void testTouchNonExistingRecordInStorage() {
        assertThrows(InvalidArgumentServiceException.class,  () -> {
            RightsModuleFacade.touchRelatedStorageRecords("some-non-existing-ds-id", IdTypeEnumDto.DS_ID);
        });
    }
}
