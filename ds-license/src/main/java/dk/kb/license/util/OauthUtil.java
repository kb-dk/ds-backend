package dk.kb.license.util;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;

import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

/**
 * This can maybe later be extended to a more general usable oath/keycloak util class.
 * 
 */
public class OauthUtil {
    private static final Logger log = LoggerFactory.getLogger(OauthUtil.class);
        
    /**
     * <p>
     * From a codeToken issued by the KeyCloak use the codeToken to get the accessToken.
     * The accessToken is verified to have been issued by the trusted KeyCloak server using the public rsa-key.
     * It will also validate the accessToken has not expired yet.
     * If the accessToken validates return username and email concatenated for the user.
     * Throws Exception if the codeToken is not found or accessToken does not validate.  
     * </p>
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
    public static String getUserInformation(String code, String keyCloakClientSecret,String rsaPublicKey, String redirectUrl,String keyCloakRealmTokenUrl) throws Exception{

        String JWT=null;
        try {
            JWT = getKeyCloakAccessTokenJWT(code, keyCloakClientSecret, redirectUrl,keyCloakRealmTokenUrl);          
        }
        catch(Exception e ) {              
            log.error("Error calling KeyCloak:"+e.getMessage());
            throw new Exception(e);
        }          
        //log.debug("JWT:"+JWT); 

        JSONObject jwtJson = new JSONObject(JWT);
        String accestoken= jwtJson.getString("access_token");
        log.debug("Access token from KeyCloak:"+accestoken);                

        //Validate the JWT indeed has been issued by the KeyCloak server and not expired 
        if (!isValidAccessToken(accestoken, rsaPublicKey)) {
            log.error("AccessToken did not validate:"+accestoken);
            throw new InvalidArgumentServiceException("AccessToken did not validate");
        }
        log.debug("Access token validated");
        
        String[] base64DecodeToken = base64DecodeToken(accestoken);
                        
        //String header = base64DecodeToken[0]; //Example: {"alg":"RS256","typ" : "JWT","kid" : "5kW8sVLj3eUP3h2bey8Q4NPvP3q3YsYu8Q267zU_0Pc"}
        String payload = base64DecodeToken[1];

        String combinedUserName = extractNameAndEmail(payload);              
        log.info("Access Token verified for user:"+combinedUserName);        
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
     * Return a String[] with 2 entries. First is header and second is payload.
     * 
     * @param JWT The JWT encoded token. It must consist of 3 parts seperated by  dot '.'. First is header and second is payload. The third is has for validation.
     * 
     * @return String[] . Entry 0 is header entry 1 is payload
     */
    public static String[] base64DecodeToken(String JWT) {
        String[] chunks =  JWT.split("\\."); //The Oath token contains 3 parts separated by comma
        Base64.Decoder decoder = Base64.getUrlDecoder();        
        String header = new String(decoder.decode(chunks[0]),Charset.forName("UTF-8"));
        String payload = new String(decoder.decode(chunks[1]),Charset.forName("UTF-8"));
        String[] tokenDecoded= new String[2];
        tokenDecoded[0]=header;
        tokenDecoded[1]=payload;
        
        return tokenDecoded;
        
    }
    
    /**
     * Make a POST request to KeyCloak to  retrieve the accessToken using the codeToken 
     * 
     * @param redirectCode Redirect code returned from KeyCloak in redirect url
     * @param redirect_url The redirect url that was given to KeyCloak
     * @param keyCloakClientSecret The secret in KeyCloak for the AD realm provider. 
     * @return String The JWT token from KeyCloak containing user information
     * @throws Exception
     */
    private static String getKeyCloakAccessTokenJWT(String redirectCode, String keyCloakClientSecret, String redirect_url, String keyCloakRealmTokenUrl) throws Exception {
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
    
    /**
     * <p>
     * Validate the accessToken retrived from the KeyCloak server, using the codeToken, is valid.
     * Valid means both that it verified to have been signed by the trusted KeyCloak server using the public key
     * and also that the token has not expired. The token is only valid for 15 minutes after issued by KeyCloak.
     * </p>
     *
     * @see <a href="https://www.masterincoding.com/validate-jwt-token-with-public-key-rsa256/">Java Dcoumentation</a>
     *       
     * @param accessToken The accessToken part from the JWT base64 encoded
     * @param rsaPublicKeyBase64 The public RSA key from KeyCloak
     * 
     * @return True of false if the accessToken validates with the public key and also has not expired.
     *       
     */
    public static Boolean isValidAccessToken(String accessToken,String rsaPublicKeyBase64) {
        try {
            buildJWTVerifier(rsaPublicKeyBase64).verify(accessToken.replace("Bearer ", ""));
            // if token is valid no exception will be thrown
            return true;
        } catch (CertificateException e) {
            log.error("Invalid JWT TOKEN:"+e.getMessage() +" token:"+accessToken);
            return false;
        } catch (JWTVerificationException e) {
            // if JWT Token in invalid
            log.error("Invalid TOKEN:"+e.getMessage() +" token:"+accessToken);           
            return false;
        } catch (Exception e) {
            // If any other exception comes
            log.error("Unexpected error validating JWT token:"+e.getMessage() +" token:"+accessToken);        
            return false;
        }
    }
    
    
    /**
     * @param access_payload The decoded payload from the access token. The payload is a JSON object
     * 
     * @return Name and email combined. Example format: Thomas Egense (teg@kb.dk)
     */
    public static String extractNameAndEmail(String access_payload) {
        JSONObject payLoadJson = new JSONObject(access_payload);       
        String name= payLoadJson.getString("name");
        String email= payLoadJson.getString("email");
        String combinedUserName = name +"("+email+")"; //This will be shown in the GUI
        return combinedUserName;        
    }
    
    /*
     * Generate the RSAPublicKey object from the publicKey that is base64 encoded
     */
    private static RSAPublicKey getRSAPublicKey(String publicKeyBase64) throws Exception{    
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64.getBytes(StandardCharsets.UTF_8));
        X509EncodedKeySpec specPublic = new X509EncodedKeySpec(keyBytes);
        PublicKey fileGeneratedPublicKey = keyFactory.generatePublic(specPublic);
        RSAPublicKey rsaPub  = (RSAPublicKey)(fileGeneratedPublicKey);
        return rsaPub;       
    }
    
    private static JWTVerifier buildJWTVerifier(String rsaPublicKeyBase64) throws Exception {
        var algo = Algorithm.RSA256(getRSAPublicKey(rsaPublicKeyBase64), null);
        return JWT.require(algo).build();
    }
    
  }
