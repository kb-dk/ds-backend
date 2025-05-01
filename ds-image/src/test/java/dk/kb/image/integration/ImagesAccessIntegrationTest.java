package dk.kb.image.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.StreamingOutput;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import dk.kb.image.config.ConfigAdjuster;
import dk.kb.image.util.ImageAccessValidation;
import dk.kb.util.Resolver;


/**
 * Integration test that will call the licensemodule for id access filtering to determine access to tumbnails.  
 */
@Tag("integration")
public class ImagesAccessIntegrationTest extends IntegrationTest{
    
    @Test
    public void testPlaceholderImageStreamingOutput() throws IOException {
        //This is not a DsImageClient test,  but logic will call ds-license service.          
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        if (Resolver.getPathFromClasspath("ds-image-integration-test.yaml") == null){
            fail("Internal test config is not present. 'testPlaceholderImageStreamingOutput' is therefore not run. Please update aegis and do 'kb init' to make this run.");
        }

        try (ConfigAdjuster configAdjuster = new ConfigAdjuster("ds-image-integration-test.yaml")){
            // Get test image
            ByteArrayOutputStream testImageByteArray = getImageAsByteArrayOS("nonExisting.jpg");
            // Get image through ImageAccessValidation
            StreamingOutput streamingImage = ImageAccessValidation.handleNoAccessOrNoImage("notExistingImage", response, false);

            // Convert image to ByteArrayOutputStream for comparison
            ByteArrayOutputStream streamedImageByteArray = new ByteArrayOutputStream();
            streamingImage.write(streamedImageByteArray);

            assertEquals(testImageByteArray.size(), streamedImageByteArray.size());
        }
    }



    private static ByteArrayOutputStream getImageAsByteArrayOS(String imgName) throws IOException {
        String imgPath = Resolver.getPathFromClasspath(imgName).toString();

        BufferedImage trueImage = ImageIO.read(new File(imgPath));
        ByteArrayOutputStream trueImageByteArray = new ByteArrayOutputStream();

        // Write the BufferedImage to the ByteArrayOutputStream in the specified format
        ImageIO.write(trueImage, "jpg", trueImageByteArray);
        trueImageByteArray.flush();
        return trueImageByteArray;
    }


}
