package dk.kb.license.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AbstractSolrJClient {
    private static final Logger log = LoggerFactory.getLogger(AbstractSolrJClient.class);
    
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
    }

    
    
    /**
     * Filter ID for a given ID field. Both id and resource_id are used as id's.
     * This method is used for record resources such as images.
     * Will only return ID's that is part of the query request.
     * Due to multi fields, Solr can return values that was not in the request
     * <p>
     * The Solr requests are issued using POST and in batches, ensuring that an arbitrary amount and size of IDs
     * (within Solr's limits for ID size) can be handled.
     * @param ids List of id's that will be matched agaist the solrIdField param.
     * @param queryPartAccess A filter query used to filter the ID's. If null then no filter is used.
     * @param solrIdField So far only options are 'id' or 'resource_id' fields in Solr.
     */
    public List<String> filterIds(List<String> ids, String queryPartAccess, String solrIdField)
            throws SolrServerException, IOException {

        if (ids == null || ids.isEmpty()){
            return Collections.emptyList();
        }

        SolrQuery query = new SolrQuery();
        if (queryPartAccess != null) { //Can be used without filter
          query.setFilterQueries(queryPartAccess);
        }
        query.setFields(solrIdField); //only this field is used from resultset
        query.setRows(Math.min(10000, ids.size()*200)); // Powerrating... each page can have max 200 segments (rare).. with 20 pages query this is 4000..               
        query.set("facet", "false"); //  Must be parameter set, because this java method does NOT work: query.setFacet(false);          

        ArrayList<String> filteredIds = new ArrayList<>(ids.size());
        for (int i = 0 ; i < ids.size() ; i+=1000) { // Solr has a default max of 1024 unique clauses in a query
            query.setQuery(makeAuthIdPart(ids.subList(i, Math.min(i+1000, ids.size())), solrIdField));
            QueryResponse response = solrServer.query(query, SolrRequest.METHOD.POST);
            filteredIds.addAll(getIdsFromResponse(response, solrIdField));
        }
        //Due to multivalue fields, Solr can return ID's that was not in the  query request list
        filteredIds.retainAll(ids);               
        return filteredIds;
    }
       
    //Will also remove " from all ids to prevent Query-injection
    //Public so it can be used by unittest
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

    private static ArrayList<String> getIdsFromResponse(QueryResponse response, String solrField){
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
