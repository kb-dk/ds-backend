/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.util.webservice;

import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.stream.Collectors;

/**
 * Provides glue and supporting methods for OpenAPI impl-classes. The impl-classes must extend ImplBase in order
 * to populate the transients.
 */
public abstract class ImplBase {
    private static final Logger log = LoggerFactory.getLogger(ImplBase.class);

    /* How to access the various web contexts. See https://cxf.apache.org/docs/jax-rs-basics.html#JAX-RSBasics-Contextannotations */

    @Context
    protected transient UriInfo uriInfo;

    @Context
    protected transient SecurityContext securityContext;

    @Context
    protected transient HttpHeaders httpHeaders;

    @Context
    protected transient Providers providers;

    @Context
    protected transient Request request;

    // Disabled as it is always null? TODO: Investigate when it can be not-null, then re-enable with type
    //@Context
    //protected transient ContextResolver contextResolver;

    @Context
    protected transient HttpServletRequest httpServletRequest;

    @Context
    protected transient HttpServletResponse httpServletResponse;

    @Context
    protected transient ServletContext servletContext;

    @Context
    protected transient ServletConfig servletConfig;

    @Context
    protected transient MessageContext messageContext;

    /**
     * Exception wrapper that logs the given Exception together with information on the endpoint call.
     *
     * If the given exception is inherited from {@link ServiceException}, it is wrapped in a new ServiceException,
     * else it is wrapped in a {@link InternalServiceException}. In both cases the message will state the endpoint
     * and the parameters from the client call.
     * Also, a warning is logged, stating the endpoint and the parameters plus the given exception.
     *
     * This is a shortcut for {@code handleException(e, true, true)}.
     * @param e: any kind of exception.
     * @return a ServiceException stating endpoint and parameters.
     */
    protected ServiceException handleException(Exception e) {
        return handleException(e, true, true);
    }

    /**
     * Exception wrapper with flexible handling.
     *
     * If the given Exception is not inherited from {@link ServiceException}, it is wrapped in a new ServiceException,
     * with the message stating the endpoint and parameters from the client call.
     * If the Exception is a ServiceException, it is wrapped in a similar fashion if WrapServiceExceptions is true,
     * else it is returned as-is.
     * @param e: any kind of exception.
     * @param logWarning if true, a warning is logged, stating the endpoint and the parameters plus the given exception.
     *                   If the given Exception is not a ServiceException, it is always logged.
     * @param wrapServiceExceptions if true, all Exceptions are wrapped, even if they are already ServiceExceptions.
     * @return a ServiceException stating endpoint and parameters, as well as accept-header.
     */
    private ServiceException handleException(Exception e, boolean logWarning, boolean wrapServiceExceptions) {
        final String call = getCallDetails();
        if (logWarning || !(e instanceof ServiceException)) {
            log.warn("Exception processing " + call, e);
        }

        final String eMessage = "Exception processing " + call + ": " + e.getMessage();
        if (e instanceof ServiceException) {
            ServiceException se = (ServiceException)e;
            return wrapServiceExceptions ? se.extend(eMessage) : se;
        }
        // Unforeseen exception (should not happen). Wrap in internal service exception
        return new InternalServiceException(eMessage, e);
    }

    /**
     * Returns the current endpoint and all parameters, including parameters that the endpoint does not support.
     * Intended for exceptions and debugging.
     * @return current endpoint and parameters, as well as accept-header.
     */
    public String getCallDetails() {
        String method = httpServletRequest.getMethod(); // GET, DELETE...
        String context = httpServletRequest.getContextPath(); // java-webapp
        String mapping = httpServletRequest.getHttpServletMapping().getMatchValue(); // v1
        String endpoint = httpServletRequest.getPathInfo(); // /hello
        String params = httpServletRequest.getParameterMap().entrySet().stream()
                .map(param -> param.getKey() + "=" + Arrays.toString(param.getValue()))
                .collect(Collectors.joining("&"));
        StringBuilder accepts = new StringBuilder();
        Enumeration<String> aHeaders = httpServletRequest.getHeaders("Accept");
        while (aHeaders.hasMoreElements()) {
            if (accepts.length() > 0) {
                accepts.append(", ");
            }
            accepts.append(aHeaders.nextElement());
        }


        boolean mappingEqualsEndpoint = compareMappingWithEndpoint(mapping, endpoint);

        if (mappingEqualsEndpoint){
            if (!endpoint.startsWith("/")){
                endpoint = "/" + endpoint;
            }

            return method + " " + context +  endpoint + (params.isEmpty() ? "" : "?" + params) +
                    " accepts: " + accepts;
        }

        return method + " " + context + "/" + mapping + endpoint + (params.isEmpty() ? "" : "?" + params) +
                    " accepts: " + accepts;
    }

    private boolean compareMappingWithEndpoint(String mapping, String endpoint) {

        if (mapping != null && endpoint != null){
            String cleanMapping = mapping.replaceAll("/", "");
            String cleanEndpoint = endpoint.replaceAll("/", "");
            return cleanMapping.equals(cleanEndpoint);
        }

        log.debug("Either mapping or endpoint was null, so they can't be compared. Mapping: '{}', Endpoint: '{}'", mapping, endpoint);
        return false;
    }

    /**
     * Set the {@code Content-Disposition} header, used for signalling display/download behaviour as well as filename
     * to the caller, e.g. a browser.
     *
     * The combination of {@code downloadInSwaggerGUI=false} and {@code downloadInBrowser=true} is not supported.
     * @param filename the filename to signal to the caller.
     * @param downloadInSwaggerGUI if true, a download link will be displayed in the Swagger GUI instead of displaying
     *                             the content inline.
     * @param downloadInBrowser    if true, a download dialog will be suggested to the browser instead of displaying
     *                             the content inline.
     */
    protected void setFilename(String filename, boolean downloadInSwaggerGUI, boolean downloadInBrowser) {
        if (downloadInSwaggerGUI) {
            if (downloadInBrowser) {
                // Show download link in Swagger UI, download dialog when opened directly in browser
                httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            } else {
                // Show download link in Swagger UI, inline when opened directly in browser
                // https://github.com/swagger-api/swagger-ui/issues/3832
                httpServletResponse.setHeader(
                        "Content-Disposition", "inline; swaggerDownload=\"attachment\"; filename=\"" + filename + "\"");
            }
        } else {
            // Show inline in Swagger UI, inline when opened directly in browser
            httpServletResponse.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
        }
    }

}
