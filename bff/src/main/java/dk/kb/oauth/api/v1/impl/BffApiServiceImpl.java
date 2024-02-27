package dk.kb.oauth.api.v1.impl;

import dk.kb.oauth.EncryptionHelper;
import dk.kb.oauth.OauthHelper;
import dk.kb.oauth.ProxyHelper;

import dk.kb.oauth.api.v1.BffApi;
import dk.kb.oauth.config.ServiceConfig;
import dk.kb.util.webservice.exception.ServiceException;

import org.apache.commons.lang3.StringUtils;
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


/**
 * bff
 *
 * <p>bff by the Royal Danish Library
 *
 */

public class BffApiServiceImpl extends ImplBase implements BffApi {
    private final Logger log = LoggerFactory.getLogger(this.toString());


    public String authenticate() throws ServiceException {
        String accessTokenString = OauthHelper.getNewAccessToken();
        addCookieToResponse(accessTokenString);
        return "";
    }

    @GET
    @Path("/proxy/{api}/{path: .*}")
    public StreamingOutput proxyGetRequest(@PathParam("api") String api, @PathParam("path") String path, @CookieParam("Authorization") String encryptedAccessToken) {
        if (StringUtils.isEmpty(encryptedAccessToken)) {
            throw new ServiceException(Response.Status.UNAUTHORIZED);
        }
        String accessTokenString = EncryptionHelper.decryptString(encryptedAccessToken);

        try {
            URI uri = ProxyHelper.getApiUri(api, path, uriInfo.getRequestUri().getRawQuery());
            HttpURLConnection apiConnection = ProxyHelper.openConnection("GET", uri, httpHeaders, accessTokenString);
            httpServletResponse.setStatus(apiConnection.getResponseCode());
            httpServletResponse.setHeader("Content-Type", apiConnection.getHeaderField("Content-Type"));
            httpServletResponse.setHeader("Content-Disposition", apiConnection.getHeaderField("Content-Disposition"));
            return ProxyHelper.createStreamingOutput(apiConnection);
        } catch (IOException e) {
            log.error("IO EXception",e);
            throw new ServiceException(Response.Status.BAD_GATEWAY);
        }
    }


    private void addCookieToResponse(String accessTokenString) {
        Cookie newCookie = new Cookie("Authorization", EncryptionHelper.encryptString(accessTokenString));
        newCookie.setSecure(ServiceConfig.getConfig().getBoolean("config.use-secure-cookie",true));
        newCookie.setHttpOnly(true);
        httpServletResponse.addCookie(newCookie);
    }

}
