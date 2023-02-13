package dk.kb.image;

import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IIPValidationTest {
    private static final Logger log = LoggerFactory.getLogger(IIPValidationTest.class);
    static String fif = "test.png";
    static Long wid;
    static Long hei;
    static List<Float> rgn = new ArrayList<>(Arrays.asList(0.5F,0.2F,0.7F,0.8F));
    static Integer qlt;
    static Float cnt;
    static List<Integer> shd;
    static Integer lyr;
    static String rot;
    static Float gam;
    static String cmp;
    static String pfl;
    static String ctw;
    static Boolean inv;
    static String col;
    static List<Integer> jtl;
    static List<Integer> ptl;
    static String cvt = "jpeg";
    @Test
    public void IipRegionTest(){
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
            IIPFacade.getInstance().validateIIPRequest(fif, wid, hei, wrongRegionX, qlt, cnt, shd, lyr, rot, gam, cmp, pfl, ctw, inv, col, jtl, ptl, cvt);
        });
        Exception exceptionY = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPFacade.getInstance().validateIIPRequest(fif, wid, hei, wrongRegionY, qlt, cnt, shd, lyr, rot, gam, cmp, pfl, ctw, inv, col, jtl, ptl, cvt);
        });
        Exception exceptionW = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPFacade.getInstance().validateIIPRequest(fif, wid, hei, wrongRegionW, qlt, cnt, shd, lyr, rot, gam, cmp, pfl, ctw, inv, col, jtl, ptl, cvt);
        });
        Exception exceptionH = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPFacade.getInstance().validateIIPRequest(fif, wid, hei, wrongRegionH, qlt, cnt, shd, lyr, rot, gam, cmp, pfl, ctw, inv, col, jtl, ptl, cvt);
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
    public void IipQltTest(){
        String pngCvt = "png";
        int highJpegValue = 200;
        int highPngValue = 15;
        int lowValue = -30;

        // Test for negative value
        Exception exceptionForLowValue = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPFacade.getInstance().validateIIPRequest(fif, wid, hei, rgn, lowValue, cnt, shd, lyr, rot, gam, cmp, pfl, ctw, inv, col, jtl, ptl, cvt);
        });
        String expectedMessageForLowValue = "QLT has to be equal to or greater than 0.";
        String actualMessageForLowValue = exceptionForLowValue.getMessage();
        assertTrue(actualMessageForLowValue.contains(expectedMessageForLowValue));

        // Test for value above 100 while cvt is jpeg
        Exception exceptionForHighValue = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPFacade.getInstance().validateIIPRequest(fif, wid, hei, rgn, highJpegValue, cnt, shd, lyr, rot, gam, cmp, pfl, ctw, inv, col, jtl, ptl, cvt);
        });
        String expectedMessageForHighValue = "QLT has to be less than or equal to 100, when CVT is set to JPEG";
        String actualMessageForHighValue = exceptionForHighValue.getMessage();
        assertTrue(actualMessageForHighValue.contains(expectedMessageForHighValue));

        // Test for too high value, while cvt is set to png
        Exception exceptionForHighPngValue = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPFacade.getInstance().validateIIPRequest(fif, wid, hei, rgn, highPngValue, cnt, shd, lyr, rot, gam, cmp, pfl, ctw, inv, col, jtl, ptl, pngCvt);
        });
        String expectedMessageForHighPngValue = "QLT has to be less than or equal to 9, when CVT is set to PNG";
        String actualMessageForHighPngValue = exceptionForHighPngValue.getMessage();
        assertTrue(actualMessageForHighPngValue.contains(expectedMessageForHighPngValue));
    }

    @Test
    public void templateTest(){
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPFacade.getInstance().validateIIPRequest(fif, wid, hei, rgn, qlt, cnt, shd, lyr, rot, gam, cmp, pfl, ctw, inv, col, jtl, ptl, cvt);
        });

        String expectedMessage = "For input string";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
}
