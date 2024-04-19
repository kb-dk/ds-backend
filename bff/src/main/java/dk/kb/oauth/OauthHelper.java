package dk.kb.oauth;

import dk.kb.oauth.config.ServiceConfig;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.keycloak.representations.AccessTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class OauthHelper {
    private static final Logger log = LoggerFactory.getLogger(OauthHelper.class);
    private static String clientId;
    private static String clientSecret;
    private static String keyCloakUrl;
    private static String realm;
    private static AuthzClient authzClient = null;


    private static synchronized AuthzClient getAuthzClient() {
        if (authzClient == null) {
            try {
                keyCloakUrl = ServiceConfig.getConfig().getString("keycloak.url");
                realm = ServiceConfig.getConfig().getString("keycloak.realm");
                clientId = ServiceConfig.getConfig().getString("keycloak.client-id");
                clientSecret = ServiceConfig.getConfig().getString("keycloak.secret");

                Configuration authzClientConfig = new Configuration(keyCloakUrl, realm, clientId, Map.of("secret", clientSecret), null);
                authzClient = AuthzClient.create(authzClientConfig);
            } catch(Exception e) {
                log.error("cannot connect to Oauth server",e);
                throw new InternalServiceException("cannot connect to to Oauth server ");
            }
        }
        return authzClient;
    }

    /**
     * Get a new access-token from the keyCloak server
     * @return the accessToken-string
     */
    public static String getNewAccessToken() {
        try {
            AccessTokenResponse accessTokenResponse = getAuthzClient().obtainAccessToken();
            return accessTokenResponse.getToken();
        } catch(HttpResponseException e) {
            log.error("error obtaining accesstoken",e);
            throw new InternalServiceException("cannot obtain accesstoken: "+e.getMessage());
        }
    }


}
