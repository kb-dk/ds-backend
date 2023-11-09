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

import dk.kb.present.copyright.XsltCopyrightMapper;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * The image rights extractor parses the rights section in MODS (packed in METS) input from the
 * Digital Preservation Service at the Royal Danish Library. It updates metadata but returns the given input unchanged.
 */
public class ImageRightsExtractor implements DSTransformer {
    private static final Logger log = LoggerFactory.getLogger(ImageRightsExtractor.class);
    public static final String ID = "imagerights";

    /**
     * Construct a transformer that returns its input unchanged.
     */
    public ImageRightsExtractor() {
        log.debug("Constructed " + this);
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String apply(String modsInMetsXml, Map<String, String> metadata) {
        try {
            metadata.putAll(XsltCopyrightMapper.applyXsltCopyrightTransformer(modsInMetsXml));
        } catch (Exception e) {
            log.warn("Unable to parse rights section from mods-in-mets XML with metadata {}", metadata);
            throw new InternalServiceException("Unable to parse rights section");
        }
        return modsInMetsXml;
    }

    @Override
    public String toString() {
        return "ImageRightsExtractor()";
    }

}
