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
import org.w3c.dom.Document;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helpers for transforming XML using XSLTs. All methods are Thread-safe,
 * as long as Threads do not share the same Transformer.
 *
 * Most of the helpers have an option for ifnoring XML namespace. Setting this
 * to true strips namespaces from the input by doing a full DOM-parsing.
 * Besides being fairly expensive in terms of processing time and temporary
 * memory allocation, this is also a bad practice with regard to QA of the
 * input.
 *
 * Note: Transformer-errors and exceptions are thrown when they occur while
 * warnings are logged on {@link #warnlog}.
 */
public class XSLT {
    private static final Logger warnlog = LoggerFactory.getLogger(XSLT.class.getName() + "#warnings");
    private static final Logger log = LoggerFactory.getLogger(XSLT.class);

    /**
     * Creates a new transformer based on the given XSLTLocation.
     *
     * @param xslt the location of the XSLT.
     * @return a Transformer based on the given XSLT.
     * @throws javax.xml.transform.TransformerException thrown if for some
     *          reason a Transformer could not be instantiated.
     *          This is normally due to problems with the {@code xslt} URL
     * @see #getLocalTransformer for reusing Transformers.
     */
    public static Transformer createTransformer(URL xslt) throws TransformerException {
        return createTransformer(tfactory, xslt);
    }
    /**
     * Creates a new transformer based on the given XSLTLocation.
     * Useful for e.g. using Saxon instead of the default Xalan.
     *
     * @param factory the factory to use for creating the transformer.
     * @param xslt the location of the XSLT.
     * @return a Transformer based on the given XSLT.
     * @throws javax.xml.transform.TransformerException thrown if for some
     *          reason a Transformer could not be instantiated.
     *          This is normally due to problems with the {@code xslt} URL
     * @see #getLocalTransformer for reusing Transformers.
     */
    public static Transformer createTransformer(TransformerFactory factory, URL xslt) throws TransformerException {
        log.trace("createTransformer: Requesting and compiling XSLT from '" + xslt + "'");
        final long startTime = System.nanoTime();

        InputStream in = null;
        Transformer transformer;
        try {
            if (xslt == null) {
                throw new NullPointerException("xslt URL is null");
            }
            in = xslt.openStream();
            transformer = factory.newTransformer(new StreamSource(in, xslt.toString()));
            transformer.setErrorListener(getErrorListener());
        } catch (TransformerException e) {
            throw new TransformerException(String.format(
                    Locale.ROOT, "Unable to instantiate Transformer, a system configuration error for XSLT at '%s'", xslt), e);
        } catch (MalformedURLException e) {
            throw new TransformerException(String.format(
                    Locale.ROOT, "The URL to the XSLT is not a valid URL: '%s'", xslt), e);
        } catch (IOException e) {
            throw new TransformerException(String.format(
                    Locale.ROOT, "Unable to open the XSLT resource due to IOException '%s'", xslt), e);
        } catch (Exception e) {
            throw new TransformerException(String.format(
                    Locale.ROOT, "Unable to open the XSLT resource '%s'", xslt), e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                log.warn("Non-fatal IOException while closing stream to '" + xslt + "'");
            }
        }
        log.debug("createTransformer: Requested and compiled XSLT from '" + xslt + "' in " +
                  (System.nanoTime()-startTime)/1000000 + "ms");
        return transformer;
    }
    final static TransformerFactory tfactory = TransformerFactory.newInstance();

    private static ErrorListener ERRORLISTENER; // Singleton

    private static ErrorListener getErrorListener() {
        if (ERRORLISTENER == null) {
            ERRORLISTENER = new ErrorListener() {
                @Override
                public void warning(TransformerException exception) {
                    warnlog.debug("A transformer warning occured", exception);
                }

                @Override
                public void error(TransformerException exception)
                        throws TransformerException {
                    throw new TransformerException("A Transformer error occured", exception);
                }

                @Override
                public void fatalError(TransformerException exception)
                        throws TransformerException {
                    throw new TransformerException("A Transformer exception occurred", exception);
                }
            };
        }
        return ERRORLISTENER;
    }

    private static ThreadLocal<Map<String, Transformer>> localMapCache = createLocalMapCache();

    private static ThreadLocal<Map<String, Transformer>> createLocalMapCache() {
        return new ThreadLocal<>() {
            private final AtomicInteger counter = new AtomicInteger(0);

            @Override
            protected Map<String, Transformer> initialValue() {
                log.trace("Creating ThreadLocal localMapCache #" + counter.incrementAndGet());
                return new HashMap<>();
            }
        };
    }

    /**
     * Create or re-use a Transformer for the given xsltLocation.
     * The Transformer is {@link ThreadLocal}, so the method is thread-safe.
     *
     * Warning: A list is maintained for all XSLTs so changes to the xslt will
     * not be reflected. Call {@link #clearTransformerCache} to clear
     * the list.
     *
     * @param xslt the location of the XSLT.
     * @return a Transformer using the given XSLT.
     * @throws TransformerException if the Transformor could not be constructed.
     */
    public static Transformer getLocalTransformer(URL xslt) throws TransformerException {
        return getLocalTransformer(xslt, null);
    }

    /**
     * Create or re-use a Transformer for the given xsltLocation.
     * The Transformer is {@link ThreadLocal}, so the method is thread-safe.
     *
     * Warning: A list is maintained for all XSLTs so changes to the xslt will
     * not be reflected. Call {@link #clearTransformerCache} to clear
     * the list.
     *
     * @param xslt       the location of the XSLT.
     * @param parameters for the Transformer. The keys must be Strings. If the map is null, it will be ignored.
     * @return a Transformer using the given XSLT.
     * @throws TransformerException if the Transformer could not be constructed.
     */
    public static Transformer getLocalTransformer(URL xslt, Map<Object, Object> parameters) throws TransformerException {
        if (xslt == null) {
            throw new NullPointerException("The xslt was null");
        }
        Map<String, Transformer> map = localMapCache.get();
        Transformer transformer = map.get(xslt.toString());
        if (transformer == null) {
            transformer = createTransformer(xslt);
            map.put(xslt.toString(), transformer);
        }
        assignParameters(transformer, parameters);
        return transformer;
    }

    /**
     * Assigns the given parameters to the given Transformer. Previously assigned parameters are cleared first.
     * @param transformer an existing transformer.
     * @param parameters key-value pairs for parameters to assign.
     * @return the given transformer, with the given parameters assigned.
     */
    public static <K, V> Transformer assignParameters(Transformer transformer, Map<K, V> parameters) {
        try {
            transformer.clearParameters(); // Is this safe? Any defaults lost?
            if (parameters != null) {
                for (Map.Entry<K, V> entry : parameters.entrySet()) {
                    if (entry.getKey() == null || entry.getValue() == null) {
                        log.warn(String.format(
                                Locale.ROOT, "Skipping assignment of key/value pair '%s/%s' to Transformer " +
                                             "as null is not an acceptable value",
                                entry.getKey(), entry.getValue()));
                    }
                    transformer.setParameter(Objects.toString(entry.getKey()), entry.getValue());
                }
            }
        } catch (Exception e) {
            // We catch all exceptions (although not Errors) as the cache will be depleted and never refilled otherwise
            log.error("Unhandled Exception assigning parameters to Transformer." +
                      "The Transformer is unlikely to function properly", e);
        }
        return transformer;
    }

    /**
     * Clears the cache used by {@link #getLocalTransformer(java.net.URL)}.
     * This is safe to call as it only affects performance. Clearing the cache
     * means that changes to underlying XSLTs will be reflected and that any
     * memory allocated for caching is freed.
     *
     * Except for special cases, such as a huge number of different XSLTs,
     * the cache should only be cleared when the underlying XSLTs are changed.
     */
    public static void clearTransformerCache() {
        localMapCache = createLocalMapCache();
    }

    /* ******************** Transformer-calls below this ******************** */

    /**
     * Requests a cached ThreadLocal Transformer and performs the transformation.
     * {@code ignoreXMLNamespaces = false} is implied.
     * @param xslt the location of the XSLT to use.
     * @param in   the content to transform.
     * @return the transformed content.
     * @throws TransformerException if the transformation failed.
     * @see TransformerPool for an alternative with a fixed limit on the number of created Transformers.
     */
    public static String transform(URL xslt, String in) throws TransformerException {
        return transform(xslt, in, null, false);
    }

    /**
     * Requests a cached ThreadLocal Transformer and performs the
     * transformation.
     *
     * @param xslt                the location of the XSLT to use.
     * @param in                  the content to transform.
     * @param ignoreXMLNamespaces if true, namespaces in the input content will be stripped. This is not recommended,
     *                            but a lot of XML and XSLTs does not match namespaces correctly. Setting this to true
     *                            will have an impact on performance.
     * @return the transformed content.
     * @throws TransformerException if the transformation failed.
     */
    public static String transform(URL xslt, String in, boolean ignoreXMLNamespaces) throws TransformerException {
        return transform(xslt, in, null, ignoreXMLNamespaces);
    }

    /**
     * Requests a cached ThreadLocal Transformer and performs the
     * transformation.
     *
     * @param xslt       the location of the XSLT to use.
     * @param in         the content to transform.
     * @param parameters for the Transformer. The keys must be Strings. If the map is null, it will be ignored.
     * @return the transformed content.
     * @throws TransformerException if the transformation failed.
     * @see TransformerPool for an alternative with a fixed limit on the number of created Transformers.
     */
    public static String transform(URL xslt, String in, Map<Object, Object> parameters) throws TransformerException {
        return transform(xslt, in, parameters, false);
    }

    /**
     * Requests a cached ThreadLocal Transformer and performs the transformation.
     *
     * @param xslt                the location of the XSLT to use.
     * @param in                  the content to transform.
     * @param parameters          for the Transformer. The keys must be Strings.
     *                            If the map is null, it will be ignored.
     * @param ignoreXMLNamespaces if true, namespaces in the input content will
     *                            be stripped. This is not recommended, but a lot of XML and XSLTs
     *                            does not match namespaces correctly. Setting this to true will
     *                            have an impact on performance.
     * @return the transformed content.
     * @throws TransformerException if the transformation failed.
     * @see TransformerPool for an alternative with a fixed limit on the number of created Transformers.
     */
    public static String transform(URL xslt, String in, Map<Object, Object> parameters, boolean ignoreXMLNamespaces)
            throws TransformerException {
        StringWriter sw = new StringWriter();
        if (!ignoreXMLNamespaces) {
            transform(xslt, new StringReader(in), sw, parameters);
        } else {
            // Slowest
            //Reader noNamespace = removeNamespaces(new StringReader(in));
            //transform(getLocalTransformer(xslt, parameters), noNamespace, sw);

            // Slow
            //Document dom = DOM.stringToDOM(in);
            //transform(getLocalTransformer(xslt, parameters), dom, sw);

            // Roughly 30% faster than DOM-base NS stripping
            /*try {
                XMLFilter filter = new ParsingNamespaceRemover(
                        XMLReaderFactory.createXMLReader());
                Source source =
                   new SAXSource(filter, new InputSource(new StringReader(in)));

                getLocalTransformer(xslt, parameters).transform(
                        source, new StreamResult(sw));
            } catch (SAXException e) {
                // The Java runtime doesn't provide an XMLReader,
                // so we are doomed
                throw new RuntimeException(
                        "Failed to load default XMLReader implementation", e);
            }*/

            // More than twice as fast as DOM base NS stripping
            Reader noNamespace = new NamespaceRemover(new StringReader(in));
            transform(getLocalTransformer(xslt, parameters), noNamespace, sw);
        }
        return sw.toString();
    }

    /**
     * Performs a transformation.
     * @param transformer         the transformer to use.
     * @param in                  the content to transform.
     * @param ignoreXMLNamespaces if true, namespaces in the input content will
     *                            be stripped. This is not recommended, but a lot of XML and XSLTs
     *                            does not match namespaces correctly. Setting this to true will
     *                            have an impact on performance.
     * @return the transformed content.
     * @throws TransformerException if the transformation failed.
     */
    public static String transform(Transformer transformer, String in, boolean ignoreXMLNamespaces)
            throws TransformerException {
        StringWriter sw = new StringWriter();
        if (!ignoreXMLNamespaces) {
            transform(transformer, new StringReader(in), sw);
        } else {
            Reader noNamespace = new NamespaceRemover(new StringReader(in));
            transform(transformer, noNamespace, sw);
        }
        return sw.toString();

    }


    /**
     * Requests a cached ThreadLocal Transformer and performs the transformation.
     *
     * @param xslt       the location of the XSLT to use.
     * @param in         the content to transform.
     * @param parameters for the Transformer. The keys must be Strings.
     *                   If the map is null, it will be ignored.
     * @return the transformed content.
     * @throws TransformerException if the transformation failed.
     * @see TransformerPool for an alternative with a fixed limit on the number of created Transformers.
     */
    public static String transform(URL xslt, Reader in, Map<Object, Object> parameters) throws TransformerException {
        return transform(xslt, in, parameters, false);
    }

    /**
     * Requests a cached ThreadLocal Transformer and performs the transformation.
     *
     * @param xslt                the location of the XSLT to use.
     * @param in                  the content to transform.
     * @param parameters          for the Transformer. The keys must be Strings.
     *                            If the map is null, it will be ignored.
     * @param ignoreXMLNamespaces if true, namespaces in the input content will be stripped. This is not recommended,
     *                            but a lot of XML and XSLTs does not match namespaces correctly. Setting this to true
     *                            will have an impact on performance.
     * @return the transformed content.
     * @throws TransformerException if the transformation failed.
     * @see TransformerPool for an alternative with a fixed limit on the number of created Transformers.
     */
    public static String transform(URL xslt, Reader in, Map<Object, Object> parameters, boolean ignoreXMLNamespaces)
            throws TransformerException {
        StringWriter sw = new StringWriter();
        if (!ignoreXMLNamespaces) {
            transform(xslt, in, sw, parameters);
        } else {
            transform(getLocalTransformer(xslt, parameters), new NamespaceRemover(in), sw);
        }
        return sw.toString();
    }

    /**
     * Requests a cached ThreadLocal Transformer and performs the transformation.
     *
     * @param xslt       the location of the XSLT to use.
     * @param in         the content to transform.
     * @param parameters for the Transformer. The keys must be Strings. If the map is null, it will be ignored.
     * @return the transformed content. Note that the correct charset must be
     *         supplied to toString("charset") to get proper String results.
     *         The charset is specified by the XSLT.
     * @throws TransformerException if the transformation failed.
     * @see TransformerPool for an alternative with a fixed limit on the number of created Transformers.
     */
    public static ByteArrayOutputStream transform(URL xslt, byte[] in, Map<Object, Object> parameters) throws TransformerException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transform(xslt, new ByteArrayInputStream(in), out, parameters);
        return out;
    }

    /**
     * Requests a cached ThreadLocal Transformer and performs the transformation.
     *
     * @param xslt                the location of the XSLT to use.
     * @param in                  the content to transform.
     * @param parameters          for the Transformer. The keys must be Strings.
     *                            If the map is null, it will be ignored.
     * @param ignoreXMLNamespaces if true, namespaces in the input content will be stripped. This is not recommended,
     *                            but a lot of XML and XSLTs does not match namespaces correctly. Setting this to true
     *                            will have an impact on performance.
     * @return the transformed content. Note that the correct charset must be
     *         supplied to toString("charset") to get proper String results.
     *         The charset is specified by the XSLT.
     * @throws TransformerException if the transformation failed.
     * @see TransformerPool for an alternative with a fixed limit on the number of created Transformers.
     */
    public static ByteArrayOutputStream transform(URL xslt, byte[] in, Map<Object, Object> parameters, boolean ignoreXMLNamespaces)
            throws TransformerException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ignoreXMLNamespaces) {
            transform(xslt, new ByteArrayInputStream(in), out, parameters);
        } else {
            Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            Reader reader = new NamespaceRemover(new InputStreamReader(new ByteArrayInputStream(in), StandardCharsets.UTF_8));
            transform(getLocalTransformer(xslt, parameters), reader, writer);
        }
        return out;
    }

    /**
     * Requests a cached ThreadLocal Transformer and performs the transformation.
     *
     * @param xslt       the location of the XSLT to use.
     * @param in         the content to transform.
     * @param parameters for the Transformer. The keys must be Strings. If the map is null, it will be ignored.
     * @return the transformed content. Note that the correct charset must be
     *         supplied to toString("charset") to get proper String results.
     *         The charset is specified by the XSLT.
     * @throws TransformerException if the transformation failed.
     * @see TransformerPool for an alternative with a fixed limit on the number of created Transformers.
     */
    public static ByteArrayOutputStream transform(URL xslt, InputStream in, Map<Object, Object> parameters)
            throws TransformerException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transform(getLocalTransformer(xslt, parameters), in, out);
        return out;
    }

    /**
     * Requests a cached ThreadLocal Transformer and performs the transformation.
     *
     * @param xslt                the location of the XSLT to use.
     * @param in                  the content to transform.
     * @param parameters          for the Transformer. The keys must be Strings. If the map is null, it will be ignored.
     * @param ignoreXMLNamespaces if true, namespaces in the input content will be stripped. This is not recommended,
     *                            but a lot of XML and XSLTs does not match namespaces correctly. Setting this to true
     *                            will have an impact on performance.
     * @return the transformed content. Note that the correct charset must be
     *         supplied to toString("charset") to get proper String results.
     *         The charset is specified by the XSLT.
     * @throws TransformerException if the transformation failed.
     * @see TransformerPool for an alternative with a fixed limit on the number of created Transformers.
     */
    public static ByteArrayOutputStream transform(URL xslt, InputStream in, Map<Object, Object> parameters,
                                                  boolean ignoreXMLNamespaces) throws TransformerException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ignoreXMLNamespaces) {
            transform(getLocalTransformer(xslt, parameters), in, out);
        } else {
            Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            Reader reader = new NamespaceRemover(new InputStreamReader(in, StandardCharsets.UTF_8));
            transform(getLocalTransformer(xslt, parameters), reader, writer);
        }
        return out;
    }

    /**
     * Requests a cached ThreadLocal Transformer and performs the transformation.
     *
     * @param xslt       the location of the XSLT to use.
     * @param dom        the content to transform.
     * @param parameters for the Transformer. The keys must be Strings. If the map is null, it will be ignored.
     * @return the transformed content. Note that the correct charset must be
     *         supplied to toString("charset") to get proper String results.
     *         The charset is specified by the XSLT.
     * @throws TransformerException if the transformation failed.
     * @see TransformerPool for an alternative with a fixed limit on the number of created Transformers.
     */
    public static ByteArrayOutputStream transform(URL xslt, Document dom, Map<Object, Object> parameters) throws TransformerException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transform(getLocalTransformer(xslt, parameters), dom, out);
        return out;
    }

    /**
     * Requests a cached ThreadLocal Transformer and performs a transformation from Stream to Stream.
     *
     * @param xslt       the location of the XSLT to use.
     * @param in         input Stream.
     * @param out        output Stream.
     * @param parameters for the Transformer. The keys must be Strings.
     *                   If the map is null, it will be ignored.
     * @throws TransformerException if the transformation failed.
     * @see TransformerPool for an alternative with a fixed limit on the number of created Transformers.
     */
    public static void transform(URL xslt, InputStream in, OutputStream out, Map<Object, Object> parameters)
            throws TransformerException {
        transform(getLocalTransformer(xslt, parameters), in, out);
    }

    /**
     * Requests a cached ThreadLocal Transformer and performs a transformation from Reader to Writer.
     *
     * @param xslt       the location of the XSLT to use.
     * @param in         input.
     * @param out        output.
     * @param parameters for the Transformer. The keys must be Strings.
     *                   If the map is null, it will be ignored.
     * @throws TransformerException if the transformation failed.
     * @see TransformerPool for an alternative with a fixed limit on the number of created Transformers.
     */
    public static void transform(URL xslt, Reader in, Writer out, Map<Object, Object> parameters) throws TransformerException {
        transform(getLocalTransformer(xslt, parameters), in, out);
    }


    /* ********************* Resolved transformer below this ***************  */

    /**
     * Performs a transformation from Document to Stream with the transformer.
     *
     * @param transformer probably retrieved by {@link #getLocalTransformer} or using {@link TransformerPool}.
     * @param dom         input.
     * @param out         output.
     * @throws TransformerException if the transformation failed.
     */
    public static void transform(Transformer transformer, Document dom, OutputStream out) throws TransformerException {
        transformer.transform(new DOMSource(dom), new StreamResult(out));
    }

    /**
     * Performs a transformation from Stream to Stream with the transformer.
     *
     * @param transformer probably retrieved by {@link #getLocalTransformer} or using {@link TransformerPool}.
     * @param in          input Stream.
     * @param out         output Stream.
     * @throws TransformerException if the transformation failed.
     */
    public static void transform(Transformer transformer, InputStream in, OutputStream out)
            throws TransformerException {
        transformer.transform(new StreamSource(in), new StreamResult(out));
    }

    /**
     * Performs a transformation from Reader to Writer with the transformer.
     *
     * @param transformer probably retrieved by {@link #getLocalTransformer} or using {@link TransformerPool}.
     * @param in          input.
     * @param out         output.
     * @throws TransformerException if the transformation failed.
     */
    public static void transform(Transformer transformer, Reader in, Writer out) throws TransformerException {
        transformer.transform(new StreamSource(in), new StreamResult(out));
    }

    /**
     * Performs a transformation from DOM to Writer with the transformer.
     *
     * @param transformer probably retrieved by {@link #getLocalTransformer} or using {@link TransformerPool}.
     * @param dom         input.
     * @param out         output.
     * @throws TransformerException if the transformation failed.
     */
    public static void transform(Transformer transformer, Document dom, Writer out) throws TransformerException {
        transformer.transform(new DOMSource(dom), new StreamResult(out));
    }

    /* Using XSLT's to remove the namespaces is slower than DOM-parsing.
       Memory-usage has not been tested. 
     */

    static URL NAMESPACE_XSLT;

    static {
        NAMESPACE_XSLT = Thread.currentThread().getContextClassLoader().getResource(
                "dk/statsbiblioteket/util/xml/namespace_remover.xslt");
    }
    /*static ByteArrayOutputStream removeNamespaces(InputStream in) throws
                                                          TransformerException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transform(NAMESPACE_XSLT, in, out, null);
        return out;
    }

    static Reader removeNamespaces(Reader in) throws TransformerException {
        StringWriter sw = new StringWriter();
        transform(NAMESPACE_XSLT, in, sw, null);
        return new StringReader(sw.toString());
    }*/

    /**
     * Holds a number of {@link TransformerCache}s, auto-creating new caches when a new XSLT is encountered.
     * Using a transformer pool is recommended when the ThreadLocal approach used by {@link #transform(URL, String)}
     * and others is unviable, such as doing heavy XSLT in Tomcat from potentially hundreds of unique Threads.
     */
    public static class TransformerPool {
        private final Map<URL, TransformerCache> pool = new HashMap<>();
        private final int cacheSize;
        private final TransformerFactory factory;

        /**
         * Creates a new pool of {@link TransformerCache}s, where each cache is initialized with cacheSize
         * Transformers upon first request for a previously unseen XSLT.
         * @param cacheSize used when a new {@link TransformerCache} is created.
         */
        public TransformerPool(int cacheSize) {
            this(tfactory, cacheSize);
        }

        /**
         * Creates a new pool of {@link TransformerCache}s, where each cache is initialized with cacheSize
         * Transformers upon first request for a previously unseen XSLT.
         *
         * @param factory the factory to use for creating the transformer. e.g. Saxon instead of the default Xalan.
         * @param cacheSize used when a new {@link TransformerCache} is created.
         */
        public TransformerPool(TransformerFactory factory, int cacheSize) {
            this.factory = factory;
            this.cacheSize = cacheSize;
        }

        public void put(URL xslt, Transformer transformer) {
            TransformerCache cache;
            synchronized (pool) {
                cache = pool.get(xslt);
            }
            if (cache == null) {
                throw new IllegalStateException(
                        "Trying to deliver a Transformer when no TransformerCahce exists for XSLT '" + xslt + "'");
            }
            cache.put(transformer);
        }

        /**
         * Gets and potentially creates a cache for the given XSLT, the return a Transformer from the cache.
         * Note: If the xslt has previously not been requested, this method will block for _all_ threads
         * until the TransformerCache has been created.
         *
         * Important: Must be returned after use by calling {@link #put(URL, Transformer)}.
         * It is recommended to wrap processing in a try-finally.
         * @param xslt used together with {@link #cacheSize} to get or create the cache.
         * @return a transformer for the given XSLT.
         */
        public Transformer take(URL xslt) {
            return getCache(xslt).take();
        }

        /**
         * Gets and potentially creates a cache for the given XSLT, the return a Transformer from the cache.
         * Note: If the xslt has previously not been requested, this method will block for _all_ threads
         * until the TransformerCache has been created.
         *
         * Important: Must be returned after use by calling {@link #put(URL, Transformer)}.
         * It is recommended to wrap processing in a try-finally.
         * @param xslt       used together with {@link #cacheSize} to get or create the cache.
         * @param parameters key-value pairs of parameters to assign to the transformer.
         * @return a transformer for the given XSLT.
         */
        public Transformer take(URL xslt, Map<String, String> parameters) {
            return getCache(xslt).take(parameters);
        }

        /**
         * Shorthand for calling {@link #take(URL)} and {@link XSLT#transform(Transformer, String, boolean)}
         * @param xslt       used together with {@link #cacheSize} to get or create the cache.
         * @param xml the input to transform.
         * @param ignoreXMLNamespaces true if namespaces should be removed from the input before transforming.
         * @return the xml transformed by the xslt.
         * @throws TransformerException if the transformation failed or a Transformer could not be created.
         */
        public String transform(URL xslt, String xml, boolean ignoreXMLNamespaces) throws TransformerException {
            return transform(xslt, xml, null, ignoreXMLNamespaces);
        }

        /**
         * Shorthand for calling {@link #take(URL, Map)} and {@link XSLT#transform(Transformer, String, boolean)}
         * @param xslt       used together with {@link #cacheSize} to get or create the cache.
         * @param xml the input to transform.
         * @param parameters for the Transformer.
         * @param ignoreXMLNamespaces true if namespaces should be removed from the input before transforming.
         * @return the xml transformed by the xslt.
         * @throws TransformerException if the transformation failed or a Transformer could not be created.
         */
        public String transform(URL xslt, String xml, Map<String, String> parameters, boolean ignoreXMLNamespaces)
                throws TransformerException {
            return getCache(xslt).transform(xml, parameters, ignoreXMLNamespaces);
        }

        /**
         * Optional creation and initialisation of a {@link TransformerCache} for the given xslt.
         * Call this on start up of the application to take the initialisation hit up front instead of
         * taking it at the first call.
         * Subsequent calls to this method with the same URL will return immediately.
         * @param xslt the XSLT for the TransformerCache to initialise.
         */
        public void fillCache(URL xslt) {
            getCache(xslt);
        }

        /**
         * Gets and potentially creates a cache for the given XSLT.
         * Note: If the xslt has previously not been requested, this method will block for _all_ threads
         * until the TransformerCache has been created.
         * @param xslt used together with {@link #cacheSize} to get or create the cache.
         * @return a cache for the given XSLT.
         */
        public TransformerCache getCache(URL xslt) {
            TransformerCache cache;
            synchronized (pool) {
                cache = pool.get(xslt);
                if (cache != null) {
                    return cache;
                }
                // Cache does not exist. Create & add it inside of the synchronization
                cache = new TransformerCache(factory, xslt, cacheSize, true);
                pool.put(xslt, cache);
            }
            // Cache is newly created and must therefore be filled
            try {
                cache.fillWithTransformers();
            } catch (Exception e) {
                synchronized (pool) {
                    pool.remove(xslt);
                }
                throw new RuntimeException(
                        "Exception during delayed fill of TransformerCache for XSLT '" + xslt + "'", e);
            }
            return cache;
        }
    }

    /**
     * A cache of Transformers, initialized from the same XSLT.
     * The cache is fixed size, completely filled upon creation and and blocks on {@link TransformerCache#take()}.
     *
     * It is essential for the calling code to return Transformers after use.
     */
    public static class TransformerCache {
        protected final URL xslt;
        private final ArrayBlockingQueue<Transformer> transformers;
        private final TransformerFactory factory;

        public TransformerCache(URL xslt, int cacheSize) {
            this(tfactory, xslt, cacheSize, false);
        }
        public TransformerCache(TransformerFactory factory, URL xslt, int cacheSize) {
            this(factory, xslt, cacheSize, false);
        }
        private TransformerCache(URL xslt, int cacheSize, boolean noFill) {
            this(tfactory, xslt, cacheSize, noFill);
        }
        private TransformerCache(TransformerFactory factory, URL xslt, int cacheSize, boolean noFill) {
            log.info("Creating TransformerCache with " + cacheSize + " entries for XSLT '" + xslt + "'");
            this.factory = factory;
            this.xslt = xslt;
            transformers = new ArrayBlockingQueue<>(cacheSize);
            if (!noFill) {
                fillWithTransformers();
            }
        }

        private void fillWithTransformers() {
            final long startTime = System.nanoTime();
            final int cacheSize = transformers.remainingCapacity();
            for (int i = 0 ; i < cacheSize ; i++) {
                try {
                    //long createTime = -System.nanoTime();
                    transformers.put(createTransformer(xslt));
                    //createTime += System.nanoTime();
                    //log.trace("Created Transformer for " + xslt + " in " + createTime/1000000 + "ms");
                } catch (InterruptedException e) {
                    throw new IllegalStateException("Interrupted while initializing cache of size " + cacheSize, e);
                } catch (TransformerException e) {
                    throw new IllegalStateException("Unable to create Transformer for '" + xslt + "'", e);
                }
            }
            log.debug("Created " + cacheSize + " Transformers for " + xslt + " in " +
                      (System.nanoTime()-startTime)/1000000 + "ms");
        }

        protected Transformer createTransformer(URL xslt) throws TransformerException {
            return XSLT.createTransformer(factory, xslt);
        }

        /**
         * Returns a Transformer after use. Never blocks, unless Transformers created outside of the TransformerCache
         * are used.
         * @param transformer the transformer to return.
         */
        public void put(Transformer transformer) {
            try {
                transformer.reset();
                transformer.clearParameters();
                transformers.put(transformer);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting for put", e);
            }
        }

        /**
         * Waits until a Transformer is available and returns it.
         * Important: Must be returned after use by calling {@link #put(Transformer)}.
         * It is recommended to wrap processing in a try-finally.
         * @return a transformer ready for use.
         */
        public Transformer take() {
            try {
                return transformers.take();
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting for take", e);
            }
        }

        /**
         * Waits until a Transformer is available, assigns the given parameters and returns it.
         * Important: Must be returned after use by calling {@link #put(Transformer)}.
         * It is recommended to wrap processing in a try-finally.
         * @param parameters key-value pairs of parameters to assign to the transformer.
         * @return a transformer ready for use.
         */
        public Transformer take(Map<String, String> parameters) {
            return assignParameters(take(), parameters);
        }

        /**
         * Shorthand for calling {@link #take()} and {@link XSLT#transform(Transformer, String, boolean)}
         * @param xml the input to transform.
         * @return the xml transformed by the Transformer.
         * @param ignoreXMLNamespaces true if namespaces should be removed from the input before transforming.
         * @throws TransformerException if a Transformer could not be created from the xslt.
         */
        public String transform(String xml, boolean ignoreXMLNamespaces) throws TransformerException {
            return transform(xml, null, ignoreXMLNamespaces);
        }

        /**
         * Shorthand for calling {@link #take(Map)} and {@link XSLT#transform(Transformer, String, boolean)}
         * @param xml the input to transform.
         * @param parameters parameters for the Transformer.
         * @param ignoreXMLNamespaces true if namespaces should be removed from the input before transforming.
         * @return the xml transformed by the Transformer.
         * @throws TransformerException if the transformation failed.
         */
        public String transform(String xml, Map<String, String> parameters, boolean ignoreXMLNamespaces)
                throws TransformerException {
            Transformer transformer = take(parameters);
            try {
                return XSLT.transform(transformer, xml, ignoreXMLNamespaces);
            } finally {
                put(transformer);
            }
        }

        public int available() {
            return transformers.size();
        }

    }
}
