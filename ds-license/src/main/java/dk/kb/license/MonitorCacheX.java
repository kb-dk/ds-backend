package dk.kb.license;

import java.util.Date;
import dk.statsbiblioteket.util.EntryCounter;

public class MonitorCacheX {

    public static String SERVER_START_TIME= (new Date()).toString();
    public static EntryCounter REST_METHOD_CALLS = new EntryCounter();
        
    //Just bump the count by 1.
    public static void registerNewRestMethodCall(String methodName){
       REST_METHOD_CALLS.inc(methodName);           
    }

          
}
