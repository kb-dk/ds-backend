/**
 * The transform package is used for transforming a textual input to a textual output.
 * Common use is to transform an XML input to a XML or JSON output, e.g. to deliver JSON-LD from a MODS source.
 *
 * The transform package uses a ServiceLoader to discover and control transformers:
 * New transformers are added by creating an {@link dk.kb.present.transform.DSTransformer} with a corresponding
 * {@link dk.kb.present.transform.DSTransformerFactory} and registering the factory in the file
 * {@code src/main/resources/META-INF/services/dk.kb.present.transform.DSTransformerFactory}.
 *
 * Transformer instances are created by {@link dk.kb.present.transform.TransformerController}.
 */
package dk.kb.present.transform;