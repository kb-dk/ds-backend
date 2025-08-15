package dk.kb.license.integrationtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.license.solr.SolrServerClient;
import dk.kb.util.Resolver;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

public class RestrictedIdsIntegrationTest {

    @Tag("integration")
    @Test
    public void test() throws SolrServerException, IOException {
        SolrServerClient solrServer = new SolrServerClient("http://search-solr9.statsbiblioteket.dk/solr/ds.2.prod");
        SolrQuery query = new SolrQuery();

        String jsonString = Resolver.resolveUTF8String("rightsModuleMigratedData/restrictedDRProductionIds.json");

        ObjectMapper objectMapper = new ObjectMapper();
        JSONArray jsonArray = new JSONArray(jsonString);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String idValue = jsonObject.getString("idValue");
            query.setQuery("dr_production_id:"+idValue);
            query.setStart(0);
            query.setRows(100);
            QueryResponse result = solrServer.query(query);
            result.getResults().stream().forEach(
                    document -> {
                        if (!((boolean) document.get("dr_id_restricted"))) {
                            System.out.println(document.get("id"));
                        }
                    }
            );
        }
        int i = 0;
    }


}
