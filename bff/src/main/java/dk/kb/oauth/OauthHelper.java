package dk.kb.oauth;

import dk.kb.oauth.config.ServiceConfig;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.AccessTokenResponse;

import java.util.Map;

public class OauthHelper {

    private static String clientId;
    private static String clientSecret;
    private static String keyCloakUrl;
    private static String realm;
    private static AuthzClient authzClient = null;


    private static synchronized AuthzClient getAuthzClient() {
        if (authzClient == null) {
            keyCloakUrl = ServiceConfig.getInstance().getConfig().getString("config.keycloak.url");
            realm = ServiceConfig.getInstance().getConfig().getString("config.keycloak.realm");
            clientId = ServiceConfig.getInstance().getConfig().getString("config.keycloak.client-id");
            clientSecret = ServiceConfig.getInstance().getConfig().getString("config.keycloak.secret");

            Configuration authzClientConfig = new Configuration(keyCloakUrl,realm,clientId, Map.of("secret",clientSecret),null);
            authzClient = AuthzClient.create(authzClientConfig);
        }
        return authzClient;
    }


    public static String getNewAccessToken() {
        AccessTokenResponse accessTokenResponse = getAuthzClient().obtainAccessToken();
        return accessTokenResponse.getToken();
    }


}
