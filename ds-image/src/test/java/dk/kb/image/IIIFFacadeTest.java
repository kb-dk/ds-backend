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
package dk.kb.image;

import dk.kb.image.config.ConfigAdjuster;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IIIFFacadeTest {

    private final static URI SOURCE;
    static {
        try {
            SOURCE = new URI("http://notusedforthisunittest.kb.dk/");
        } catch (URISyntaxException e) {
            throw new RuntimeException("construction of mock source URI failed", e);
        }
    }

    @Tag("fast")
    @Test
    void iiifImage() {
        try (ConfigAdjuster ignored = new ConfigAdjuster("image_server_param.yaml")) {
            final List<String> EXPECTED = List.of(
                    "http://example.com/foo.jpg/full/max/0/color.jpg",
                    "http://example.com/foo.jpg/square/1200,/87.65/gray.tif",
                    "http://example.com/foo.jpg/1,2,3,4/!12,34/!90/bitonal.png",
                    // ^ is encoded although it is requested not to. Should not be a problem but it is strange
                    "http://example.com/foo.jpg/pct:10.5,20,30,40/%5E!56,78/!0.5/default.webp",
                    "http://example.com/bar%2Fspa%20ce.jpg/full/max/0/color.jpg"
            );
            List<URI> requestedURIs = ProxyHelperTest.collectProxyURIs(() -> {
                // https://iiif.io/api/image/3.0/

                IIIFFacade.getInstance().getIIIFImage(SOURCE, "foo.jpg", "full", "max", "0", "color", "jpg", null);
                IIIFFacade.getInstance().getIIIFImage(SOURCE, "foo.jpg", "square", "1200,", "87.65", "gray", "tif", null);
                IIIFFacade.getInstance().getIIIFImage(SOURCE, "foo.jpg", "1,2,3,4", "!12,34", "!90", "bitonal", "png", null);
                IIIFFacade.getInstance().getIIIFImage(SOURCE, "foo.jpg", "pct:10.5,20,30,40", "^!56,78", "!0.5", "default", "webp", null);
                IIIFFacade.getInstance().getIIIFImage(SOURCE, "bar/spa ce.jpg", "full", "max", "0", "color", "jpg", null);
            });
            assertEquals(EXPECTED.toString(), requestedURIs.toString());
        }
    }

    @Tag("fast")
    @Test
    void iiifInfo() {
        try (ConfigAdjuster ignored = new ConfigAdjuster("image_server_param.yaml")) {
            final List<String> EXPECTED = List.of(
                    "http://example.com/foo.jpg/info.json",
                    "http://example.com/bar%2Fspa%20ce.jpg/info.json"
            );
            List<URI> requestedURIs = ProxyHelperTest.collectProxyURIs(() -> {
                // https://iiif.io/api/presentation/3.0/

                IIIFFacade.getInstance().getIIIFInfo(SOURCE, "foo.jpg", "json", null);
                IIIFFacade.getInstance().getIIIFInfo(SOURCE, "bar/spa ce.jpg", "json", null);
            });
            assertEquals(EXPECTED.toString(), requestedURIs.toString());
        }

    }
}
