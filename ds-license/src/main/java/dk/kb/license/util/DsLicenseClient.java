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
package dk.kb.license.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dk.kb.license.model.v1.CheckAccessForIdsInputDto;
import dk.kb.license.model.v1.CheckAccessForIdsOutputDto;
import dk.kb.license.model.v1.GetUserGroupsAndLicensesInputDto;
import dk.kb.license.model.v1.GetUserGroupsAndLicensesOutputDto;
import dk.kb.license.model.v1.GetUserGroupsInputDto;
import dk.kb.license.model.v1.GetUserGroupsOutputDto;
import dk.kb.license.model.v1.GetUserQueryInputDto;
import dk.kb.license.model.v1.GetUsersFilterQueryOutputDto;
import dk.kb.license.model.v1.GetUsersLicensesInputDto;
import dk.kb.license.model.v1.GetUsersLicensesOutputDto;
import dk.kb.license.model.v1.ValidateAccessInputDto;
import dk.kb.license.model.v1.ValidateAccessOutputDto;
import dk.kb.util.webservice.Service2ServiceRequest;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import dk.kb.util.yaml.YAML;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

/**
 * Client for the service. Intended for use by other projects that calls this service.
 * See the {@code README.md} for details on usage.
 * </p>
 * This class is not used internally.
 * </p>
 * The client is Thread safe and handles parallel requests independently.
 * It is recommended to persist the client and to re-use it between calls.
 * <p>
 * The client supports caching for {@link #checkAccessForIds} and {@link #checkAccessForResourceIds}.
 * See the {@link DsLicenseApi(YAML)} constructor for details.
 */
public class DsLicenseClient{
    private static final Logger log = LoggerFactory.getLogger(DsLicenseClient.class);
    private final String serviceURI;
    private final static String CLIENT_URL_EXCEPTION="The client url was not constructed correct";
    
    public static final String URL_KEY = "licensemodule.url";
    public static final String CACHE_ID_COUNT_KEY = "licensemodule.cache.id.count";
    public static final int CACHE_ID_COUNT_DEFAULT = 100;
    public static final String CACHE_ID_MS_KEY = "licensemodule.cache.id.ms";
    public static final long CACHE_ID_MS_DEFAULT = 60000;

    Cache<CheckAccessForIdsInputDto, CheckAccessForIdsOutputDto> idcache =
            Caffeine.newBuilder()
            .maximumSize(CACHE_ID_COUNT_DEFAULT)
            .expireAfterWrite(CACHE_ID_MS_DEFAULT, TimeUnit.MILLISECONDS)
            .build();

    /**
     * Creates a client for the service based on YAML config with the following structure:
     * <pre>
     * licensemodule:
     *   url: 'http://localhost:9076/ds-license/v1' # Mandatory
     *   cache:
     *     id:
     *       count: 100 # Default
     *       ms: 60000  # Default
     * </pre>
     * @param yaml setup for the license client, as outlined above.
     */
    @SuppressWarnings("JavadocLinkAsPlainText")
    public DsLicenseClient(YAML yaml) {
        this(yaml.getString(URL_KEY),
                yaml.getInteger(CACHE_ID_COUNT_KEY, CACHE_ID_COUNT_DEFAULT),
                yaml.getLong(CACHE_ID_MS_KEY, CACHE_ID_MS_DEFAULT));
    }

    /**
     * Creates a client for the service.
     * @param serviceURI the URI for the service, e.g. {@code https://example.com/ds-license/v1}.
     */
    public DsLicenseClient(String serviceURI, int cacheIDCount, long cacheIDms) {
        cache(cacheIDCount, cacheIDms);
        this.serviceURI = serviceURI;
        log.info("Created OpenAPI client for '{}' with ID-cache(count={}, ms={})",
                serviceURI, cacheIDCount, cacheIDms);
    }

    /**
     * Adjust caching if {@link #checkAccessForIds} and {@link #checkAccessForResourceIds}.
     * <p>
     * Calling this method resets the cache.
     * @param cacheIDCount the maximum number of {@link CheckAccessForIdsInputDto} to cache.
     * @param cacheIDms the maximum amount of milliseconds for any object in the cache.
     * @return this object with caching adjusted
     */
    @SuppressWarnings("UnusedReturnValue")
    public DsLicenseClient cache(int cacheIDCount, long cacheIDms) {
        log.debug("Setting ID cache to count={}, ms={}", cacheIDCount, cacheIDms);
        idcache = Caffeine.newBuilder()
                .maximumSize(cacheIDCount)
                .expireAfterWrite(cacheIDms, TimeUnit.MILLISECONDS)
                .build();
        return this;
    }

