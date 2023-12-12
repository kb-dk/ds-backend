package dk.kb.license.solr;


import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SolrServerClient extends AbstractSolrJClient{

    private static final Logger log = LoggerFactory.getLogger(SolrServerClient .class);
    private String serverUrl = null;
    
    public SolrServerClient (String serverUrl){
        try{           
          this.serverUrl = serverUrl;
            solrServer = new HttpSolrClient.Builder(serverUrl).build();       
           //solrServer.setParser(new NoOpResponseParser("json"));
            solrServer.setParser(new XMLResponseParser());
        }
        catch(Exception e){            
            log.error("Unable to connect to solr-server:"+serverUrl,e);
        }


    }

    public  String getServerUrl() {
      return serverUrl;
    }
    
}

