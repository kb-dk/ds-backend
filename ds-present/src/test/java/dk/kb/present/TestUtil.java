package dk.kb.present;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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

import static dk.kb.present.solr.EmbeddedSolrTest.MODS2SOLR;
import static dk.kb.present.solr.EmbeddedSolrTest.PRESERVICA2SOLR;

public class TestUtil {

	public static String getTransformed(String xsltResource, String xmlResource) throws IOException {
        return getTransformed(xsltResource, xmlResource, null, null);
	}

	public static String getTransformed(String xsltResource, String xmlResource, Map<String,String> metadata)
            throws IOException {
        return getTransformed(xsltResource, xmlResource, null, metadata);
	}

	public static String getTransformed(String xsltResource, String xmlResource, Map<String,String> fixedInjections,
                                        Map<String,String> metadata) throws IOException {
		XSLTTransformer transformer = new XSLTTransformer(xsltResource, fixedInjections);
		String mods = Resolver.resolveUTF8String(xmlResource);
        // Ensure metadata is defined and that it is mutable
        metadata = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
        metadata.put("recordID", "ds.test:" + Path.of(xmlResource).getFileName().toString());
		return transformer.apply(mods, metadata);
	}

	public static String getTransformedWithAccessFieldsAdded(
            String xsltResource, String xmlResource) throws IOException {
        return getTransformedWithAccessFieldsAdded(xsltResource, xmlResource, null);
	}

	public static String getTransformedWithAccessFieldsAdded(
            String xsltResource, String xmlResource, Map<String, String> injections) throws IOException {
		XSLTTransformer transformer = new XSLTTransformer(xsltResource, injections);
		String mods = Resolver.resolveUTF8String(xmlResource);
		HashMap<String, String> accessFields = XsltCopyrightMapper.applyXsltCopyrightTransformer(mods);
        accessFields.put("recordID", "ds.test:" + Path.of(xmlResource).getFileName().toString());
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
        accessFields.put("recordID", "ds.test:" + Path.of(xmlResource).getFileName().toString());
		//System.out.println("access fields:"+accessFields);
		return transformer.apply(mods, accessFields);
	}

	/**
	 * Transform the input MODS record with an XSLT and return as pretty JSON
	 */
	public static void prettyPrintSolrJsonFromMods(String record) throws Exception {
		String yamlStr =
				"stylesheet: '" + MODS2SOLR + "'\n" +
						"injections:\n" +
						"  - imageserver: 'https://example.com/imageserver/'\n" +
						"  - old_imageserver: 'http://kb-images.kb.dk'\n" +
						"  - origin: 'ds.test'\n";
		YAML yaml = YAML.parse(new ByteArrayInputStream(yamlStr.getBytes(StandardCharsets.UTF_8)));
		String solrString = getTransformedFromConfigWithAccessFields(yaml, record);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString);
	}

	public static void prettyPrintSolrJsonFromPreservica(String record) throws Exception {
		String yamlStr =
				"stylesheet: '" + PRESERVICA2SOLR + "'\n" +
						"injections:\n" +
						"  - streamingserver: 'example.com/streaming'\n" +
						"  - origin: 'ds.test'\n";
		YAML yaml = YAML.parse(new ByteArrayInputStream(yamlStr.getBytes(StandardCharsets.UTF_8)));
		String solrString = getTransformedFromConfigWithAccessFields(yaml, record);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString);
	}
}
