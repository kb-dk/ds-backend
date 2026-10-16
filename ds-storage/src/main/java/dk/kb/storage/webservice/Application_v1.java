package dk.kb.storage.webservice;

import dk.kb.shared.webservice.CustomJacksonJsonProvider;
import dk.kb.storage.api.v1.impl.RerunClusterApiServiceImpl;
import dk.kb.storage.api.v1.impl.TranscriptionApiServiceImpl;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import dk.kb.storage.api.v1.impl.RecordApiServiceImpl;
import dk.kb.storage.api.v1.impl.ServiceApiServiceImpl;
import dk.kb.util.webservice.OpenApiResource;
import dk.kb.util.webservice.exception.ServiceExceptionMapper;
import dk.kb.storage.config.ServiceConfig;

public class Application_v1 extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        OpenApiResource.setConfig(ServiceConfig.getConfig());

        return new HashSet<>(Arrays.asList(
                CustomJacksonJsonProvider.class,
                RecordApiServiceImpl.class,
                RerunClusterApiServiceImpl.class,
                ServiceApiServiceImpl.class,
                TranscriptionApiServiceImpl.class,
                ServiceExceptionMapper.class,
                OpenApiResource.class
        ));
    }
}
