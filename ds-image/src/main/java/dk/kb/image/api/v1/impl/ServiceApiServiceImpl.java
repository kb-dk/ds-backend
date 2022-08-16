package dk.kb.image.api.v1.impl;

import dk.kb.image.api.v1.ServiceApi;
import dk.kb.image.model.v1.StatusDto;
import dk.kb.util.BuildInfoManager;
import dk.kb.util.webservice.ImplBase;
import dk.kb.util.webservice.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Basic endpoints relevant for all services.
 * TODO: The {@link #status()} should be extended with application specific status information
 */
public class ServiceApiServiceImpl extends ImplBase implements ServiceApi {
    private static final Logger log = LoggerFactory.getLogger(ServiceApiServiceImpl.class);

    /**
     * Ping the server to check if the server is reachable.
     */
    @Override
    public String ping() throws ServiceException {
        try{
            return "Pong";
        } catch (Exception e){
            throw handleException(e);
        }
    }

    /**
     * Detailed status / health check for the service.
     *
     * The default implementation presents status information available to all web applications.
     * This should be extended with application specific information, such as number of running jobs or
     * current load.
     */
    @Override
    public StatusDto status() {
        String host = "N/A";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("Exception resolving hostname", e);
        }
        return new StatusDto()
                .application(BuildInfoManager.getName())
                .version(BuildInfoManager.getVersion())
                .build(BuildInfoManager.getBuildTime())
                .java(System.getProperty("java.version"))
                .heap(Runtime.getRuntime().maxMemory()/1000000L)
                .server(host)
                .health("ok");
    }
}
