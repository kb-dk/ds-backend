package dk.kb.license.solr;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Create a Solr client.
 * See the yaml-configuration to see which Solr URLs are used
 */
public class SolrServerClient extends AbstractSolrJClient {
    
    public SolrServerClient(String serverUrl) {
        super(serverUrl);
    }

}

