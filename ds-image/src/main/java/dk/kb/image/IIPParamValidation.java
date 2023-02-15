package dk.kb.image;

import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

import java.util.Arrays;
import java.util.List;

public class IIPParamValidation {

    public static void fifValidation(String fif){
        if (fif == null || fif.isEmpty()) {
            throw new InvalidArgumentServiceException("The parameter FIF must be defined");
        }
    }

    public static void cvtValidation(String cvt){
        if (!("jpeg".equals(cvt) | "png".equals(cvt))) {
            // Maybe add a fallback to either one of them here?
            throw new InvalidArgumentServiceException(
                    "The parameter CVT must be defined and must be either 'jpeg' or 'png'. It was '" + cvt + "'");
        }
    }

    // TODO: Perform validation of JTL
    // TODO: Perform validation of PTL
    // TODO: Perform validation of WID
    // TODO: Perform validation of HEI

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

    public static void cntValidation(float cnt){
        if (cnt < 0.0){
            throw new InvalidArgumentServiceException("CNT has to be equal to or greater than 0");
        }
    }

    public static void rotValidation(String rot){
        // Only 90, 180 and 270 supported. ! can be used to flip horizontally.
        String[] values = {"90","180","270","!90", "!180", "!270"};
        boolean b = Arrays.asList(values).contains(rot);
        if (!b){
            throw new InvalidArgumentServiceException("ROT has to be specified as one of the following values when set: 90, 180, 270, !90, !180, !270");
        }
    }

    // TODO: Perform validation of GAM
    // TODO: Perform validation of CMP
    // TODO: Perform validation of PFL
    // TODO: Perform validation of MINMAX
    // TODO: Perform validation of CTW
    // TODO: Perform validation of INV
    // TODO: Perform validation of COL

}
