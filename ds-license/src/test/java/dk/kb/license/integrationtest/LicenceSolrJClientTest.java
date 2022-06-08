package dk.kb.license.integrationtest;

import java.util.ArrayList;
import java.util.List;

import dk.kb.license.solr.SolrServerClient;


//Integration test. Run manually to see solr integration is working
public class LicenceSolrJClientTest {


     //Find url in property file on devel06 etc.
	private static SolrServerClient solrServer = new SolrServerClient("http://localhost:50001/solr/aviser.2.devel/");
		
	public static void main(String[] args) throws Exception{
		
		ArrayList<String> ids = new ArrayList<String>(); 
		ids.add("doms_radioTVCollection:uuid:12efb195-194f-4795-bdc8-4efb2fa43152");//radio TV
		ids.add("doms_reklamefilm:uuid:b12445f8-8b88-4d32-bc14-d7494debb491"); //reklame
		
		String queryPartAccess="recordBase:doms_radioTVCollection";
		List<String> filteredIds =solrServer.filterIds(ids, queryPartAccess);		
		System.out.println("Size:"+filteredIds.size());
	    System.out.println(filteredIds);		
	}
	
	
	
	
}