    /**
     * Creates a client for the service.
     * @param serviceURI the URI for the service, e.g. {@code https://example.com/ds-license/v1}.
     * @deprecated use {@link DsLicenseApi(YAML)} or {@link DsLicenseApi(YAML, int, long)} instead.
     */
    @Deprecated
    public DsLicenseClient(String serviceURI) {
        this(serviceURI, CACHE_ID_COUNT_DEFAULT, CACHE_ID_MS_DEFAULT);
    }

 
    /**
     * Bypass the cache and always perform a remote check for access. 
     * Use the Cached method for performance instead: {@link DsLicenseClient#checkAccessForIdsGeneral}
     * 
     * @param idInputDto request for access information. 
     * @return direct result from {@link DsLicenseApi#checkAccessForIds} 
     * @throws ServiceException if the remote call failed.
     */
    public CheckAccessForIdsOutputDto checkAccessForResourceIds(CheckAccessForIdsInputDto idInputDto) throws ServiceException{
        try {
            URI uri = new URIBuilder(serviceURI + "/checkAccessForResourceIds")                                                                
                    .build();
            return Service2ServiceRequest.httpCallWithOAuthToken(uri,"POST",new CheckAccessForIdsOutputDto(),idInputDto);              
        }
        catch (URISyntaxException e) {
            log.error("Invalid url:"+e.getMessage());
            throw new InternalServiceException(CLIENT_URL_EXCEPTION);               
         }                    
    }
    
    /**
     * Validate if user has access to all groups in input.
     * 
     * @param ValidateAccessInputDto
     * @return ValidateAccessOutputDto
     * @throws ServiceException if fails to make API call
     */
    public ValidateAccessOutputDto validateAccess(ValidateAccessInputDto idInputDto) throws ServiceException{
        try {
            URI uri = new URIBuilder(serviceURI + "/validateAccess")                                                                
                    .build();
            return Service2ServiceRequest.httpCallWithOAuthToken(uri,"POST",new ValidateAccessOutputDto(),idInputDto);              
        }
        catch (URISyntaxException e) {                
            log.error("Invalid url:"+e.getMessage());
            throw new InternalServiceException(CLIENT_URL_EXCEPTION);               
         }                   
    }
    

    /**
     * Bypass the cache and always perform a remote check for access.
     * Use the Cached method for performance instead: {@link  DsLicenseClient#checkAccessForIdsGeneral}
     * 
     *   
     * @param idInputDto request for access information.
     * @return direct result from {@link DsLicenseApi#checkAccessForIds} 
     * @throws ServiceException if the remote call failed.
     */
    public CheckAccessForIdsOutputDto checkAccessForIds(CheckAccessForIdsInputDto idInputDto) throws ServiceException {       
            URI uri;
            try {
                uri = new URIBuilder(serviceURI + "/checkAccessForIds")                                                                
                        .build();
            }
            catch (URISyntaxException e) {                
                log.error("Invalid url:"+e.getMessage());
                throw new InternalServiceException(CLIENT_URL_EXCEPTION);               
            }
            return Service2ServiceRequest.httpCallWithOAuthToken(uri,"POST",new CheckAccessForIdsOutputDto(),idInputDto);                                                     
    }
    
    /**
     * Get the groups that the user has access to
     * 
     * @param getUserGroupsInputDto  (optional)
     * @return GetUserGroupsOutputDto
     * @throws ServiceException if fails to make API call
     */
    public GetUserGroupsOutputDto getUserGroups (GetUserGroupsInputDto getUserGroupsInputDto) throws ServiceException {

        try {
            URI uri = new URIBuilder(serviceURI + "/getUserGroups")                                                                
                    .build();
            return Service2ServiceRequest.httpCallWithOAuthToken(uri,"POST",new GetUserGroupsOutputDto(),getUserGroupsInputDto);              
        }
        catch (URISyntaxException e) {                
            log.error("Invalid url:"+e.getMessage());
            throw new InternalServiceException(CLIENT_URL_EXCEPTION);               
         }                                
    }
    
