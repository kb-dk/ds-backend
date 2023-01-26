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
     * IIIF Presentation Image Resources
     * 
     * @param identifier: The identifier of the requested image. This may be an ARK, URN, filename, or other identifier. Special characters must be URI encoded.
     * 
     * @param name: The {name} parameter in the URI structure must distinguish it from any other sequences that may be available for the physical object. Typical default names for sequences are “normal” or “basic”.
     * 
     * @return <ul>
      *   <li>code = 200, message = "Succes!", response = AnnotationsDto.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public AnnotationsDto getPresentationAnnotation(String identifier, String name) throws ServiceException {
        // TODO: Implement...
    
        
        try { 
            AnnotationsDto response = new AnnotationsDto();
        response.setId("r7877n7");
        response.setType("yqyZDoUT31nRP");
        List<AnnotationsItemsDto> items = new ArrayList<>();
        AnnotationsItemsDto items2 = new AnnotationsItemsDto();
        items2.setId("Lb97SV7t1A68w67b1");
        items2.setType("Q62f7tAYF");
        items2.setMotivation("oUIN3");
        AnnotationsBodyDto body = new AnnotationsBodyDto();
        body.setType("kw6CVOb1NUR427y");
        body.setLanguage("Q140o");
        body.setValue("s1k4p1");
        items2.setBody(body);
        items2.setTarget("SC7u3");
        items.add(items2);
        response.setItems(items);
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

    /**
     * IIIF Presentation canvas
     * 
     * @param identifier: The identifier of the requested image. This may be an ARK, URN, filename, or other identifier. Special characters must be URI encoded.
     * 
     * @param name: The {name} parameter in the URI structure must distinguish it from any other sequences that may be available for the physical object. Typical default names for sequences are “normal” or “basic”.
     * 
     * @return <ul>
      *   <li>code = 200, message = "Succes!", response = CanvasDto.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public CanvasDto getPresentationCanvas(String identifier, String name) throws ServiceException {
        // TODO: Implement...
    
        
        try { 
            CanvasDto response = new CanvasDto();
        response.setId("R5wIQ");
        response.setType("Kd2RZ");
        CanvasLabelDto label = new CanvasLabelDto();
        List<String> none = new ArrayList<>();
        none.add("Ip9uS");
        label.setNone(none);
        response.setLabel(label);
        response.setHeight(-803599505);
        response.setWidth(146128940);
        List<CanvasItemsDto> items = new ArrayList<>();
        CanvasItemsDto items2 = new CanvasItemsDto();
        items2.setId("lnxeh4");
        items2.setType("t3L1w");
        List<String> items3 = new ArrayList<>();
        items3.add("grWVr");
        items2.setItems(items3);
        items.add(items2);
        response.setItems(items);
        List<CanvasItemsDto> annotations = new ArrayList<>();
        CanvasItemsDto annotations2 = new CanvasItemsDto();
        annotations2.setId("tKMtZ14");
        annotations2.setType("mD2vB90RJ");
        List<String> items4 = new ArrayList<>();
        items4.add("B14JkD");
        annotations2.setItems(items4);
        annotations.add(annotations2);
        response.setAnnotations(annotations);
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

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
        response.setId("SS2oF");
        response.setPrefix("ylbz4d3WL");
        response.setDescription("UvPEpc9");
        List<ViewDto> views = new ArrayList<>();
        ViewDto views2 = new ViewDto();
        views2.setId("Ps66E3iss");
        views2.setMime("Ws0oPD");
        views.add(views2);
        response.setViews(views);
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

    /**
     * IIIF Presentation Annotation List
     * 
     * @param identifier: The identifier of the requested image. This may be an ARK, URN, filename, or other identifier. Special characters must be URI encoded.
     * 
     * @param name: The {name} parameter in the URI structure must distinguish it from any other sequences that may be available for the physical object. Typical default names for sequences are “normal” or “basic”.
     * 
     * @return <ul>
      *   <li>code = 200, message = "Succes!"</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public void getPresentationList(String identifier, String name) throws ServiceException {
        // TODO: Implement...
    
        
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
        response.setAtContext("qR1DVW3z");
        response.setAtId("s1DULc5gB1o");
        response.setAtType("T4322Z1uq");
        ManifestLabelDto label = new ManifestLabelDto();
        List<String> en = new ArrayList<>();
        en.add("auA4f");
        label.setEn(en);
        response.setLabel(label);
        List<ManifestMetadataDto> metadata = new ArrayList<>();
        ManifestMetadataDto metadata2 = new ManifestMetadataDto();
        metadata2.setLabel("e4R3gJn0ri");
        metadata2.setValue("S0keg");
        metadata.add(metadata2);
        response.setMetadata(metadata);
        response.setSummary("YUvNU1");
        List<ManifestThumbnailDto> thumbnail = new ArrayList<>();
        ManifestThumbnailDto thumbnail2 = new ManifestThumbnailDto();
        thumbnail2.setId("w36jJ6f7");
        thumbnail2.setType("aP6M76");
        thumbnail2.setFormat("x5rIw");
        List<ManifestServiceDto> service = new ArrayList<>();
        ManifestServiceDto service2 = new ManifestServiceDto();
        service2.setId("d4A0FZ");
        service2.setType("Txo0od");
        service2.setProfile("i2wDEH");
        service.add(service2);
        thumbnail2.setService(service);
        thumbnail.add(thumbnail2);
        response.setThumbnail(thumbnail);
        response.setViewingDirection("I3c6i");
        response.setViewingHint("ISmYEJA");
        Date navDate = new Date(0);
        response.setNavDate(navDate);
        response.setRights("q2nTon4O27");
        ManifestRequiredStatementDto requiredStatement = new ManifestRequiredStatementDto();
        requiredStatement.setLabel("R6028");
        requiredStatement.setValue("CwJ5e");
        response.setRequiredStatement(requiredStatement);
        List<ManifestProviderDto> provider = new ArrayList<>();
        ManifestProviderDto provider2 = new ManifestProviderDto();
        provider2.setId("S54Wu");
        provider2.setType("l2JwC");
        provider2.setLabel("UEJJo1E");
        List<ManifestHomepageDto> homepage = new ArrayList<>();
        ManifestHomepageDto homepage2 = new ManifestHomepageDto();
        homepage2.setId("R2z4P0");
        homepage2.setType("jLuql");
        homepage2.setLabel("Q33KU");
        homepage2.setFormat("O42UI");
        homepage.add(homepage2);
        provider2.setHomepage(homepage);
        List<ManifestThumbnailDto> logo = new ArrayList<>();
        ManifestThumbnailDto logo2 = new ManifestThumbnailDto();
        logo2.setId("Ia38A803");
        logo2.setType("kMob2sdLf");
        logo2.setFormat("sj05d");
        List<ManifestServiceDto> service3 = new ArrayList<>();
        ManifestServiceDto service4 = new ManifestServiceDto();
        service4.setId("lX4I0T7b");
        service4.setType("bO218");
        service4.setProfile("q4wBvMh");
        service3.add(service4);
        logo2.setService(service3);
        logo.add(logo2);
        provider2.setLogo(logo);
        List<ManifestSeeAlsoDto> seeAlso = new ArrayList<>();
        ManifestSeeAlsoDto seeAlso2 = new ManifestSeeAlsoDto();
        seeAlso2.setId("g7j15");
        seeAlso2.setType("Lg6rb70");
        seeAlso2.setFormat("WSI49Wb");
        seeAlso2.setProfile("K89X0t");
        seeAlso.add(seeAlso2);
        provider2.setSeeAlso(seeAlso);
        provider.add(provider2);
        response.setProvider(provider);
        List<ManifestHomepageDto> homepage3 = new ArrayList<>();
        ManifestHomepageDto homepage4 = new ManifestHomepageDto();
        homepage4.setId("Zhj4Z21");
        homepage4.setType("sr6Cr");
        homepage4.setLabel("KmSpx");
        homepage4.setFormat("tA518P4");
        homepage3.add(homepage4);
        response.setHomepage(homepage3);
        List<ManifestServiceDto> service5 = new ArrayList<>();
        ManifestServiceDto service6 = new ManifestServiceDto();
        service6.setId("kL5utv");
        service6.setType("OFYoz");
        service6.setProfile("zM6BsL");
        service5.add(service6);
        response.setService(service5);
        List<ManifestSeeAlsoDto> seeAlso3 = new ArrayList<>();
        ManifestSeeAlsoDto seeAlso4 = new ManifestSeeAlsoDto();
        seeAlso4.setId("H99Lxt");
        seeAlso4.setType("bU0bT");
        seeAlso4.setFormat("Fg13ZB");
        seeAlso4.setProfile("WU4i055");
        seeAlso3.add(seeAlso4);
        response.setSeeAlso(seeAlso3);
        List<ManifestRenderingDto> rendering = new ArrayList<>();
        ManifestRenderingDto rendering2 = new ManifestRenderingDto();
        rendering2.setId("T3Pfg9J3l");
        rendering2.setType("Dzw0m8");
        rendering2.setLabel("m27oLo6z5");
        rendering2.setFormat("NSbZTYYI7f90");
        rendering.add(rendering2);
        response.setRendering(rendering);
        List<ManifestPartOfDto> partOf = new ArrayList<>();
        ManifestPartOfDto partOf2 = new ManifestPartOfDto();
        partOf2.setId("xR9Ox");
        partOf2.setType("jGvAT8");
        partOf.add(partOf2);
        response.setPartOf(partOf);
        ManifestStartDto start = new ManifestStartDto();
        start.setId("p60M5Ctg78v5P");
        start.setType("XDs7u86g");
        response.setStart(start);
        List<ManifestServicesDto> services = new ArrayList<>();
        ManifestServicesDto services2 = new ManifestServicesDto();
        services2.setAtId("h15hgRo3");
        services2.setAtType("fssaP1");
        services2.setProfile("VD7SPeQ4Zf");
        services2.setLabel("SwJrl");
        ManifestService1Dto service7 = new ManifestService1Dto();
        service7.setAtId("Tzh625TC7");
        service7.setAtType("x67C4");
        service7.setProfile("XmrW7ROo");
        services2.setService(service7);
        services.add(services2);
        response.setServices(services);
        List<CanvasDto> items = new ArrayList<>();
        CanvasDto items2 = new CanvasDto();
        items2.setId("q8uvnjZ");
        items2.setType("q7WWE");
        CanvasLabelDto label2 = new CanvasLabelDto();
        List<String> none = new ArrayList<>();
        none.add("a9RrMl");
        label2.setNone(none);
        items2.setLabel(label2);
        items2.setHeight(-740510432);
        items2.setWidth(785874991);
        List<CanvasItemsDto> items3 = new ArrayList<>();
        CanvasItemsDto items4 = new CanvasItemsDto();
        items4.setId("R5Q21");
        items4.setType("N7oRl");
        List<String> items5 = new ArrayList<>();
        items5.add("xn22yG");
        items4.setItems(items5);
        items3.add(items4);
        items2.setItems(items3);
        List<CanvasItemsDto> annotations = new ArrayList<>();
        CanvasItemsDto annotations2 = new CanvasItemsDto();
        annotations2.setId("Qqb53");
        annotations2.setType("YERjAp");
        List<String> items6 = new ArrayList<>();
        items6.add("LVRwvD");
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
        annotations3.setId("wOZ7f");
        annotations3.setType("o1le7N");
        List<AnnotationsItemsDto> items7 = new ArrayList<>();
        AnnotationsItemsDto items8 = new AnnotationsItemsDto();
        items8.setId("paiM5");
        items8.setType("G9QsN");
        items8.setMotivation("Ciag1U");
        AnnotationsBodyDto body = new AnnotationsBodyDto();
        body.setType("nj4N4");
        body.setLanguage("hDtzgZk5d");
        body.setValue("T2TGf");
        items8.setBody(body);
        items8.setTarget("lMjT8AR4");
        items7.add(items8);
        annotations3.setItems(items7);
        response.setAnnotations(annotations3);
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

    /**
     * IIIF Presentation Range
     * 
     * @param identifier: The identifier of the requested image. This may be an ARK, URN, filename, or other identifier. Special characters must be URI encoded.
     * 
     * @param name: The {name} parameter in the URI structure must distinguish it from any other sequences that may be available for the physical object. Typical default names for sequences are “normal” or “basic”.
     * 
     * @return <ul>
      *   <li>code = 200, message = "Succes!", response = Object.class, responseContainer = "List"</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public List<Object> getPresentationRange(String identifier, String name) throws ServiceException {
        // TODO: Implement...
    
        
        try { 
            List<Object> response = new ArrayList<>();
        Object item = JsonNodeFactory.instance.objectNode();
        response.add(item);
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
