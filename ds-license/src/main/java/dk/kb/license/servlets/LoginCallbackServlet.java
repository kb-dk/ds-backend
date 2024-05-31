package dk.kb.license.servlets;

import java.io.IOException;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.util.OauthUtil;

/**
 * This servlet handles the callback after a succesfull login at KeyCloak.
 * The username will be saved on the session object.     
 */
public class LoginCallbackServlet extends HttpServlet {


    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(CreateLicenseServlet.class);
    private static final String CODE_PARAMETER="code";

    /**
     * <p>
     * After login at KeyCloak users are redirected to this servlet. <br>
     * Login URL at KeyCloak: https://<keycloak_server>realms/master/protocol/openid-connect/auth?response_type=code&client_id=kb-ad&redirect_uri=http://<redirect-server>/ds-license/loginCallbackServlet<br>
     * After successful login, users are redirected back to this servlet.<br>
     * Example redirect url: http://<redirect-server>/ds-license/loginCallbackServlet?session_state=96bff8da-63d4-4f61-850d-23317ece7f90&iss=https%3A%2F%2F<keycloak_server>%2Frealms%2Fmaster&code=b6374b3d-b182-4e9f-acd1-20cf00a2c0d2.96bff8da-63d4-4f61-850d-23317ece7f90.34931351-1c06-48e8-a120-36ae04e1bff1<b>
     * </p>
     * The url parameter 'code' and keycloak secret (from property file) are used to fetch user data from keycloak and validate redirect came from keycloak.<br>
     * The response from keycloak is JWT encoded. The value user name and user email are extracted from the JWT and set on session to mark user is logged in.<br>
     *  
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html; charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        String keyCloakRealmTokenUrl=ServiceConfig.getKeycloakRealmTokenUrl();
        String keyCloakClientSecret=ServiceConfig.getKeycloakClientSecret();
        String redirectUrl=ServiceConfig.getKeycloakRedirectUrl();
        
        String code = request.getParameter(CODE_PARAMETER);
        String combinedUserName=null;
        
        try {
           combinedUserName=OauthUtil.validateCode(code,keyCloakClientSecret , redirectUrl, keyCloakRealmTokenUrl);
        }
        catch(Exception e) {
          log.error("Error validating redirect code against keycloak",e);
          throw new ServletException(e);
          
        }        
        log.info("Login from KeyCloak success for user:"+combinedUserName);
        request.getSession().setAttribute("oauth_user", combinedUserName); //Save on session           
        response.sendRedirect(request.getContextPath() + "/configuration.jsp"); //Default landing page
        return;
    }

}
