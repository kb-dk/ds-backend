package dk.kb.util.webservice;


/**
 * This class has the values defined in the KBAuthorizationInterceptor classes in each module.
 * If the KBAuthorizationInterceptor can be removed to kb-util, then this class can be removed.  
 * Unfortunately it seems complicated to move these classes due to module package specific annotation and because the class is generated in each module. 
 */

public class OAuthConstants {

    public static final String ACCESS_TOKEN_STRING = "AccessTokenString"; // Raw accessToken value
    public static final String TOKEN_ROLES = "TokenRoles"; // Set<String>
    
}
