package dk.kb.discover;

import dk.kb.util.Resolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import dk.kb.present.transform.XSLTTransformer;

public class DocumentationExtractor {





    public static String getTransformed(String xsltResource, String xmlResource) throws IOException {
        return getTransformed(xsltResource, xmlResource, null, null);
    }
    public static String getTransformed(String xsltResource, String xmlResource, Map<String,String> fixedInjections,
                                        Map<String,String> metadata) throws IOException {
        XSLTTransformer transformer = new XSLTTransformer(xsltResource, fixedInjections);
        String mods = Resolver.resolveUTF8String(xmlResource);
        // Ensure metadata is defined and that it is mutable
        metadata = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
        return transformer.apply(mods, metadata);
    }
}
