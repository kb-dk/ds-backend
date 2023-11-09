package dk.kb.present.transform;

import dk.kb.util.yaml.YAML;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class XSLTSolrFromSchemaFactory extends XSLTFactory{
    public static final String STYLESHEET_KEY = "stylesheet";
    public static final String INJECTIONS_KEY = "injections";

    @Override
    public String getTransformerID() {
        return XSLTSolrFromSchemaTransformer.ID;
    }

    @Override
    public DSTransformer createTransformer(YAML conf) throws IOException {
        assertConfigKeys(conf, STYLESHEET_KEY);

        Map<String, String> injections = null;
        if (conf.containsKey(INJECTIONS_KEY)) {
            injections = new HashMap<>();
            for (YAML yInjection: conf.getYAMLList(INJECTIONS_KEY)) {
                if (yInjection.size() != 1) {
                    throw new IllegalArgumentException(
                            "Expected a single entry (key-value pair) in injection '" + yInjection +
                                    "' but got " + yInjection.size());
                }
                // TODO: Move away from the strange "listed maps with one entry" way of stating injections
                String firstKey = yInjection.keySet().stream().findFirst().get();
                injections.put(firstKey, yInjection.getString(firstKey));
            }
        }
        return new XSLTSolrFromSchemaTransformer(conf.getString(STYLESHEET_KEY), injections);
    }
}