    /**
     * Get all licenses and groups/presentationtypes that the user has access to. Will also list all licenses defined and presentationtypes
     * 
     * @param getUserGroupsAndLicensesInputDto  (optional)
     * @return GetUserGroupsAndLicensesOutputDto
     * @throws ServiceException if fails to make API call
     */
    public GetUserGroupsAndLicensesOutputDto getUserGroupsAndLicenses (GetUserGroupsAndLicensesInputDto getUserGroupsAndLicensesInputDto) throws Exception {
        try {
            URI uri = new URIBuilder(serviceURI + "/getUserGroupsAndLicenses")                                                                
                    .build();                       
            return Service2ServiceRequest.httpCallWithOAuthToken(uri,"POST",new GetUserGroupsAndLicensesOutputDto(),getUserGroupsAndLicensesInputDto);              
        }
        catch (URISyntaxException e) {                
            log.error("Invalid url:"+e.getMessage());
            throw new InternalServiceException(CLIENT_URL_EXCEPTION);               
         }                          
    }
      
    
    /**
     * Shows the filter query for Solr generated from the user attributes. PresentationType are defined in configuration. Example: Search
     * 
     * @param getUserQueryInputDto  (optional)
     * @return GetUsersFilterQueryOutputDto
     * @throws ServiceException if fails to make API call
     */
    public GetUsersFilterQueryOutputDto getUserLicenseQuery (GetUserQueryInputDto getUserQueryInputDto) throws Exception {
        try {
            URI uri = new URIBuilder(serviceURI + "/getUserLicenseQuery")                                                                
                    .build();                       
            return Service2ServiceRequest.httpCallWithOAuthToken(uri,"POST",new GetUsersFilterQueryOutputDto(),getUserQueryInputDto);              
        }
        catch (URISyntaxException e) {                
            log.error("Invalid url:"+e.getMessage());
            throw new InternalServiceException(CLIENT_URL_EXCEPTION);               
         }        
    }
      
    
    /**
     * Get a list of all licences that validates from user attributes.
     * 
     * @param getUsersLicensesInputDto  (optional)
     * @return GetUsersLicensesOutputDto
     * @throws Exception if fails to make API call
     */
    public GetUsersLicensesOutputDto getUserLicenses(GetUsersLicensesInputDto getUsersLicensesInputDto) throws Exception {
        try {
            URI uri = new URIBuilder(serviceURI + "/getUserLicenses")                                                                
                    .build();                       
            return Service2ServiceRequest.httpCallWithOAuthToken(uri,"POST",new GetUsersLicensesOutputDto(),getUsersLicensesInputDto);              
        }
        catch (URISyntaxException e) {                
            log.error("Invalid url:"+e.getMessage());
            throw new InternalServiceException(CLIENT_URL_EXCEPTION);               
         }    
        
    }
    
    
    /**
     * Helper for {@link #checkAccessForIds} and {@link #checkAccessForResourceIds} that takes care of wrapping and
     * unwrapping the use of {@link #idcache}.
     * @param idInputDto an input for either ID or resourceID lookup.
     * @param directID if true, the given IDs are treated as primary IDs. If false, they are treated as resourceIDs.
     *                 This switches between forwarding to {@link #checkAccessForIds} and
     *                 {@link #checkAccessForResourceIds}.
     * @return the response from the forward call, potentially fetched from cache.
     * @throws ServiceException if the ID access check failed.
     */
    public CheckAccessForIdsOutputDto checkAccessForIdsGeneral(CheckAccessForIdsInputDto idInputDto, boolean directID)
            throws ServiceException {
        // This is a mess of try-catches, but it is hard to avoid as we really want to use the Function based
        // idcache.get(idInputDto, inputDTO -> ...)
        // That method guards against multiple concurrent checks for the same IDs,
        // which is an extremely common occurrence when handling Tile based image viewing.       
            return idcache.get(idInputDto, inputDTO -> {               
                    return directID ?
                            checkAccessForIds(inputDTO) :
                            checkAccessForResourceIds(inputDTO);                                    
            });       
    }

}
