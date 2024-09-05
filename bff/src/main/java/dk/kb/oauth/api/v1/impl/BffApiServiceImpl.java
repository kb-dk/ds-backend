package dk.kb.oauth.api.v1.impl;

import dk.kb.oauth.EncryptionHelper;
import dk.kb.oauth.OauthHelper;
import dk.kb.oauth.ProxyHelper;

import dk.kb.oauth.api.v1.BffApi;
import dk.kb.oauth.config.ServiceConfig;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.ServiceException;

import dk.kb.util.yaml.YAML;
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
        log.debug("authenticate "+httpServletRequest.getRemoteHost()+"return url "+returnUrl);
        String accessTokenString = OauthHelper.getNewAccessToken();
        addCookieToResponse(accessTokenString);
        return "";
    }

    @GET
    @Path("/proxy/{api}/{path: .*}")
    public StreamingOutput proxyGetRequest(@PathParam("api") String api, @PathParam("path") String path, @CookieParam("Authorization") String authorization) {
        log.debug("proxy request to: '{}/{}' authorization:'{}'",api,path,authorization);
        if (StringUtils.isEmpty(authorization)) {
            log.debug("authorization is empty");
            throw new ServiceException("Authorization missing",Response.Status.UNAUTHORIZED);
        }
        String accessTokenString = EncryptionHelper.decryptString(authorization);
        if (!verifyAccessTokenString(accessTokenString)) {
            log.debug("expired authorization");
            throw new ServiceException("Expired",Response.Status.UNAUTHORIZED);
        }

        URI uri = ProxyHelper.getApiUri(api, path, uriInfo.getRequestUri().getRawQuery());
        try {
            HttpURLConnection apiConnection = ProxyHelper.openConnection("GET", uri, httpHeaders, accessTokenString);
            int status = apiConnection.getResponseCode();
            httpServletResponse.setStatus(apiConnection.getResponseCode());
            httpServletResponse.setHeader("Content-Type", apiConnection.getHeaderField("Content-Type"));
            httpServletResponse.setHeader("Content-Disposition", apiConnection.getHeaderField("Content-Disposition"));
            if (status >= 400) {
                // API Error
                return ProxyHelper.createStreamingOutput(apiConnection.getErrorStream());
            }
            return ProxyHelper.createStreamingOutput(apiConnection.getInputStream());

        } catch (SocketTimeoutException e) {
                log.warn("Proxy Error: connection timeout uri:'{}'",uri.toString(),e);
                throw new ServiceException("Proxy Error: connection timeout uri:'"+uri.toString(),Response.Status.GATEWAY_TIMEOUT);
        } catch (IOException e) {
                log.warn("Proxy Error: unable to read data uri:'{}'",uri.toString(),e);
                throw new ServiceException("Proxy Error: unable to connect to uri:'"+uri.toString(),Response.Status.BAD_GATEWAY);
        }
    }


    private void addCookieToResponse(String accessTokenString) {
        final YAML cookieConf = ServiceConfig.getConfig().getSubMap("cookies");
        String cookieString = "Authorization="+EncryptionHelper.encryptString(accessTokenString);

        if (cookieConf.getBoolean("httponly",true)) {
            cookieString += "; HttpOnly";
        }
        if (cookieConf.getBoolean("secure",true)) {
            cookieString += "; secure";
        }
        if (cookieConf.containsKey("domain")) {
            cookieString += "; domain=";
            cookieString += cookieConf.getString("domain");
        }
        if (cookieConf.containsKey("path")) {
            cookieString += "; path=";
            cookieString += cookieConf.getString("path");
        } else {
            cookieString += "; path=";
            cookieString += this.servletContext.getContextPath();
        }
        cookieString += "; SameSite="+cookieConf.getString("samesite","Strict");
        httpServletResponse.setHeader("Set-Cookie",cookieString);
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
