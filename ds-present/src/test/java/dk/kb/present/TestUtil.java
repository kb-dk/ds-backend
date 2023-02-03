package dk.kb.present;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import dk.kb.present.copyright.XsltCopyrightMapper;
import dk.kb.present.transform.XSLTTransformer;
import dk.kb.util.Resolver;

public class TestUtil {

	
	 public static String getTransformed(String xsltResource, String xmlResource) throws IOException {
	        XSLTTransformer transformer = new XSLTTransformer(xsltResource);
	        String mods = Resolver.resolveUTF8String(xmlResource);
	        return transformer.apply(mods, new HashMap<String,String>());
	    }	
	 
	 
	  public static String getTransformed(String xsltResource, String xmlResource, Map<String,String> injections) throws IOException {
	        XSLTTransformer transformer = new XSLTTransformer(xsltResource);
	        String mods = Resolver.resolveUTF8String(xmlResource);
	        if (injections == null) {
	        	injections = new HashMap<String,String>();
	        }        
	        return transformer.apply(mods, injections);
	    }
	    
	  

		 public static String getTransformedWithAccessFieldsAdded(String xsltResource, String xmlResource) throws Exception {
		        XSLTTransformer transformer = new XSLTTransformer(xsltResource);	       	        
		   
		        String mods = Resolver.resolveUTF8String(xmlResource);
		        HashMap<String, String> accessFields = XsltCopyrightMapper.xsltCopyrightTransformer(mods);
		        
		        System.out.println(accessFields);
		        return transformer.apply(mods, accessFields);
		    }
	 
}
