package dk.kb.util.webservice;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import dk.kb.util.webservice.exception.ServiceException;

/**
 * Service2Service call wrapper will inject an existing OAuth token into the next call.<
 * <p>
 * Current supported:
 * <ul>
 *   <li>Standard Http Request call </li>
 *   <li>Custom ContinuationInputStream</li>
 * </ul>
 * 
 *  Http-requests and continuationStreams requests and inject existing OAuth tokens.   
 */
public class Service2ServiceRequest {
    private static final Logger log = LoggerFactory.getLogger(Service2ServiceRequest.class);
        
    /**
    * <p>
    * Make service call to another web service and set the same OAuth token on the call that was used for the initiating service call. 
    * <p>
    * Maybe this method should be extended to also take additional RequestHeaders, but implement this if situation occurs. 
    *
    * @param uri the full URI with path and parameters set.
    * @param httpMethod The http-method to use for the service call. GET, POST, DELETE etc.
    * @param objectClass The DTO type or String that the response should be parsed to. Only those two return types supported.
    * @param postJsonDto If present must be a JSON serialize objectDTO. Set to null if method call does not require a DTO to be POST'ed.  
    * @return DtoObject (objectClass) of the same type as given as input. Use null as input for void API methods that has no return dto.
    * @throws ServiceException If anything unexpected happens.   
    **/    
    public static <T> T httpCallWithOAuthToken (URI uri , String httpMethod, T objectClass, Object postJsonDto) throws ServiceException {                 
        //The token (message) will be set if the service method that initiated this call required OAuth token. 
        String token= getOAuth2Token();
        Map<String, String> requestHeaders= new HashMap<String, String>();
            if (postJsonDto != null) { // only set application/json if and DTO is posted as JSON.
                requestHeaders.put("Content-Type", "application/json");
                                
            }   
            //requestHeaders.put("Accept", "application/json"); Do we need to set this if objectClass is not null?            
        if (token != null) {                                          
            requestHeaders.put("Authorization","Bearer "+token);
            log.debug("OAuth2 Bearer token added to service2service call");
        }
        else {
             log.debug("No OAuth token was found for service2service request");  
        }
             
        try {
            HttpURLConnection con = getHttpURLConnection(uri, httpMethod, requestHeaders,postJsonDto);
            log.debug("Establishing connection to:"+uri);
            int status = con.getResponseCode();
            if (status < 200 || status > 299) { // Could be mapped to a more precise exception type, but an exception here is most likely a coding error. 
                String msg="Got HTTP " + status + " establishing connection to '" + uri + "'"+ con.getResponseCode();
                log.error(msg);
                throw mapServiceException(status);
            }
            
            String json = IOUtils.toString(con.getInputStream(), StandardCharsets.UTF_8);                               
            
            if (objectClass != null && objectClass instanceof String) {
                @SuppressWarnings("unchecked")
                T jsonString = (T) json;
                return jsonString;
            }
            
            if (objectClass != null) { //Convert to DTO
               ObjectMapper mapper = new ObjectMapper();
               @SuppressWarnings("unchecked")
               T dto = (T) mapper.readValue(json, objectClass.getClass()); //Need to return an object of type <T>.            
               return dto;
            }
            return null; //Void API methods will return null
        }
        catch(ServiceException e) {
            throw e; //Already correct
        }        
        catch(Exception e) { 
            log.error(e.getMessage(),e);
            throw new InternalServiceException(e.getMessage()); 
        }

    }

    
    /**
     * <p>
     * Make service call to another web service and set the same OAuth token on the call that was used for the initiating service call. 
     * <p>
     * Maybe this method should be extended to also take additional RequestHeaders, but implement this if situation occurs. 
     *
     * @param uri the full URI with path and parameters set.
     * @param httpMethod The http-method to use for the service call. GET, POST, DELETE etc.
     * @param objectClass The DTO type or String that the response should be parsed to. Only those two return types supported.
     * @param postJsonDto If present must be a JSON serialize objectDTO. Set to null if method call does not require a DTO to be POST'ed.  
     * @return List<DtoObject> (objectClass) of the same type as given as input. Use null as input for void API methods that has no return dto.
     * @throws ServiceException If anything unexpected happens.   
     **/    
     public static <T> List<T> httpCallWithOAuthTokenAsDtoList(URI uri , String httpMethod, T objectClass, Object postJsonDto) throws ServiceException {                 
         //The token (message) will be set if the service method that initiated this call required OAuth token. 
         String token= getOAuth2Token();
         Map<String, String> requestHeaders= new HashMap<String, String>();
             if (postJsonDto != null) { // only set application/json if and DTO is posted as JSON.
                 requestHeaders.put("Content-Type", "application/json");
                                 
             }   
             //requestHeaders.put("Accept", "application/json"); Do we need to set this if objectClass is not null?            
         if (token != null) {                                          
             requestHeaders.put("Authorization","Bearer "+token);
             log.debug("OAuth2 Bearer token added to service2service call");
         }
         else {
              log.debug("No OAuth token was found for service2service request");  
         }
              
         try {
             HttpURLConnection con = getHttpURLConnection(uri, httpMethod, requestHeaders,postJsonDto);
             log.debug("Establishing connection to:"+uri);
             int status = con.getResponseCode();
             if (status < 200 || status > 299) { // Could be mapped to a more precise exception type, but an exception here is most likely a coding error. 
                 String msg="Got HTTP " + status + " establishing connection to '" + uri + "'"+ con.getResponseCode();
                 log.error(msg);
                 throw mapServiceException(status);
             }             
             String json = IOUtils.toString(con.getInputStream(), StandardCharsets.UTF_8);                       
             ObjectMapper mapper = new ObjectMapper();
             CollectionType listType = mapper.getTypeFactory().constructCollectionType(ArrayList.class,  objectClass.getClass());
             List<T> dtoList = mapper.readValue(json, listType);                                                              
             return dtoList;                                                           
         }
         catch(ServiceException e) {
             throw e; //Already correct
         }        
         catch(Exception e) { 
             log.error(e.getMessage(),e);
             throw new InternalServiceException(e.getMessage()); 
         }
     }
    
