package dk.kb.license.solr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.license.config.ServiceConfig;




public class AbstractSolrJClient {
    private static final Logger log = LoggerFactory.getLogger(AbstractSolrJClient.class);
    private static String filterIdField;
    private static String filterResourceIdField;
    
    protected HttpSolrClient solrServer; 
    static{ 
        //Silent all the debugs log from HTTP Client (used by SolrJ)
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR"); 
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR"); 
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");        
        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.OFF); 
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.OFF);
   
        filterIdField=ServiceConfig.SOLR_FILTER_ID_FIELD;
        filterResourceIdField= ServiceConfig.SOLR_FILTER_RESOURCE_ID_FIELD;
    }

  
    /**
     * Filter ID using the primary ID field 
     *  
     */      
    public List<String> filterIds(List<String> ids, String queryPartAccess) throws Exception{

        if (ids == null || ids.size() == 0){
            return new ArrayList<String>();
        }

        String queryStr= makeAuthIdPart(ids,filterIdField); 
        System.out.println("query:"+queryStr);
        
        SolrQuery query = new SolrQuery( queryStr);        
        
        query.setFilterQueries(queryPartAccess);
        log.debug("filter:"+queryPartAccess);
        
        query.setFields(filterIdField); //only this field is used from resultset
        query.setRows(Math.min(10000, ids.size()*200)); // Powerrating... each page can have max 200 segments (rare).. with 20 pages query this is 4000..               
        query.set("facet", "false"); //  Must be parameter set, because this java method does NOT work: query.setFacet(false);          
        QueryResponse response = solrServer.query(query);        
        ArrayList<String> filteredIds = getIdsFromResponse(response,ServiceConfig.SOLR_FILTER_ID_FIELD);

        return filteredIds;

    }
     
    /**
     * Filter resourceIDs from the resourceid field (multifield in solr)
     * This method is used for record resources such as images 
     *  
     */
    public List<String> filterResourceIds(List<String> ids, String queryPartAccess) throws Exception{

        if (ids == null || ids.size() == 0){
            return new ArrayList<String>();
        }

        String queryStr= makeAuthIdPart(ids, ServiceConfig.SOLR_FILTER_RESOURCE_ID_FIELD); 
        System.out.println("query:"+queryStr);
        
        SolrQuery query = new SolrQuery( queryStr);        
        
        query.setFilterQueries(queryPartAccess);
        log.debug("filter:"+queryPartAccess);
        
        query.setFields(filterResourceIdField); //only this field is used from resultset
        query.setRows(Math.min(10000, ids.size()*200)); // Powerrating... each page can have max 200 segments (rare).. with 20 pages query this is 4000..               
        query.set("facet", "false"); //  Must be parameter set, because this java method does NOT work: query.setFacet(false);          
        QueryResponse response = solrServer.query(query); 
        log.info("response:"+response);
        ArrayList<String> filteredIds = getIdsFromResponse(response, ServiceConfig.SOLR_FILTER_RESOURCE_ID_FIELD);

        return filteredIds;

    }
     
    
    
    //Will also remove " from all ids to prevent Query-injection
    public static String makeAuthIdPart (List<String> ids, String filterField){
        StringBuilder queryIdPart = new StringBuilder();
        queryIdPart.append("(");
        for (int i = 0 ;i<ids.size();i++){
            String id = ids.get(i);
            //Remove all \ and " from the string
            id= id.replaceAll("\\\\", "");
            id= id.replaceAll("\\\"", "");
            queryIdPart.append(filterField+":\""+id +"\"");
            if (i<ids.size()-1){
                queryIdPart.append(" OR ");
            }                       
        }
        queryIdPart.append(")");
        return queryIdPart.toString();
    }

    public static ArrayList<String> getIdsFromResponse(QueryResponse response, String solrField){
        ArrayList<String> ids= new ArrayList<String>();

        for (SolrDocument current : response.getResults()){
                      
            Collection<Object> fieldValues = current.getFieldValues(solrField); //Multivalued
             for (Object idFound : fieldValues){
                 ids.add(idFound.toString());                 
             }                           
        }   
        return ids;
    }


    public SolrClient getSolrServer() {
        return solrServer;
    }

   
}
