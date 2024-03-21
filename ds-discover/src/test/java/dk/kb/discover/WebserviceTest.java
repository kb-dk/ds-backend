package dk.kb.discover;

import dk.kb.discover.webservice.OpenApiResource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class WebserviceTest {

    @Test
    public void testGetYamlSpec(){
        OpenApiResource apiResource = new OpenApiResource();
        String yamlSpec = apiResource.getYamlSpec()
                            .getEntity().toString();

        assertFalse(yamlSpec.contains("${config."));

    }

    @Test
    public void testGetJsonSpec(){
        OpenApiResource apiResource = new OpenApiResource();
        String jsonSpec = apiResource.getJsonSpec()
                            .getEntity().toString();

        assertFalse(jsonSpec.contains("${config."));

    }

}
