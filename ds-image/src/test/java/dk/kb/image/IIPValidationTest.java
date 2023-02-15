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
    // TODO: Add logging for all tests
    private static final Logger log = LoggerFactory.getLogger(IIPValidationTest.class);
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
            IIPParamValidation.rgnValidation(wrongRegionX);
        });
        Exception exceptionY = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.rgnValidation(wrongRegionY);
        });
        Exception exceptionW = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.rgnValidation(wrongRegionW);
        });
        Exception exceptionH = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.rgnValidation(wrongRegionH);
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

        String expectedMessage = "ROT has to be specified as one of the following values when set: 90, 180, 270, !90, !180, !270";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void jtlTest(){
        List<Integer> jtlParam = new ArrayList<>();
        jtlParam.add(2);

        // One value
        Exception exception1 = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.jtlValidation(jtlParam);
        });

        // Three values
        jtlParam.add(3);
        jtlParam.add(45);
        Exception exception2 = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.jtlValidation(jtlParam);
        });

        String expectedMessage = "The parameter JTL has to contain two values index x and resolution level r";
        String actualMessage1 = exception1.getMessage();
        String actualMessage2 = exception2.getMessage();

        assertTrue(actualMessage1.contains(expectedMessage));
        assertTrue(actualMessage2.contains(expectedMessage));
    }
    @Test
    public void ptlTest(){
        List<Integer> ptlParam = new ArrayList<>();
        ptlParam.add(2);

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

        String expectedMessage = "The parameter PTL has to contain two values index x and resolution level r";
        String actualMessage1 = exception1.getMessage();
        String actualMessage2 = exception2.getMessage();

        assertTrue(actualMessage1.contains(expectedMessage));
        assertTrue(actualMessage2.contains(expectedMessage));
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
    public void shdTest(){
        List<Integer> shd = new ArrayList<>();

        // Test for wrong size
        Exception exception1 = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.shdValidation(shd);
        });
        String expectedMessage1 = "The parameter SHD has to contain exactly two values: h and v";
        String actualMessage1 = exception1.getMessage();

        // Test for wrong h value
        shd.add(200);
        shd.add(4);
        Exception exception2 = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.shdValidation(shd);
        });
        String expectedMessage2 = "The h value of parameter SHD is set incorrectly. It has to be an angle between -90 and 90.";
        String actualMessage2 = exception2.getMessage();

        // Test for wrong v value
        shd.set(0, 40);
        Exception exception3 = assertThrows(InvalidArgumentServiceException.class, () -> {
            IIPParamValidation.shdValidation(shd);
        });
        String expectedMessage3 = "The v value of parameter SHD is set incorrectly. It has to be a number between -1 and 1.";
        String actualMessage3 = exception3.getMessage();

        assertTrue(actualMessage1.contains(expectedMessage1));
        assertTrue(actualMessage2.contains(expectedMessage2));
        assertTrue(actualMessage3.contains(expectedMessage3));
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
