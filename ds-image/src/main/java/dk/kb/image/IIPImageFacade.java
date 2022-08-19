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
package dk.kb.image;

import dk.kb.image.config.ServiceConfig;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;

/**
 * Proxy for an image server that supports the same APIs as <a href="https://iipimage.sourceforge.io/">IIPImage</a>.
 *
 * Supported APIs are IIP, IIIF image, Deepzoom and Zoomify.
 *
 */
public class IIPImageFacade {
    private static final Logger log = LoggerFactory.getLogger(IIPImageFacade.class);

    private static IIPImageFacade instance;

    public static final String KEY_IIP_SERVER = "config.imageservers.iip";
    public static final String KEY_IIIF_SERVER = "config.imageservers.iiif";

    public static synchronized IIPImageFacade getInstance() {
        if (instance == null) {
            instance = new IIPImageFacade();
        }
        return instance;
    }

    private IIPImageFacade() {
        log.info("Created " + this);
    }


    /**
     * Internet Imaging Protocol available through OpenAPI specification. The specification can be found at: https://iipimage.sourceforge.io/documentation/protocol/
     *
     * @param requestURI full request URI, used for logging af debugging.
     *
     * @param FIF: Full image path. If the FILESYSTEM_PREFIX server parameter has been set, the path is relative to that path.  Note that all IIP protocol requests must start with the FIF parameter
     *
     * @param WID: Specify width in pixels, w, for exports with CVT request.  **WID requires CVT**
     *
     * @param HEI: Specify height in pixels, h, for exports with CVT requests  **HEI requires CVT**
     *
     * @param RGN: Define a region of interest starting at relative coordinates x,y with width w and height h.  For use with CVT requests. All values should be ratios in the range 0 – 1.0    **RGN has to be defined as x,y,w,h and requires CVT, WID &amp; HEI**
     *
     * @param QLT: Set the output compression level, q.    Valid ranges are JPEG: 0-100 and for PNG: 0-9 where a higher value means more compression  JPEG is always lossy even with a value of 100, while PNG is lossles.
     *
     * @param CNT: Contrast adjustment: multiplication of pixel values by factor, c. Value should be an integer or float &gt; 0. A value of 1.0 indicates no contrast change
     *
     * @param SHD: Simulated hill-shading for image normal data. The argument is the angle of incidence of the light source in the horizontal plane (from 12 o’clock), h and the vertical angle of incidence, v, with 0 representing a horizontal direction and -1 vertically downwards  **SHD has to be defined as h,v**
     *
     * @param LYR: The number of quality layers, l, in an image to decode. This is for file types that can contain multiple quality layers, such as JPEG2000.  For example, a request for LYR&#x3D;3 will decode only the first 3 quality layers present in the image.  The number of layers decoded will be limited to a maximum given by the MAX_LAYERS environment variable if this has been set in the server configuration. This can be useful to either limit the quality of the images users may see or to speed up decoding by only decoding the faster lower quality layers.
     *
     * @param ROT: Rotate (and flip) image by given number of degrees, r. Only 90, 180 and 270 supported.  If angle is prefixed by an exclamation mark !, the image is flipped horizontally before rotation (ex: ROT&#x3D;!90). Vertical flipping can be achieved by combining horizontal flipping and 180° rotation
     *
     * @param GAM: Apply gamma correction, g: each pixel value to the power of g.  If g&#x3D;log or g&#x3D;logarithm, the logarithm is applied
     *
     * @param CMP: Generate colormap using one of the standard colormap schemes, s: GREY, JET, COLD, HOT, RED, GREEN and BLUE.
     *
     * @param PFL: Export profile in JSON format at resolution r from position x1,y1 to x2, y2.  Only horizontal or vertical profiles are currently supported.  **PFL has to be defined specifically as r:x1,y1-x2,y2**  **An example: 800:20,20-440,440**
     *
     * @param CTW: Color twist / channel recombination. Recombine the available image channels into a new color image by multiplication through a matrix. Columns are separated by commas and rows are separated by semi-colons. Values can also be negative.  Thus, for the 3×3 matrix example provided below, the RGB output image will have bands R &#x3D; R*r1 + G*g1 + B*b1, G &#x3D; R*r2 + G*g2 + B*b2, B &#x3D; R*r3 + G*g3 + B*b3.  For multi-band images, the row length should correspond to the number of available bands within the image. The number of output bands depends on the number of rows in the matrix. Thus, to output a 1 band greyscale image, specify just a single row.  Examples: To perform naive conversion from 3 channel color to 1 channel grayscale: CTW&#x3D;[0.33,0.33,0.33]  To flip the R and B channels and map an RGB image to BGR: CTW&#x3D;[0,0,1;0,1,0;1,0,0]  For a 5-band multispectral image, to show the difference between the 5th and 2nd band (i.e. 5th-2nd) and outputting the result as grayscale: CTW&#x3D;[0,-1,0,0,1]  To create a false-color image from a 4-band RGB-IR image by mapping the G,R,IR channels to the output RGB: CTW&#x3D;[0,1,0,0;0,0,1,0;0,0,0,1]  **CTW has to be defined as [array;array;array] using ; as delimter between arrays and , between integers**
     *
     * @param INV: Invert image (no argument)
     *
     * @param COL: Color transformation to output space, c. Valid values are greyscale (GREY or GRAY) or to binary (BINARY).   Examples: Convert to greyscale: COL&#x3D;gray  Convert to binary: COL&#x3D;binary
     *
     * @param JTL: Return a tile in JPEG format with index n at resolution level r  **JTL has to be specified as r,n**
     *
     * @param PTL: Return a tile in PNG format at resolution level r with index n    **PTL has to be specified as r,n**
     *
     * @param CVT: Export the full image or a region in the specified format (JPEG and PNG currently supported)
     *
     * @return a bitmap.
     * @throws ServiceException when other http codes should be returned
     */
    public javax.ws.rs.core.StreamingOutput getIIPImage(
            URI requestURI,
            String FIF, Integer WID, Integer HEI, List<Float> RGN, Integer QLT, Float CNT, List<Integer> SHD,
            Integer LYR, String ROT, Float GAM, String CMP, String PFL, String CTW, Boolean INV, String COL,
            List<Integer> JTL, List<Integer> PTL, String CVT) throws ServiceException {
        validateIIPRequest(FIF, WID, HEI, RGN, QLT, CNT, SHD, LYR, ROT, GAM, CMP, PFL, CTW, INV, COL, JTL, PTL, CVT);

        // Defaults
        if (CVT == null || "jpg".equals(CVT)) {
            CVT = "jpeg";
        }

        // http://example.com/iipsrv.fcgi?FIF=/mymount/85/c1/85c1df89-bffe-48e0-8813-111f6f0fba50.jp2&CVT=jpeg
        UriBuilder builder = UriBuilder.
                fromUri(ServiceConfig.getConfig().getString(KEY_IIP_SERVER)).
                queryParam("FIF", FIF); // Mandatory
        addIfPresent(builder, "WID", WID);
        addIfPresent(builder, "HEI", HEI);
        addIfPresent(builder, "RGN", RGN);
        addIfPresent(builder, "QLT", QLT);
        addIfPresent(builder, "CNT", CNT);
        addIfPresent(builder, "SHD", SHD);

        addIfPresent(builder, "LYR", LYR);
        addIfPresent(builder, "ROT", ROT);
        addIfPresent(builder, "GAM", GAM);
        addIfPresent(builder, "CMP", CMP);
        addIfPresent(builder, "PFL", PFL);
        addIfPresent(builder, "CTW", CTW);
        addIfPresent(builder, "INV", INV); // TODO: Figure out how to add INV as a value-less param
        addIfPresent(builder, "COL", COL);

        addIfPresent(builder, "JTL", JTL);
        addIfPresent(builder, "PTL", PTL);
        addIfPresent(builder, "CVT", CVT);

        final URI uri = builder.build();

        return proxy(FIF, uri, requestURI);
    }

