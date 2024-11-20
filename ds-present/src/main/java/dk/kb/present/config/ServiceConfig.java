package dk.kb.present.config;

import java.io.IOException;
import java.util.List;

import dk.kb.present.View;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample configuration class using the Singleton pattern.
 * This should work well for most projects with non-dynamic properties.
 */
public class ServiceConfig {
    private static final Logger log = LoggerFactory.getLogger(ServiceConfig.class);

    /**
     * Besides parsing of YAML files using SnakeYAML, the YAML helper class provides convenience
     * methods like {@code getInteger("someKey", defaultValue)} and {@code getSubMap("config.sub1.sub2")}.
     */
    private static YAML serviceConfig;

    /**
     * To calculate which records that are allowed to be shown in the publicly available DR Archive platform, we use this value to check records against. Records with tvmeter
     * origin less than this value are considered ownProduction and can be shown in the DR Archive.
     */
    private static int ownProductionCode;

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

        ownProductionCode = setValidOwnProductionCode();
    }

    /**
     * Method to securely load own production code from config. Performs validation on the record being exactly four digits.
     * Logs a warning if the values is above 3400, which defines the upper limit for DR produced material.
     * @return the maxAllowedProductionCode from the backing YAML configuration file.
     */
    private static int setValidOwnProductionCode() {
        // Setting default value to include own, co- and enterprise production
        int valueFromConf = serviceConfig.getInteger("dr.ownProduction.maxAllowedProductionCode", 3400);
        if (valueFromConf > 3400){
            log.warn("The specified maxAllowedProductionCode is '{}' which is greater than 3400. This means that records produced by other broadcasters than DR can be marked as " +
                    "own production", valueFromConf);
        }

        if (valueFromConf >= 1000 && valueFromConf <= 9999) {
            return valueFromConf;
        } else {
            throw new IllegalArgumentException("Invalid own production code: '" + valueFromConf + "'. The own production code must be a four digit number.");
        }

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

    public static int getOwnProductionCode() {
        return ownProductionCode;
    }
}
