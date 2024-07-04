/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.present.transform;

import dk.kb.present.config.ServiceConfig;
import dk.kb.util.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * XSLT transformer using Saxon HE 3.
 *
 */
public class XSLTTransformer implements DSTransformer {
    private static final Logger log = LoggerFactory.getLogger(XSLTTransformer.class);
    public static final String ID = "xslt";

    public static final TransformerFactory transformerFactory;

    protected static final Semaphore semaphore;
    protected static final boolean useSemaphore;

    static {
        System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
        transformerFactory = TransformerFactory.newInstance();
        // Ignoring base as it is always null in the ds-present code
        transformerFactory.setURIResolver((href, base) -> new StreamSource(Resolver.resolveStream(href)));

        useSemaphore =  ServiceConfig.getConfig().getInteger("transformations.threads",0) > 0;
        semaphore = new Semaphore(ServiceConfig.getConfig().getInteger("transformations.threads",0));
    }
    public final String stylesheet;
    public final Templates templates;
    public final Map<String, String> fixedInjections;

    /**
     * Construct a transformer that uses Saxon to perform an XSLT transformation on its input.
     * @param stylesheet the stylesheet for the transformation. This can be be a file resolved relatively to the current
     *                   folder, under the user.home or on the classpath.
     * @param fixedInjections variables that should always be injected into the transformator.
     *                   These co-exists with {@code metadata} in {@link #apply(String, Map)}.
     *                   If the same key is in both {@code injections} and {@code metadata}, {@code metadata} wins.
     *                   Ignored if null.
     * @throws IOException if the stylesheet could not be resolved.
     */
    public XSLTTransformer(String stylesheet, Map<String, String> fixedInjections) throws IOException {
        this.stylesheet = stylesheet;
        this.fixedInjections = fixedInjections;
        URL stylesheetURL = Resolver.resolveURL(stylesheet);
        if (stylesheetURL == null) {
            throw new FileNotFoundException("Unable to resolve stylesheet '" + stylesheet + "'");
        }
        try (InputStream is = stylesheetURL.openStream()) {
            templates = transformerFactory.newTemplates(new StreamSource(is));
        } catch (IOException e) {
            throw new IOException("Unable to retrieve stylesheet from '" + stylesheet + "'", e);
        } catch (TransformerConfigurationException e) {
            throw new IllegalStateException("Unable to parse stylesheet at '" + stylesheet + "'", e);
        }
        log.debug("Constructed " + this);
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String getStylesheet() {
        return stylesheet;
    }

    @Override
    public String apply(String s, Map<String, String> metadata) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Transformer transformer = templates.newTransformer();
            try (Reader in = new StringReader(s)) {
                if (fixedInjections != null) {
                    fixedInjections.forEach(transformer::setParameter);
                }
                metadata.forEach(transformer::setParameter);

                if (useSemaphore) {
                    semaphore.acquire();
                }
                transformer.transform(new StreamSource(in), new StreamResult(out));
            }
            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException | TransformerException e) {
            throw new RuntimeTransformerException(
                    "Exception transforming with stylesheet '" + stylesheet + "' and metadata '" + metadata + "'", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (useSemaphore) {
                semaphore.release();
            }
        }
    }

    @Override
    public String toString() {
        return "XSLTTransformer(stylesheet='" + stylesheet + "', fixedInjections='" + fixedInjections + "'.)";
    }

}
