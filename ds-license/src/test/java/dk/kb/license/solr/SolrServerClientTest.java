package dk.kb.license.solr;

import dk.kb.license.config.ServiceConfig;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SolrServerClientTest {
    // Arrange
    final String drProductionId = "9213163000";
    final String dsId = "ds.tv:oai:io:d5ec7b20-c1f2-491e-a2cb-f143683a40f8";
    final String title = "P2 Radioavis";
    final String startTime = "Thu Apr 05 08:00:00 CEST 2018";
    final String endTime = "Thu Apr 05 08:06:00 CEST 2018";

    final String queryDsId = "id:\"" + dsId + "\"";
    final String fieldListDsId = "dr_production_id, id, title, startTime, endTime";

    @Test
    public void createSolrQuery_WhenGivenQueryAndFieldList_ReturnSolrQuery() {
        // To be able to mock List<SolrServerClient> servers we use the getServers getter method
        try (MockedStatic<ServiceConfig> mockedServiceConfig = mockStatic(ServiceConfig.class)) {
            mockedServiceConfig.when(ServiceConfig::getSolrServers).thenReturn(List.of());

            SolrServerClient solrServerClient = new SolrServerClient();

            // Act
            SolrQuery solrQuery = solrServerClient.createSolrQuery(queryDsId, fieldListDsId);

            // Assert
            assertNotNull(solrQuery);
            assertEquals(queryDsId, solrQuery.getQuery());
            assertEquals(fieldListDsId, solrQuery.getFields());
        }
    }

    @Test
    public void callSolr_WhenListOfServersIsEmpty_ThrowInternalServiceException() {
        // Arrange
        String expectedMessage = "List of SolrServerClient is never populated so it is empty";

        // To be able to mock List<SolrServerClient> servers we use the getServers getter method
        try (MockedStatic<ServiceConfig> mockedServiceConfig = mockStatic(ServiceConfig.class)) {
            mockedServiceConfig.when(ServiceConfig::getSolrServers).thenReturn(List.of());

            SolrServerClient solrServerClient = new SolrServerClient();

            // Act
            // Call to Solr backend is mocked, so query parameters actually don't matter
            Exception exception = assertThrows(InternalServiceException.class, () -> solrServerClient.callSolr(queryDsId, fieldListDsId));

            // Assert
            assertEquals(expectedMessage, exception.getMessage());
        }
    }

    @Test
    public void callSolr_WhenResponseFromSolrIsNull_ReturnOptionalEmpty() throws SolrServerException, IOException {
        // Instead of calling an actual Solr service, we mock `query(SolrParams solrParams`
        SolrServerClient mockedSolrServerClient = mock(SolrServerClient.class);
        when(mockedSolrServerClient.query(any())).thenReturn(null);

        // To be able to mock List<SolrServerClient> servers we use the getServers getter method
        try (MockedStatic<ServiceConfig> mockedServiceConfig = mockStatic(ServiceConfig.class)) {
            mockedServiceConfig.when(ServiceConfig::getSolrServers).thenReturn(List.of(mockedSolrServerClient));

            SolrServerClient solrServerClient = new SolrServerClient();

            // Act
            // Call to Solr backend is mocked, so query parameters actually don't matter
            Optional<SolrDocumentList> actualSolrDocumentList = solrServerClient.callSolr(queryDsId, fieldListDsId);

            // Assert
            assertTrue(actualSolrDocumentList.isEmpty());
        }
    }

    @Test
    public void callSolr_WhenGivenQueryAndFieldList_ReturnSolrDocumentList() throws SolrServerException, IOException {
        // Arrange
        SolrDocument solrDocument = new SolrDocument();
        solrDocument.put("dr_production_id", drProductionId);
        solrDocument.put("id", dsId);
        solrDocument.put("title", title);
        solrDocument.put("startTime", startTime);
        solrDocument.put("endTime", endTime);

        SolrDocumentList solrDocumentList = new SolrDocumentList();
        solrDocumentList.setNumFound(1);
        solrDocumentList.add(solrDocument);

        QueryResponse mockedQueryResponse = mock(QueryResponse.class);
        when(mockedQueryResponse.getResults()).thenReturn(solrDocumentList);

        // Instead of calling an actual Solr service, we mock `query(SolrParams solrParams`
        SolrServerClient mockedSolrServerClient = mock(SolrServerClient.class);
        when(mockedSolrServerClient.query(any())).thenReturn(mockedQueryResponse);

        // To be able to mock List<SolrServerClient> servers we use the getServers getter method
        try (MockedStatic<ServiceConfig> mockedServiceConfig = mockStatic(ServiceConfig.class)) {
            mockedServiceConfig.when(ServiceConfig::getSolrServers).thenReturn(List.of(mockedSolrServerClient));

            SolrServerClient solrServerClient = new SolrServerClient();

            // Act
            // Call to Solr backend is mocked, so query parameters actually don't matter
            Optional<SolrDocumentList> actualSolrDocumentList = solrServerClient.callSolr(queryDsId, fieldListDsId);

            // Assert
            assertTrue(actualSolrDocumentList.isPresent());
            assertEquals(solrDocumentList.getNumFound(), actualSolrDocumentList.get().getNumFound());
            assertEquals(drProductionId, actualSolrDocumentList.get().get(0).getFieldValue("dr_production_id"));
            assertEquals(dsId, actualSolrDocumentList.get().get(0).getFieldValue("id"));
            assertEquals(title, actualSolrDocumentList.get().get(0).getFieldValue("title"));
            assertEquals(startTime, actualSolrDocumentList.get().get(0).getFieldValue("startTime"));
            assertEquals(endTime, actualSolrDocumentList.get().get(0).getFieldValue("endTime"));
        }
    }
}