    /**
     * Invoke a HTTP of a given HttpMethod and requestHeaders.
     * 
     * @param uri the full URI with path and parameters set.
     * @param httpMethod The http-method to use for the service call. GET, POST, DELETE etc. 
     * @return HttpUrlConnection that will have the status code and response can be read with an InputStream. 
     */
     private static HttpURLConnection getHttpURLConnection(URI uri, String httpMethod, Map<String, String> requestHeaders, Object postJsonDto) throws IOException {
         //Do not log requestHeader since this would expose a valid OAuth token in the log file.
         log.debug("Opening streaming connection to '{}' with, method={}, headers={}", uri, httpMethod,requestHeaders);
         HttpURLConnection con = (HttpURLConnection) uri.toURL().openConnection();
         con.setRequestMethod(httpMethod);               
         con.setInstanceFollowRedirects(true);
         if (requestHeaders != null) {
             requestHeaders.forEach(con::setRequestProperty);
         }
         
         //Make a new ObjectMapper each time.  Reports that it can deadlock if static.
         //Performance hardly an issue since it is only used for service2service calls
         ObjectMapper memberVarObjectMapper = new ObjectMapper();
         String localVarPostBody = memberVarObjectMapper.writeValueAsString( postJsonDto);
         if (postJsonDto !=  null) {
             con.setDoOutput(true); //Required if we post data
             OutputStream os = con.getOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");    
             osw.write(localVarPostBody);
             osw.flush();
             osw.close();
             os.close();  //don't forget to close the OutputStream
          }
         return con;
     }
          
     /**
      * Will return  the oauth token that has been set by the OAuth Interceptor in webservice call. 
      * Return null if no message what set. This will happen in unittests etc. that has not set it implicit
      * 
      * @return token if any was set  by intercepter. Else return null.
      */
     public static String getOAuth2Token() {
         Message m = JAXRSUtils.getCurrentMessage();
         if (m==null) { //Happens in unittest etc, since no interceptor was activated on webservice call.
             return null;             
         }
         
         return (String) m.get(OAuthConstants.ACCESS_TOKEN_STRING); 
                  
     }
     /**
      * Map status codes to known exceptions. 
      * Http 2xx codes should not be mapped here.
      * 
      */
     private static ServiceException mapServiceException(int statusCode) {         
         switch (statusCode) {                      
         case 400:            
            return new InvalidArgumentServiceException();            
         case 404:
             return new NotFoundServiceException();            
        default:
            return new InternalServiceException();
        }         
     }
     
}
