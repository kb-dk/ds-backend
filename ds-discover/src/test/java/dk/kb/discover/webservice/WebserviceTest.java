package dk.kb.discover.webservice;

import dk.kb.discover.config.ServiceConfig;
import dk.kb.util.webservice.exception.NotFoundServiceException;
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

        assertThrows(NotFoundServiceException.class, () ->
                apiResource.getYamlSpec("conf/ds-discover-behaviour")
                        .getEntity().toString());
    }

    @Test
    public void testPathHacking(){
        OpenApiResource apiResource = new OpenApiResource();

        assertThrows(NotFoundServiceException.class, () ->
                apiResource.getYamlSpec("secret/very").getEntity().toString());
    }

    @Test
    public void testGettingConfigWithoutPath(){
        OpenApiResource apiResource = new OpenApiResource();

        assertThrows(NotFoundServiceException.class, () ->
                apiResource.getYamlSpec("ds-discover-behaviour")
                        .getEntity().toString());
    }

    @SuppressWarnings("resource")
    @Test
    public void testGetJsonSpec(){
        String jsonSpec = OpenApiResource.createJson("ds-discover-openapi_v1")
                .getEntity().toString();

        assertFalse(jsonSpec.contains("${config:"));

    }

    @SuppressWarnings("resource")
    @Test
    public void testGettingConfigWithPathJson(){
        assertThrows(NotFoundServiceException.class, () ->
                OpenApiResource.createJson("conf/ds-discover-behaviour")
                        .getEntity().toString());
    }

    @SuppressWarnings("resource")
    @Test
    public void testGettingConfigWithoutPathJson(){
        assertThrows(NotFoundServiceException.class, () ->
                OpenApiResource.createJson("ds-discover-behaviour")
                        .getEntity().toString());
    }

}
