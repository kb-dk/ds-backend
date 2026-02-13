package dk.kb.license.solr;

import dk.kb.license.config.ServiceConfig;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;

public class SolrMultiClient {
    private static final Logger log = LoggerFactory.getLogger(SolrMultiClient.class);
    
    private final Collection<SolrServerClient> servers;
    
        // determines the amount of documents returned by the query
    public final int rows = 1000;
    
    
    public SolrMultiClient() {
        servers = ServiceConfig.getSolrServers();
    }
    
    /**
     * Call Solr with the given solrQuery and return the response
     *
     * @param query     what query we want to query in Solr.
     * @param fieldList what fields should be in the Solr response.
     * @return combined response from Solr
     * @throws SolrServerException
     * @throws IOException
     */
    public SolrDocumentList callSolr(String query, String fieldList) throws SolrServerException, IOException {
        QueryResponse response = null;
        SolrDocumentList resultSolrDocumentList = new SolrDocumentList();
        
        
        if (servers.isEmpty()) {
            final String errorMessage = "List of SolrServerClient is never populated";
            log.error(errorMessage);
            throw new InternalServiceException(errorMessage);
        }
        
        // Ds-license supports multiple backing Solr servers. So we have to wrap it in this for-loop
        for (SolrServerClient server : servers) {
            SolrQuery solrQuery = createSolrQuery(query, fieldList);
            response = server.getSolrServer().query(solrQuery);
            
            // If the query match with more than 1000 results, we throw an exception
            if (response.getResults().getNumFound() > rows) {
                final String errorMessage = "Too many results for query: " + query;
                log.error(errorMessage);
                throw new InvalidArgumentServiceException(errorMessage);
            }
            
            resultSolrDocumentList.addAll(response.getResults());
        }
        
        // Add NumFound to the SolrDocumentList
        resultSolrDocumentList.setNumFound(response.getResults().getNumFound());
        
        return resultSolrDocumentList;
    }
    
        /**
     * Create a Solr query.
     *
     * @param query     what query we want to query in Solr.
     * @param fieldList what fields should be in the Solr response.
     * @return a {@link SolrQuery} that can be used in request to Solr.
     */
    public SolrQuery createSolrQuery(String query, String fieldList) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setFields(fieldList);
        solrQuery.setRows(rows);
        return solrQuery;
    }
}
