package dk.kb.license.webservice;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jaxrs.xml.JacksonXMLProvider;
import dk.kb.license.api.v1.impl.DsAuditApiServiceImpl;
import dk.kb.license.api.v1.impl.DsLicenseApiServiceImpl;
import dk.kb.license.api.v1.impl.DsRightsApiServiceImpl;
import dk.kb.license.api.v1.impl.ServiceApiServiceImpl;
import dk.kb.license.config.ServiceConfig;
import dk.kb.util.webservice.OpenApiResource;
import dk.kb.util.webservice.exception.ServiceExceptionMapper;


public class Application_v1 extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        OpenApiResource.setConfig(ServiceConfig.getConfig());

        return new HashSet<>(Arrays.asList(
                JacksonJsonProvider.class,
                JacksonXMLProvider.class,
                DsLicenseApiServiceImpl.class,
                DsRightsApiServiceImpl.class,
                DsAuditApiServiceImpl.class,
                ServiceApiServiceImpl.class,
                ServiceExceptionMapper.class,
                OpenApiResource.class
        ));
    }


}
