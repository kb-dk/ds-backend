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

import com.damnhandy.uri.template.UriTemplate;
import dk.kb.image.config.ServiceConfig;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.HttpHeaders;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Proxy for an image server that supports the
 * <a href="https://iipimage.sourceforge.io/documentation/protocol/">IIP Protocol</a>.
 *
 */
public class IIPFacade {
    private static final Logger log = LoggerFactory.getLogger(IIPFacade.class);

    private static IIPFacade instance;

    public static final String KEY_IIP_SERVER = "imageservers.iip.server";

    public static final String KEY_DEEPZOOM_SERVER_PATH = "imageservers.deepzoom.path";

    public static final String KEY_DEEPZOOM_SERVER_PARAM = "imageservers.deepzoom.param";

    // https://iipimage.sourceforge.io/documentation/protocol
    // https://datatracker.ietf.org/doc/html/rfc6570
    // https://www.emmanuelgautier.com/blog/uri-template-rfc-6570
    public static final String IIP_TEMPLATE = 
            "{?FIF}" + // Full image path (mandatory and must be first): FIF=/path/image.tif
                    "{&WID}" + // Specify width in pixels, w, for exports with CVT request: WID=w
                    "{&HEI}" + // Specify height in pixels, h, for exports with CVT requests: HEI=h
                    "{&RGN}" + // Define a region of interest starting at relative coordinates x,y with width w and height h. For use with CVT: RGN=x,y,w,h
                    "{&QLT}" + // Set the output compression level, q: QLT=q
                    "{&CNT}" + // Contrast adjustment: multiplication of pixel values by factor, c.: CNT=c
                    // "{&SHD}" + // Simulated hill-shading for image normal data: SHD=h,v. Only version 0.9.7. Not supported here
                    // "{&LYR}" + // The number of quality layers, l, in an image to decode.: LYR=l. Only version 0.9.9. Not supported here
                    "{&ROT}" + // Rotate (and flip) image by given number of degrees, r: ROT=r
                    "{&GAM}" + // Apply gamma correction, g: GAM=g
                    "{&CMP}" + // Generate colormap using one of the standard colormap schemes, s: CMP=s
                    "{&PFL}" + // Export profile in JSON format at resolution r from position x1,y1 to x2, y2: PFL=r:x1,y1-x2,y2
                    // "{&MINMAX}" + // Define the range of input pixel values to use for a particular channel, c: MINMAX=c:min,max. Not supported here
                    "{&CTW}" + // Color twist / channel recombination: CTW=[r1,g1,b1;r2,g2,b2;r3,g3,b3]
                    "{+INV}" + // Invert image (no argument). Note missing &, which must be added if INV is set
                    "{&COL}" +  // Color transformation to output space, c.
                    // Parameters below must be after all other parameters
                    "{&JTL}" + // Return a tile in JPEG format with index n at resolution level r: JTL=r,n
                    "{&PTL}" + // Return a tile in PNG format with index n at resolution level r: PTL=r,n
                    "{&CVT}"; // Export the full image or a region in the specified format (JPEG and PNG currently supported): CVT=jpeg

    // Path based DeepZoom server

    // http://example.com:1234/image_identifier.dzi
    public static final String DEEPZOOM_PATH_DZI_TEMPLATE =
            "/{+dzipath}"; // Unescaped as it might be multi-path
    
    // https://example.com/example-images/fooimage_files/11/2_0.jpg
    // https://example.com/example-images/fooimage/fooimage_files/11/2_0.jpg
    public static final String DEEPZOOM_PATH_TEMPLATE =
            "/{+imageid}_files" + // Mandatory. No percent-escaping for imageid as it might include path
                    "/{layer}" + // Integer: 11
                    "/{tile}" + // x_y: 2_0
                    ".{format}" + // Image format: jpg
                    // Params below are not DeepZoom per se, but are supported by IIPImage. All optional
                    "{?CNT}" + // Contrast adjustment: multiplication of pixel values by factor, c.: CNT=c
                    "{?GAM}" + // Apply gamma correction, g: GAM=g
                    "{?CMP}" + // Generate colormap using one of the standard colormap schemes, s: CMP=s
                    "{?CTW}" + // Color twist / channel recombination: CTW=[r1,g1,b1;r2,g2,b2;r3,g3,b3]
                    "{+INV}" + // Invert image (no argument). Note missing &, which must be added if INV is set
                    "{?COL}";  // Color transformation to output space, c.
    
    // Param based DeepZoom server: http://example.com:1234/iipsrv/iipsrv.fcgi?DeepZoom=Path_to_your_image.jpg.dzi
    public static final String DEEPZOOM_PARAM_DZI_TEMPLATE =
            "?DeepZoom={+dzipath}"; // Unescaped as it might be multi-path

