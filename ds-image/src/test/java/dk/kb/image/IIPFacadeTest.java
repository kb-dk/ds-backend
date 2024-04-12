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

import static org.junit.jupiter.api.Assertions.assertEquals;

class IIPFacadeTest {

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
     public void iipImageTemplate() {
         final List<String> EXPECTED = List.of(
                 "http://example.com/iipsrv/iipsrv.fcgi?FIF=foo.jpg&CVT=png",
                 "http://example.com/iipsrv/iipsrv.fcgi?FIF=foo.jpg&QLT=75&CNT=1.2&ROT=90&GAM=2.2&CMP=COLD&PFL=5%3A10%2C10-20%2C20&CTW=%5B0.1%2C0.2%2C0.3%3B0.4%2C0.5%2C0.6%3B0.7%2C0.8%2C0.9%5D&INV&COL=grey&JTL=%5B1%2C%202%5D&CVT=jpeg",
                 "http://example.com/iipsrv/iipsrv.fcgi?FIF=foo.jpg&QLT=75&CNT=1.2&ROT=90&GAM=2.2&CMP=COLD&PFL=5%3A10%2C10-20%2C20&CTW=%5B0.1%2C0.2%2C0.3%3B0.4%2C0.5%2C0.6%3B0.7%2C0.8%2C0.9%5D&INV&COL=grey&PTL=%5B1%2C%202%5D&CVT=jpeg",
                 "http://example.com/iipsrv/iipsrv.fcgi?FIF=foo.jpg&WID=640&HEI=480&RGN=%5B0.1%2C%200.2%2C%200.3%2C%200.4%5D&QLT=75&CNT=1.2&ROT=90&GAM=2.2&CMP=COLD&PFL=5%3A10%2C10-20%2C20&CTW=%5B0.1%2C0.2%2C0.3%3B0.4%2C0.5%2C0.6%3B0.7%2C0.8%2C0.9%5D&INV&COL=grey&CVT=jpeg"
         );

         try (ConfigAdjuster ignored = new ConfigAdjuster("image_server_param.yaml")) {
             List<URI> requestedURIs = ProxyHelperTest.collectProxyURIs(() -> {
                 IIPFacade.getInstance().getIIPImage( // Minimal call
                         SOURCE, "foo.jpg", null, null, null, null,
                         null, null, null, null, null,
                         null,null,null,null,null,"png", null);
                 IIPFacade.getInstance().getIIPImage( // JTL-request
                         SOURCE, "foo.jpg", null, null, null, 75,
                         1.2f, "90", 2.2F, "COLD", "5:10,10-20,20",
                         "[0.1,0.2,0.3;0.4,0.5,0.6;0.7,0.8,0.9]", true, "grey",
                         List.of(1, 2), null, null, null);
                 IIPFacade.getInstance().getIIPImage( // JTL-request
                         SOURCE, "foo.jpg", null, null, null, 75,
                         1.2f, "90", 2.2F, "COLD", "5:10,10-20,20",
                         "[0.1,0.2,0.3;0.4,0.5,0.6;0.7,0.8,0.9]", true, "grey",
                         null, List.of(1, 2), null, null);
                 IIPFacade.getInstance().getIIPImage( // Full CVT-request
                         SOURCE, "foo.jpg", 640L, 480L, List.of(0.1f, 0.2f, 0.3f, 0.4f), 75,
                         1.2f, "90", 2.2F, "COLD", "5:10,10-20,20",
                         "[0.1,0.2,0.3;0.4,0.5,0.6;0.7,0.8,0.9]", true, "grey",
                         null, null, "jpeg",null);
             });
             assertEquals(EXPECTED.toString(), requestedURIs.toString());
         }
     }

    @Tag("fast")
    @Test
    public void dziPathTemplate() {
        final List<String> IDS = List.of(
                "bar.jpg.dzi",
                "foo/bar.png.tif");
        final List<String> EXPECTED = List.of(
                "http://example.com/bar.jpg.dzi",
                "http://example.com/foo/bar.png.tif.dzi");

        try (ConfigAdjuster ignored = new ConfigAdjuster("image_server_path.yaml")) {
            List<URI> requestedURIs = ProxyHelperTest.collectProxyURIs(() -> {
                for (String id : IDS) {
                    IIPFacade.getInstance().getDeepzoomDZI(SOURCE, id, null, null);
                }
            });
            assertEquals(EXPECTED.toString(), requestedURIs.toString());
        }
    }

