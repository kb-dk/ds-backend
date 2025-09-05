package dk.kb.license.solr;

import dk.kb.license.config.ServiceConfig;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Create a Solr client.
 * See the yaml-configuration to see which Solr URLs are used
 */
public class SolrServerClient extends AbstractSolrJClient {
    private static final Logger log = LoggerFactory.getLogger(SolrServerClient.class);
    private String serverUrl = null;
    private List<SolrServerClient> servers = Collections.emptyList();

    /**
     * Automatically populates the List of SolrServerClient when the class gets initialized
     */
    public SolrServerClient() {
        servers = ServiceConfig.getSolrServers();
    }

    /**
     * Create a Solr client from a serverUrl that is used to call Solr
     *
     * @param serverUrl
     */
    public SolrServerClient(String serverUrl) {
        try {
            this.serverUrl = serverUrl;
            solrServer = new HttpSolrClient.Builder(serverUrl).build();
            //solrServer.setParser(new NoOpResponseParser("json"));
            solrServer.setParser(new XMLResponseParser());
        } catch (RuntimeException e) {
            log.error("Unable to connect to solr-server: {}", serverUrl, e);
        }
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public QueryResponse query(SolrParams solrParams) throws SolrServerException, IOException {
        return solrServer.query(solrParams);
    }

    /**
     * Create a Solr query.
     *
     * @param query     what query we want to query in Solr.
     * @param fieldList what fields should be in the Solr response.
     * @param pageSize  which determines the amount of documents returned by the query.
     * @return a {@link SolrQuery} that can be used in request to Solr.
     */
    public SolrQuery createSolrQuery(String query, String fieldList, int pageSize) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setFields(fieldList);
        solrQuery.setRows(pageSize);
        return solrQuery;
    }

    /**
     * Call Solr with the given solrQuery and return the response
     *
     * @param query     what query we want to query in Solr.
     * @param fieldList what fields should be in the Solr response.
     * @return combined response from Solr
     */
    public Optional<SolrDocumentList> callSolr(String query, String fieldList) {
        QueryResponse response = null;
        SolrDocumentList resultSolrDocumentList = new SolrDocumentList();

        if (servers == null || servers.isEmpty()) {
            log.error("List of SolrServerClient is never populated so it is empty");
            throw new InternalServiceException("List of SolrServerClient is never populated so it is empty");
        }

        // Ds-license supports multiple backing Solr servers. So we have to wrap it in this for-loop
        for (SolrServerClient server : servers) {
            int pageSize = 500;
            int start = 0;

            SolrQuery solrQuery = createSolrQuery(query, fieldList, pageSize);

            while (true) {
                // Update start value before the query is fired against the server
                solrQuery.setStart(start);

                try {
                    // Query Solr for a response
                    response = server.query(solrQuery);
                } catch (SolrServerException | IOException e) {
                    log.error("callSolr an error appeared when calling Solr backend: {}", e.getMessage());
                    throw new InternalServiceException(e);
                }

                if (response == null || response.getResults() == null) {
                    log.error("Response from Solr is null");
                    return Optional.empty();
                } else {
                    resultSolrDocumentList.addAll(response.getResults());
                    long totalResults = response.getResults().getNumFound();

                    // Break the loop of no more records are available
                    if (start + pageSize >= totalResults) {
                        break;
                    }

                    // Increment start by pageSize
                    start += pageSize;
                }
            }
        }

        // Add NumFound to the SolrDocumentList
        resultSolrDocumentList.setNumFound(response.getResults().getNumFound());

        return Optional.of(resultSolrDocumentList);
    }
}

