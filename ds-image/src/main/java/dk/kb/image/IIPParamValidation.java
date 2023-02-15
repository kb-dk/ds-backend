package dk.kb.image;

import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parameter validation for the Internet Imaging Protocol. The protocol can be found : <a href="https://iipimage.sourceforge.io/documentation/protocol/">here</a>.
 * Each method in this class has been made from this specification.
 */
public class IIPParamValidation {
    private static final Logger log = LoggerFactory.getLogger(IIPParamValidation.class);

    /**
     * Validate that FIF is set  and isn't empty as it is required in IIP
     */
    public static void fifValidation(String fif){
        if (fif == null || fif.isEmpty()) {
            throw new InvalidArgumentServiceException("The parameter FIF must be defined");
        }
    }

    /**
     * Validate that CVT is set to either jpeg or png, as these are the allowed types for export in IIP
     */
    public static void cvtValidation(String cvt){
        if (!(cvt.equals("jpeg") | cvt.equals("png"))) {
            // Maybe add a fallback to either one of them here?
            throw new InvalidArgumentServiceException(
                    "The parameter CVT must be defined and must be either 'jpeg' or 'png'. It was '" + cvt + "'");
        }
    }

    /**
     * Validate that JTL is correctly set, when exporting JPEG tiles.
     */
    public static void jtlValidation(List<Integer> jtl){
        if (jtl.size() < 2){
            throw new InvalidArgumentServiceException("The parameter JTL has to contain two values index x and resolution level r");
        }
        if (jtl.size() > 2){
            log.warn("JTL contains more than 2 values. JTL can only contain two values: index x and resolution level r");
            throw new InvalidArgumentServiceException("The parameter JTL has to contain two values index x and resolution level r");
        }
    }

    /**
     * Validate that PTL is correctly set, when exporting PNG tiles.
     */
    public static void ptlValidation(List<Integer> ptl) {
        if (ptl.size() < 2) {
            throw new InvalidArgumentServiceException("The parameter PTL has to contain two values index x and resolution level r");
        }
        if (ptl.size() > 2) {
            log.warn("JTL contains more than 2 values. PTL can only contain two values: index x and resolution level r");
            throw new InvalidArgumentServiceException("The parameter PTL has to contain two values index x and resolution level r");
        }
    }

    // TODO: Add validation, that only one of CVT, JTL or PTL is set

    /**
     * Validate that WID(width) is only set, when used together with CVT.
     */
    public static void widValidation(Long wid, String cvt) {
        if (cvt == null || cvt.isEmpty() && wid != null) {
            throw new InvalidArgumentServiceException("The parameter WID is only to be set, when the parameter CVT is in use");
        }
    }

    /**
     * Validate that HEI(height) is only set, when used together with CVT.
     */
    public static void heiValidation(Long hei, String cvt) {
        if (cvt == null || cvt.isEmpty() && hei != null) {
            throw new InvalidArgumentServiceException("The parameter HEI is only to be set, when the parameter CVT is in use");
        }
    }

    /**
     * Validate that RGN(region) is correctly set, by validating each element of the list.
     */
    public static void rgnValidation(List<Float> rgn, String cvt){
        if (cvt == null || cvt.isEmpty() && rgn != null) {
            throw new InvalidArgumentServiceException("The parameter RGN can only be used when the parameter CVT is in use");
        }
        if (rgn.size() != 4){
            throw new InvalidArgumentServiceException("The parameter RGN has to contain four numbers. " +
                    "The first number representing X. The second number representing Y. The third number representing W " +
                    "The fourth number representing H between 0.0 and 1.0. All numbers should be between 0.0 and 1.0");
        }
        String[] regionValueNames = new String[]{"x", "y", "w", "h"};
        for (int i = 0; i < rgn.size(); i++) {
            if (!(rgn.get(i) >= 0.0F && rgn.get(i) <= 1.0)){
                throw new InvalidArgumentServiceException("The value of " + regionValueNames[i] + " in parameter RGN is out of bounds. It has to be between 0.0 and 1.0");
            }
        }
    }

    /**
     * Validate that QLT(compression level) is set correctly, depending on the value of CVT.
     */
    public static void qltValidation(int qlt, String cvt){
        if (qlt < 0){
            throw new InvalidArgumentServiceException("QLT has to be equal to or greater than 0.");
        }
        if (cvt.equals("jpeg") && qlt > 100){
            throw new InvalidArgumentServiceException("QLT has to be less than or equal to 100, when CVT is set to JPEG");
        }
        if (cvt.equals("png") && qlt > 9){
            throw new InvalidArgumentServiceException("QLT has to be less than or equal to 9, when CVT is set to PNG");
        }
    }

    /**
     * Validate that CNT(contrast adjustment) is done correctly.
     */
    public static void cntValidation(float cnt){
        if (cnt < 0.0){
            throw new InvalidArgumentServiceException("CNT has to be equal to or greater than 0");
        }
    }

    /**
     * Validate that SHD(Simulated hill-shading) is done correctly and that each value given are in range.
     */
    public static void shdValidation(List<Integer> shd){
        if (shd.size() != 2){
            throw new InvalidArgumentServiceException("The parameter SHD has to contain exactly two values: h and v");
        }
        // TODO: Sanity check description of h and v values with Toke
        if (shd.get(0) < -90 || shd.get(0) > 90){
            throw new InvalidArgumentServiceException("The h value of parameter SHD is set incorrectly. It has to be an angle between -90 and 90.");
        }
        if (shd.get(1) < -1 || shd.get(1) > 1){
            throw new InvalidArgumentServiceException("The v value of parameter SHD is set incorrectly. It has to be a number between -1 and 1.");
        }

    }

