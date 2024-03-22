package dk.kb.discover;

import dk.kb.discover.config.ServiceConfig;
import dk.kb.discover.webservice.OpenApiResource;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WebserviceTest {

    @BeforeAll
    public static void setup() throws IOException {
        ServiceConfig.initialize("conf/ds-discover-*.yaml");
    }

    @Test
    public void testGetYamlSpec(){
        OpenApiResource apiResource = new OpenApiResource();
        String yamlSpec = apiResource.getYamlSpec("ds-discover-openapi_v1")
                            .getEntity().toString();

        assertFalse(yamlSpec.contains("${config:"));

    }

    @Test
    public void testGettingConfigWithPath(){
        OpenApiResource apiResource = new OpenApiResource();

        assertThrows(InvalidArgumentServiceException.class, () -> {
            apiResource.getYamlSpec("conf/ds-discover-behaviour")
                    .getEntity().toString();
        });
    }

    @Test
    public void testPathHacking(){
        OpenApiResource apiResource = new OpenApiResource();

        assertThrows(InvalidArgumentServiceException.class, () -> {
            apiResource.getYamlSpec("secret/very")
                    .getEntity().toString();
        });
    }

    @Test
    public void testGettingConfigWithoutPath(){
        OpenApiResource apiResource = new OpenApiResource();

        assertThrows(InvalidArgumentServiceException.class, () -> {
            apiResource.getYamlSpec("ds-discover-behaviour")
                    .getEntity().toString();
        });
    }

    @Test
    public void testGetJsonSpec(){
        OpenApiResource apiResource = new OpenApiResource();
        String jsonSpec = apiResource.getJsonSpec("ds-discover-openapi_v1")
                            .getEntity().toString();

        assertFalse(jsonSpec.contains("${config:"));

    }

    @Test
    public void testGettingConfigWithPathJson(){
        OpenApiResource apiResource = new OpenApiResource();

        assertThrows(InvalidArgumentServiceException.class, () -> {
            apiResource.getJsonSpec("conf/ds-discover-behaviour")
                    .getEntity().toString();
        });
    }

    @Test
    public void testGettingConfigWithoutPathJson(){
        OpenApiResource apiResource = new OpenApiResource();

        assertThrows(InvalidArgumentServiceException.class, () -> {
            apiResource.getJsonSpec("ds-discover-behaviour")
                    .getEntity().toString();
        });
    }

}
