package dk.kb.image;

import com.damnhandy.uri.template.UriTemplate;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IIPValidationTest {
    // TODO: Add logging for all tests
    private static final Logger log = LoggerFactory.getLogger(IIPValidationTest.class);

    @Test
    public void templateTest() {
        // Check to see if the UriTemplate properly escapes arguments
        String path = UriTemplate.fromTemplate("http://example.com/{foo}/{bar}")
                .set("foo", "&%+ {/")
                .set("bar", "zoo")
                .expand();
        assertEquals("http://example.com/%26%25%2B%20%7B%2F/zoo", path);
    }

    @Test
    public void rgnTest(){
        // Wrong X
        List<Float> wrongRegionX = new ArrayList<>(Arrays.asList(2F,0.2F,0.7F,0.8F));
        // Wrong Y
        List<Float> wrongRegionY = new ArrayList<>(Arrays.asList(0.5F,2F,0.7F,0.8F));
        // Wrong W
        List<Float> wrongRegionW = new ArrayList<>(Arrays.asList(0.5F,0.5F,3F,0.8F));
        // Wrong H
        List<Float> wrongRegionH = new ArrayList<>(Arrays.asList(0.5F,0.5F,0.7F,6F));

        // Collecting exceptions
        Exception exceptionX = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.rgnValidation(wrongRegionX, "jpeg");
        });
        Exception exceptionY = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.rgnValidation(wrongRegionY, "jpeg");
        });
        Exception exceptionW = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.rgnValidation(wrongRegionW, "jpeg");
        });
        Exception exceptionH = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.rgnValidation(wrongRegionH, "jpeg");
        });

        // Creating test values and getting exception messages
        String expectedXMessage = "The value of x in parameter RGN is out of bounds. It has to be between 0.0 and 1.0";
        String actualXMessage = exceptionX.getMessage();
        String expectedYMessage = "The value of y in parameter RGN is out of bounds. It has to be between 0.0 and 1.0";
        String actualYMessage = exceptionY.getMessage();
        String expectedWMessage = "The value of w in parameter RGN is out of bounds. It has to be between 0.0 and 1.0";
        String actualWMessage = exceptionW.getMessage();
        String expectedHMessage = "The value of h in parameter RGN is out of bounds. It has to be between 0.0 and 1.0";
        String actualHMessage = exceptionH.getMessage();

        // Evaluate tests
        assertTrue(actualXMessage.contains(expectedXMessage));
        assertTrue(actualYMessage.contains(expectedYMessage));
        assertTrue(actualWMessage.contains(expectedWMessage));
        assertTrue(actualHMessage.contains(expectedHMessage));
        log.info("IIP region params gets validated correctly.");
    }


    @Test
    public void qltTest(){
        String pngCvt = "png";
        String jpegCvt = "jpeg";
        int highJpegValue = 200;
        int highPngValue = 15;
        int lowValue = -30;

        // Test for negative value
        Exception exceptionForLowValue = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.qltValidation(lowValue, jpegCvt);
        });
        String expectedMessageForLowValue = "QLT has to be equal to or greater than 0.";
        String actualMessageForLowValue = exceptionForLowValue.getMessage();
        assertTrue(actualMessageForLowValue.contains(expectedMessageForLowValue));

        // Test for value above 100 while cvt is jpeg
        Exception exceptionForHighValue = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.qltValidation(highJpegValue, jpegCvt);
        });
        String expectedMessageForHighValue = "QLT has to be less than or equal to 100, when CVT is set to JPEG";
        String actualMessageForHighValue = exceptionForHighValue.getMessage();
        assertTrue(actualMessageForHighValue.contains(expectedMessageForHighValue));

        // Test for too high value, while cvt is set to png
        Exception exceptionForHighPngValue = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.qltValidation(highPngValue, pngCvt);
        });
        String expectedMessageForHighPngValue = "QLT has to be less than or equal to 9, when CVT is set to PNG";
        String actualMessageForHighPngValue = exceptionForHighPngValue.getMessage();
        assertTrue(actualMessageForHighPngValue.contains(expectedMessageForHighPngValue));
    }

    @Test
    public void cntTest(){
        float lowCnt = -5;
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.cntValidation(lowCnt);
        });

        String expectedMessage = "CNT has to be equal to or greater than 0";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }


    @Test
    public void rotTest(){
        String wrongRot = "!67";
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.rotValidation(wrongRot);
        });

        String expectedMessage = "ROT has to be specified as one of the following values when set: 90, 180, 270, !90, !180, !270. The provided ROT was: '" + wrongRot + "'";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void jtlTest(){
        List<Integer> jtl = new ArrayList<>();
        jtl.add(2);
        String expectedMessage1 = "The parameter JTL has to contain two values index x and resolution level r. Input was: '" + jtl + "'";

        // One value
        Exception exception1 = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.jtlValidation(jtl);
        });

        // Three values
        jtl.add(3);
        jtl.add(45);
        Exception exception2 = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.jtlValidation(jtl);
        });

        String expectedMessage2 = "The parameter JTL has to contain two values index x and resolution level r. Input was: '" + jtl + "'";
        String actualMessage1 = exception1.getMessage();
        String actualMessage2 = exception2.getMessage();

        assertTrue(actualMessage1.contains(expectedMessage1));
        assertTrue(actualMessage2.contains(expectedMessage2));
    }

    @Test
    public void ptlTest(){
        List<Integer> ptlParam = new ArrayList<>();
        ptlParam.add(2);
        String expectedMessage1 = "The parameter PTL has to contain two values index x and resolution level r. Input was: '" + ptlParam + "'";

        // One value
        Exception exception1 = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.ptlValidation(ptlParam);
        });

        // Three values
        ptlParam.add(3);
        ptlParam.add(45);
        Exception exception2 = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.ptlValidation(ptlParam);
        });

        String expectedMessage2 = "The parameter PTL has to contain two values index x and resolution level r. Input was: '" + ptlParam + "'";
        String actualMessage1 = exception1.getMessage();
        String actualMessage2 = exception2.getMessage();

        assertTrue(actualMessage1.contains(expectedMessage1));
        assertTrue(actualMessage2.contains(expectedMessage2));
    }

    @Test
    public void widTest(){
        Long wid = 800L;

        // Null cvt
        Exception exception1 = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.widValidation(wid, null);
        });

        // Empty cvt
        Exception exception2 = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.widValidation(wid, "");
        });

        String expectedMessage = "The parameter WID is only to be set, when the parameter CVT is in use";
        String actualMessage1 = exception1.getMessage();
        String actualMessage2 = exception2.getMessage();

        assertTrue(actualMessage1.contains(expectedMessage));
        assertTrue(actualMessage2.contains(expectedMessage));
    }

    @Test
    public void heiTest(){
        Long hei = 800L;

        // Null cvt
        Exception exception1 = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.heiValidation(hei, null);
        });

        // Empty cvt
        Exception exception2 = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.heiValidation(hei, "");
        });

        String expectedMessage = "The parameter HEI is only to be set, when the parameter CVT is in use";
        String actualMessage1 = exception1.getMessage();
        String actualMessage2 = exception2.getMessage();

        assertTrue(actualMessage1.contains(expectedMessage));
        assertTrue(actualMessage2.contains(expectedMessage));
    }

    @Test
    public void cmpTest(){
        String wrongCmp = "pink";
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.cmpValidation(wrongCmp);
        });

        String expectedMessage = "CMP has to be specified as one of the following values when set: GREY, JET, COLD, HOT, RED, GREEN or BLUE. The provided CMP was: '" + wrongCmp + "'";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void pflTest(){
        String pfl = "notNumber:23,1-2,3";

        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.pflValidation(pfl);
        });

        String expectedMessage = "The value of PFL needs to be defined specifically as r:x1,y1-x2,y2 by was: '" + pfl + "'";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));

    }

    @Test
    public void ctwTest(){
        String ctw= "[2,3,3;3,4,5;6,7,hest]";

        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.ctwValidation(ctw);
        });

        String expectedMessage = "The value of b in array3 needs to be a number, but was: hest";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void colTest(){
        String col = "multicolor";
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.colValidation(col);
        });

        String expectedMessage = "COL has to be specified as one of the following values when set: grey, gray or binary";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));

    }

    @Test
    public void deepzoomTileTest(){
        String testTiles = "3.4";

        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.deepzoomTileValidation(testTiles);
        });

        String expectedMessage = "Deepzoom parameter 'tiles' is specified incorrectly. it has to be defined as x_y";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void deepzoomFormatTest(){
        String testFormat = "tif";
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.deepzoomFormatValidation(testFormat);
        });

        String expectedMessage = "Format for Deepzoom tile has to be either 'jpg', 'jpeg' or 'png'";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void testOnlyOneOfJtlPtlCvtExists(){
        List<Integer> ptl = new ArrayList<>();
        ptl.add(2);
        ptl.add(24);

        List<Integer> jtl = new ArrayList<>();
        jtl.add(2);
        jtl.add(12);

        String cvt = "jpeg";

        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.validateOneJtlPtlCvtExists(jtl, ptl, cvt);
        });

        String expectedMessage = "More than one of the parameters JTL, PTL and CVT are set. Only one can be set at a time";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    /*
    @Test
    public void templateTest(){
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> {
        IIPParamValidation.
        });

        String expectedMessage = "For input string";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
    */
}
