package dk.kb.present.transform;

import dk.kb.present.config.TransformerConfig;
import dk.kb.present.config.XsltConfig;

import java.io.IOException;

public class XSLTSolrFromSchemaFactory extends XSLTFactory {

    @Override
    public String getTransformerID() {
        return XSLTSolrFromSchemaTransformer.ID;
    }

    @Override
    public DSTransformer createTransformer(TransformerConfig conf) throws IOException {
        XsltConfig c = (XsltConfig) conf;
        return new XSLTSolrFromSchemaTransformer(c.getStylesheet(), c.getInjections());
    }
}
