package dk.kb.oauth;

import dk.kb.oauth.config.ServiceConfig;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.AccessTokenResponse;

import javax.servlet.http.Cookie;
import java.util.Date;
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


    public static Cookie getNewAuthzCookie() {
        AccessTokenResponse accessTokenResponse = getAuthzClient().obtainAccessToken();

        boolean secure = ServiceConfig.getConfig().getBoolean("config.use-secure-cookie",true);
        Cookie authCookie = new Cookie("Authentication",accessTokenResponse.getToken());
        authCookie.setHttpOnly(true);
        authCookie.setSecure(secure);
        return authCookie;
    }

    public static String getAccessTokenFromAuthzCookie(Cookie cookie) {
        return cookie.getValue();
    }

}
