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
package dk.kb.image.util;

import dk.kb.image.client.v1.AccessApi;
import dk.kb.image.invoker.v1.ApiClient;
import dk.kb.image.invoker.v1.ApiException;
import dk.kb.image.invoker.v1.Configuration;
import dk.kb.image.model.v1.DeepzoomDZIDto;
import dk.kb.image.model.v1.IIIFInfoDto;
import dk.kb.image.model.v1.ThumbnailsDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.List;

/**
 * Client for the service. Intended for use by other projects that calls this service.
 * See the {@code README.md} for details on usage.
 * </p>
 * This class is not used internally.
 * </p>
 * The client is Thread safe and handles parallel requests independently.
 * It is recommended to persist the client and to re-use it between calls.
 */
public class DsImageClient extends AccessApi {
    private static final Logger log = LoggerFactory.getLogger(DsImageClient.class);

    /**
     * Creates a client for the service.
     * @param serviceURI the URI for the service, e.g. {@code https://example.com/ds-image/v1}.
     */
    public DsImageClient(String serviceURI) {
        super(createClient(serviceURI));
        log.info("Created OpenAPI client for '{}'", serviceURI);
    }

    /**
     * Deconstruct the given URI and use the components to create an ApiClient.
     * @param serviceURIString an URI to a service.
     * @return an ApiClient constructed from the serviceURIString.
     */
    private static ApiClient createClient(String serviceURIString) {
        log.debug("Creating OpenAPI client with URI '{}'", serviceURIString);

        URI serviceURI = URI.create(serviceURIString);
        // No mechanism for just providing the full URI. We have to deconstruct it
        return Configuration.getDefaultApiClient().
                setScheme(serviceURI.getScheme()).
                setHost(serviceURI.getHost()).
                setPort(serviceURI.getPort()).
                setBasePath(serviceURI.getRawPath());
    }
    
    @Deprecated
    @Override
    public DeepzoomDZIDto getDeepzoomDZI (String imageid) throws ApiException {
        throw new ApiException(403, "Method getDeepzoomDZI not allowed to be called on DsImageClient");         
        
    }
    
    @Override
    public File getDeepzoomTile (String imageid, Integer layer, String tiles, String format, Float CNT, Float GAM, String CMP, String CTW, Boolean INV, String COL) throws ApiException {
        throw new ApiException(403, "Method getDeepzoomTile not allowed to be called on DsImageClient");
    }
    
    @Override
    public IIIFInfoDto getImageInformation (String identifier, String format) throws ApiException {
        throw new ApiException(403, "Method getImageInformation not allowed to be called on DsImageClient");
        
    }
    
    @Override
    public File iIIFImageRequest (String identifier, String region, String size, String rotation, String quality, String format) throws ApiException {
        throw new ApiException(403, "Method iIIFImageRequest not allowed to be called on DsImageClient");
    }
    
    @Override
    public File iIPImageRequest (String FIF, Long WID, Long HEI, List<Float> RGN, Integer QLT, Float CNT, String ROT, Float GAM, String CMP, String PFL, String CTW, Boolean INV, String COL, List<Integer> JTL, List<Integer> PTL, String CVT) throws ApiException {
        throw new ApiException(403, "Method iIPImageRequest not allowed to be called on DsImageClient");        
    }
    
    @Override
    public ThumbnailsDto kalturaThumbnails (String fileId, Integer numberOfThumbnails, Integer secondsStartSeek, Integer secondsEndSeek, Integer width, Integer height) throws ApiException {
        throw new ApiException(403, "Method kalturaThumbnails not allowed to be called on DsImageClient");   
    }
}
