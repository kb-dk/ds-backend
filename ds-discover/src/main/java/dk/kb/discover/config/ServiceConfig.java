package dk.kb.discover.config;

import java.io.IOException;
import javax.servlet.ServletContextEvent;

import dk.kb.util.yaml.AutoYAML;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceConfig extends AutoYAML {
    private static final Logger log = LoggerFactory.getLogger(ServiceConfig.class);

    private static final boolean AUTO_UPDATE_DEFAULT = false;
    private static final long AUTO_UPDATE_MS_DEFAULT = 60*1000; // every minute

    
    private static ServiceConfig instance;
    /**
     * Construct a ServiceConfig without a concrete YAML assigned. In order to use the ServiceConfig,
     * {@link #initialize(String)} must be called. This is done automatically when the container is started
     * in {@link dk.kb.discover.config.ServiceConfig}, which
     * takes the glob for the configurations from the property {@code application-config}.
     * @throws IOException if initialization failed.
     */
    public ServiceConfig() throws IOException {
        super(null, AUTO_UPDATE_DEFAULT, AUTO_UPDATE_MS_DEFAULT);
    }

   
    
    /**
     * @return singleton instance of ServiceConfig.
     */
    public static synchronized ServiceConfig getInstance() {
        if (instance == null) {
            try {
                instance = new ServiceConfig();
            } catch (IOException e) {
                throw new RuntimeException("Exception constructing instance", e);
            }
        }
        return instance;
    }
  
    /**
     * Direct access to the backing YAML-class is used for configurations with more flexible content
     * and/or if the service developer prefers key-based property access.
     * @see #getHelloLines() for alternative.
     * @return the backing YAML-handler for the configuration.
     */
    public static YAML getConfig() {

        if (getInstance().getYAML() == null) {
            throw new IllegalStateException("The configuration should have been loaded, but was not");
        }
        return getInstance().getYAML();
    }

}
