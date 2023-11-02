package dk.kb.present.api.v1.impl;

import dk.kb.license.client.v1.DsLicenseApi;
import dk.kb.license.invoker.v1.ApiException;
import dk.kb.license.model.v1.CheckAccessForIdsInputDto;
import dk.kb.license.model.v1.CheckAccessForIdsOutputDto;
import dk.kb.license.model.v1.UserObjAttributeDto;
import dk.kb.license.util.DsLicenseClient;
import dk.kb.present.PresentFacade;
import dk.kb.present.api.v1.DsPresentApi;
import dk.kb.present.config.ServiceConfig;
import dk.kb.present.model.v1.CollectionDto;
import dk.kb.present.webservice.exception.ForbiddenServiceException;
import dk.kb.util.webservice.ImplBase;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.StreamingOutput;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ds-present
 *
 * <p>Metadata delivery for the Royal Danish Library 
 *
 */
public class DsPresentApiServiceImpl extends ImplBase implements DsPresentApi {
    private static final Logger log = LoggerFactory.getLogger(DsPresentApiServiceImpl.class);

    // License handling in DsPresentApiServiceImpl as tokens from the request will be needed later on
    private static final String LICENSE_URL_KEY = "config.licensemodule.url";
    private static final String LICENSE_ALLOWALL_KEY = "config.licensemodule.allowall";
    static DsLicenseApi licenseClient;     // Shared between instances
    static boolean licenseAllowAll = ServiceConfig.getConfig().getBoolean(LICENSE_ALLOWALL_KEY, false);
    public static final String RECORD_ACCESS_TYPE = "Search"; // TODO: Evaluate if a specific type is needed

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
            ACCESS access = createAccessChecker(RECORD_ACCESS_TYPE).apply(id);
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
                    createAccessFilter(RECORD_ACCESS_TYPE));
        } catch (Exception e){
            throw handleException(e);
        }
    }

    /**
     * The ds-license client is used to verify access to individual records.
     * @return a ds-license client, ready for use.
     */
    static DsLicenseApi getLicenseClient() {
        if (licenseClient != null) {
          return licenseClient;
        }

        String dsLicenseUrl = ServiceConfig.getConfig().getString(LICENSE_URL_KEY, null);
        if (dsLicenseUrl == null) {
            throw new IllegalStateException("No ds-license URL specified at " + LICENSE_URL_KEY);
        }
        licenseClient = new DsLicenseClient(dsLicenseUrl);
        log.info("Created client for ds-license at URL '{}' with allowall={}", dsLicenseUrl, licenseAllowAll);
        return licenseClient;
    }

    /**
     * Based on user credentials (not used yet as it requires pending OAuth2-integration) and ds-license setup,
     * the produced function return an {@link ACCESS} state for metadata for a given id.
     * @param presentationType as defined in ds-license, e.g. {@code Search}, {@code Stream}, {@code Thumbnails}...
     * @return function for evaluating access to metadata for the current caller.
     */
    static Function<String, ACCESS> createAccessChecker(String presentationType) {
        return id -> {
            if (licenseAllowAll) {
                return ACCESS.ok;
            }

            UserObjAttributeDto everybody = new UserObjAttributeDto()
                    .attribute("everybody")
                    .values(List.of("yes"));

            CheckAccessForIdsInputDto input = new CheckAccessForIdsInputDto()
                    .accessIds(List.of(id))
                    .presentationType(presentationType)
                    .attributes(List.of(everybody));
            CheckAccessForIdsOutputDto response;
            try {
                response = getLicenseClient().checkAccessForIds(input);
            } catch (Exception e) {
                String message = String.format(Locale.ROOT,
                        "Exception calling license server for ID '%s' with attributes %s",
                        id, everybody);
                log.warn(message, e);
                throw new InternalServiceException(message + ". This error has been logged");
            }
            if (response.getAccessIds() != null && response.getAccessIds().contains(id)) {
                return ACCESS.ok;
            }
            if (response.getNonAccessIds() != null && response.getNonAccessIds().contains(id)) {
                return ACCESS.not_allowed;
            }
            if (response.getNonExistingIds() != null && response.getNonExistingIds().contains(id)) {
                return ACCESS.not_exists;
            }
            throw new InternalServiceException("Unable to determine access for '" + id + "'");
        };
    }

    /**
     * Based on user credentials (not used yet as it requires pending OAuth2-integration) and ds-license setup,
     * the produced function isolate the IDs that the caller is allowed to see metadata for.
     * Order is preserved, input is never changed, output is always a new list.
     * @param presentationType as defined in ds-license, e.g. {@code Search}, {@code Stream}, {@code Thumbnails}...
     * @return function converting a list of IDs to allowed IDs.
     */
    private static Function<List<String>, List<String>> createAccessFilter(String presentationType) {
        return ids -> {
            if (licenseAllowAll || ids.isEmpty()) {
                return new ArrayList<>(ids);
            }

            UserObjAttributeDto everybody = new UserObjAttributeDto()
                    .attribute("everybody")
                    .values(List.of("yes"));

            CheckAccessForIdsInputDto input = new CheckAccessForIdsInputDto()
                    .accessIds(ids)
                    .presentationType(presentationType)
                    .attributes(List.of(everybody));
            CheckAccessForIdsOutputDto response;
            try {
                response = getLicenseClient().checkAccessForIds(input);
            } catch (Exception e) {
                String message = String.format(Locale.ROOT,
                        "Exception calling license server for %d IDs with attributes %s. First ID='%s'",
                        ids.size(), everybody, ids.get(0));
                log.warn(message, e);
                throw new InternalServiceException(message + ". This error has been logged");
            }
            if (response.getAccessIds() != null) {
                if (response.getAccessIds().size() == ids.size()) {
                    // New array creation to ensure decoupling of input & output
                    return new ArrayList<>(ids);
                }
                Set<String> allowed = new HashSet<>(response.getAccessIds());
                return ids.stream().filter(allowed::contains).collect(Collectors.toList());
            }
            // TODO: Should a warning be logged here? Will getAccessIds return null if no IDs are allowed?
            return Collections.emptyList();
        };
    }

}
