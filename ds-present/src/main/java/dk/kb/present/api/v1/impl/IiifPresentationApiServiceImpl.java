package dk.kb.present.api.v1.impl;

import dk.kb.present.api.v1.IiifPresentationApi;
import dk.kb.present.model.v1.AnnotationsBodyDto;
import dk.kb.present.model.v1.AnnotationsDto;
import dk.kb.present.model.v1.AnnotationsItemsDto;
import dk.kb.present.model.v1.CanvasDto;
import dk.kb.present.model.v1.CanvasItemsDto;
import dk.kb.present.model.v1.CanvasLabelDto;
import dk.kb.present.model.v1.CollectionDto;
import dk.kb.present.model.v1.ManifestDto;
import dk.kb.present.model.v1.ManifestHomepageDto;
import dk.kb.present.model.v1.ManifestLabelDto;
import dk.kb.present.model.v1.ManifestMetadataDto;
import dk.kb.present.model.v1.ManifestPartOfDto;
import dk.kb.present.model.v1.ManifestProviderDto;
import dk.kb.present.model.v1.ManifestRenderingDto;
import dk.kb.present.model.v1.ManifestRequiredStatementDto;
import dk.kb.present.model.v1.ManifestSeeAlsoDto;
import dk.kb.present.model.v1.ManifestService1Dto;
import dk.kb.present.model.v1.ManifestServiceDto;
import dk.kb.present.model.v1.ManifestServicesDto;
import dk.kb.present.model.v1.ManifestStartDto;
import dk.kb.present.model.v1.ManifestThumbnailDto;
import dk.kb.present.model.v1.RangeDto;
import dk.kb.present.model.v1.RangeLabelDto;
import dk.kb.present.model.v1.ViewDto;
import dk.kb.util.webservice.ImplBase;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ds-present
 *
 * <p>Metadata delivery for the Royal Danish Library  This API delivers metadata from collections at the Royal Danish Library. Metadata can be delivered as IIIF Presentation manifests.  For information on the IIIF Presentation API see the following [link](https://iiif.io/api/presentation/3.0/). This API supports version 3.= and should be backwards compatible with version 2.1.1 
 *
 */
public class IiifPresentationApiServiceImpl extends ImplBase implements IiifPresentationApi {
    private Logger log = LoggerFactory.getLogger(this.toString());

    // foo/bar/manifest instead of foo%2B2Fbar/manifest
    private final Pattern NONESCAPED_MANIFEST_PATH = Pattern.compile("(.*)/manifest");


    /* How to access the various web contexts. See https://cxf.apache.org/docs/jax-rs-basics.html#JAX-RSBasics-Contextannotations */

    @Context
    private transient UriInfo uriInfo;

    @Context
    private transient SecurityContext securityContext;

    @Context
    private transient HttpHeaders httpHeaders;

    @Context
    private transient Providers providers;

    @Context
    private transient Request request;

    // Disabled as it is always null? TODO: Investigate when it can be not-null, then re-enable with type
    //@Context
    //private transient ContextResolver contextResolver;

    @Context
    private transient HttpServletRequest httpServletRequest;

    @Context
    private transient HttpServletResponse httpServletResponse;

    @Context
    private transient ServletContext servletContext;

    @Context
    private transient ServletConfig servletConfig;

    @Context
    private transient MessageContext messageContext;


    /**
     * IIIF Presentation Collection
     * 
     * @param name: The {name} parameter in the URI structure must distinguish it from any other sequences that may be available for the physical object. Typical default names for sequences are “normal” or “basic”.
     * 
     * @return <ul>
      *   <li>code = 200, message = "Succes!", response = CollectionDto.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public CollectionDto getPresentationCollection(String name) throws ServiceException {
        // TODO: Implement...
        log.debug("getPresentationCollection(name='{}') called with call details: {}", name, getCallDetails());

        
        try { 
            CollectionDto response = new CollectionDto();
        response.setId("i9HXo7kIBw");
        response.setPrefix("RH3O0k65");
        response.setDescription("wy66D");
        List<ViewDto> views = new ArrayList<>();
        ViewDto views2 = new ViewDto();
        views2.setId("gCu5C1B");
        views2.setMime("xp322");
        views.add(views2);
        response.setViews(views);
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

    /**
     * IIIF Presentation manifest
     * 
     * @param identifier: The identifier of the requested image. This may be an ARK, URN, filename, or other identifier. Special characters must be URI encoded.
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK", response = ManifestDto.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * The manifest resource represents a single object and any intellectual work or works embodied within that object. In particular it includes the descriptive, rights and linking information for the object.
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public ManifestDto getPresentationManifest(String identifier) throws ServiceException {
        // Note the replace that handles double encoding (%252F) of '/' being single-decoded to '%2F'
        return rawGetPresentationManifest(identifier.replace("%2F", "/"));
    }

    /**
     * The implementation of {@link #getPresentationManifest(String)}.
     * They need to be two different methods as {@link #getPresentationManifest(String)} is Apache CXF annotated and
     * cannot be called directly from {@link #getPresentationManifestNonescaped}.
     * @param identifier the IIIF image identifier.
     * @return a Manifest for the image.
     * @throws ServiceException if lookup failed.
     */
    public ManifestDto rawGetPresentationManifest(String identifier) throws ServiceException {
        // TODO: Implement...
        log.debug("rawGetPresentationManifest(identifier='{}') called with call details: {}",
                  identifier, getCallDetails());

        try { 
            ManifestDto response = new ManifestDto();
        response.setAtContext("o011L6A5w");
        response.setAtId("O33S95G1");
        response.setAtType("Jnr9WB");
        ManifestLabelDto label = new ManifestLabelDto();
        List<String> en = new ArrayList<>();
        en.add("x17CVb");
        label.setEn(en);
        response.setLabel(label);
        List<ManifestMetadataDto> metadata = new ArrayList<>();
        ManifestMetadataDto metadata2 = new ManifestMetadataDto();
        metadata2.setLabel("HL1R0o");
        metadata2.setValue("W3IBe");
        metadata.add(metadata2);
        response.setMetadata(metadata);
        response.setSummary("D9dzl9T");
        List<ManifestThumbnailDto> thumbnail = new ArrayList<>();
        ManifestThumbnailDto thumbnail2 = new ManifestThumbnailDto();
        thumbnail2.setId("B52j7Q");
        thumbnail2.setType("C8Bdc9Zc");
        thumbnail2.setFormat("cBFG7Gg8");
        List<ManifestServiceDto> service = new ArrayList<>();
        ManifestServiceDto service2 = new ManifestServiceDto();
        service2.setId("pru6j5Pz3Ir");
        service2.setType("cvMDP");
        service2.setProfile("rPXjb");
        service.add(service2);
        thumbnail2.setService(service);
        thumbnail.add(thumbnail2);
        response.setThumbnail(thumbnail);
        response.setViewingDirection("BgbbNW");
        response.setViewingHint("mHmF3317");
        Date navDate = new Date(0);
        response.setNavDate(navDate);
        response.setRights("tq42K");
        ManifestRequiredStatementDto requiredStatement = new ManifestRequiredStatementDto();
        requiredStatement.setLabel("j0RpwJl");
        requiredStatement.setValue("TIjN9alI");
        response.setRequiredStatement(requiredStatement);
        List<ManifestProviderDto> provider = new ArrayList<>();
        ManifestProviderDto provider2 = new ManifestProviderDto();
        provider2.setId("w7wuDhJ");
        provider2.setType("vYu9Bv");
        provider2.setLabel("Rh95V657R");
        List<ManifestHomepageDto> homepage = new ArrayList<>();
        ManifestHomepageDto homepage2 = new ManifestHomepageDto();
        homepage2.setId("PNU3TCEQd5");
        homepage2.setType("s6vnr");
        homepage2.setLabel("UhXpM5eL6");
        homepage2.setFormat("XKqsuw6Zb");
        homepage.add(homepage2);
        provider2.setHomepage(homepage);
        List<ManifestThumbnailDto> logo = new ArrayList<>();
        ManifestThumbnailDto logo2 = new ManifestThumbnailDto();
        logo2.setId("v7qRD");
        logo2.setType("lNn9l");
        logo2.setFormat("x7938");
        List<ManifestServiceDto> service3 = new ArrayList<>();
        ManifestServiceDto service4 = new ManifestServiceDto();
        service4.setId("kndTH");
        service4.setType("E05T6");
        service4.setProfile("BD3BTmS");
        service3.add(service4);
        logo2.setService(service3);
        logo.add(logo2);
        provider2.setLogo(logo);
        List<ManifestSeeAlsoDto> seeAlso = new ArrayList<>();
        ManifestSeeAlsoDto seeAlso2 = new ManifestSeeAlsoDto();
        seeAlso2.setId("jm21W");
        seeAlso2.setType("krug9C");
        seeAlso2.setFormat("XE3YVwJ8USv");
        seeAlso2.setProfile("Xi54jx");
        seeAlso.add(seeAlso2);
        provider2.setSeeAlso(seeAlso);
        provider.add(provider2);
        response.setProvider(provider);
        List<ManifestHomepageDto> homepage3 = new ArrayList<>();
        ManifestHomepageDto homepage4 = new ManifestHomepageDto();
        homepage4.setId("Bx6r9");
        homepage4.setType("G9cr0");
        homepage4.setLabel("fKX1X89Pr");
        homepage4.setFormat("y1bDZ");
        homepage3.add(homepage4);
        response.setHomepage(homepage3);
        List<ManifestServiceDto> service5 = new ArrayList<>();
        ManifestServiceDto service6 = new ManifestServiceDto();
        service6.setId("p99Jo");
        service6.setType("hN34c");
        service6.setProfile("dYg173");
        service5.add(service6);
        response.setService(service5);
        List<ManifestSeeAlsoDto> seeAlso3 = new ArrayList<>();
        ManifestSeeAlsoDto seeAlso4 = new ManifestSeeAlsoDto();
        seeAlso4.setId("JcifN");
        seeAlso4.setType("sRV4Z6AyfTon");
        seeAlso4.setFormat("SpsXBz9");
        seeAlso4.setProfile("iyi8v");
        seeAlso3.add(seeAlso4);
        response.setSeeAlso(seeAlso3);
        List<ManifestRenderingDto> rendering = new ArrayList<>();
        ManifestRenderingDto rendering2 = new ManifestRenderingDto();
        rendering2.setId("h71CG11ONgE");
        rendering2.setType("j6O6aaSf");
        rendering2.setLabel("vY1iEGE");
        rendering2.setFormat("E0sJ645");
        rendering.add(rendering2);
        response.setRendering(rendering);
        List<ManifestPartOfDto> partOf = new ArrayList<>();
        ManifestPartOfDto partOf2 = new ManifestPartOfDto();
        partOf2.setId("W7WB84L");
        partOf2.setType("G5Gwrd85o8Qv");
        partOf.add(partOf2);
        response.setPartOf(partOf);
        ManifestStartDto start = new ManifestStartDto();
        start.setId("XR70Y");
        start.setType("o8vpu");
        response.setStart(start);
        List<ManifestServicesDto> services = new ArrayList<>();
        ManifestServicesDto services2 = new ManifestServicesDto();
        services2.setAtId("bE5Km");
        services2.setAtType("L150zE");
        services2.setProfile("Mpr0864s");
        services2.setLabel("KRiN6wt6");
        ManifestService1Dto service7 = new ManifestService1Dto();
        service7.setAtId("wQ7xll861EC0");
        service7.setAtType("w3jd7F");
        service7.setProfile("FBmgjS");
        services2.setService(service7);
        services.add(services2);
        response.setServices(services);
        List<CanvasDto> items = new ArrayList<>();
        CanvasDto items2 = new CanvasDto();
        items2.setId("B7c4d1");
        items2.setType("KTsyp9");
        CanvasLabelDto label2 = new CanvasLabelDto();
        List<String> none = new ArrayList<>();
        none.add("KMo7R");
        label2.setNone(none);
        items2.setLabel(label2);
        items2.setHeight(2089194619);
        items2.setWidth(1956909659);
        List<CanvasItemsDto> items3 = new ArrayList<>();
        CanvasItemsDto items4 = new CanvasItemsDto();
        items4.setId("zRR3p");
        items4.setType("ru0I55vs");
        List<String> items5 = new ArrayList<>();
        items5.add("y106N");
        items4.setItems(items5);
        items3.add(items4);
        items2.setItems(items3);
        List<CanvasItemsDto> annotations = new ArrayList<>();
        CanvasItemsDto annotations2 = new CanvasItemsDto();
        annotations2.setId("yP8UzS");
        annotations2.setType("Dy3v5MrQA");
        List<String> items6 = new ArrayList<>();
        items6.add("aAbyL");
        annotations2.setItems(items6);
        annotations.add(annotations2);
        items2.setAnnotations(annotations);
        items.add(items2);
        response.setItems(items);
        List<RangeDto> structures = new ArrayList<>();
        RangeDto structures2 = new RangeDto();
        structures2.setId("enX96Aj");
        structures2.setType("G3e01jm8");
        RangeLabelDto label3 = new RangeLabelDto();
        label3.setEn("Pzz3sC");
        structures.add(structures2);
        response.setStructures(structures);
        AnnotationsDto annotations3 = new AnnotationsDto();
        annotations3.setId("C030E");
        annotations3.setType("LOvW7");
        List<AnnotationsItemsDto> items7 = new ArrayList<>();
        AnnotationsItemsDto items8 = new AnnotationsItemsDto();
        items8.setId("DjCZCUQ7t");
        items8.setType("ZP4G5");
        items8.setMotivation("eN0tH");
        AnnotationsBodyDto body = new AnnotationsBodyDto();
        body.setType("h2UdF0");
        body.setLanguage("J51e20E7I");
        body.setValue("ykLE3gf6t");
        items8.setBody(body);
        items8.setTarget("fCW9UP");
        items7.add(items8);
        annotations3.setItems(items7);
        response.setAnnotations(annotations3);
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

    /*
     * Hand held handler for IIIF IDs containing non-escaped slashes
     */
    @GET
    @Path("/IIIF/{nonescaped:.*}")
    @Produces({ "application/json" })
    @ApiOperation(value = "IIIF Presentation manifest fallback", tags={ "IIIFPresentation" })
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = ManifestDto.class) })
    public ManifestDto getPresentationManifestNonescaped(@PathParam("nonescaped") String nonescaped)
            throws ServiceException {
        Matcher m = NONESCAPED_MANIFEST_PATH.matcher(nonescaped);
        if (!m.matches()) {
            throw new InvalidArgumentServiceException(
                    "The endpoint IIIF/ expected an input of the form 'foo/manifest' but got '" + nonescaped + "'");
        }
        log.debug("Re-routing nonescaped IIIF Manifest request '" + nonescaped + "'");
        return rawGetPresentationManifest(m.group(1));
    }
}
