package dk.kb.discover.webservice;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;
import org.yaml.snakeyaml.Yaml;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenApiResource {

    public static final String APPLICATION_YAML = "application/yaml";

    /**
     * Pattern to allow search-replace for variabels defined as ${config.yaml.path} in OpenAPI specifications.
     * Everything after 'config.' is treated as a path to an entry in the backing configuration.
     */
    private static final Pattern CONFIG_REPLACEMENT= Pattern.compile("\\$\\{config:([^}]+)\\}");

    /**
     * Deliver the OpenAPI specification with substituted configuration values as a YAML file.
     */
    @GET
    @Produces(APPLICATION_YAML)
    @Path("/ds-discover-openapi_v1.yaml")
    public Response getYamlSpec() {
        InputStream yamlStream = Resolver.openFileFromClasspath("ds-discover-openapi_v1.yaml");
        String openApiSpec = YAML.parse(yamlStream).toString();

        String replacedText = replaceConfigPlaceholders(openApiSpec);

        Response.ResponseBuilder builder = Response.ok(replacedText).header("Content-Disposition", "inline; filename=ds-discover-openapi_v1.yaml");
        return builder.build();
    }

    /**
     * Deliver the OpenAPI specification with substituted configuration values as a JSON file.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{path: (ds-discover-openapi_v1.json|openapi.json)}")
    public Response getJsonSpec(){
        try {
            InputStream yamlStream = Resolver.openFileFromClasspath("ds-discover-openapi_v1.yaml");
            String openApiSpec = YAML.parse(yamlStream).toString();
            String correctString = OpenApiResource.replaceConfigPlaceholders(openApiSpec);

            String jsonString = getJsonString(correctString);

            Response.ResponseBuilder builder = Response.ok(jsonString).header("Content-Disposition", "inline; filename=openapi.json");
            return builder.build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    /**
     * Replace placeholders in the original OpenAPI YAML specification. These placeholders have the format ${config.yamlpath},
     * where the value inside the '{}' and after 'config.' is treated as a YAML path which is used to find the
     * replacement value in the backing configuration files.
     * @param originalApiSpec the content of the original YAML specification
     * @return an updated YAML string, where config placeholders have been replaced.
     */
    private static String replaceConfigPlaceholders(String originalApiSpec) {
        Matcher matcher = CONFIG_REPLACEMENT.matcher(originalApiSpec);

        StringBuilder replacedText = new StringBuilder();
        while (matcher.find()){
           String replacementPath = matcher.group(1);
           String replacement = getReplacementForMatch(replacementPath);
           matcher.appendReplacement(replacedText, replacement);
        }
        matcher.appendTail(replacedText);
        return replacedText.toString();
    }

    /**
     * Resolve the value for the given YAML path in the configuration files for the project.
     * @param yPath to extract value from.
     * @return the value at the given path in the configuration files.
     */
    private static String getReplacementForMatch(String yPath) {
        try {
            YAML conf = YAML.resolveLayeredConfigs("conf/ds-discover-*.yaml");
            return conf.get(yPath).toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert a YAML string to a JSON string
     * @param yamlString which is to be converted to JSON.
     * @return JSON representation of the input YAML.
     */
    private static String getJsonString(String yamlString) throws JsonProcessingException {
        Yaml yaml = new Yaml();
        Object yamlObject = yaml.load(yamlString);
        ObjectMapper jsonMapper = new ObjectMapper();
        return jsonMapper.writeValueAsString(yamlObject);
    }
}



