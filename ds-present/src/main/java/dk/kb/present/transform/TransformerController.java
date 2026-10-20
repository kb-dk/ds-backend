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

import dk.kb.present.config.TransformerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scans the classes for implementations of {@link DSTransformerFactory} and makes it possible to get
 * {@link DSTransformer}s by their ID and a configuration.
 */
public class TransformerController {
    private static final Logger log = LoggerFactory.getLogger(TransformerController.class);

    private static final Map<String, DSTransformerFactory> factories = getTransformerFactories();

    /**
     * Create a transformer with the given configuration.
     *
     * @param conf the transformer configuration; {@link TransformerConfig#getType()} determines which factory is
     *             used to create it.
     * @return a configured transformer for the given type, ready for use.
     * @throws NullPointerException if no transformer with the given type could be located.
     * @throws Exception if the transformer could not be created.
     */
    public static DSTransformer createTransformer(TransformerConfig conf) throws Exception {
        DSTransformerFactory factory = factories.get(conf.getType());
        if (factory == null) {
            throw new NullPointerException(String.format(
                    Locale.ROOT, "A factory with the ID '%s' was not available. Supported factories are %s",
                    conf.getType(), getSupportedTransformerIDs()));
        }
        log.debug("Creating a {} transformer", factory.getTransformerID());
        return factory.createTransformer(conf);
    }

    /**
     * @return the IDs for the {@link DSTransformer}s that can be created.
     */
    public static Set<String> getSupportedTransformerIDs() {
        return factories.keySet();
    }

    /**
     * Build a map of transformer factories from the classpath.
     *
     * @return map of [transformerID, transformer].
     */
    private static Map<String, DSTransformerFactory> getTransformerFactories() {
        return ServiceLoader.load(DSTransformerFactory.class).stream()
                .map(ServiceLoader.Provider::get)
                .peek(factory -> log.info("Discovered {} factory", factory.getTransformerID()))
                .collect(Collectors.toMap(DSTransformerFactory::getTransformerID, factory -> factory));
    }
}
