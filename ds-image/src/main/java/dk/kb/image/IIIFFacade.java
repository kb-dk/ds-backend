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
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;

/**
 * Proxy for an image server that supports the <a href="https://iiif.io/api/image/3.0/">IIIF Image API</a>.
 *
 */
public class IIIFFacade {
    private static final Logger log = LoggerFactory.getLogger(IIIFFacade.class);

    private static IIIFFacade instance;

    public static final String KEY_IIIF_SERVER = "imageservers.iiif.server";

    // https://iiif.io/api/image/3.0/
    public static final String IIIF_IMAGE3_TEMPLATE = "/{identifier}/{region}/{size}/{rotation}/{quality}.{format}";
    public static final String IIIF_INFO3_TEMPLATE = "/{identifier}/info.{ext}";

    public static synchronized IIIFFacade getInstance() {
        if (instance == null) {
            instance = new IIIFFacade();
        }
        return instance;
    }

    private IIIFFacade() {
        log.info("Created " + this);
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
    public StreamingOutput getIIIFImage(URI requestURI, String identifier, String region, String size,
                                        String rotation, String quality, String format) {
        validateIIIFImageRequest(requestURI, identifier, region, size, rotation, quality, format);
        if (format == null) {
            format = "jpg";
        }

        // TODO: Add versioning to config so that default/standard for quality can be handled according to image server
        String path = UriTemplate.fromTemplate(IIIF_IMAGE3_TEMPLATE)
                .set("identifier", identifier)
                .set("region", region)
                .set("size", size)
                .set("rotation", rotation)
                .set("quality", quality)
                .set("format", format)
                .expand();

        UriBuilder builder = UriBuilder.
                fromUri(ServiceConfig.getConfig().getString(KEY_IIIF_SERVER)).
                path(path);

        final URI uri = builder.build();
        return ProxyHelper.proxy(identifier, uri, requestURI);
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
    public StreamingOutput getIIIFInfo(URI requestURI, String identifier, String extension) {
        // TODO: Verify extension
        String path = UriTemplate.fromTemplate(IIIF_INFO3_TEMPLATE)
                .set("identifier", identifier)
                .set("ext", extension)
                .expand();
        UriBuilder builder = UriBuilder.
                fromUri(ServiceConfig.getConfig().getString(KEY_IIIF_SERVER)).
                path(path);

        final URI uri = builder.build();
        return ProxyHelper.proxy(identifier, uri, requestURI);
    }

    /**
     * Validates IIIF Image API parameters and throws appropriate exceptions if they are not valid.
     * See https://iiif.io/api/image/3.0/
     * @throws ServiceException thrown if any parameters are not conforming to the IIIF specification.
     */
    private void validateIIIFImageRequest(URI requestUri, String identifier, String region, String size,
                                          String rotation, String quality, String format) {
        if (identifier == null || identifier.isEmpty()) {
            throw new InvalidArgumentServiceException("An identifier must be given");
        }

        // TODO: Implement proper validation
    }

}
