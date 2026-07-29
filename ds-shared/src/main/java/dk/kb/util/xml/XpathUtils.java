/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.kb.util.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helpers for doing DOM parsing and manipulations. The methods are thread-safe and allows for parallel execution of the
 * same xpath.
 */
public class XpathUtils {
    private static final Logger log = LoggerFactory.getLogger(XpathUtils.class);
    
    /**
     * Importatnt: All access to the xpathCompiler should be synchronized on it since it is not thread safe!
     */
    private static final XPath xpathCompiler =
            XPathFactory.newInstance().newXPath();
    
    
    /**
     * Create a new xpath selector configured with the given namespaces
     *
     * @param nsContextStrings map of namespace prefix to namespace uri
     * @return a brand new xpath selector
     */
    public static XPathSelector createXPathSelector(Map<String, String> nsContextStrings) {
        return new XPathSelectorImpl(nsContextStrings);
    }
    
    
    /**
     * Create a new xpath selector configured with the given namespaces
     *
     * @param nsContextStrings list of strings. Every pair is a namespace-prefix and namespace uri
     * @return a brand new xpath selector
     */
    public static XPathSelector createXPathSelector(String... nsContextStrings) {
        return new XPathSelectorImpl(nsContextStrings);
        
    }
    
    private static class XPathSelectorImpl implements XPathSelector {
        
        private final NamespaceContext nsContext;
        
        private final LRUCache<String, XPathExpression> cache = new LRUCache<>(50);
        
        private XPathSelectorImpl(String... nsContextStrings) {
            nsContext = new DefaultNamespaceContext(null, nsContextStrings);
        }
        
        private XPathSelectorImpl(Map<String, String> nsContextStrings) {
            nsContext = new DefaultNamespaceContext(null, nsContextStrings);
        }
        
        @Override
        public Integer selectInteger(Node node, String xpath, Integer defaultValue) {
            String strVal = selectString(node, xpath);
            if (strVal == null || "".equals(strVal)) {
                return defaultValue;
            }
            return Integer.valueOf(strVal);
        }
        
        @Override
        public Integer selectInteger(Node node, String xpath) {
            return selectInteger(node, xpath, null);
        }
        
        @Override
        public Double selectDouble(Node node, String xpath, Double defaultValue) {
            Double d = (Double) selectObject(node, xpath, XPathConstants.NUMBER);
            if (d == null || d.equals(Double.NaN)) {
                d = defaultValue;
            }
            return d;
        }
        
        
        @Override
        public Double selectDouble(Node node, String xpath) {
            return selectDouble(node, xpath, null);
        }
        
        @Override
        public Boolean selectBoolean(Node node, String xpath, Boolean defaultValue) {
            String tmp = selectString(node, xpath, null);
            if (tmp == null) {
                return defaultValue;
            }
            return Boolean.parseBoolean(tmp);
        }
        
        @Override
        public Boolean selectBoolean(Node node, String xpath) {
            return selectBoolean(node, xpath, null);
        }
        
        @Override
        public String selectString(Node node, String xpath, String defaultValue) {
            if ("".equals(defaultValue)) {
                // By default the XPath engine will return an empty string
                // if it is unable to find the requested path
                return (String) selectObject(node, xpath, XPathConstants.STRING);
            }
            
            Node n = selectNode(node, xpath);
            if (n == null) {
                return defaultValue;
            }
            
            // FIXME: Can we avoid running the xpath twice?
            //        The local expression cache helps, but anyway...
            return (String) selectObject(node, xpath, XPathConstants.STRING);
        }
        
        @Override
        public String selectString(Node node, String xpath) {
            return selectString(node, xpath, "");
        }
        
        @Override
        public NodeList selectNodeList(Node dom, String xpath) {
            return (NodeList) nodeList((NodeList) selectObject(dom, xpath, XPathConstants.NODESET));
        }
        
        @Override
        public List<String> selectStringList(Node dom, String xpath) {
            return nodeList((NodeList) selectObject(dom, xpath, XPathConstants.NODESET))
                      .stream()
                      .map(node -> node.getNodeValue())
                      .collect(Collectors.toList());
        }
        
        
        @Override
        public Node selectNode(Node dom, String xpath) {
            return (Node) selectObject(dom, xpath, XPathConstants.NODE);
        }
        
