package dk.kb.oauth.api.v1.impl;

import dk.kb.oauth.OauthHelper;
import dk.kb.oauth.ProxyHelper;

import dk.kb.oauth.api.v1.BffApi;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.ServiceException;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.util.webservice.ImplBase;

import javax.servlet.http.Cookie;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Instant;


/**
 * bff
 *
 * <p>bff by the Royal Danish Library
 *
 */

public class BffApiServiceImpl extends ImplBase implements BffApi {
    private Logger log = LoggerFactory.getLogger(this.toString());


    public String authenticate() throws ServiceException {
        Cookie authzCookie = OauthHelper.getNewAuthzCookie();
        httpServletResponse.addCookie(authzCookie);
        return "";
    }

    @GET
    @Path("/proxy/{api}/{path: .*}")
    public StreamingOutput proxyGetRequest(@PathParam("api") String api, @PathParam("path") String path, @CookieParam("Authorization") String authorization) {
        if (StringUtils.isEmpty(authorization)) {
            sendRedirectToAuthentication();
            return null;
        }
        String accessTokenString = authorization;
        if (!verifyAccessTokenString(accessTokenString)) {
            sendRedirectToAuthentication();
            return null;
        }

        try {
            URI uri = ProxyHelper.getApiUri(api, path, uriInfo.getRequestUri().getRawQuery());
            HttpURLConnection apiConnection = ProxyHelper.openConnection("GET", uri, httpHeaders, authorization);
            httpServletResponse.setStatus(apiConnection.getResponseCode());
            httpServletResponse.setHeader("Content-Type", apiConnection.getHeaderField("Content-Type"));
            httpServletResponse.setHeader("Content-Disposition", apiConnection.getHeaderField("Content-Disposition"));
            return ProxyHelper.createStreamingOutput(apiConnection);
        } catch (IOException e) {
            log.error("IOEXception",e);
            throw new ServiceException(Response.Status.BAD_GATEWAY);
        }
    }

    private void sendRedirectToAuthentication() {
        try {
            httpServletResponse.sendRedirect(uriInfo.getBaseUri()+"/authentication");
        } catch (IOException e) {
            log.error("Error sending redirect",e);
            throw new InternalServiceException();
        }
    }

    private boolean verifyAccessTokenString(String accessTokenString) {
        try {
            AccessToken accessToken = TokenVerifier.create(accessTokenString, AccessToken.class).getToken();
            return Instant.now().getEpochSecond() > accessToken.getExp() - 60;
        } catch (VerificationException e) {
            log.error("Unable to parse access token ", e);
            throw new InternalServiceException();
        }
    }
}
