package dk.kb.util.oauth2;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.ServiceException;

public class KeycloakUtil {

    private static final Logger log = LoggerFactory.getLogger(KeycloakUtil.class);
          
    
    /**
     * <p>
     * Retrieve an JWT access_token that can be used as Bearer token for calling services with OAuth2 authentication enabled.   
     * </p>
     * 
     * <p>
     * This method uses JDK classes only for https call since any new dependencies will be included in all projects using kb-util
     * </p>
     * 
     * @param keyCloakRealmUrl url to keycloak. The url should end with: /protocol/openid-connect/token
     * @param clientId The id configured in the realm for the service. is 'bff' for devel and 'DS' in production
     * @param clientSecret clientSecret in keycloak to access the service 
     * 
     * @return The access bearer JWT token.
     * 
     */
    public static String getKeycloakAccessToken(String keyCloakRealmUrl,String clientId, String clientSecret) throws ServiceException {
    try {     
        HttpsURLConnection con = (HttpsURLConnection) new URL(keyCloakRealmUrl).openConnection();
        con.setRequestMethod("POST");
        con.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");  //header attribute

        con.setDoInput(true);
        con.setDoOutput(true);

        String requestParameters = "grant_type=client_credentials&client_id="+clientId+"&client_secret="+clientSecret;
        byte[] postData = requestParameters.getBytes(StandardCharsets.UTF_8);
        
        try (DataOutputStream dos = new DataOutputStream(con.getOutputStream())) { //autoclose
            dos.write(postData);
        }

        StringBuilder content;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(),StandardCharsets.UTF_8))) {  //autoclose

            String line;
            content = new StringBuilder();

            while ((line = br.readLine()) != null) {
                content.append(line);
                content.append(System.lineSeparator());
            }
        }                       
        con.connect();                

        //response is now a json response {"access_token":"1234abc...." expires_in:.....} 
        String response=content.toString();        
        JSONObject json = new JSONObject(response);
        String bearer = json.getString("access_token");
        return bearer;                
    }
    catch(Exception e) {
        log.error("Error retrieving access_token",e);
        throw new InternalServiceException("Error retrieving access_token",e);
    }
    
  }
}
