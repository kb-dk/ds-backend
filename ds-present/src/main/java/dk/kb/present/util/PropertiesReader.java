package dk.kb.present.util;

import dk.kb.util.Resolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * PropertiesReader used to read properties from a given property file and then use them in the java code.
 */
public class PropertiesReader {
    private final Properties properties;

    /**
     * Construct a PropertiesReader reading properties from the file specified as the argument.
     * @param propertyFileName the path to the file containing properties to be loaded.
     */
    public PropertiesReader(String propertyFileName) throws IOException {
        InputStream propertyStream = Resolver.resolveStream(propertyFileName);
        Properties propertiesFromMaven = new Properties();
        propertiesFromMaven.load(propertyStream);
        this.properties = propertiesFromMaven;
    }

    /**
     * Get the value of a property.
     * @param propertyName of the value to be extracted.
     * @return the value of the property.
     */
    public String getProperty(String propertyName) {
        return this.properties.getProperty(propertyName);
    }
}
