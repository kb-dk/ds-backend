package dk.kb.present.api.v1.impl;

import dk.kb.present.PresentFacade;
import dk.kb.present.api.v1.*;
import dk.kb.present.model.v1.CollectionDto;

import dk.kb.present.webservice.exception.ServiceException;
import dk.kb.present.webservice.exception.InternalServiceException;

import dk.kb.util.webservice.ImplBase;
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
public class DsPresentApiServiceImpl extends ImplBase implements DsPresentApi {
    private static final Logger log = LoggerFactory.getLogger(DsPresentApiServiceImpl.class);

    /* How to access the various web contexts. See https://cxf.apache.org/docs/jax-rs-basics.html#JAX-RSBasics-Contextannotations */

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
    public StreamingOutput getRecords(String collection, Long mTime, Long maxRecords, String format) {
        if (collection == null) {
            throw new InternalServiceException("collection must be specified but was not");
        }
        long finalMTime = mTime == null ? 0L : mTime;
        long finalMaxRecords = maxRecords == null ? 1000L : maxRecords;
        try {
            return PresentFacade.getRecords(httpServletResponse, collection, finalMTime, finalMaxRecords, format);
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

}
