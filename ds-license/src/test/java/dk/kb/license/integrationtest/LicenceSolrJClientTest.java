package dk.kb.license.integrationtest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dk.kb.license.config.ServiceConfig;

import dk.kb.license.solr.SolrServerClient;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;


/**
 *  Integration test, will not be run by automatic build flow.
 *  Notice data in Solr may change over time. For this unittest two ID's
 */
public class LicenceSolrJClientTest {

    @Tag("integration")   
    @Test
    public void testIdFiltering() throws  IOException, SolrServerException{
        ServiceConfig.initialize("conf/ds-license-behaviour.yaml"); //Load the ID filtering fields
        SolrServerClient solrServer = new SolrServerClient("http://devel11:10007/solr/ds"); //Do not use the one in ds-license-behaviour.yaml

        String idVideo="ds.tv:oai:du:3006e2f8-3f73-477a-a504-4d7cb1ae1e1c";
        String idRadio="ds.radio:oai:du:e683b0b8-425b-45aa-be86-78ac2b4ef0ca";
        ArrayList<String> ids = new ArrayList<String>(); 
        ids.add(idVideo);// Video
        ids.add(idRadio); //radio
        ids.add("none_existing_id"); //not found

        String queryPartAccess="resource_description:VideoObject"; //So radio will not be found
        List<String> filteredIds =solrServer.filterIds(ids, queryPartAccess, "id");		
        assertEquals(1,filteredIds.size());
        assertEquals(idVideo, filteredIds.get(0));                
    }
}