    /**
     * Validate that ROT(rotate) has been set to one of the allowed values.
     */
    public static void rotValidation(String rot){
        // Only 90, 180 and 270 supported. ! can be used to flip horizontally.
        String[] values = {"90","180","270","!90", "!180", "!270"};
        boolean b = Arrays.asList(values).contains(rot);
        if (!b){
            throw new InvalidArgumentServiceException("ROT has to be specified as one of the following values when set: 90, 180, 270, !90, !180, !270");
        }
    }

    /**
     * Validate that CMP(colormap) has been set to one of the allowed values.
     */
    public static void cmpValidation(String cmp){
        String[] values = {"GREY","JET","COLD","HOT", "RED", "GREEN", "BLUE"};
        boolean b = Arrays.asList(values).contains(cmp);
        if (!b){
            throw new InvalidArgumentServiceException("CMP has to be specified as one of the following values when set: GREY, JET, COLD, HOT, RED, GREEN or BLUE");
        }
    }

    /**
     * Validate that JSON profile has been defined correctly.
     */
    public static void pflValidation(String pfl){
        // Slice input string and get indexes of delimiters
        int endOfR = pfl.indexOf(":");
        int endOfPairOne = pfl.indexOf("-");
        String pairOne = pfl.substring(endOfR + 1, endOfPairOne);
        String pairTwo = pfl.substring(endOfPairOne + 1);
        int pairOneComma = pairOne.indexOf(",");
        int pairTwoComma = pairTwo.indexOf(",");

        // Create map with values as string
        Map<String, String> values = new HashMap<>();
        values.put("r", pfl.substring(0, endOfR));
        values.put("x1", pairOne.substring(0, pairOneComma));
        values.put("y1", pairOne.substring(pairOneComma+1));
        values.put("x2", pairTwo.substring(0, pairTwoComma));
        values.put("y2", pairTwo.substring(pairTwoComma+1));

        // Convert string values to integers, throws exception when fails
        for (Map.Entry<String, String> entry: values.entrySet()) {
            try {
                int realValue = Integer.parseInt(entry.getValue());
                if (realValue < 0){
                    throw new InvalidArgumentServiceException("The value of " + entry.getKey() + " needs to be a positive number, but was: " + entry.getValue());
                }
            } catch (NumberFormatException e){
                throw new InvalidArgumentServiceException("The value of " + entry.getKey() + " needs to be a positive number, but was: " + entry.getValue());
            }
        }
    }

    public static void minmaxValidation(){
        // TODO: Perform validation of MINMAX, when param has been implemented
    }

    /**
     * Validate correct use of CTW(Color twist/ channel recombination)
     */
    public static void ctwValidation(String ctw){
        // Strip string for brackets
        ctw = ctw.replace("[","");
        ctw = ctw.replace("]","");

        // Split CWT string into 3 strings, each containing an array
        int endOfFirstArray = ctw.indexOf(";");
        int endOfSecondArray = ctw.lastIndexOf(";");

        String stringArray1 = ctw.substring(0,endOfFirstArray);
        String stringArray2 = ctw.substring(endOfFirstArray + 1, endOfSecondArray);
        String stringArray3 = ctw.substring(endOfSecondArray + 1);

        String[] stringArrays = new String[]{stringArray1, stringArray2, stringArray3};
        // Splits each array formatted as a comma separated string into a map containing r,g,b keys and values as string
        splitCommaSeparatedStringToMap(stringArrays);
    }

    /**
     * Split string that contains comma separated RGB values into a map where each value has its own entry
     */
    private static void splitCommaSeparatedStringToMap(String[] stringArrays) {
        for (int i = 0; i < stringArrays.length; i++) {
            int count = i+1;
            int endOfR = stringArrays[i].indexOf(",");
            int endOfG = stringArrays[i].lastIndexOf(",");

            // Defined as map, so that exception makes sense
            Map<String, String> array = new HashMap<>();
            array.put("r", stringArrays[i].substring(0, endOfR));
            array.put("g", stringArrays[i].substring(endOfR + 1, endOfG));
            array.put("b", stringArrays[i].substring(endOfG + 1));

            // Validates that r, g and b can be converted to floats in each map of arrays
            ValidateStringToFloatConversion(count, array);
        }
    }

    /**
     * Validate that each value from given map can be converted from string to float
     * @param count used to deliver meaningfully service exception
     * @param array map consisting of r, g and b values to be checked for conversion
     */
    private static void ValidateStringToFloatConversion(int count, Map<String, String> array) {
        for (Map.Entry<String, String> entry: array.entrySet()) {
            try {
                float realValue = Float.parseFloat(entry.getValue());
            } catch (NumberFormatException e){
                throw new InvalidArgumentServiceException(
                        "The value of " + entry.getKey() + " in array" + count + " needs to be a number, but was: " + entry.getValue());
            }
        }
    }

    // TODO: Perform validation of INV
    /**
     * Validate that COL(color transformation) has been set to one of the allowed values.
     */
    public static void colValidation(String col){
        String[] values = {"grey", "gray", "binary"};
        boolean b = Arrays.asList(values).contains(col);
        if (!b){
            throw new InvalidArgumentServiceException("COL has to be specified as one of the following values when set: grey, gray or binary");
        }
    }

}
