package dk.kb.present.config;

import java.io.IOException;
import java.util.List;

import dk.kb.util.yaml.YAML;

/**
 * Sample configuration class using the Singleton pattern.
 * This should work well for most projects with non-dynamic properties.
 */
public class ServiceConfig {

    /**
     * Besides parsing of YAML files using SnakeYAML, the YAML helper class provides convenience
     * methods like {@code getInteger("someKey", defaultValue)} and {@code getSubMap("config.sub1.sub2")}.
     */
    private static YAML serviceConfig;

    /**
     * Initialized the configuration from the provided configFiles.
     * This should normally be called from {@link dk.kb.present.webservice.ContextListener} as
     * part of web server initialization of the container.
     * @param configFiles globs for the configurations to load.
     * @throws IOException if the configurations could not be loaded or parsed.
     */
    public static synchronized void initialize(String... configFiles) throws IOException {
        serviceConfig = YAML.resolveLayeredConfigs(configFiles);
        serviceConfig.setExtrapolate(true);
    }

    /**
     * Demonstration of a first-class property, meaning that an explicit method has been provided.
     * @see #getConfig() for alternative.
     * @return the "Hello World" lines defined in the config file.
     */
    public static List<String> getHelloLines() {
        List<String> lines = serviceConfig.getList("helloLines");
        return lines;
    }

    /**
     * Direct access to the backing YAML-class is used for configurations with more flexible content
     * and/or if the service developer prefers key-based property access.
     * @see #getHelloLines() for alternative.
     * @return the backing YAML-handler for the configuration.
     */
    public static YAML getConfig() {
        if (serviceConfig == null) {
            throw new IllegalStateException("The configuration should have been loaded, but was not");
        }
        return serviceConfig;
    }
  
}
