package dk.kb.license.api.v1.impl;

import dk.kb.license.api.v1.*;
import dk.kb.license.facade.LicenseModuleFacade;
import dk.kb.license.model.v1.GetUserGroupsInputDto;
import dk.kb.license.model.v1.GetUserGroupsOutputDto;
import dk.kb.license.model.v1.CheckAccessForIdsInputDto;
import dk.kb.license.model.v1.CheckAccessForIdsOutputDto;
import dk.kb.license.model.v1.ErrorDto;

import dk.kb.license.model.v1.GetUserQueryInputDto;
import dk.kb.license.model.v1.GetUsersLicensesInputDto;
import dk.kb.license.model.v1.GetUsersLicensesOutputDto;

import java.io.File;
import dk.kb.license.model.v1.HelloReplyDto;
import dk.kb.license.model.v1.ValidateAccessInputDto;
import dk.kb.license.model.v1.ValidateAccessOutputDto;
import dk.kb.license.storage.ConfiguredLicensePresentationType;
import dk.kb.license.validation.LicenseValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dk.kb.license.webservice.exception.ServiceException;
import dk.kb.license.webservice.exception.InternalServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.io.File;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.validation.Valid;
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
import javax.ws.rs.core.StreamingOutput;
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
 * ds-license
 *
 * <p>ds-license by the Royal Danish Library 
 *
 */
public class DsLicenseApiServiceImpl implements DsLicenseApi {
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
     * Request a Hello World message, for testing purposes
     * 
     * @param alternateHello: Optional alternative to using the word &#39;Hello&#39; in the reply
     * 
     * @return <ul>
      *   <li>code = 200, message = "A JSON structure containing a Hello World message", response = HelloReplyDto.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public HelloReplyDto getGreeting(String alternateHello) throws ServiceException {
        // TODO: Implement...
    
        
        try { 
            HelloReplyDto response = new HelloReplyDto();
        response.setMessage("KbqLzzD6");
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

    /**
     * Ping the server to check if the server is reachable.
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK", response = String.class</li>
      *   <li>code = 406, message = "Not Acceptable", response = ErrorDto.class</li>
      *   <li>code = 500, message = "Internal Error", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public String ping() throws ServiceException {
        // TODO: Implement...
    
        
        try { 
            String response = "C8wES";
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

    @Override
    public CheckAccessForIdsOutputDto checkAccessForIds(@NotNull CheckAccessForIdsInputDto input) {
        try{
             
            ConfiguredLicensePresentationType presentationType = LicenseValidator.matchPresentationtype(input.getPresentationType());
          }
          catch(IllegalArgumentException e){
           log.error("Unknown presentationtype:"+input.getPresentationType());
           CheckAccessForIdsOutputDto output =  new CheckAccessForIdsOutputDto();
           output.setAccessIds(new ArrayList<String>());
           output.setPresentationType(input.getPresentationType());
           output.setQuery("(NoAccess:NoAccess)"); //query that returns nothing
              return output;
          }
          
          try {      
           CheckAccessForIdsOutputDto output = LicenseValidator.checkAccessForIds(input);      
                 return output;
          } catch (Exception e) {
           log.error("Error in checkAccessForIds:",e);
              throw handleException(e);
          }   
      
    }

    @Override
    public GetUsersLicensesOutputDto getUserLicenses(@NotNull GetUsersLicensesInputDto getUsersLicensesInput) {
        // TODO Auto-generated method stub
        return null;
    }

    
    @Override
    public ValidateAccessOutputDto validateAccess(@Valid ValidateAccessInputDto validateAccessInputDto) {
        // TODO Auto-generated method stub
        return null;
    }


    
    @Override
    public String getUserLicenseQuery(@NotNull GetUserQueryInputDto getUserQueryInput) {
        // TODO Auto-generated method stub
        return null;
    }

   

    @Override
    public GetUserGroupsOutputDto getUserGroups(GetUserGroupsInputDto getUserGroups) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public String extractStatistics() {
        // TODO Auto-generated method stub
        return null;
    }

    
    /**
    * This method simply converts any Exception into a Service exception
    * @param e: Any kind of exception
    * @return A ServiceException
    * @see dk.kb.license.webservice.ServiceExceptionMapper
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
