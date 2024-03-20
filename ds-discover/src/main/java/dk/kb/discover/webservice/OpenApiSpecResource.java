package dk.kb.discover.webservice;


import dk.kb.discover.config.ServiceConfig;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/test")
public class OpenApiSpecResource {

    public static final String APPLICATION_YAML = "application/yaml";
    private static final Pattern CONFIG_REPLACEMENT= Pattern.compile("\\$\\{config\\.([^}]+)\\}");

    @GET
    @Produces(APPLICATION_YAML)
    public Response getResource() {
        ServiceConfig.getConfig().get("url");
        InputStream yamlStream = Resolver.openFileFromClasspath("ds-discover-openapi_v1.yaml");
        String openApiSpec = YAML.parse(yamlStream).toString();

        Matcher matcher = CONFIG_REPLACEMENT.matcher(openApiSpec);

        StringBuffer replacedText = new StringBuffer();
        while (matcher.find()){
           String matchedGroup = matcher.group(1);
           String replacement = getReplacementForMatch(matchedGroup);
           matcher.appendReplacement(replacedText, replacement);
           System.out.println("Found a match for: " + matchedGroup);
        }
        matcher.appendTail(replacedText);

        Response.ResponseBuilder builder = Response.ok(replacedText.toString()).header("Content-Disposition", "inline; filename=openapi.yaml");
        return builder.build();
    }

    private String getReplacementForMatch(String matchedGroup) {
        try {
            YAML conf = YAML.resolveLayeredConfigs("conf/ds-discover-*.yaml");
            return conf.get(matchedGroup).toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}



