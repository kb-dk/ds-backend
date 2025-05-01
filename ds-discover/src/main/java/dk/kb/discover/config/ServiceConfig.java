package dk.kb.discover.config;

import java.io.IOException;
import javax.servlet.ServletContextEvent;

import dk.kb.util.yaml.AutoYAML;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample configuration class using the Singleton and Observer patterns.
 * See {@link {@url https://en.wikipedia.org/wiki/Observer_pattern}}
 *
 * If wanted, changes to the configuration source (typically files) can result in an update of the ServiceConfig and
 * a callback to relevant classes. To enable this, add autoupdate keys to the YAML config:
 * <pre>
 * autoupdate:
 *   enabled: true
 *   intervalms: 60000
 * </pre>
 * Notifications on config changes can be received using {@link #registerObserver(Observer)}.
 *
 * Alternatively {@link #AUTO_UPDATE_DEFAULT} and {@link #AUTO_UPDATE_MS_DEFAULT} can be set so that auto-update is
 * enabled by default for the application.
 *
 * Implementation note: Watching for changes is a busy-wait, i.e. the ServiceConfig actively reloads the configuration
 * each {@link #autoUpdateMS} milliseconds and checks if is has changed. This is necessary as the source for the
 * configuration is not guaranteed to be a file (it could be a URL or packed in a WAR instead), so watching for file
 * system changes is not solid enough. This also means that the check does have a non-trivial overhead so setting the
 * autoupdate interval to less than a minute is not recommended.
 */
public class ServiceConfig extends AutoYAML {
    private static final Logger log = LoggerFactory.getLogger(ServiceConfig.class);

    private static final boolean AUTO_UPDATE_DEFAULT = false;
    private static final long AUTO_UPDATE_MS_DEFAULT = 60*1000; // every minute

    
    private static ServiceConfig instance;
    /**
     * Construct a ServiceConfig without a concrete YAML assigned. In order to use the ServiceConfig,
     * {@link #initialize(String)} must be called. This is done automatically when the container is started
     * in {@link dk.kb.oauth.webservice.ContextListener#contextInitialized(ServletContextEvent)}, which
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
