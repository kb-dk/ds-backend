package dk.kb.present;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import dk.kb.present.copyright.XsltCopyrightMapper;
import dk.kb.present.transform.XSLTTransformer;
import dk.kb.util.Resolver;

public class TestUtil {


	public static String getTransformed(String xsltResource, String xmlResource) throws IOException {
		XSLTTransformer transformer = new XSLTTransformer(xsltResource, null);
		String mods = Resolver.resolveUTF8String(xmlResource);
		return transformer.apply(mods, new HashMap<String,String>());
	}	


	public static String getTransformed(String xsltResource, String xmlResource, Map<String,String> metadata) throws IOException {
		XSLTTransformer transformer = new XSLTTransformer(xsltResource, null);
		String mods = Resolver.resolveUTF8String(xmlResource);
		if (metadata == null) {
			metadata = new HashMap<String,String>();
		}        
		return transformer.apply(mods, metadata);
	}

	public static String getTransformed(String xsltResource, String xmlResource, Map<String,String> fixedInjections,
                                        Map<String,String> metadata) throws IOException {
		XSLTTransformer transformer = new XSLTTransformer(xsltResource, fixedInjections);
		String mods = Resolver.resolveUTF8String(xmlResource);
		if (metadata == null) {
			metadata = new HashMap<String,String>();
		}
		return transformer.apply(mods, metadata);
	}



	public static String getTransformedWithAccessFieldsAdded(String xsltResource, String xmlResource) throws Exception {
		XSLTTransformer transformer = new XSLTTransformer(xsltResource, null);
		String mods = Resolver.resolveUTF8String(xmlResource);
		HashMap<String, String> accessFields = XsltCopyrightMapper.applyXsltCopyrightTransformer(mods);		        
		//System.out.println("access fields:"+accessFields);
		return transformer.apply(mods, accessFields);
	}

	public static String getTransformedWithAccessFieldsAdded(
            String xsltResource, String xmlResource, Map<String, String> injections) throws Exception {
		XSLTTransformer transformer = new XSLTTransformer(xsltResource, injections);
		String mods = Resolver.resolveUTF8String(xmlResource);
		HashMap<String, String> accessFields = XsltCopyrightMapper.applyXsltCopyrightTransformer(mods);
		//System.out.println("access fields:"+accessFields);
		return transformer.apply(mods, accessFields);
	}

}
