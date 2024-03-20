package dk.kb.discover.webservice;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;

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
    private static final Pattern CONFIG_REPLACEMENT= Pattern.compile("\\$\\{config\\.([^}]+)\\}");

    @GET
    @Produces(APPLICATION_YAML)
    @Path("/ds-discover-openapi_v1.yaml")
    public Response getYamlSpec() {
        InputStream yamlStream = Resolver.openFileFromClasspath("ds-discover-openapi_v1.yaml");
        String openApiSpec = YAML.parse(yamlStream).toString();

        String replacedText = replaceConfigPlaceholders(openApiSpec);

        Response.ResponseBuilder builder = Response.ok(replacedText).header("Content-Disposition", "inline; filename=openapi.yaml");
        return builder.build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/ds-discover-openapi_v1.json")
    public Response getJsonSpec(){
        try {
            InputStream yamlStream = Resolver.openFileFromClasspath("ds-discover-openapi_v1.yaml");
            String openApiSpec = YAML.parse(yamlStream).toString();

            String correctString = OpenApiResource.replaceConfigPlaceholders(openApiSpec);

            // Create ObjectMapper for YAML
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

            // Read YAML string
            Object yamlObject = yamlMapper.readValue(correctString, Object.class);

            // Create ObjectMapper for JSON
            ObjectMapper jsonMapper = new ObjectMapper();

            // Convert YAML object to JSON
            String jsonString = jsonMapper.writeValueAsString(yamlObject);

            Response.ResponseBuilder builder = Response.ok(jsonString).header("Content-Disposition", "inline; filename=openapi.json");
            return builder.build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public static String replaceConfigPlaceholders(String openApiSpec) {
        Matcher matcher = CONFIG_REPLACEMENT.matcher(openApiSpec);

        StringBuilder replacedText = new StringBuilder();
        while (matcher.find()){
           String matchedGroup = matcher.group(1);
           String replacement = getReplacementForMatch(matchedGroup);
           matcher.appendReplacement(replacedText, replacement);
           System.out.println("Found a match for: " + matchedGroup);
        }
        matcher.appendTail(replacedText);
        return replacedText.toString();
    }

    private static String getReplacementForMatch(String matchedGroup) {
        try {
            YAML conf = YAML.resolveLayeredConfigs("conf/ds-discover-*.yaml");
            return conf.get(matchedGroup).toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}



