package dk.kb.present;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dk.kb.present.copyright.XsltCopyrightMapper;
import dk.kb.present.transform.DSTransformer;
import dk.kb.present.transform.XSLTFactory;
import dk.kb.present.transform.XSLTTransformer;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;

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

    /**
     * Implicit test of {@link dk.kb.present.transform.XSLTFactory}.
     * @param config XSLTFactory compliant YAML.
     * @return the result generating a {@link dk.kb.present.transform.DSTransformer} using XSLTFactory and
     *         transforming the {@code xmlResource} with that, including access fields resolved using
     *         {@link XsltCopyrightMapper}.
     */
	public static String getTransformedFromConfigWithAccessFields(YAML config, String xmlResource) throws Exception {
		DSTransformer transformer = new XSLTFactory().createTransformer(config);
		String mods = Resolver.resolveUTF8String(xmlResource);
		HashMap<String, String> accessFields = XsltCopyrightMapper.applyXsltCopyrightTransformer(mods);
		//System.out.println("access fields:"+accessFields);
		return transformer.apply(mods, accessFields);
	}

	/**
	 * Transform the input MODS record with an XSLT and return as pretty JSON
	 */
	public static void prettyPrintSolrJsonFromMetadata(String xslt, String record) throws Exception {
		String solrString = getTransformedWithAccessFieldsAdded(xslt, record);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString);
	}
}