        protected Object selectObject(Node dom, String xpath, QName returnType) {
            Object retval = null;
            
            try {
                XPathExpression exp = getXPathExpression(xpath);
                
                retval = exp.evaluate(dom, returnType);
            } catch (NullPointerException e) {
                log.debug(String.format(Locale.ROOT, 
                                        "NullPointerException when extracting XPath '%s' on " +
                                        "element type %s. Returning null",
                                        xpath, returnType.getLocalPart()), e);
            } catch (XPathExpressionException e) {
                log.warn(String.format(Locale.ROOT, 
                                       "Error in XPath expression '%s' when selecting %s: %s",
                                       xpath, returnType.getLocalPart(), e.getMessage()), e);
            }
            
            return retval;
        }
        
        
        private XPathExpression getXPathExpression(String xpath) throws XPathExpressionException {
            // Get the compiled xpath from the cache or compile and
            // cache it if we don't have it
            
            XPathExpression exp = cache.get(xpath);
            if (exp == null) {
                synchronized (xpathCompiler) {
                    if (nsContext != null) {
                        xpathCompiler.setNamespaceContext(nsContext);
                    }
                    exp = xpathCompiler.compile(xpath);
                    cache.put(xpath, exp);
                }
            }
            return exp;
        }
        
    }
    
    /**
     * Utility to convert a NodeList into a {@code List<Node>}
     * @param list the NodeList
     * @return the same list as a {@code List<Node>}
     */
    public static List<Node> nodeList(NodeList list) {
        List<Node> result = new ArrayList<>();
        for (int i = 0; i < list.getLength(); i++) {
            result.add(list.item(i));
        }
        return result;
    }
    
    private static class DefaultNamespaceContext implements NamespaceContext {
        
        /* URI -> prefixes map */
        private final Map<String, Collection<String>> namespace;
        
        /* prefix -> URI map */
        private final Map<String, String> prefixes;
        
        private final String defaultNamespaceURI;
        
        
        /**
         * Constructs a NamespaceContext with no default namespace.
         * <p>
         * Beware that the default namespace can only be set during construction.
         */
        public DefaultNamespaceContext() {
            this(null);
        }
        
        /**
         * Constructs a NamespaceContext with a default namespace.
         *
         * @param defaultNamespaceURI , the default namespace for this context.
         */
        public DefaultNamespaceContext(String defaultNamespaceURI) {
            namespace = new HashMap<>();
            prefixes = new HashMap<>();
            this.defaultNamespaceURI = defaultNamespaceURI;
            
            namespace.put(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                          Collections.singletonList(XMLConstants.XMLNS_ATTRIBUTE));
            
            prefixes.put(XMLConstants.XMLNS_ATTRIBUTE,
                         XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
            
            namespace.put(XMLConstants.XML_NS_URI,
                          Collections.singletonList(XMLConstants.XML_NS_PREFIX));
            
            prefixes.put(XMLConstants.XML_NS_PREFIX,
                         XMLConstants.XML_NS_URI);
        }
        
        public DefaultNamespaceContext(String defaultNamespaceURI,
                                       String... nsContext) {
            this(defaultNamespaceURI);
            
            if (nsContext.length % 2 != 0) {
                throw new IllegalArgumentException("Odd number of arguments. " +
                                                   "Prefix/URI pairs must match up");
            }
            
            for (int i = 0; i < nsContext.length; i += 2) {
                setNameSpace(nsContext[i + 1], nsContext[i]);
            }
        }
        
        public DefaultNamespaceContext(String defaultNamespaceURI,
                                       Map<String, String> nsContexts) {
            this(defaultNamespaceURI);
            
            for (Map.Entry<String, String> nscontext : nsContexts.entrySet()) {
                setNameSpace(nscontext.getValue(),nscontext.getKey());
            }
        }
        
        /**
         * Set or add a namespace to the context and associated it with a prefix.
         * <p>
         * A given prefix can only be associated with one namespace in the context. A namespace can have multiple
         * prefixes.
         * <p>
         * The prefixes: {@code xml}, and {@code xmlns} are reserved and predefined in any context.
         *
         * @param namespaceURL the namespace uri
         * @param prefix       the prifix to registere with the uri
         * @throws IllegalArgumentException thrown when trying to assign a namespace to a reserved prefix
         */
        public void setNameSpace(String namespaceURL, String prefix)
                throws IllegalArgumentException {
            Collection<String> s = namespace.get(namespaceURL);
            
            if (s == null) {
                s = new HashSet<String>();
            }
            
            s.add(prefix);
            namespace.put(namespaceURL, s);
            this.prefixes.put(prefix, namespaceURL);
        }
        
        public String getNamespaceURI(String prefix) {
            if (prefix == null) {
                throw new IllegalArgumentException();
            }
            
            if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)
                && this.defaultNamespaceURI != null) {
                return this.defaultNamespaceURI;
            }
            
            
            if (!this.prefixes.containsKey(prefix)) {
                return XMLConstants.NULL_NS_URI;
            }
            
