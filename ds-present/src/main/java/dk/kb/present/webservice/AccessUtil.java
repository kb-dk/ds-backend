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
package dk.kb.present.webservice;

import dk.kb.license.client.v1.DsLicenseApi;
import dk.kb.license.model.v1.CheckAccessForIdsInputDto;
import dk.kb.license.model.v1.CheckAccessForIdsOutputDto;
import dk.kb.license.model.v1.UserObjAttributeDto;
import dk.kb.license.util.DsLicenseClient;
import dk.kb.present.api.v1.impl.DsPresentApiServiceImpl;
import dk.kb.present.config.ServiceConfig;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helpers for determining access conditions for material, based on caller credentials and ds-license setup.
 */
public class AccessUtil {
    private static final Logger log = LoggerFactory.getLogger(AccessUtil.class);

    public static final String HEADER_SIMULATED_GROUP = "Simulated-OAuth2-Group";
    public static final String GROUP_INTERNAL_SERVICE = "internal_service";
    public static final String GROUP_ADMIN = "admin";

    private static final String LICENSE_URL_KEY = "licensemodule.url"; // Used for creating licenseClient
    private static final String LICENSE_ALLOWALL_KEY = "licensemodule.allowall";

    public static DsLicenseApi licenseClient;     // Shared between instances
    public static boolean licenseAllowAll = ServiceConfig.getConfig().getBoolean(LICENSE_ALLOWALL_KEY, false);

    /**
     * Based on user credentials (not used yet as it requires pending OAuth2-integration) and ds-license setup,
     * the produced function return an {@link DsPresentApiServiceImpl.ACCESS} state for metadata for a given id.
     *
     * @param groups token derived user groups.
     * @param presentationType as defined in ds-license, e.g. {@code Search}, {@code Stream}, {@code Thumbnails}...
     * @return function for evaluating access to metadata for the current caller.
     */
    // Note: When OAuth2 support is added, this method should probably be extended with a HttpServletRequest parameter
    public static Function<String, DsPresentApiServiceImpl.ACCESS> createAccessChecker(
            Set<String> groups, String presentationType) {
        return id -> {
            if (licenseAllowAll ||
                    groups.contains(GROUP_ADMIN) || groups.contains(GROUP_INTERNAL_SERVICE)) {
                return DsPresentApiServiceImpl.ACCESS.ok;
            }

            // Temporary singular role while we wait for OAuth2
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
                return DsPresentApiServiceImpl.ACCESS.ok;
            }
            if (response.getNonAccessIds() != null && response.getNonAccessIds().contains(id)) {
                return DsPresentApiServiceImpl.ACCESS.not_allowed;
            }
            if (response.getNonExistingIds() != null && response.getNonExistingIds().contains(id)) {
                return DsPresentApiServiceImpl.ACCESS.not_exists;
            }
            throw new InternalServiceException("Unable to determine access for '" + id + "'");
        };
    }

    /**
     * Based on user credentials (not used yet as it requires pending OAuth2-integration) and ds-license setup,
     * the produced function isolate the IDs that the caller is allowed to see metadata for.
     * Order is preserved, input is never changed, output is always a new list.
     * @param groups token derived user groups.
     * @param presentationType as defined in ds-license, e.g. {@code Search}, {@code Stream}, {@code Thumbnails}...
     * @return function converting a list of IDs to allowed IDs.
     */
    // Note: When OAuth2 support is added, this method should probably be extended with a HttpServletRequest parameter
    public static Function<List<String>, List<String>> createAccessFilter(
            Set<String> groups, String presentationType) {
        return ids -> {
            if (licenseAllowAll || ids.isEmpty() ||
                    groups.contains(GROUP_ADMIN) || groups.contains(GROUP_INTERNAL_SERVICE)) {
                return new ArrayList<>(ids);
            }

            // Temporary singular role while we wait for OAuth2
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
     * @param httpHeaders HTTP headers from the initiating webservice call.
     * @return true is the webservice call has a {@link #HEADER_SIMULATED_GROUP} token.
     */
    public static boolean hasAuthorization(HttpHeaders httpHeaders) {
        return httpHeaders.getRequestHeader(HEADER_SIMULATED_GROUP) != null;
    }

    /**
     * Important: This is a place holder until OAuth2-support is added.
     * This has zero security checks and only exists for testing token based access flows.
     * @param httpHeaders HTTP headers from the initiating webservice call.
     * @return the groups that the user belongs to or empty if there are no associated groups.
     */
    public static Set<String> getGroups(HttpHeaders httpHeaders) {
        List<String> groupHeaders;
        if (httpHeaders == null || (groupHeaders = httpHeaders.getRequestHeader(HEADER_SIMULATED_GROUP)) == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(groupHeaders);
    }
}
