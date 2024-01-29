package dk.kb.image;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import dk.kb.image.util.ImageAccessValidation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;



public class ImageAccessValidationTest {

	
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
}
