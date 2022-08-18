package dk.kb.image.api.v1.impl;

import dk.kb.image.IIPImageFacade;
import dk.kb.image.api.v1.*;
import java.util.ArrayList;
import java.io.File;
import java.util.List;
import java.util.Map;

import dk.kb.util.webservice.exception.ServiceException;
import dk.kb.util.webservice.exception.InternalServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.io.File;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.kb.util.webservice.ImplBase;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.*;

import io.swagger.annotations.Api;

/**
 * ds-image
 *
 * <p>This API implements the functionality of the IIPImage API into the OpenAPI framework used at KB.  The goal is to implement all four of the APIs from [IIPImage](https://iipimage.sourceforge.io/documentation/protocol/). These are as follows: - [Internet Imaging Protocol](https://iipimage.sourceforge.io/IIPv105.pdf) - [IIIF API](https://iiif.io/api/image/3.0/) - Deepzoom - Zoomify  Specification for OpenAPI can be found [here](https://swagger.io/docs/specification/about/).
 *
 */
public class DsImageApiServiceImpl extends ImplBase implements DsImageApi {
    private Logger log = LoggerFactory.getLogger(this.toString());



    /**
     * Internet Imaging Protocol available through OpenAPI specification. The specification can be found at: https://iipimage.sourceforge.io/documentation/protocol/
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
    public javax.ws.rs.core.StreamingOutput getImage(
            String FIF, Integer WID, Integer HEI, List<Float> RGN, Integer QLT, Float CNT, List<Integer> SHD,
            Integer LYR, String ROT, Float GAM, String CMP, String PFL, String CTW, Boolean INV, String COL,
            List<Integer> JTL, List<Integer> PTL, String CVT) throws ServiceException {
        try {
            String[] elements = FIF.split("[/\\\\]");
            String filename = elements[elements.length-1] + "." + CVT;
            // Show download link in Swagger UI, inline when opened directly in browser
            setFilename(filename, false, false);
            return IIPImageFacade.getInstance().getIIPImage(
                    FIF, WID, HEI, RGN, QLT, CNT, SHD, LYR, ROT, GAM, CMP, PFL, CTW, INV, COL, JTL, PTL, CVT);
        } catch (Exception e){
            throw handleException(e);
        }
    
    }


}
