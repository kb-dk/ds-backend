package dk.kb.discover.api.v1.impl;

import dk.kb.discover.SolrManager;
import dk.kb.discover.SolrService;
import dk.kb.discover.api.v1.*;
import dk.kb.discover.model.v1.ErrorDto;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import dk.kb.discover.model.v1.StatusDto;

import dk.kb.discover.webservice.BuildInfoManager;
import dk.kb.discover.webservice.exception.InvalidArgumentServiceException;
import dk.kb.discover.webservice.exception.ServiceException;
import dk.kb.discover.webservice.exception.InternalServiceException;

import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.*;

import io.swagger.annotations.Api;

/**
 * ds-discover
 *
 * <p>ds-discover by the Royal Danish Library 
 *
 */
public class DsDiscoverApiServiceImpl implements DsDiscoverApi {
    private Logger log = LoggerFactory.getLogger(this.toString());



    /* How to access the various web contexts. See https://cxf.apache.org/docs/jax-rs-basics.html#JAX-RSBasics-Contextannotations */

    @Context
    private transient UriInfo uriInfo;

    @Context
    private transient SecurityContext securityContext;

    @Context
    private transient HttpHeaders httpHeaders;

    @Context
    private transient Providers providers;

    @Context
    private transient Request request;

    // Disabled as it is always null? TODO: Investigate when it can be not-null, then re-enable with type
    //@Context
    //private transient ContextResolver contextResolver;

    @Context
    private transient HttpServletRequest httpServletRequest;

    @Context
    private transient HttpServletResponse httpServletResponse;

    @Context
    private transient ServletContext servletContext;

    @Context
    private transient ServletConfig servletConfig;

    @Context
    private transient MessageContext messageContext;


    /**
     * Solr [Collection Management Commands](https://solr.apache.org/guide/8_10/collection-management.html)
     * 
     * @param action: Collection-action to perform. Note that some actions require high-level permissions: * [LIST](https://solr.apache.org/guide/8_10/collection-management.html#list) * [DELETE](https://solr.apache.org/guide/8_10/collection-management.html#delete) 
     * 
     * @param name: Mandatory for the action [DELETE](https://solr.apache.org/guide/8_10/collection-management.html#delete) The name of the collection to delete. 
     * 
     * @param async: Optional for the action [DELETE](https://solr.apache.org/guide/8_10/collection-management.html#delete) Request ID to track this action which will be processed asynchronously. 
     * 
     * @return <ul>
      *   <li>code = 200, message = "JSON structure with Solr collection action response", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public String collectionAction(String action, String name, String async) throws ServiceException {
        try {
            String response = "MnOL42";
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

    /**
     * Solr [Configsets Commands](https://solr.apache.org/guide/8_10/configsets-api.html)
     * 
     * @param action: Configset-action to perform. Note that some actions require high-level permissions: * [LIST](https://solr.apache.org/guide/8_10/configsets-api.html#configsets-list) * [DELETE](https://solr.apache.org/guide/8_10/configsets-api.html#configsets-delete) 
     * 
     * @param name: Mandatory for the action [DELETE](https://solr.apache.org/guide/8_10/configsets-api.html#configsets-delete) The name of the configset to delete. 
     * 
     * @return <ul>
      *   <li>code = 200, message = "JSON structure with Solr configset action response", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public String configAction(String action, String name) throws ServiceException {
        // TODO: Implement...
    
        
        try { 
            String response = "BQBHQ5M0";
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

    /**
     * Ping the server to check if the server is reachable.
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK", response = String.class</li>
      *   <li>code = 406, message = "Not Acceptable", response = ErrorDto.class</li>
      *   <li>code = 500, message = "Internal Error", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public String ping() throws ServiceException {
        try {
            return "pong";
        } catch (Exception e){
            throw handleException(e);
        }
    
    }


    /**
     * Perform a Solr-compatible search in the stated collection
     * 
     * @param collection: The ID of the Solr collection to search. Available collections can be requested from /solr/admin/collections
     * 
     * @param q: Solr query param [https://solr.apache.org/guide/8_10/the-standard-query-parser.html#standard-query-parser-parameters](q)
     * 
     * @return <ul>
      *   <li>code = 200, message = "JSON structure with Solr response", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public String solrSearch(String collection, String q, List<String> fq, Integer rows, String fl, String facet, List<String> facetField, String qOp, String wt, String version, String indent, String debug, String debugExplainStructured) {
        Map<String, String[]> extra = getExtraParams();
        if (!extra.isEmpty()) {
            throw new InvalidArgumentServiceException("Unsupported parameters: " + extra.keySet());
        }
        SolrService solr = SolrManager.getSolrService(collection);
        try {
            // TODO: Pass the map of request parameters instead of all parameters as first class
            httpServletResponse.setContentType(solr.getResponseMIMEType(wt)); // Needed by SolrJ
            return solr.query(q, fq, rows, fl, facet, facetField, qOp, wt, version, indent, debug, debugExplainStructured);
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

    /**
     * Subtracts parameters defined for the called endpoint from the total set of parameters in the called URI,
     * resulting in a map of unhendled parameters.
     * @return a map of unhandled parameters.
     */
    private Map<String, String[]> getExtraParams() {
        // All query params from the client call
        Map<String, String[]> extras = new HashMap<>(httpServletRequest.getParameterMap());

        // Iterate defined params and remove them from the client params, leaving unhandled params
        // JAXRSUtils.getCurrentMessage() uses ThreadLocal, so it must be called from the caller Thread
        JAXRSUtils.getCurrentMessage().getExchange().get(OperationResourceInfo.class).getParameters().stream()
                .filter(param -> param.getType() == ParameterType.QUERY)
                .map(Parameter::getName)
                .forEach(extras::remove);
        return extras;
    }

    /**
     * Detailed status / health check for the service
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK", response = StatusDto.class</li>
      *   <li>code = 500, message = "Internal Error", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public StatusDto status() throws ServiceException {
        String host = "N/A";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("Exception resolving hostname", e);
        }
        return new StatusDto()
                .application(BuildInfoManager.getName())
                .version(BuildInfoManager.getVersion())
                .build(BuildInfoManager.getBuildTime())
                .java(System.getProperty("java.version"))
                .heap(Runtime.getRuntime().maxMemory()/1000000L)
                .server(host)
                .health("ok");
    }


    /**
    * This method simply converts any Exception into a Service exception
    * @param e: Any kind of exception
    * @return A ServiceException
    * @see dk.kb.discover.webservice.ServiceExceptionMapper
    */
    private ServiceException handleException(Exception e) {
        if (e instanceof ServiceException) {
            return (ServiceException) e; // Do nothing - this is a declared ServiceException from within module.
        } else {// Unforseen exception (should not happen). Wrap in internal service exception
            log.error("ServiceException(HTTP 500):", e); //You probably want to log this.
            return new InternalServiceException(e.getMessage());
        }
    }

}
