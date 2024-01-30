package dk.kb.discover.api.v1.impl;

import dk.kb.discover.api.v1.ServiceApi;
import dk.kb.discover.model.v1.StatusDto;
import dk.kb.util.BuildInfoManager;
import dk.kb.util.webservice.ImplBase;
import dk.kb.util.webservice.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * ds-discover
 *
 * <p>ds-discover by the Royal Danish Library 
 *
 */
public class ServiceApiServiceImpl extends ImplBase implements ServiceApi {
    private final Logger log = LoggerFactory.getLogger(this.toString());

    /**
     * Ping the server to check if the server is reachable.
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public String ping() throws ServiceException {
        try {
            log.debug("ping() called with call details: {}", getCallDetails());
            return "Pong";
        } catch (Exception e){
            throw handleException(e);
        }
    }

    /**
     * Detailed status / health check for the service
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK", response = StatusDto.class</li>
      *   <li>code = 500, message = "Internal Error", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public StatusDto status() throws ServiceException {
        try {
            log.debug("status() called with call details: {}", getCallDetails());
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
                    .heap(Runtime.getRuntime().maxMemory()/1048576L)
                    .server(host)
                    .gitCommitChecksum(BuildInfoManager.getGitCommitChecksum())
                    .gitBranch(BuildInfoManager.getGitBranch())
                    .gitClosestTag(BuildInfoManager.getGitClosestTag())
                    .gitCurrentTag(BuildInfoManager.getGitCurrentTag())
                    .gitCommitTime(BuildInfoManager.getGitCommitTime())
                    .health("ok");
        } catch (Exception e){
            throw handleException(e);
        }
    }

}