    // https://example.com/fcgi-bin/iipsrv.fcgi?Deepzoom=hs-2007-16-a-full_tif.tif_files/12/2_4.jpg
    public static final String DEEPZOOM_PARAM_TEMPLATE =
            "?DeepZoom={+imageid}_files" + // Mandatory. No percent-escaping for imageid as it might include path
                    "/{layer}" + // Integer: 12
                    "/{tile}" + // 2_4: 2_0
                    ".{format}"  + // Image format: jpg
                    // Params below are not DeepZoom per se, but are supported by IIPImage. All optional
                    "{&CNT}" + // Contrast adjustment: multiplication of pixel values by factor, c.: CNT=c
                    "{&GAM}" + // Apply gamma correction, g: GAM=g
                    "{&CMP}" + // Generate colormap using one of the standard colormap schemes, s: CMP=s
                    "{&CTW}" + // Color twist / channel recombination: CTW=[r1,g1,b1;r2,g2,b2;r3,g3,b3]
                    "{+INV}" + // Invert image (no argument). Note missing &, which must be added if INV is set
                    "{&COL}";  // Color transformation to output space, c.

    public static synchronized IIPFacade getInstance() {
        if (instance == null) {
            instance = new IIPFacade();
        }
        return instance;
    }

    private IIPFacade() {
        log.info("Created " + this);
    }


    /**
     * Internet Imaging Protocol available through OpenAPI specification. The specification can be found <a href="https://iipimage.sourceforge.io/documentation/protocol/">here</a>.
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
     * @param httpHeaders the original httpHeaders from the client. Used to transfer specific header fields to image server request.
     *
     * @return a bitmap.
     * @throws ServiceException when other http codes should be returned
     */
    public javax.ws.rs.core.StreamingOutput getIIPImage(
            URI requestURI,
            String FIF, Long WID, Long HEI, List<Float> RGN, Integer QLT, Float CNT,
            String ROT, Float GAM, String CMP, String PFL, String CTW, Boolean INV, String COL,
            List<Integer> JTL, List<Integer> PTL, String CVT, HttpHeaders httpHeaders) throws ServiceException {

        IIPParamValidation.validateIIPRequest(FIF, WID, HEI, RGN, QLT, CNT, ROT, GAM, CMP, PFL, CTW, INV, COL, JTL, PTL, CVT);

        // Defaults
        if (CVT == null || "jpg".equals(CVT)) {
            CVT = "jpeg";
        }

        // http://example.com/iipsrv.fcgi?FIF=/mymount/85/c1/85c1df89-bffe-48e0-8813-111f6f0fba50.jp2&CVT=jpeg
        try {
            UriTemplate template = UriTemplate.fromTemplate(ServiceConfig.getServer(KEY_IIP_SERVER) + IIP_TEMPLATE)
                    .set("FIF", FIF); // Mandatory. Note that slashes will be encoded

            ProxyHelper.addIfPresent(template, "WID", WID);
            ProxyHelper.addIfPresent(template, "HEI", HEI);
            ProxyHelper.addIfPresent(template, "RGN", RGN);
            ProxyHelper.addIfPresent(template, "QLT", QLT);
            ProxyHelper.addIfPresent(template, "CNT", CNT);

            ProxyHelper.addIfPresent(template, "ROT", ROT);
            ProxyHelper.addIfPresent(template, "GAM", GAM);
            ProxyHelper.addIfPresent(template, "CMP", CMP);
            ProxyHelper.addIfPresent(template, "PFL", PFL);
            ProxyHelper.addIfPresent(template, "CTW", CTW);
            if (Boolean.TRUE.equals(INV)) {
                template.set("INV", "&INV"); // Special case as INV is without value
            }
            ProxyHelper.addIfPresent(template, "COL", COL);

            ProxyHelper.addIfPresent(template, "JTL", JTL);
            ProxyHelper.addIfPresent(template, "PTL", PTL);
            ProxyHelper.addIfPresent(template, "CVT", CVT);

            final URI uri = new URI(template.expand());

            return ProxyHelper.proxy(FIF, uri, requestURI, httpHeaders);
        } catch (URISyntaxException e) {
            log.warn("getIIPImage: Unable to construct URL requestURI='{}'", requestURI, e);
            throw new InternalServiceException(
                    "Unable to construct getIIPImage URL for request '" + requestURI + "'");
        }
    }

