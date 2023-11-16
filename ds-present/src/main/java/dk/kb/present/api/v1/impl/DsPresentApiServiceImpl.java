package dk.kb.present.api.v1.impl;

import dk.kb.present.PresentFacade;
import dk.kb.present.api.v1.DsPresentApi;
import dk.kb.present.model.v1.CollectionDto;
import dk.kb.present.webservice.AccessUtil;
import dk.kb.present.webservice.exception.ForbiddenServiceException;
import dk.kb.util.webservice.ImplBase;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.NotFoundServiceException;
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

    // "Search" is best guess for the access type for now
    // This might be changed when we make a major evaluation of the license system
    public static final String RECORD_ACCESS_TYPE = "Search";

    /**
     * The different types of access for a given material.
     */
    public enum ACCESS {
        /**
         * Material exists and the caller is allowed to access it.
         */
        ok,
        /**
         * Material exists but the caller is not allowed to access it.
         */
        not_allowed,
        /**
         * The material does not exist in the system.
         */
        not_exists}

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
            // Allowed for everyone
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
            // Allowed for everyone
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
            ACCESS access = AccessUtil.createAccessChecker(RECORD_ACCESS_TYPE).apply(id);
            switch (access) {
                case ok:
                    return PresentFacade.getRecord(id, format);
                case not_allowed:
                    // TODO: Log access tokens or roles when available
                    log.debug("getRecord(id='{}', format='{}'): User access not allowed", id, format);
                    throw new ForbiddenServiceException("User not allowed to retrieve metadata for '" + id + "'");
                case not_exists:
                    log.debug("getRecord(id='{}', format='{}'): Not found", id, format);
                    throw new NotFoundServiceException("The material '" + id + "' could not be found");
                default:
                    throw new UnsupportedOperationException("The access condition '" + access + "' is unsupported");
            }
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
            return PresentFacade.getRecords(
                    httpServletResponse, collection, finalMTime, finalMaxRecords, format,
                    AccessUtil.createAccessFilter(RECORD_ACCESS_TYPE));
        } catch (Exception e){
            throw handleException(e);
        }
    }

    @Override
    public StreamingOutput getRawRecords(String origin, Long mTime, Long maxRecords, Boolean asJsonLines) {
        log.debug("getRawRecords(origin='{}', mTime={}, maxRecords={}, asJsonLines={}) called with call details: {}",
                origin, mTime, maxRecords, getCallDetails(), asJsonLines);
        if (origin == null) {
            throw new InternalServiceException("origin must be specified but was not");
        }
        if (asJsonLines == null){
            asJsonLines = false;
        }
        try {
            long finalMTime = mTime == null ? 0L : mTime;
            long finalMaxRecords = maxRecords == null ? 1000L : maxRecords;
            return PresentFacade.getRecordsRaw(
                    httpServletResponse, origin, finalMTime, finalMaxRecords,
                    AccessUtil.createAccessFilter(RECORD_ACCESS_TYPE), asJsonLines);
        } catch (Exception e){
            throw handleException(e);
        }
    }

}
