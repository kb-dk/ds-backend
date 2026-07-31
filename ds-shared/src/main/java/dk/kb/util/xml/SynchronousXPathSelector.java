package dk.kb.util.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Thread safe implementation of XPathSelector. This implementation allows for
 * parallel execution of the same xpath by making multiple instances of the
 * underlying XPathExpression.
 */
public class SynchronousXPathSelector extends XPathSelectorImpl {

    private static final Logger log = LoggerFactory.getLogger(SynchronousXPathSelector.class);

    /**
     * Important: All access to the xpathCompiler should be synchronized on it
     * since it is not thread safe!
     */
    private static final XPath xpathCompiler =
            XPathFactory.newInstance().newXPath();

    private LRUCache<String, List<XPathExpression>> cache;
    private NamespaceContext nsContext;

    public SynchronousXPathSelector(NamespaceContext nsContext, int cacheSize) {
        super(nsContext, 1);
        cache = new LRUCache<String, List<XPathExpression>>(cacheSize);
    }

    @Override
    protected Object selectObject(Node dom, String xpath, QName returnType) {
        Object retval = null;

        try {
            XPathExpression exp;
            // Get the compiled xpath from the cache or compile if not present
            synchronized (xpathCompiler) {
                List<XPathExpression> expl = cache.get(xpath);
                if (expl == null || expl.size() == 0) {
                    if (nsContext != null) {
                        xpathCompiler.setNamespaceContext(nsContext);
                    }
                    exp = xpathCompiler.compile(xpath);
                } else {
                    exp = expl.remove(expl.size() - 1);
                }
            }
            try {
                Document doc = dom.getOwnerDocument();
                if (doc != null){
                    synchronized (doc){
                        retval = exp.evaluate(dom, returnType);
                    }
                } else {
                    synchronized (dom){
                        retval = exp.evaluate(dom, returnType);
                    }
                }
            } finally {
                // Put back the compiled xpath in the cache
                synchronized (xpathCompiler) {
                    List<XPathExpression> expl = cache.get(xpath);
                    if (expl == null) {
                        expl = new ArrayList<XPathExpression>(2);
                    }
                    expl.add(exp);
                    cache.put(xpath, expl); // Always touch the LRU
                }
            }
        } catch (NullPointerException e) {
            log.debug(String.format(
                    Locale.ROOT, "NullPointerException when extracting XPath '%s' on element type %s. Returning null",
                    xpath, returnType.getLocalPart()), e);
        } catch (XPathExpressionException e) {
            log.warn(String.format(
                    Locale.ROOT, "Error in XPath expression '%s' when selecting %s: %s",
                    xpath, returnType.getLocalPart(), e.getMessage()), e);
        }

        return retval;
    }

    @Override
    void clearCache() {
        cache.clear();
    }
}