package dk.kb.image;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IIPValidationTest {
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
    static String cvt = "png";
    @Test
    public void IIPValidationTest(){
        List<Float> wrongRgnX = new ArrayList<>(Arrays.asList(2F,0.2F,0.7F,0.8F));
        IIPFacade.getInstance().validateIIPRequest(fif, wid, hei, wrongRgnX, qlt, cnt, shd, lyr, rot, gam, cmp, pfl, ctw, inv, col, jtl, ptl, cvt);


    }
}
