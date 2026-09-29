package dk.kb.license.integrationtest;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.facade.RightsModuleFacade;
import dk.kb.license.model.v1.*;
import dk.kb.license.util.DsLicenseUnitTestUtil;
import dk.kb.license.storage.RightsModuleStorage;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mockStatic;

public class RightsModuleIntegrationTestDsLicense extends DsLicenseUnitTestUtil {
    private static final Logger log = LoggerFactory.getLogger( RightsModuleIntegrationTestDsLicense.class);

    private static RightsModuleStorage storage;

    @BeforeAll
    static void setup() throws Exception {
        try {
            // Note: the second argument is now a devops/operations *properties* override file (see
            // ServiceConfig#initialize(String, String)), not a second YAML file merged via kb-util's old
            // multi-file layering. This test is tagged @Tag("integration") and not run by the automatic build
            // flow either way.
            ServiceConfig.initialize("conf/ds-license-behaviour.yaml","ds-license-integration-test.yaml");

            // Instantiate the RightsModuleStorage without it being able to touch records in a backing DS-storage
            storage = new RightsModuleStorage();
        } catch (IOException | SQLException e) {
            log.error("Integration yaml 'ds-license-integration-test.yaml' file most be present. Call 'kb init'");
            fail();
        }

        try {
            String keyCloakRealmUrl = ServiceConfig.getConfig().getValue("integration.devel.keycloak.realmUrl", String.class);
            String clientId = ServiceConfig.getConfig().getValue("integration.devel.keycloak.clientId", String.class);
            String clientSecret = ServiceConfig.getConfig().getValue("integration.devel.keycloak.clientSecret", String.class);
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
        int touchedRecords = RightsModuleFacade.touchRelatedStorageRecords("ds.tv:oai:io:b1a557d6-f505-445c-ae71-5e593b5fabe2", IdTypeEnumDto.DS_ID);

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
