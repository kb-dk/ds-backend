package dk.kb.image.api.v1.impl;

import dk.kb.image.IIIFFacade;
import dk.kb.image.IIPFacade;
import dk.kb.image.ProxyHelper;
import dk.kb.image.api.v1.AccessApi;
import dk.kb.image.model.v1.DeepzoomDZIDto;
import dk.kb.image.model.v1.DeepzoomDZISizeDto;
import dk.kb.util.webservice.ImplBase;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.util.List;

/**
 * ds-image
 *
 * <p>This API implements the functionality of the IIPImage API into the OpenAPI framework used at KB.  The goal is to implement all four of the APIs from [IIPImage](https://iipimage.sourceforge.io/documentation/protocol/). These are as follows: - [Internet Imaging Protocol](https://iipimage.sourceforge.io/IIPv105.pdf) - [IIIF API](https://iiif.io/api/image/3.0/) - Deepzoom - Zoomify  Specification for OpenAPI can be found [here](https://swagger.io/docs/specification/about/).
 *
 */
public class AccessApiServiceImpl extends ImplBase implements AccessApi {
    private Logger log = LoggerFactory.getLogger(this.toString());

    /**
     * DeepZoom Image information
     *
     * @param imageid: Identifier/path for image.
     *
     * @return <ul>
      *   <li>code = 200, message = "Succes!", response = DeepzoomDZIDto.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * Deep Zoom provides the ability to interactively view high-resolution images. You can zoom in and out of images rapidly without affecting the performance of your application. Deep Zoom enables smooth loading and panning by serving up multi-resolution images and using spring animations.
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public StreamingOutput getDeepzoomDZI(String imageid) throws ServiceException {
        // TODO: Implement...

        try {
            httpServletResponse.setContentType(getMIME("json"));
            setFilename(new File(imageid).getName() + ".dzi", false, false);
            return IIPFacade.getInstance().getDeepzoomDZI(
                    uriInfo.getRequestUri(),
                    imageid);

        } catch (Exception e){
            throw handleException(e);
        }

        /*try {
            DeepzoomDZIDto response = new DeepzoomDZIDto();
        response.setTileSize(17467352);
        response.setOverlap(-1859027536);
        response.setFormat("k35O02");
        DeepzoomDZISizeDto size = new DeepzoomDZISizeDto();
        size.setWidth(-5436086360946841600L);
        size.setHeight(3285390032067405824L);
        response.setSize(size);
        return response;
        } catch (Exception e){
            throw handleException(e);
        } */

    }

    /**
     * DeepZoom Tile
     *
     * @param imageid: Identifier/path for image.
     *
     * @param layer: Zoom layer for the tile
     *
     * @param tiles: Tile specified as x_y at the given layer
     *
     * @param format: Output format
     *
     * @param CNT: Contrast adjustment: multiplication of pixel values by factor, c. Value should be an integer or float &gt; 0. A value of 1.0 indicates no contrast change
     *
     * @param SHD: Simulated hill-shading for image normal data. The argument is the angle of incidence of the light source in the horizontal plane (from 12 o’clock), h and the vertical angle of incidence, v, with 0 representing a horizontal direction and -1 vertically downwards  **SHD has to be defined as h,v**
     *
     * @param GAM: Apply gamma correction, g: each pixel value to the power of g.  If g&#x3D;log or g&#x3D;logarithm, the logarithm is applied
     *
     * @param CMP: Generate colormap using one of the standard colormap schemes, s: GREY, JET, COLD, HOT, RED, GREEN and BLUE.
     *
     * @param CTW: Color twist / channel recombination. Recombine the available image channels into a new color image by multiplication through a matrix. Columns are separated by commas and rows are separated by semi-colons. Values can also be negative.  Thus, for the 3×3 matrix example provided below, the RGB output image will have bands R &#x3D; R*r1 + G*g1 + B*b1, G &#x3D; R*r2 + G*g2 + B*b2, B &#x3D; R*r3 + G*g3 + B*b3.  For multi-band images, the row length should correspond to the number of available bands within the image. The number of output bands depends on the number of rows in the matrix. Thus, to output a 1 band greyscale image, specify just a single row.  Examples: To perform naive conversion from 3 channel color to 1 channel grayscale: CTW&#x3D;[0.33,0.33,0.33]  To flip the R and B channels and map an RGB image to BGR: CTW&#x3D;[0,0,1;0,1,0;1,0,0]  For a 5-band multispectral image, to show the difference between the 5th and 2nd band (i.e. 5th-2nd) and outputting the result as grayscale: CTW&#x3D;[0,-1,0,0,1]  To create a false-color image from a 4-band RGB-IR image by mapping the G,R,IR channels to the output RGB: CTW&#x3D;[0,1,0,0;0,0,1,0;0,0,0,1]  **CTW has to be defined as [array;array;array] using ; as delimter between arrays and , between integers**
     *
     * @param INV: Invert image (no argument)
     *
     * @param COL: Color transformation to output space, c. Valid values are greyscale (GREY or GRAY) or to binary (BINARY).   Examples: Convert to greyscale: COL&#x3D;gray  Convert to binary: COL&#x3D;binary
     *
     * @return <ul>
      *   <li>code = 200, message = "Succes!", response = File.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * DeepZoom can bee used with the Internet Imaging Protocol (IIP). This endpoint only requires the DeepZoom parameter to work. Besides, this endpoint has the capability to make use of the IIP parameters shown below.
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public javax.ws.rs.core.StreamingOutput getDeepzoomTile(String imageid, Integer layer, String tiles, String format, Float CNT, List<Integer> SHD, Float GAM, String CMP, String CTW, Boolean INV, String COL) throws ServiceException {
        // TODO: Implement...


        try {
            httpServletResponse.setContentType(getMIME(format));
            return IIPFacade.getInstance().getDeepzoomTile(
                    uriInfo.getRequestUri(),
                    imageid, layer, tiles, format, CNT, SHD, GAM, CMP, CTW, INV, COL);

        } catch (Exception e){
            throw handleException(e);
        }

    }


    /**
     * IIIF Image Information
     * 
     * @param identifier: The identifier of the requested image. This may be an ARK, URN, filename, or other identifier. Special characters must be URI encoded.
     * 
     * @return <ul>
      *   <li>code = 200, message = "Succes!", response = JsonldDto.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public javax.ws.rs.core.StreamingOutput getImageInformation(String identifier, String format) throws ServiceException {
        try {
            String[] elements = identifier.split("[/\\\\]");
            String filename = "info_" + elements[elements.length - 1] + "." + format;
            // Show download link in Swagger UI, inline when opened directly in browser
            setFilename(filename, false, false);
            httpServletResponse.setContentType(getMIME(format));

            // TODO: Add support for XML when the OpenAPI specification has been corrected
            return IIIFFacade.getInstance().getIIIFInfo(uriInfo.getRequestUri(), identifier, "json");
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    /**
     * IIIF Image Request
     * 
     * @param identifier: The identifier of the requested image. This may be an ARK, URN, filename, or other identifier. Special characters must be URI encoded, such as / and ?. When using the OpenAPI GUI this encoding is done automatically
     * 
     * @param region: The region parameter defines the rectangular portion of the underlying image content to be returned  Region can be specified by pixel coordinates, percentage or by the value full, which specifies that the full image should be returned.  | Form | Description | |------|-------------| |**full**|The full image is returned, without any cropping.| |**square**|The region is defined as an area where the width and height are both equal to the length of the shorter dimension of the full image. The region may be positioned anywhere in the longer dimension of the full image at the server’s discretion, and centered is often a reasonable default.| |**x,y,w,h**|The region of the full image to be returned is specified in terms of absolute pixel values. The value of x represents the number of pixels from the 0 position on the horizontal axis. The value of y represents the number of pixels from the 0 position on the vertical axis. Thus the x,y position 0,0 is the upper left-most pixel of the image. w represents the width of the region and h represents the height of the region in pixels.| |**pct:x,y,w,h**|The region to be returned is specified as a sequence of percentages of the full image’s dimensions, as reported in the image information document. Thus, x represents the number of pixels from the 0 position on the horizontal axis, calculated as a percentage of the reported width. w represents the width of the region, also calculated as a percentage of the reported width. The same applies to y and h respectively.|  **EXAMPLE: square**
     * 
     * @param size: The size parameter specifies the dimensions to which the extracted region, which might be the full image, is to be scaled. With the exception of the w,h and ^w,h forms, the returned image maintains the aspect ratio of the extracted region as closely as possible.   Sizes prefixed with ^ allow upscaling of the extracted region when its pixel dimensions are less than the pixel dimensions of the scaled region.  | Form | Description | |------|-------------| |**max** |  The extracted region is returned at the maximum size available, but will not be upscaled. The resulting image will have the pixel dimensions of the extracted region, unless it is constrained to a smaller size by maxWidth, maxHeight, or maxArea as defined in the Technical Properties section.| |**^max**|  The extracted region is scaled to the maximum size permitted by maxWidth, maxHeight, or maxArea as defined in the Technical Properties section. If the resulting dimensions are greater than the pixel width and height of the extracted region, the extracted region is upscaled.| |**w,**|  The extracted region should be scaled so that the width of the returned image is exactly equal to w. The value of w must not be greater than the width of the extracted region.| |**^w,**|  The extracted region should be scaled so that the width of the returned image is exactly equal to w. If w is greater than the pixel width of the extracted region, the extracted region is upscaled.| |**,h**|  The extracted region should be scaled so that the height of the returned image is exactly equal to h. The value of h must not be greater than the height of the extracted region.| |**^,h**|  The extracted region should be scaled so that the height of the returned image is exactly equal to h. If h is greater than the pixel height of the extracted region, the extracted region is upscaled.| |**pct:n**|  The width and height of the returned image is scaled to n percent of the width and height of the extracted region. The value of n must not be greater than 100.| |**^pct:n**|  The width and height of the returned image is scaled to n percent of the width and height of the extracted region. For values of n greater than 100, the extracted region is upscaled.| |**w,h**|  The width and height of the returned image are exactly w and h. The aspect ratio of the returned image may be significantly different than the extracted region, resulting in a distorted image. The values of w and h must not be greater than the corresponding pixel dimensions of the extracted region.| |**^w,h**|  The width and height of the returned image are exactly w and h. The aspect ratio of the returned image may be significantly different than the extracted region, resulting in a distorted image. If w and/or h are greater than the corresponding pixel dimensions of the extracted region, the extracted region is upscaled.| |**!w,h**|  The extracted region is scaled so that the width and height of the returned image are not greater than w and h, while maintaining the aspect ratio. The returned image must be as large as possible but not larger than the extracted region, w or h, or server-imposed limits.| |**^!w,h**|  The extracted region is scaled so that the width and height of the returned image are not greater than w and h, while maintaining the aspect ratio. The returned image must be as large as possible but not larger than w, h, or server-imposed limits.|  **EXAMPLE: ^200,180**
     * 
     * @param rotation: The rotation parameter specifies mirroring and rotation. A leading exclamation mark (“!”) indicates that the image should be mirrored by reflection on the vertical axis before any rotation is applied.  The numerical value represents the number of degrees of clockwise rotation, and may be any floating point number from 0 to 360.  | Form | Description | |------|-------------| |**n**|The degrees of clockwise rotation from 0 up to 360.| |**!n**|The image should be mirrored and then rotated as above.|  In most cases, rotation will change the width and height dimensions of the returned image. The service should return an image that contains all of the image contents requested in the region and size parameters, even if the dimensions of the returned image file are different than specified in the size parameter. The image contents should not be scaled as a result of the rotation, and there should be no additional space between the corners of the rotated image contents and the bounding box of the returned image.  For rotations which are not multiples of 90 degrees, it is recommended that the client request the image in a format that supports transparency, such as png, and that the server return the image with a transparent background. There is no facility in the API for the client to request a particular background color or other fill pattern.
     * 
     * @param quality: The quality parameter determines whether the image is delivered in color, grayscale or black and white.  | Form | Description | |------|-------------| |**color**|  The image is returned with all of its color information.| |**gray**|  The image is returned in grayscale, where each pixel is black, white or any shade of gray in between.| |**bitonal**|  The image returned is bitonal, where each pixel is either black or white.| |**default**|  The image is returned using the server’s default quality (e.g. color, gray or bitonal) for the image.|  The default quality exists to support [level 0 compliant implementations](https://iiif.io/api/image/3.0/compliance/#quality) that may not know the qualities of individual images in their collections. It also provides a convenience for clients that know the values for all other parameters of a request except the quality (e.g. .../full/120,80/90/{quality}.png to request a thumbnail) in that a preliminary image information request that would only serve to find out which qualities are available can be avoided.
     * 
     * @param format: The format of the returned image is expressed as a suffix, mirroring common filename extensions.
     * 
     * @return <ul>
      *   <li>code = 200, message = "Succes!", response = File.class</li>
      *   <li>code = 400, message = "Bad Request. Please check the formating of the parameters: region, size and rotation.  Check if the requested region’s height or width is zero, or if the region is entirely outside the bounds of the reported dimensions  Requests for sizes not prefixed with ^ that result in a scaled region with pixel dimensions greater than the pixel dimensions of the extracted region are errors that should result in a 400 (Bad Request) status code.  Check for syntax errors in size parameter  A rotation value that is out of range or unsupported should result in a 400 (Bad Request) status code."</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public javax.ws.rs.core.StreamingOutput iIIFImageRequest(
            String identifier, String region, String size, String rotation, String quality, String format)
            throws ServiceException {
        try {
            String[] elements = identifier.split("[/\\\\]");
            String filename = elements[elements.length - 1] + "." + format;
            // Show download link in Swagger UI, inline when opened directly in browser
            setFilename(filename, false, false);
            httpServletResponse.setContentType(getMIME(format));

            return IIIFFacade.getInstance().getIIIFImage(
                    uriInfo.getRequestUri(),
                    identifier, region, size, rotation, quality, format);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    /**
     * Internet Imaging Protocol 
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
     * @return <ul>
      *   <li>code = 200, message = "OK", response = File.class</li>
      *   <li>code = 400, message = "Bad Request. PFL or CTW has been defined incorrectly."</li>
      *   <li>code = 404, message = "An image with the provided FIF was not found."</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public StreamingOutput iIPImageRequest(
            String FIF, Long WID, Long HEI, List<Float> RGN, Integer QLT, Float CNT, List<Integer> SHD,
            Integer LYR, String ROT, Float GAM, String CMP, String PFL, String CTW, Boolean INV, String COL,
            List<Integer> JTL, List<Integer> PTL, String CVT) throws ServiceException {
        try {
            String[] elements = FIF.split("[/\\\\]");
            String filename = elements[elements.length - 1] + "." + CVT;
            // Show download link in Swagger UI, inline when opened directly in browser
            setFilename(filename, false, false);
            httpServletResponse.setContentType(getMIME(CVT));

            return IIPFacade.getInstance().getIIPImage(
                    uriInfo.getRequestUri(),
                    FIF, WID, HEI, RGN, QLT, CNT, SHD, LYR, ROT, GAM, CMP, PFL, CTW, INV, COL, JTL, PTL, CVT);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    /**
     * Derives the MIME type for replies. Only supports formats from IIIF Image and IIP protocols.
     * @param format simple form, e.g. {@code jpeg}, {@code pdf}...
     */
    private String getMIME(String format) {
        switch (format) {
            // IIIF
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "tif":
            case "tiff":
                return "image/tiff";
            case "png":
                return "image/png";
            case "jp2k":
            case "jpeg2k":
            case "jpeg2000":
            case "jp2":
                return "image/jp2";
            case "webp":
                return "image/webp";
            case "pdf":
                return "application/pdf";
            case "json":
                return "application/json";
            case "xml":
                return "application/xml";
            default:
                throw new InternalServiceException("Unknown format, unable to determine mime type: '" + format + "'");
        }
    }

}
