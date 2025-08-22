package dk.kb.license.solr;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Create a solr client.
 * See the yaml-configuration to see which solr-url's are used
 */
public class SolrServerClient extends AbstractSolrJClient {

    private static final Logger log = LoggerFactory.getLogger(SolrServerClient.class);
    private String serverUrl = null;

    /**
     * Create a solr client from a serverUrl
     *
     * @param serverUrl
     * @return SolrServerClient which will be used to call Solr
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
}

