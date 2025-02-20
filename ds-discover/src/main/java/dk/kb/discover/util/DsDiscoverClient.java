/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.discover.util;

import dk.kb.discover.client.v1.DsDiscoverApi;
import dk.kb.discover.invoker.v1.ApiClient;
import dk.kb.discover.invoker.v1.ApiException;
import dk.kb.discover.invoker.v1.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

/**
 * Client for the service. Intended for use by other projects that calls this service.
 * See the {@code README.md} for details on usage.
 * </p>
 * This class is not used internally.
 * </p>
 * The client is Thread safe and handles parallel requests independently.
 * It is recommended to persist the client and to re-use it between calls.
 */
public class DsDiscoverClient extends DsDiscoverApi {
    private static final Logger log = LoggerFactory.getLogger(DsDiscoverClient.class);

    /**
     * Creates a client for the service.
     * @param serviceURI the URI for the service, e.g. {@code https://example.com/ds-discover/v1}.
     */
    public DsDiscoverClient(String serviceURI) {
        super(createClient(serviceURI));
        log.info("Created OpenAPI client for '" + serviceURI + "'");
    }

    /**
     * Deconstruct the given URI and use the components to create an ApiClient.
     * @param serviceURIString an URI to a service.
     * @return an ApiClient constructed from the serviceURIString.
     */
    private static ApiClient createClient(String serviceURIString) {
        log.debug("Creating OpenAPI client with URI '{}'", serviceURIString);

        URI serviceURI = URI.create(serviceURIString);
        // No mechanism for just providing the full URI. We have to deconstruct it
        return Configuration.getDefaultApiClient().
                setScheme(serviceURI.getScheme()).
                setHost(serviceURI.getHost()).
                setPort(serviceURI.getPort()).
                setBasePath(serviceURI.getRawPath());
    }
    
    @Override
    public String configAction (String action, String name) throws ApiException {
        throw new ApiException(403, "Method configAction not allowed to be called on DsDiscoverClient");
    }
    
     @Override
    public String collectionAction (String action, String name, String async) throws ApiException {
         throw new ApiException(403, "Method collectionAction  not allowed to be called on DsDiscoverClient");        
     }
           
     @Override
     public String documentedSchema (String collection, String format) throws ApiException {
         throw new ApiException(403, "Method documentedSchema not allowed to be called on DsDiscoverClient");
     }
         
     @Override
     public String solrMLT (String collection, String q, String mltFl, Integer mltMintf, Integer mltMindf, Integer mltMaxdf, Integer mltMaxdfpct, Integer mltMinwl, Integer mltMaxwl, Integer mltMaxqt, Boolean mltBoost, String mltInterestingTerms, List<String> fq, Integer rows, Integer start, String fl, String qOp, String wt) throws ApiException {
         throw new ApiException(403, "Method solrMLT not allowed to be called on DsDiscoverClient");        
     }
         
     @Override
     public String solrSchema (String collection, String wt) throws ApiException {
         throw new ApiException(403, "Method solrSchema not allowed to be called on DsDiscoverClient");         
     }
         
     @Override
     public String solrSearch (String collection, String q, List<String> fq, Integer rows, Integer start, String fl, String facet, List<String> facetField, String spellcheck, String spellcheckBuild, String spellcheckReload, String spellcheckQ, String spellcheckDictionary, Integer spellcheckCount, String spellcheckOnlyMorePopular, String spellcheckExtendedResults, String spellcheckCollate, Integer spellcheckMaxCollations, Integer spellcheckMaxCollationTries, Double spellcheckAccuracy, String qOp, String wt, String version, String indent, String debug, String debugExplainStructured) throws ApiException {
         throw new ApiException(403, "Method  solrSearch not allowed to be called on DsDiscoverClient");              
     }
     
     @Override
     public String solrSuggest (String collection, String suggestDictionary, String suggestQ, Integer suggestCount, String wt) throws ApiException {
         throw new ApiException(403, "Method solrSuggest not allowed to be called on DsDiscoverClient");  
     }
     
}
