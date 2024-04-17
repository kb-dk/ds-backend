package dk.kb.oauth.api.v1.impl;

import dk.kb.oauth.EncryptionHelper;
import dk.kb.oauth.OauthHelper;
import dk.kb.oauth.ProxyHelper;

import dk.kb.oauth.api.v1.BffApi;
import dk.kb.oauth.config.ServiceConfig;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.ServiceException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.util.webservice.ImplBase;


import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;


/**
 * bff
 *
 * <p>bff by the Royal Danish Library
 *
 */

public class BffApiServiceImpl extends ImplBase implements BffApi {
    private final Logger log = LoggerFactory.getLogger(this.toString());

    @Override
    public String authenticate(String returnUrl) throws ServiceException {
        log.debug("authenticate "+httpServletRequest.getRemoteHost());
        String accessTokenString = OauthHelper.getNewAccessToken();
        addCookieToResponse(accessTokenString);
        if (!StringUtils.isEmpty(returnUrl)) {
            try {
                httpServletResponse.sendRedirect(returnUrl);
            } catch (IOException e) {
                log.error("Redirect error ",e);
                throw new InternalServiceException("Invalid returnUrl "+returnUrl);
            }
        }
        return "";
    }

    @GET
    @Path("/proxy/{api}/{path: .*}")
    public StreamingOutput proxyGetRequest(@PathParam("api") String api, @PathParam("path") String path, @CookieParam("Authorization") String authorization) {
        log.debug("proxy request to: '{}/{}' authorization:'{}'",api,path,authorization);
        if (StringUtils.isEmpty(authorization)) {
            sendRedirectToAuthentication();
            return null;
        }
        String accessTokenString = EncryptionHelper.decryptString(authorization);
        if (!verifyAccessTokenString(accessTokenString)) {
            sendRedirectToAuthentication();
            return null;
        }

        URI uri = ProxyHelper.getApiUri(api, path, uriInfo.getRequestUri().getRawQuery());
        HttpURLConnection apiConnection = ProxyHelper.openConnection("GET", uri, httpHeaders, accessTokenString);
        try {
            httpServletResponse.setStatus(apiConnection.getResponseCode());
            httpServletResponse.setHeader("Content-Type", apiConnection.getHeaderField("Content-Type"));
            httpServletResponse.setHeader("Content-Disposition", apiConnection.getHeaderField("Content-Disposition"));
            return ProxyHelper.createStreamingOutput(apiConnection);
        } catch (SocketTimeoutException e) {
                log.warn("Proxy Error: connection timeout uri:'{}'",uri.toString(),e);
                throw new ServiceException("Proxy Error: connection timeout uri:'"+uri.toString(),Response.Status.GATEWAY_TIMEOUT);
        } catch (IOException e) {
                log.warn("Proxy Error: unable to connect uri:'{}'",uri.toString(),e);
                throw new ServiceException("Proxy Error: unable to connect to uri:'"+uri.toString(),Response.Status.BAD_GATEWAY);
        }
    }


    private void addCookieToResponse(String accessTokenString) {
        String cookieString = "Authorization="+EncryptionHelper.encryptString(accessTokenString);
        if (ServiceConfig.getConfig().getBoolean("config.httponly-cookie",true)) {
            cookieString += "; HttpOnly";
        }
        if (ServiceConfig.getConfig().getBoolean("config.secure-cookie",true)) {
            cookieString += "; secure";
        }
        if (ServiceConfig.getConfig().getString("config.cookie-domain",null) !=null ) {
            cookieString += "; domain=";
            cookieString += ServiceConfig.getConfig().getString("config.cookie-domain");
        }
        if (ServiceConfig.getConfig().getString("config.cookie-path",null) !=null ) {
            cookieString += "; path=";
            cookieString += ServiceConfig.getConfig().getString("config.cookie-path");
        }
        cookieString += "; SameSite="+ServiceConfig.getConfig().getString("config.samesite-cookie","Strict");
        httpServletResponse.setHeader("Set-Cookie",cookieString);
    }


    private void sendRedirectToAuthentication() {
        try {
            URIBuilder uriBuilder = new URIBuilder(uriInfo.getBaseUri()+"authenticate");
            if (ServiceConfig.getConfig().getBoolean("config.redirect-after-authentication",false)) {
                uriBuilder.addParameter("returnUrl",uriInfo.getRequestUri().toString());
            }
            httpServletResponse.sendRedirect(uriBuilder.build().toString());
        } catch (IOException | URISyntaxException e) {
            log.error("Error sending redirect",e);
            throw new InternalServiceException();
        }
    }

    private boolean verifyAccessTokenString(String accessTokenString) {
        try {
            AccessToken accessToken = TokenVerifier.create(accessTokenString, AccessToken.class).getToken();
            return Instant.now().getEpochSecond() < accessToken.getExp() - 60;
        } catch (VerificationException e) {
            log.error("Unable to parse access token ", e);
            throw new InternalServiceException();
        }
    }
}
