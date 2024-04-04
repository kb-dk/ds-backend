package dk.kb.present.webservice;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import dk.kb.present.PresentFacade;
import dk.kb.present.api.v1.impl.DsPresentApiServiceImpl;
import dk.kb.present.api.v1.impl.IiifPresentationApiServiceImpl;
import dk.kb.present.api.v1.impl.ServiceApiServiceImpl;
import dk.kb.util.webservice.exception.ServiceExceptionMapper;


public class Application_v1 extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        PresentFacade.warmUp(); // Fail early
        return new HashSet<>(Arrays.asList(
                JacksonJsonProvider.class,
                DsPresentApiServiceImpl.class,
                ServiceApiServiceImpl.class,
                IiifPresentationApiServiceImpl.class,
                ServiceExceptionMapper.class,
                OpenApiResource.class
        ));
    }


}
