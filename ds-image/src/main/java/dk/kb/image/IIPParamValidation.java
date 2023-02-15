package dk.kb.image;

import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

// TODO: JAVADOC
public class IIPParamValidation {
    private static final Logger log = LoggerFactory.getLogger(IIPParamValidation.class);


    // TODO: JAVADOC
    public static void fifValidation(String fif){
        if (fif == null || fif.isEmpty()) {
            throw new InvalidArgumentServiceException("The parameter FIF must be defined");
        }
    }

    // TODO: JAVADOC
    public static void cvtValidation(String cvt){
        if (!(cvt.equals("jpeg") | cvt.equals("png"))) {
            // Maybe add a fallback to either one of them here?
            throw new InvalidArgumentServiceException(
                    "The parameter CVT must be defined and must be either 'jpeg' or 'png'. It was '" + cvt + "'");
        }
    }

    // TODO: JAVADOC
    public static void jtlValidation(List<Integer> jtl){
        if (jtl.size() < 2){
            throw new InvalidArgumentServiceException("The parameter JTL has to contain two values index x and resolution level r");
        }
        if (jtl.size() > 2){
            log.warn("JTL contains more than 2 values. JTL can only contain two values: index x and resolution level r");
            throw new InvalidArgumentServiceException("The parameter JTL has to contain two values index x and resolution level r");
        }
    }
    // TODO: JAVADOC
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

    // TODO: JAVADOC
    // Only for use with cvt, should validate that cvt is set
    public static void widValidation(Long wid, String cvt) {
        if (cvt == null || cvt.isEmpty() && wid != null) {
            throw new InvalidArgumentServiceException("The parameter WID is only to be set, when the parameter CVT is in use");
        }
    }
    // TODO: JAVADOC
    // Only for use with cvt, should validate that cvt is set
    public static void heiValidation(Long hei, String cvt) {
        if (cvt == null || cvt.isEmpty() && hei != null) {
            throw new InvalidArgumentServiceException("The parameter HEI is only to be set, when the parameter CVT is in use");
        }
    }

    // TODO: JAVADOC
    public static void rgnValidation(List<Float> rgn){
        if (rgn.size() != 4){
            throw new InvalidArgumentServiceException("The parameter RGN has to contain four numbers. " +
                    "The first number representing X. The second number representing Y. The third number representing W " +
                    "The fourth number representing H between 0.0 and 1.0. All numbers should be between 0.0 and 1.0");
        }
        if (!(rgn.get(0) >= 0.0F && rgn.get(0) <= 1.0)){
            throw new InvalidArgumentServiceException("The value of x in parameter RGN is out of bounds. It has to be between 0.0 and 1.0");
        }
        if (!(rgn.get(1) >= 0.0F && rgn.get(1) <= 1.0)){
            throw new InvalidArgumentServiceException("The value of y in parameter RGN is out of bounds. It has to be between 0.0 and 1.0");
        }
        if (!(rgn.get(2) >= 0.0F && rgn.get(2) <= 1.0)){
            throw new InvalidArgumentServiceException("The value of w in parameter RGN is out of bounds. It has to be between 0.0 and 1.0");
        }
        if (!(rgn.get(3) >= 0.0F && rgn.get(3) <= 1.0)){
            throw new InvalidArgumentServiceException("The value of h in parameter RGN is out of bounds. It has to be between 0.0 and 1.0");
        }
    }

    // TODO: JAVADOC
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

    // TODO: JAVADOC
    public static void cntValidation(float cnt){
        if (cnt < 0.0){
            throw new InvalidArgumentServiceException("CNT has to be equal to or greater than 0");
        }
    }

    // TODO: JAVADOC
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

    // TODO: JAVADOC
    public static void rotValidation(String rot){
        // Only 90, 180 and 270 supported. ! can be used to flip horizontally.
        String[] values = {"90","180","270","!90", "!180", "!270"};
        boolean b = Arrays.asList(values).contains(rot);
        if (!b){
            throw new InvalidArgumentServiceException("ROT has to be specified as one of the following values when set: 90, 180, 270, !90, !180, !270");
        }
    }

    // TODO: Perform validation of CMP
    public static void cmpValidation(String cmp){
        String[] values = {"GREY","JET","COLD","HOT", "RED", "GREEN", "BLUE"};
        boolean b = Arrays.asList(values).contains(cmp);
        if (!b){
            throw new InvalidArgumentServiceException("CMP has to be specified as one of the following values when set: GREY, JET, COLD, HOT, RED, GREEN or BLUE");
        }
    }
    // TODO: Perform validation of PFL
    // TODO: Perform validation of MINMAX
    // TODO: Perform validation of CTW
    // TODO: Perform validation of INV
    // TODO: Perform validation of COL

}
