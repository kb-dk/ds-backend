package dk.kb.present.api.v1.impl;

import dk.kb.present.PresentFacade;
import dk.kb.present.api.v1.*;
import dk.kb.present.model.v1.CollectionDto;

import dk.kb.present.webservice.exception.ServiceException;
import dk.kb.present.webservice.exception.InternalServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Providers;

import org.apache.cxf.jaxrs.ext.MessageContext;

/**
 * ds-present
 *
 * <p>Metadata delivery for the Royal Danish Library 
 *
 */
public class DsPresentApiServiceImpl implements DsPresentApi {
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
     * Retrieve a formal description of a single collection
     * 
     * @param id: The ID of the collection
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK: The collection was known and a description is returned", response = CollectionDto.class</li>
      *   <li>code = 404, message = "Collection is unknown", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public CollectionDto getCollection(String id) throws ServiceException {
        try {
            return PresentFacade.getCollection(id);
        } catch (Exception e){
            throw handleException(e);
        }
    }

    /**
     * Retrieve a formal description of all collections
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK: Collections are returned", response = CollectionDto.class, responseContainer = "List"</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public List<CollectionDto> getCollections() throws ServiceException {
        try {
            return PresentFacade.getCollections();
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

    /**
     * Retrieve metadata for the record with the given ID and in the given format
     * 
     * @param id: The ID of the record
     * 
     * @param format: The delivery format for the record: * JSON-LD: [Linked Data in JSON](https://json-ld.org/) (default) * MODS: [Metadata Object Description Schema[(http://www.loc.gov/standards/mods/) * SolrXML: [Solr XML Formatted Index Updates)(https://solr.apache.org/guide/8_8/uploading-data-with-index-handlers.html#xml-formatted-index-updates) * raw: Metadata unchanged from the source. 
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK: The record was available in the requested format", response = String.class</li>
      *   <li>code = 404, message = "Record ID is unknown", response = ErrorDto.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public String getRecord(String id, String format) throws ServiceException {
        try {
            return PresentFacade.getRecord(id, format);
        } catch (Exception e){
            throw handleException(e);
        }
    }

    @Override
    public StreamingOutput getRecords(String recordBase, Long mTime, Long maxRecords, String format) {
        if (recordBase == null) {
            throw new InternalServiceException("recordBase must be specified but was not");
        }
        long finalMTime = mTime == null ? 0L : mTime;
        long finalMaxRecords = maxRecords == null ? 1000L : maxRecords;
        try {
            return PresentFacade.getRecords(httpServletResponse, recordBase, finalMTime, finalMaxRecords, format);
        } catch (Exception e){
            throw handleException(e);
        }
    }

    /**
     * Ping the server to check if the server is reachable.
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK", response = String.class</li>
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
    * This method simply converts any Exception into a Service exception
    * @param e: Any kind of exception
    * @return A ServiceException
    * @see dk.kb.present.webservice.ServiceExceptionMapper
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