    public javax.ws.rs.core.StreamingOutput getDeepzoomDZI(
            URI requestURI, String imageid,
            HttpServletResponse httpServletResponse, HttpHeaders httpHeaders) throws ServiceException {
        validateDeepzoomDZIRequest(imageid);
        final String idDZI = imageid + (imageid.endsWith(".dzi") ? "" : ".dzi");

        UriTemplate template;
        if (ServiceConfig.getConfig().containsKey(KEY_DEEPZOOM_SERVER_PATH)){
            // Path based DeepZoom server: http://example.com:1234/image_identifier.dzi
            template = UriTemplate
                    .fromTemplate(ServiceConfig.getServer(KEY_DEEPZOOM_SERVER_PATH) + DEEPZOOM_PATH_DZI_TEMPLATE);

        } else if (ServiceConfig.getConfig().containsKey(KEY_DEEPZOOM_SERVER_PARAM)){
            // Param based DeepZoom server: http://example.com:1234/iipsrv/iipsrv.fcgi?DeepZoom=Path_to_your_image.jpg.dzi
            template = UriTemplate
                    .fromTemplate(ServiceConfig.getServer(KEY_DEEPZOOM_SERVER_PARAM) + DEEPZOOM_PARAM_DZI_TEMPLATE);

        } else {
            log.error("No DeepZoom server defined");
            throw new InternalServiceException("No DeepZoom server defined");
        }

        template.set("dzipath", idDZI);


        final URI uri;
        try {
            uri = new URI(template.expand());
        } catch (URISyntaxException e) {
            log.warn("Error finalizing DeepZoom-dzi proxy URI for image ID '{}' from request '{}'",
                    imageid, requestURI, e);
            throw new InternalServerErrorException(
                    "Error finalizing DeepZoom-dzi proxy URI for request '" + requestURI + "'");
        }

        return ProxyHelper.proxy(imageid, uri, requestURI, httpServletResponse, httpHeaders);
    }

    public javax.ws.rs.core.StreamingOutput getDeepzoomTile(
            URI requestURI,
            String imageid, Integer layer, String tiles, String format, Float CNT,
            Float GAM, String CMP, String CTW, Boolean INV, String COL, HttpHeaders httpHeaders) throws ServiceException {
        IIPParamValidation.validateDeepzoomTileRequest(imageid, layer, tiles, format, CNT, GAM, CMP, CTW, INV, COL);

        // Defaults
        if (format == null) {
            format = "jpeg";
        }

        boolean isPath = true;
        UriTemplate template;
        if (ServiceConfig.getConfig().containsKey(KEY_DEEPZOOM_SERVER_PATH)) {
            // Path based DeepZoom server
            // https://example.com/example-images/fooimage/fooimage_files/11/2_0.jpg
            template = UriTemplate
                    .fromTemplate(ServiceConfig.getServer(KEY_DEEPZOOM_SERVER_PATH) + DEEPZOOM_PATH_TEMPLATE);

        } else if (ServiceConfig.getConfig().containsKey(KEY_DEEPZOOM_SERVER_PARAM)) {
            // Param based DeepZoom server
            // https://example.com/fcgi-bin/iipsrv.fcgi?Deepzoom=hs-2007-16-a-full_tif.tif_files/12/2_4.jpg
            template = UriTemplate
                    .fromTemplate(ServiceConfig.getServer(KEY_DEEPZOOM_SERVER_PARAM) + DEEPZOOM_PARAM_TEMPLATE);
            isPath = false;

        } else {
            log.error("No Deepzoom server defined");
            throw new InternalServiceException("No Deepzoom server defined");
        }

        // Mandatory arguments
        template.set("imageid", imageid)
                .set("layer", Integer.toString(layer))
                .set("tile", tiles)
                .set("format", format);

        // Optional arguments
        ProxyHelper.addIfPresent(template, "CNT", CNT);
        ProxyHelper.addIfPresent(template, "GAM", GAM);
        ProxyHelper.addIfPresent(template, "CMP", CMP);
        ProxyHelper.addIfPresent(template, "CTW", CTW);
        if (Boolean.TRUE.equals(INV)) {
            // INV should start with '?' if this is the first parameter for an othwerwise path based request
            if (isPath && CNT==null && GAM==null && CMP==null && CTW==null) {
                template.set("INV", "?INV"); // Special case as INV is without value
            } else {
                template.set("INV", "&INV"); // Special case as INV is without value
            }
        }
        ProxyHelper.addIfPresent(template, "COL", COL);

        final URI uri;
        try {
            uri = new URI(template.expand());
        } catch (URISyntaxException e) {
            log.warn("Error finalizing DeepZoom-tile proxy URI for image ID '{}' from request '{}'",
                    imageid, requestURI, e);
            throw new InternalServerErrorException(
                    "Error finalizing DeepZoom-tile proxy URI for request '" + requestURI + "'");
        }

        return ProxyHelper.proxy(imageid, uri, requestURI, httpHeaders);
    }

    /**
     * Validates Deepzoom DZI parameters and throws appropriate exceptions if any are invalid.
     * See <a href="https://iipimage.sourceforge.io/documentation/protocol/">documentation</a>
     * The documentation is very subtle on Deepzoom. One could also look at OpenSeadragon
     * <a href="https://openseadragon.github.io/docs/">documentation</a>
     * @throws ServiceException thrown if any parameters are not conforming to the IIP specification.
     */
    private void validateDeepzoomDZIRequest(
            String imageid) {
        if (imageid == null || imageid.isEmpty()) {
            throw new InvalidArgumentServiceException("The parameter imageid must be defined");
        }
    }

}
