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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dk.kb.present.solr.EmbeddedSolrTest.MODS2SOLR;
import static dk.kb.present.solr.EmbeddedSolrTest.PRESERVICA2SOLR;
import static dk.kb.present.transform.XSLTPreservicaToSolrTransformerTest.SCHEMA2SOLR;

public class TestUtil {
	private static final Logger log = LoggerFactory.getLogger(TestUtil.class);



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
		String xml = Resolver.resolveUTF8String(xmlResource);
		HashMap<String, String> metadata = XsltCopyrightMapper.applyXsltCopyrightTransformer(xml);

		metadata.put("recordID", "ds.test:" + Path.of(xmlResource).getFileName().toString());
		metadata.put("streamingserver", "www.example.com/streaming/");
		metadata.put("origin", "ds.test");
		metadata.put("conditionsOfAccess", "placeholderCondition");
		metadata.put("mTime", "1701261949625000");
		//System.out.println("access fields:"+metadata);
		return transformer.apply(xml, metadata);
	}

	public static String getTransformedWithVideoChildAdded(
			String xsltResource, String xmlResource, Map<String, String> injections) throws IOException {
		XSLTTransformer transformer = new XSLTTransformer(xsltResource, injections);
		String xml = Resolver.resolveUTF8String(xmlResource);
		HashMap<String, String> metadata = XsltCopyrightMapper.applyXsltCopyrightTransformer(xml);
		// Preservica 7 delivers very small children, containing just the name of the file
		String childData = "8946d31d-a81c-447f-b84d-ff80644353d2.mp4";

		metadata.put("recordID", "ds.test:" + Path.of(xmlResource).getFileName().toString());
		metadata.put("streamingserver", "www.example.com/streaming/");
		metadata.put("origin", "ds.test");
		metadata.put("manifestation", childData);
		metadata.put("conditionsOfAccess", "placeholderCondition");
		metadata.put("mTime", "1701261949625000");
		//System.out.println("access fields:"+metadata);
		return transformer.apply(xml, metadata);
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
		String xml = Resolver.resolveUTF8String(xmlResource);
		HashMap<String, String> metadata = XsltCopyrightMapper.applyXsltCopyrightTransformer(xml);

        metadata.put("recordID", "ds.test:" + Path.of(xmlResource).getFileName().toString());
		metadata.put("streamingserver", "www.example.com/streaming/");
		metadata.put("origin", "ds.test");
		//System.out.println("access fields:"+metadata);
		return transformer.apply(xml, metadata);
	}

	/**
	 * Transforms the inputted XML with the given transformer to schema.org compliant JSON, then transforms the
	 * schema.org JSON to solr documents.
	 * @param schemaOrgTransformer used to transform from origin specific format to general schema.org json.
	 * @param record the record to transform.
	 * @return a solr document ready for indexing, created from the schema.org representation of the inputted XML.
	 */
	public static String getTransformedToSolrJsonThroughSchemaJsonWithPreservica7File(String schemaOrgTransformer, String record) throws IOException {
		Map<String, String> injections = Map.of("imageserver", "https://example.com/imageserver/",
				"streamingserver" ,"https://www.example.com/streamingserver/",
				"manifestation", "8946d31d-a81c-447f-b84d-ff80644353d2.mp4",
				"holdbackDate", "2026-01-17T09:34:42Z",
				"holdbackPurposeName","Aktualitet og debat",
				"kalturaId", "aVeryTrueKalturaID");
		String schemaOrgJson = TestUtil.getTransformedWithAccessFieldsAdded(schemaOrgTransformer, record, injections);
		//prettyPrintJson(schemaOrgJson);

		String placeholderXml = "placeholder.xml";
		Map<String, String> mapOfJson = Map.of("schemaorgjson", schemaOrgJson);
		String solrJson = TestUtil.getTransformedWithAccessFieldsAdded(SCHEMA2SOLR, placeholderXml, mapOfJson);
		//prettyPrintJson(solrJson);
		return solrJson;
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
		prettyPrintSolrJson(record, yamlStr);
	}

	public static void prettyPrintSolrJsonFromPreservica(String record) throws Exception {
		String yamlStr =
				"stylesheet: '" + PRESERVICA2SOLR + "'\n" +
						"injections:\n" +
						"  - streamingserver: 'example.com/streaming'\n" +
						"  - origin: 'ds.test'\n";
		prettyPrintSolrJson(record, yamlStr);
	}

	private static void prettyPrintSolrJson(String record, String yamlStr) throws Exception {
		YAML yaml = YAML.parse(new ByteArrayInputStream(yamlStr.getBytes(StandardCharsets.UTF_8)));
		String solrString = getTransformedFromConfigWithAccessFields(yaml, record);
		prettyPrintJson(solrString);
	}

	public static void prettyPrintJson(String solrString) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(solrString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString);
	}
}