    @Tag("fast")
    @Test
    public void dziPatternTemplate() {
        final List<String> IDS = List.of(
                "bar.jpg.dzi",
                "foo/bar.png.tif");
        final List<String> EXPECTED = List.of(
                "http://example.com/iipsrv/iipsrv.fcgi?DeepZoom=bar.jpg.dzi",
                "http://example.com/iipsrv/iipsrv.fcgi?DeepZoom=foo/bar.png.tif.dzi");

        try (ConfigAdjuster ignored = new ConfigAdjuster("image_server_param.yaml")) {
            List<URI> requestedURIs = ProxyHelperTest.collectProxyURIs(() -> {
                for (String id : IDS) {
                    IIPFacade.getInstance().getDeepzoomDZI(SOURCE, id, null, null);
                }
            });
            assertEquals(EXPECTED.toString(), requestedURIs.toString());
        }
    }

    @Tag("fast")
    @Test
    public void tileDeepZoomPathTemplate() {
        final List<String> EXPECTED = List.of(
                "http://example.com/foo.jpg_files/11/2_4.jpg",
                "http://example.com/bar/foo.jpg_files/11/2_4.jpg?GAM=1.2&INV",
                "http://example.com/bar/foo.jpg_files/11/2_4.jpg?INV",
                "http://example.com/bar/foo.jpg_files/11/2_4.jpg?CNT=1.0?GAM=1.1?CMP=COLD?CTW=%5B0.1%2C0.2%2C0.3%3B0.4%2C0.5%2C0.6%3B0.7%2C0.8%2C0.9%5D&INV?COL=grey"
        );

        try (ConfigAdjuster ignored = new ConfigAdjuster("image_server_path.yaml")) {
            List<URI> requestedURIs = ProxyHelperTest.collectProxyURIs(() -> {
                IIPFacade.getInstance().getDeepzoomTile(
                        SOURCE, "foo.jpg", 11, "2_4", "jpg", null, null, null, null, null, null, null);
                IIPFacade.getInstance().getDeepzoomTile(
                        SOURCE, "bar/foo.jpg", 11, "2_4", "jpg", null, 1.2f, null, null, true, null, null);
                IIPFacade.getInstance().getDeepzoomTile(
                        SOURCE, "bar/foo.jpg", 11, "2_4", "jpg", null, null, null, null, true, null, null);
                IIPFacade.getInstance().getDeepzoomTile(
                        SOURCE, "bar/foo.jpg", 11, "2_4", "jpg", 1.0f, 1.1f, "COLD", "[0.1,0.2,0.3;0.4,0.5,0.6;0.7,0.8,0.9]", true, "grey", null);
            });
            assertEquals(EXPECTED.toString(), requestedURIs.toString());
        }
    }

    @Tag("fast")
    @Test
    public void tileDeepZoomParamTemplate() {
        final List<String> EXPECTED = List.of(
                "http://example.com/iipsrv/iipsrv.fcgi?DeepZoom=foo.jpg_files/11/2_4.jpg",
                "http://example.com/iipsrv/iipsrv.fcgi?DeepZoom=bar/foo.jpg_files/11/2_4.jpg&GAM=1.2&INV",
                "http://example.com/iipsrv/iipsrv.fcgi?DeepZoom=bar/foo.jpg_files/11/2_4.jpg&INV",
                "http://example.com/iipsrv/iipsrv.fcgi?DeepZoom=bar/foo.jpg_files/11/2_4.jpg&CNT=1.0&GAM=1.1&CMP=COLD&CTW=%5B0.1%2C0.2%2C0.3%3B0.4%2C0.5%2C0.6%3B0.7%2C0.8%2C0.9%5D&INV&COL=grey"
        );

        try (ConfigAdjuster ignored = new ConfigAdjuster("image_server_param.yaml")) {
            List<URI> requestedURIs = ProxyHelperTest.collectProxyURIs(() -> {
                IIPFacade.getInstance().getDeepzoomTile(
                        SOURCE, "foo.jpg", 11, "2_4", "jpg", null, null, null, null, null, null, null);
                IIPFacade.getInstance().getDeepzoomTile(
                        SOURCE, "bar/foo.jpg", 11, "2_4", "jpg", null, 1.2f, null, null, true, null, null);
                IIPFacade.getInstance().getDeepzoomTile(
                        SOURCE, "bar/foo.jpg", 11, "2_4", "jpg", null, null, null, null, true, null, null);
                IIPFacade.getInstance().getDeepzoomTile(
                        SOURCE, "bar/foo.jpg", 11, "2_4", "jpg", 1.0f, 1.1f, "COLD", "[0.1,0.2,0.3;0.4,0.5,0.6;0.7,0.8,0.9]", true, "grey", null);
            });
            assertEquals(EXPECTED.toString(), requestedURIs.toString());
        }
    }

}