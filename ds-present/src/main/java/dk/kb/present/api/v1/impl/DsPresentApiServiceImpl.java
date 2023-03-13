package dk.kb.present.api.v1.impl;

import dk.kb.present.PresentFacade;
import dk.kb.present.api.v1.DsPresentApi;
import dk.kb.present.model.v1.CollectionDto;
import dk.kb.util.webservice.ImplBase;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.StreamingOutput;
import java.util.List;

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
            log.debug("() called with call details: {}", getCallDetails());
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
            log.debug("getCollections() called with call details: {}", getCallDetails());
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
            log.debug("getRecord(id='{}', format='{}') called with call details: {}", id, format, getCallDetails());
            return PresentFacade.getRecord(id, format);
        } catch (Exception e){
            throw handleException(e);
        }
    }

    @Override
    public StreamingOutput getRecords(String collection, Long mTime, Long maxRecords, String format) {
        log.debug("getRecords(collection='{}', mTime={}, maxRecords={}, format='{}') called with call details: {}",
                  collection, mTime, maxRecords, format, getCallDetails());
        if (collection == null) {
            throw new InternalServiceException("collection must be specified but was not");
        }
        try {
            long finalMTime = mTime == null ? 0L : mTime;
            long finalMaxRecords = maxRecords == null ? 1000L : maxRecords;
            return PresentFacade.getRecords(httpServletResponse, collection, finalMTime, finalMaxRecords, format);
        } catch (Exception e){
            throw handleException(e);
        }
    }

}
