package dk.kb.present.api.v1.impl;

import dk.kb.present.api.v1.*;
import dk.kb.present.model.v1.AnnotationsBodyDto;
import dk.kb.present.model.v1.AnnotationsDto;
import dk.kb.present.model.v1.AnnotationsItemsDto;

import java.util.*;

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
import dk.kb.present.model.v1.ViewDto;

import dk.kb.present.webservice.exception.ServiceException;
import dk.kb.present.webservice.exception.InternalServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.io.File;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
 * ds-present
 *
 * <p>Metadata delivery for the Royal Danish Library 
 *
 */
public class IiifPresentationApiServiceImpl implements IiifPresentationApi {
    private Logger log = LoggerFactory.getLogger(this.toString());



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
     * @param identifier: The identifier of the requested image. This may be an ARK, URN, filename, or other identifier. Special characters must be URI encoded.
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
    public CollectionDto getPresentationCollection(String identifier, String name) throws ServiceException {
        // TODO: Implement...
    
        
        try { 
            CollectionDto response = new CollectionDto();
        response.setId("kyLrGHb7b");
        response.setPrefix("rGs1ZTpeuTR8xYo9");
        response.setDescription("yahbUi0");
        List<ViewDto> views = new ArrayList<>();
        ViewDto views2 = new ViewDto();
        views2.setId("y83vM");
        views2.setMime("yDew9b");
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
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public ManifestDto getPresentationManifest(String identifier) throws ServiceException {
        // TODO: Implement...
    
        
        try { 
            ManifestDto response = new ManifestDto();
        response.setAtContext("cnuael");
        response.setAtId("csnzEkK2iV");
        response.setAtType("fwRvZx");
        ManifestLabelDto label = new ManifestLabelDto();
        List<String> en = new ArrayList<>();
        en.add("C85bOh7");
        label.setEn(en);
        response.setLabel(label);
        List<ManifestMetadataDto> metadata = new ArrayList<>();
        ManifestMetadataDto metadata2 = new ManifestMetadataDto();
        metadata2.setLabel("fLn26");
        metadata2.setValue("Rnr1eSxS9");
        metadata.add(metadata2);
        response.setMetadata(metadata);
        response.setSummary("x7L2J");
        List<ManifestThumbnailDto> thumbnail = new ArrayList<>();
        ManifestThumbnailDto thumbnail2 = new ManifestThumbnailDto();
        thumbnail2.setId("o1Mfzw");
        thumbnail2.setType("d438x3");
        thumbnail2.setFormat("JcvZ9nMFKE7H");
        List<ManifestServiceDto> service = new ArrayList<>();
        ManifestServiceDto service2 = new ManifestServiceDto();
        service2.setId("jt9BP");
        service2.setType("vJ335f4");
        service2.setProfile("VUbsztS");
        service.add(service2);
        thumbnail2.setService(service);
        thumbnail.add(thumbnail2);
        response.setThumbnail(thumbnail);
        response.setViewingDirection("Tb0QY");
        response.setViewingHint("T576oo");
        Date navDate = new Date(0);
        response.setNavDate(navDate);
        response.setRights("Zv01vWR");
        ManifestRequiredStatementDto requiredStatement = new ManifestRequiredStatementDto();
        requiredStatement.setLabel("XpuaXg");
        requiredStatement.setValue("rQ4A5");
        response.setRequiredStatement(requiredStatement);
        List<ManifestProviderDto> provider = new ArrayList<>();
        ManifestProviderDto provider2 = new ManifestProviderDto();
        provider2.setId("HMfHC");
        provider2.setType("Wa9FGt86");
        provider2.setLabel("bFwraMT");
        List<ManifestHomepageDto> homepage = new ArrayList<>();
        ManifestHomepageDto homepage2 = new ManifestHomepageDto();
        homepage2.setId("KW69X3V");
        homepage2.setType("b6c7uj");
        homepage2.setLabel("W4v6h");
        homepage2.setFormat("P5GYn");
        homepage.add(homepage2);
        provider2.setHomepage(homepage);
        List<ManifestThumbnailDto> logo = new ArrayList<>();
        ManifestThumbnailDto logo2 = new ManifestThumbnailDto();
        logo2.setId("Gl9s134");
        logo2.setType("eB51jd");
        logo2.setFormat("JR8UJ");
        List<ManifestServiceDto> service3 = new ArrayList<>();
        ManifestServiceDto service4 = new ManifestServiceDto();
        service4.setId("K3kD6n7Q2");
        service4.setType("K6t78");
        service4.setProfile("qH9hn7R605m");
        service3.add(service4);
        logo2.setService(service3);
        logo.add(logo2);
        provider2.setLogo(logo);
        List<ManifestSeeAlsoDto> seeAlso = new ArrayList<>();
        ManifestSeeAlsoDto seeAlso2 = new ManifestSeeAlsoDto();
        seeAlso2.setId("EWu5TYN");
        seeAlso2.setType("QG29uq");
        seeAlso2.setFormat("pVHU89A7KJz");
        seeAlso2.setProfile("gF9Lv");
        seeAlso.add(seeAlso2);
        provider2.setSeeAlso(seeAlso);
        provider.add(provider2);
        response.setProvider(provider);
        List<ManifestHomepageDto> homepage3 = new ArrayList<>();
        ManifestHomepageDto homepage4 = new ManifestHomepageDto();
        homepage4.setId("vKWW8pGar8C");
        homepage4.setType("ne8c70");
        homepage4.setLabel("EEi7zz0XiI");
        homepage4.setFormat("Zyw17c");
        homepage3.add(homepage4);
        response.setHomepage(homepage3);
        List<ManifestServiceDto> service5 = new ArrayList<>();
        ManifestServiceDto service6 = new ManifestServiceDto();
        service6.setId("FbJSVvw");
        service6.setType("Q9Ljb");
        service6.setProfile("K9Ur89xQe24");
        service5.add(service6);
        response.setService(service5);
        List<ManifestSeeAlsoDto> seeAlso3 = new ArrayList<>();
        ManifestSeeAlsoDto seeAlso4 = new ManifestSeeAlsoDto();
        seeAlso4.setId("f1ai6E31T0");
        seeAlso4.setType("d0P3117");
        seeAlso4.setFormat("YnOc5");
        seeAlso4.setProfile("dFpt0Hq");
        seeAlso3.add(seeAlso4);
        response.setSeeAlso(seeAlso3);
        List<ManifestRenderingDto> rendering = new ArrayList<>();
        ManifestRenderingDto rendering2 = new ManifestRenderingDto();
        rendering2.setId("z5ac75");
        rendering2.setType("uXbTl");
        rendering2.setLabel("bj0gLj9sO41a");
        rendering2.setFormat("HaU1X");
        rendering.add(rendering2);
        response.setRendering(rendering);
        List<ManifestPartOfDto> partOf = new ArrayList<>();
        ManifestPartOfDto partOf2 = new ManifestPartOfDto();
        partOf2.setId("SkF9Mf");
        partOf2.setType("U7j4w");
        partOf.add(partOf2);
        response.setPartOf(partOf);
        ManifestStartDto start = new ManifestStartDto();
        start.setId("xCUrpl");
        start.setType("kr3t3i");
        response.setStart(start);
        List<ManifestServicesDto> services = new ArrayList<>();
        ManifestServicesDto services2 = new ManifestServicesDto();
        services2.setAtId("eNqVkTW");
        services2.setAtType("J8NjM");
        services2.setProfile("nTIa6");
        services2.setLabel("e7ZQ4");
        ManifestService1Dto service7 = new ManifestService1Dto();
        service7.setAtId("boLR2h");
        service7.setAtType("pI6VrqG562");
        service7.setProfile("JeVF6");
        services2.setService(service7);
        services.add(services2);
        response.setServices(services);
        List<CanvasDto> items = new ArrayList<>();
        CanvasDto items2 = new CanvasDto();
        items2.setId("qUkkA");
        items2.setType("Ag01t");
        CanvasLabelDto label2 = new CanvasLabelDto();
        List<String> none = new ArrayList<>();
        none.add("S8xClW6QoCwXb");
        label2.setNone(none);
        items2.setLabel(label2);
        items2.setHeight(-2013198855);
        items2.setWidth(-500300530);
        List<CanvasItemsDto> items3 = new ArrayList<>();
        CanvasItemsDto items4 = new CanvasItemsDto();
        items4.setId("RMHSYHBX");
        items4.setType("bdjKf");
        List<String> items5 = new ArrayList<>();
        items5.add("Knn5L");
        items4.setItems(items5);
        items3.add(items4);
        items2.setItems(items3);
        List<CanvasItemsDto> annotations = new ArrayList<>();
        CanvasItemsDto annotations2 = new CanvasItemsDto();
        annotations2.setId("N9H742wxfw10");
        annotations2.setType("wSRdiy");
        List<String> items6 = new ArrayList<>();
        items6.add("Wh29rb85");
        annotations2.setItems(items6);
        annotations.add(annotations2);
        items2.setAnnotations(annotations);
        items.add(items2);
        response.setItems(items);
        List<List<Object>> structures = new ArrayList<>();
        List<Object> structures2 = new ArrayList<>();
        Object structures3 = JsonNodeFactory.instance.objectNode();
        structures2.add(structures3);
        structures.add(structures2);
        response.setStructures(structures);
        AnnotationsDto annotations3 = new AnnotationsDto();
        annotations3.setId("EEYU79");
        annotations3.setType("x0vv0");
        List<AnnotationsItemsDto> items7 = new ArrayList<>();
        AnnotationsItemsDto items8 = new AnnotationsItemsDto();
        items8.setId("kv289n");
        items8.setType("nf27Q");
        items8.setMotivation("ITW1Kb");
        AnnotationsBodyDto body = new AnnotationsBodyDto();
        body.setType("YQho39");
        body.setLanguage("Zz1jB");
        body.setValue("j0E0G");
        items8.setBody(body);
        items8.setTarget("jUq3VvdT");
        items7.add(items8);
        annotations3.setItems(items7);
        response.setAnnotations(annotations3);
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
    }


    /**
    * This method simply converts any Exception into a Service exception
    * @param e: Any kind of exception
    * @return A ServiceException
    * @see dk.kb.present.webservice.ServiceExceptionMapper
    */
    private ServiceException handleException(Exception e) {
        if (e instanceof ServiceException) {
            return (ServiceException) e; // Do nothing - this is a declared ServiceException from within module.
        } else {// Unforseen exception (should not happen). Wrap in internal service exception
            log.error("ServiceException(HTTP 500):", e); //You probably want to log this.
            return new InternalServiceException(e.getMessage());
        }
    }

}
