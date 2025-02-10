package dk.kb.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import dk.kb.image.config.ConfigAdjuster;
import dk.kb.image.config.ServiceConfig;
import dk.kb.util.Resolver;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import dk.kb.image.util.ImageAccessValidation;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.StreamingOutput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.regex.Matcher;



public class ImageAccessValidationTest {
	private static final Logger log = LoggerFactory.getLogger(ImageAccessValidationTest.class);


	private HttpServletResponse response;

	@BeforeEach
	public void setUp() {
		// Create a mock HttpServletResponse
		response = Mockito.mock(HttpServletResponse.class);
	}
	
	@Test
	public void testIiifSizeParameter() {
						
		  String validSize ="123,456";
		  Matcher matcher = ImageAccessValidation.IIIF_SIZE_PATTERN.matcher(validSize);
		  boolean matchFound = matcher.find();
		  assertTrue(matchFound);
		  assertEquals("123",matcher.group(1)); 
		  assertEquals("456",matcher.group(2));
		  		  
		  validSize ="!123,456";
		  matcher = ImageAccessValidation.IIIF_SIZE_PATTERN.matcher(validSize);
		  matchFound = matcher.find();
		  assertTrue(matchFound);
		  assertEquals("123",matcher.group(1)); 
		  assertEquals("456",matcher.group(2));
		  
		  validSize ="^123,456";
		  matcher = ImageAccessValidation.IIIF_SIZE_PATTERN.matcher(validSize);
		  matchFound = matcher.find();
		  assertTrue(matchFound);
		  assertEquals("123",matcher.group(1)); 
		  assertEquals("456",matcher.group(2));
		  
		  validSize ="^!123,456";
		  matcher = ImageAccessValidation.IIIF_SIZE_PATTERN.matcher(validSize);
		  matchFound = matcher.find();
		  assertTrue(matchFound);
		  assertEquals("123",matcher.group(1)); 
		  assertEquals("456",matcher.group(2));
		  
		  String notValidSize="x123,456";
		  matcher = ImageAccessValidation.IIIF_SIZE_PATTERN.matcher(notValidSize);
		  matchFound = matcher.find();
		  assertFalse(matchFound);
		  
		  notValidSize="pct:100"; //this is a valid parameter, but we only accept requests with both width and height for thumbnails.
		  matcher = ImageAccessValidation.IIIF_SIZE_PATTERN.matcher(notValidSize);
		  matchFound = matcher.find();
		  assertFalse(matchFound);
		  
		  
	}

	@Test
	@Tag("integration")
	public void testPlaceholderImageStreamingOutput() throws IOException {
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