            return this.prefixes.get(prefix);
            
        }
        
        public String getPrefix(String namespaceURI) {
            if (namespaceURI == null) {
                throw new IllegalArgumentException();
            }
            
            if (namespaceURI.equals(defaultNamespaceURI)) {
                return XMLConstants.DEFAULT_NS_PREFIX;
            }
            
            Collection<String> s = namespace.get(namespaceURI);
            if (s != null && !s.isEmpty()) {
                return s.iterator().next();
            } else {
                return null;
            }
        }
        
        @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            
            if (namespaceURI == null) {
                throw new IllegalArgumentException();
            }
            
            if (namespaceURI.equals(XMLConstants.XML_NS_URI)) {
                return new NonModifiableIterator<>(
                        Collections.singletonList(XMLConstants.XML_NS_PREFIX).iterator());
            }
            if (namespaceURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
                return new NonModifiableIterator<>(
                        Collections.singletonList(XMLConstants.XMLNS_ATTRIBUTE).iterator());
            }
            if (namespaceURI.equals(defaultNamespaceURI)) {
                return new NonModifiableIterator<>(
                        Collections.singletonList(XMLConstants.DEFAULT_NS_PREFIX).iterator());
            }
            
            Collection<String> s = namespace.get(namespaceURI);
            if (s != null && !namespace.isEmpty()) {
                return new NonModifiableIterator<>(s.iterator());
            } else {
                return new NonModifiableIterator<>(Collections.emptyIterator());
            }
        }
        
        /**
         * This Iterator wraps any Iterator and makes the remove() method unsupported.<br>
         *
         * @author Hans Lund, State and University Library, Aarhus Denamrk.
         * @version $Id: DefaultNamespaceContext.java,v 1.5 2007/10/04 13:28:21 te Exp $
         * @see NamespaceContext#getPrefixes(String)
         */
        static class NonModifiableIterator<T> implements Iterator<T> {
            
            Iterator<T> wrapped;
            
            NonModifiableIterator(Iterator<T> iter) {
                wrapped = iter;
            }
            
            /**
             * Returns <tt>true</tt> if the iteration has more elements. (In other words, returns <tt>true</tt> if
             * <tt>next</tt> would return an element rather than throwing an exception.)
             *
             * @return <tt>true</tt> if the iterator has more elements.
             */
            public boolean hasNext() {
                return wrapped.hasNext();
            }
            
            /**
             * Returns the next element in the iteration.  Calling this method repeatedly until the {@link #hasNext()}
             * method returns false will return each element in the underlying collection exactly once.
             *
             * @return the next element in the iteration.
             * @throws java.util.NoSuchElementException iteration has no more elements.
             */
            public T next() {
                return wrapped.next();
            }
            
            /**
             * This method is not supported on this Iterator.
             * <p>
             * Allways throws UnsupportedOperationException {@link NamespaceContext#getPrefixes(String)}
             *
             * @throws UnsupportedOperationException if the <tt>remove</tt> operation is not supported by this
             *                                       Iterator.
             */
            public void remove() {
                throw new UnsupportedOperationException("Conform to XML API please");
            }
        }
    }
}
