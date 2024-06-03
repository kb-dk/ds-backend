package dk.kb.license.util;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This can maybe later be extended to a more general usable oath/keycloak util class.
 * 
 */
public class OauthUtil {
    private static final Logger log = LoggerFactory.getLogger(OauthUtil.class);

    /**
     * Validate a JWT access code token against KeyCloak. To call KeyCloak you need to know the keycloakSecret for AD realm provider 
     * TODO also validate the JWT encoded with the HS256 algorithm. (Jira created)
     * 
     * @param code The code parameter in the redirect url
     * @param keyCloakClientSecret The KeyCloak client secret for the realm provider
     * @param redirectUrl The redirect url as given to KeyCloak as a parameter.
     * @param keyCloakRealmTokenUrl The url to the KeyCloak server for the realm used as login provider.
     * 
     * @return Combined username and email returned by KeyCloak.
     * 
     * @throws Exception if code does not validate or unexpected error from KeyCloak.
     * 
     */
    public static String validateCode(String code, String keyCloakClientSecret, String redirectUrl,String keyCloakRealmTokenUrl) throws Exception{

        String JWT=null;
        try {
            JWT = getKeyCloakAccessTokenJWT(code, redirectUrl, keyCloakClientSecret,keyCloakRealmTokenUrl);          
        }
        catch(Exception e ) {              
            log.error("Error calling KeyCloak:"+e.getMessage());
            throw new Exception(e);
        }          
        //log.debug("JWT:"+JWT); 

        JSONObject jwtJson = new JSONObject(JWT);
        String accestoken= jwtJson.getString("access_token");
        log.info("Access token from KeyCloak:"+accestoken); //Not secret.                

        //TODO validate the HS256 encoded JWT. See DRA-753
        
        String[] chunks =  accestoken.split("\\."); //The Oath token contains 3 parts separated by comma

        Base64.Decoder decoder = Base64.getUrlDecoder();        
        //String header = new String(decoder.decode(chunks[0]),Charset.forName("UTF-8"));
        String payload = new String(decoder.decode(chunks[1]),Charset.forName("UTF-8"));

        JSONObject payLoadJson = new JSONObject(payload);       
        String name= payLoadJson.getString("name");
        String email= payLoadJson.getString("email");
        String combinedUserName = name +"("+email+")"; //This will be shown in the GUI

        return combinedUserName;
    }

    /**
     * From a map of parameters create key=value pair with '&' as seperator and key,value UTF-8 encoded
     * Example: key1=value1&key2=value2 
     */
    private static String createFormEncodedParameterString(HashMap<String, String> params) throws UnsupportedEncodingException{
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first) {
                first = false;
            }
            else {
                result.append("&");
            }
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }    
        return result.toString();
    }


    /**
     * 
     * 
     * @param redirectCode Redirect code returned from KeyCloak in redirect url
     * @param redirect_url The redirect url that was given to KeyCloak
     * @param keyCloakClientSecret The secret in KeyCloak for the AD realm provider. 
     * @return String The JWT token from KeyCloak containing user information
     * @throws Exception
     */
    private static String getKeyCloakAccessTokenJWT(String redirectCode, String redirect_url, String keyCloakClientSecret, String keyCloakRealmTokenUrl) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HashMap<String,String> params= new HashMap<String,String>();
        params.put("grant_type", "authorization_code");
        params.put("client_id", "kb-ad");
        params.put("code", redirectCode);
        params.put("client_secret",keyCloakClientSecret);
        params.put("redirect_uri", redirect_url);

        String dataString = createFormEncodedParameterString(params);

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(keyCloakRealmTokenUrl))                  
                .POST(HttpRequest.BodyPublishers.ofString(dataString)) //Body encoded parameters
                .setHeader("content-type", MediaType.APPLICATION_FORM_URLENCODED) //application/x-www-form-urlencoded"                                        
                .build();

        HttpResponse<String> response = client.send( postRequest, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();
        if (statusCode != Status.OK.getStatusCode()) {
            throw new Exception("HTTP code from login redirect was not 200, status="+statusCode);
        }    
        return response.body(); //JWT      
    }

}
