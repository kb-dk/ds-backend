package dk.kb.util.webservice;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dk.kb.util.Resolver;
import dk.kb.util.string.CallbackReplacer;
import dk.kb.util.string.Strings;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import org.eclipse.microprofile.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handle serving of OpenAPI specification for a webapp. This class handles dynamic updates of the API specification.
 * Through this class it gets possible to use syntax as the following {@code ${config:yaml.path}} to access values from
 * config files in API specifications. A wildcard segment is also supported, e.g. {@code ${config:origins[*].name}},
 * which is expanded to every matching indexed property (see {@link #resolveWildcard(String)}).
 * <p>
 * JAX-RS uses the empty constructor for serving the webapp. To configure the {@link OpenApiResource} call the
 * {@link #setConfig(Config)}-method inside the given implementation of {@link Application#getClasses()} before
 * returning all classes that are part of the application. An example is provided here:
 *
 * <pre>
 *     public Set<Class<?>> getClasses() {
 *         OpenApiResource.setConfig(ServiceConfig.getConfig());
 *
 *         return new HashSet<>(Arrays.asList(
 *                 JacksonJsonProvider.class,
 *                 JacksonXMLProvider.class,
 *                 DsDiscoverApiServiceImpl.class,
 *                 ServiceApiServiceImpl.class,
 *                 ServiceExceptionMapper.class,
 *                 OpenApiResource.class
 *         ));
 * </pre>
 */
public class OpenApiResource extends ImplBase {
    private static final Logger log = LoggerFactory.getLogger(OpenApiResource.class);

    /**
     * The config where values are substituted from.
     * <p>
     * This is a standard MicroProfile Config {@link Config}, not tied to any specific implementation (the
     * concrete implementation, e.g. SmallRye Config, is chosen and wired by whichever project calls
     * {@link #setConfig(Config)}).
     */
    static private Config config;

    public static final String APPLICATION_YAML = "application/yaml";

    /**
     * Pattern to allow search-replace for variabels defined as ${config.yaml.path} in OpenAPI specifications.
     * Everything after 'config.' is treated as a path to an entry in the backing configuration.
     */
    private static final Pattern CONFIG_REPLACEMENT= Pattern.compile("\\$\\{config:([^}]+)}");

    /**
     * Replacer that use {@link #CONFIG_REPLACEMENT} for matching and {@link #getReplacementForMatch(String)}
     * for getting the replacement.
     */
    private static final CallbackReplacer CONFIG_PROCESSOR = new CallbackReplacer(
            CONFIG_REPLACEMENT, OpenApiResource::getReplacementForMatch, true);


    /**
     * JAX-RS uses the empty constructor for serving the webapp. To configure the {@link OpenApiResource} call the
     * {@link #setConfig(Config)}-method inside the given implementation of {@link Application#getClasses()} before
     * returning all classes that are part of the application. An example is provided here:
     *
     * <pre>
     *     public Set<Class<?>> getClasses() {
     *         OpenApiResource.setConfig(ServiceConfig.getConfig());
     *
     *         return new HashSet<>(Arrays.asList(
     *                 JacksonJsonProvider.class,
     *                 JacksonXMLProvider.class,
     *                 DsDiscoverApiServiceImpl.class,
     *                 ServiceApiServiceImpl.class,
     *                 ServiceExceptionMapper.class,
     *                 OpenApiResource.class
     *         ));
     * </pre>
     */
    public OpenApiResource(){}

    public static void setConfig(Config configSource){
        config = configSource;
    }

    /**
     * Deliver the OpenAPI specification with substituted configuration values as a YAML file.
     */
    @GET
    @Produces(APPLICATION_YAML)
    @Path("/{path}.yaml")
    public Response getYamlSpec(@PathParam("path") String path) {
        try {
            path = new File(path).getName();
            String inputYaml = Resolver.readFileFromClasspath(path + ".yaml");

            if (inputYaml == null){
                // We want to see if people are trying to hack their way in by trying different paths
                log.warn("No OpenAPI specification with path '{}' was found.", path);
                throw new FileNotFoundException("No OpenAPI specification with path '" + path + ".yaml' was found.");
            }

            String replacedText = replaceConfigPlaceholders(inputYaml);

            Response.ResponseBuilder builder = Response.ok(replacedText)
                    .header("Content-Disposition", "inline; filename=" + path + ".yaml");

            return builder.build();
        } catch (IOException | RuntimeException e){
            log.warn("Unable to dynamically enhance the YAML OpenAPI specification with path '{}'", path, e);
            throw new NotFoundServiceException(
                    "Unable to dynamically enhance the YAML OpenAPI specification with path '" + path + ".yaml'");
        }
    }

    /**
     * Deliver the OpenAPI specification with substituted configuration values as a JSON file.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{path}.json")
    public Response getJsonSpec(@PathParam("path") String path){
        try {
            return createJson(path);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    static Response createJson(String path) {
        try {
            path = new File(path).getName();
            String inputYaml = Resolver.readFileFromClasspath(path + ".yaml");

            if (inputYaml == null){
                // We want to see if people are trying to hack their way in by trying different paths
                log.warn("No OpenAPI specification with path '{}' was found.", path);
                throw new FileNotFoundException("No OpenAPI specification with path '" + path + ".yaml' was found.");
            }

            String correctString = OpenApiResource.replaceConfigPlaceholders(inputYaml);

            String jsonString = getJsonString(correctString);

            Response.ResponseBuilder builder = Response.ok(jsonString).header("Content-Disposition", "inline; filename=" + path + ".json");
            return builder.build();
        } catch (IOException | RuntimeException e){
            log.warn("Unable to dynamically enhance the YAML OpenAPI specification with path '{}'", path, e);
            throw new NotFoundServiceException(
                    "Unable to dynamically enhance the YAML OpenAPI specification with path '" + path + ".yaml'");
        }
    }

    /*@GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/openapi.json")
    public Response getJsonShorthand(){
        try {
            return createJson("ds-discover-openapi_v1");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }*/

    /**
     * Replace placeholders in the original OpenAPI YAML specification. These placeholders have the format ${config.yamlpath},
     * where the value inside the '{}' and after 'config.' is treated as a YAML path which is used to find the
     * replacement value in the backing configuration files.
     * @param originalApiSpec the content of the original YAML specification
     * @return an updated YAML string, where config placeholders have been replaced.
     */
    private static String replaceConfigPlaceholders(String originalApiSpec) {
        return CONFIG_PROCESSOR.apply(originalApiSpec);
    }

    /**
     * Resolve the value(s) for the given path in the configuration for the project.
     * <p>
     * Two forms are supported:
     * <ul>
     *     <li>A plain (or comma-list) property, e.g. {@code openapi.serverurl} or {@code security.realms}.</li>
     *     <li>A single wildcard segment, e.g. {@code origins[*].name}, which is expanded to every matching
     *     indexed property, in ascending index order. See {@link #resolveWildcard(String)}.</li>
     * </ul>
     *
     * @param yPath to extract value(s) from.
     * @return the value(s) at the given path in the configuration, joined the same way {@link #getYamlSpec} expects.
     */
    private static String getReplacementForMatch(String yPath) {
        if (config == null){
            throw new IllegalStateException("Config must be initialized before using the class. See JavaDoc for OpenApiResource for further details.");
        }

        List<String> result = yPath.contains("[*]") ?
                resolveWildcard(yPath) :
                config.getOptionalValues(yPath, String.class).orElse(List.of());

        if (result.isEmpty()){
            log.error("No entry has been found for yPath: '{}'.", yPath);
            throw new InvalidArgumentServiceException("No entry has been found for yPath: '{}'.", yPath);
        }

        // If there are more than one entry, then the entries are combined to a specially formatted comma seperated string.
        // All entries are seperated by ", " to make the openAPI generator see the input ["${config:yaml.string}"] as an
        // actual array resolved as ["foo", "bar", "zoo"]
        return Strings.join(result, "\", \"");
    }

    /**
     * Resolves a path containing a single {@code [*]} wildcard segment (e.g. {@code origins[*].name}) against
     * every configured index for that segment (e.g. the properties {@code origins[0].name}, {@code origins[1].name},
     * ...), which is how a YAML list-of-objects is flattened into individual configuration properties.
     * <p>
     * Note: only a single {@code [*]} occurrence per path is supported, which matches current usage in the
     * OpenAPI specifications.
     *
     * @param yPath a path containing exactly one {@code [*]} segment.
     * @return the matched values, ordered by ascending index. Empty if nothing matched.
     */
    private static List<String> resolveWildcard(String yPath) {
        String regex = "^" + Pattern.quote(yPath).replace("[*]", "\\E(\\d+)\\Q") + "$";
        Pattern indexed = Pattern.compile(regex);

        SortedMap<Integer, String> byIndex = new TreeMap<>();
        for (String name : config.getPropertyNames()) {
            Matcher m = indexed.matcher(name);
            if (m.matches()) {
                config.getOptionalValue(name, String.class)
                        .ifPresent(value -> byIndex.put(Integer.parseInt(m.group(1)), value));
            }
        }
        return new ArrayList<>(byIndex.values());
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
        return jsonMapper.enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(yamlObject);
    }
}


