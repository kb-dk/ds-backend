package dk.kb.oauth.api.v1.impl;

import dk.kb.oauth.OauthHelper;
import dk.kb.oauth.ProxyHelper;

import dk.kb.util.webservice.exception.ServiceException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.util.webservice.ImplBase;
import javax.ws.rs.*;
import javax.ws.rs.core.*;


/**
 * bff
 *
 * <p>bff by the Royal Danish Library
 *
 */

@Path("/")
public class BffApiServiceImpl extends ImplBase {
    private Logger log = LoggerFactory.getLogger(this.toString());



    @GET
    @Path("/authenticate")
    public Response login() throws ServiceException {
        log.debug("Getting cookie");
        NewCookie authzCookie = OauthHelper.getNewAuthzCookie();
        return Response.ok().cookie(authzCookie).build();
    }

    @GET
    @Path("/proxy/{api}/{path: .*}")
    public Response proxyGetRequest(@PathParam("api") String api, @PathParam("path") String path, @CookieParam("Authorization") String authorization, @Context UriInfo uriInfo, @Context HttpHeaders headers) {
        if (StringUtils.isEmpty(authorization)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return ProxyHelper.proxy("GET",api,path,uriInfo.getRequestUri().getRawQuery(),headers,authorization).build();
    }

    @POST
    @Consumes({ "application/json", "application/xml" })
    @Path("proxy/{api}/{path: .*}")
    public Response proxyPostRequest(@PathParam("api") String api, @PathParam("path") String path, @CookieParam("Authorization") String authorization, @Context UriInfo uriInfo, @Context HttpHeaders headers, String body) {
        if (StringUtils.isEmpty(authorization)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return ProxyHelper.proxy("POST",api,path,uriInfo.getRequestUri().getRawQuery(),headers,authorization).build();
    }


}