    private StreamingOutput proxy(String FIF, URI uri, URI clientRequestURI) {
        return output -> {
            HttpURLConnection connection;

            try  {
                connection = (HttpURLConnection)uri.toURL().openConnection();
            } catch (Exception e) {
                log.warn("Unable to create passive proxy connection with URI '{}' from client request '{}'",
                         uri, clientRequestURI, e);
                throw new InternalServiceException("Unable to create passive proxy for '" + FIF + "'");
            }

            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "ds-image");
            // TODO: Add timeouts, but make them configurable
            //con.setConnectTimeout(1000);
            //con.setReadTimeout(1000);

            try {
                connection.connect();
            } catch (SocketTimeoutException e) {
                log.warn("Timeout establishing connection to '{}' for from client request '{}'",
                         uri, clientRequestURI, e);
                throw new ServiceException("Timeout establishing proxy connection for '" + FIF + "'",
                                           Response.Status.GATEWAY_TIMEOUT);
            } catch (IOException e) {
                log.warn("Unable to establish connection to '{}' for from client request '{}'",
                         uri, clientRequestURI, e);
                throw new ServiceException("Unable to establish proxy connection for '" + FIF + "'",
                                           Response.Status.BAD_GATEWAY);
            }

            int statusCode = connection.getResponseCode();

            if (statusCode >= 200 && statusCode <= 299) { // All OK
                try (InputStream remoteStream = uri.toURL().openStream()) {
                    long copiedBytes = IOUtils.copyLarge(remoteStream, output);
                    log.debug("Proxied {} bytes for remote request '{}' for client request '{}'",
                              copiedBytes, uri, clientRequestURI);
                } catch (Exception e) {
                    log.warn("Unable to proxy remote request '{}' for client request '{}'", uri, clientRequestURI);
                    throw new InternalServerErrorException("Unable to serve request for image '" + FIF + "'");
                }
                return;
            }

            if (statusCode >= 400 && statusCode <= 499) { // Request problems
                log.warn("Client error {} for connection to '{}' for client request '{}'",
                         statusCode, uri, clientRequestURI);
                throw new ServiceException("Unable to proxy request for '" + FIF + "'",
                                           Response.Status.fromStatusCode(statusCode));
            }

            if (statusCode >= 500 && statusCode <= 599) { // Server problems
                log.warn("Remote server error {} for connection to '{}' for client request '{}'",
                         statusCode, uri, clientRequestURI);
                throw new ServiceException("Unable to proxy request for '" + FIF + "'",
                                           Response.Status.fromStatusCode(statusCode));
            }

            log.warn("Unhandled status code {} for connection to '{}' for client request '{}'",
                     statusCode, uri, clientRequestURI);
            throw new ServiceException("Unhandled status code " + statusCode + " for proxy connection for '" +
                                       FIF + "'",
                                       Response.Status.INTERNAL_SERVER_ERROR);
        };
    }

    /**
     * Add a query param to the URIBuilder if a value is present.
     */
    private void addIfPresent(UriBuilder builder, String key, Object value) {
        if (value != null) {
            builder.queryParam(key, value);
        }
    }

    /**
     * Add a query param to the URIBuilder if a value is present.
     * The value will be serialized as comma-deparated values.
     */
    private void addIfPresent(UriBuilder builder, String key, List<? extends Object> values) {
        if (values != null && !values.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Object value: values) {
                if (sb.length() != 0) {
                    sb.append(",");
                }
                sb.append(value.toString());
            }
            builder.queryParam(key, sb.toString());
        }
    }

    /**
     * Validates IIP parameters and throws appropriate exceptions if any are invalid.
     * See https://iipimage.sourceforge.io/documentation/protocol/
     * @throws ServiceException thrown if any parameters are not conforming to the IIP specification.
     */
    private void validateIIPRequest(
            String fif, Integer wid, Integer hei, List<Float> rgn, Integer qlt, Float cnt, List<Integer> shd,
            Integer lyr, String rot, Float gam, String cmp, String pfl, String ctw, Boolean inv, String col,
            List<Integer> jtl, List<Integer> ptl, String cvt) {
        if (fif == null || fif.isEmpty()) {
            throw new InvalidArgumentServiceException("The parameter FIF must be defined");
        }
        if (!("jpeg".equals(cvt) | "png".equals(cvt))) {
            throw new InvalidArgumentServiceException(
                    "The parameter CVT must be defined and must be either 'jpeg' or 'png'. It was '" + cvt + "'");
        }
        // TODO: Perform validation of all parameters
    }

}
