package dk.kb.image;

import com.damnhandy.uri.template.UriTemplate;
import dk.kb.image.config.ServiceConfig;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import javax.ws.rs.core.StreamingOutput;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

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
class IIPFacadeTest {

    private final static URI SOURCE;
    static {
        try {
            SOURCE = new URI("http://notusedforthisunittest.kb.dk/");
        } catch (URISyntaxException e) {
            throw new RuntimeException("construction of mock source URI failed", e);
        }
    }

    @Test
     public void iipImageTemplate() {
         final List<String> EXPECTED = List.of(
                 "http://example.com/iipsrv/iipsrv.fcgi?FIF=foo.jpg&CVT=png",
                 "http://example.com/iipsrv/iipsrv.fcgi?FIF=foo.jpg&QLT=75&CNT=1.2&ROT=90&GAM=2.2&CMP=COLD&PFL=5%3A10%2C10-20%2C20&CTW=%5B0.1%2C0.2%2C0.3%3B0.4%2C0.5%2C0.6%3B0.7%2C0.8%2C0.9%5D&INV&COL=grey&JTL=%5B1%2C%202%5D&CVT=jpeg",
                 "http://example.com/iipsrv/iipsrv.fcgi?FIF=foo.jpg&QLT=75&CNT=1.2&ROT=90&GAM=2.2&CMP=COLD&PFL=5%3A10%2C10-20%2C20&CTW=%5B0.1%2C0.2%2C0.3%3B0.4%2C0.5%2C0.6%3B0.7%2C0.8%2C0.9%5D&INV&COL=grey&PTL=%5B1%2C%202%5D&CVT=jpeg",
                 "http://example.com/iipsrv/iipsrv.fcgi?FIF=foo.jpg&WID=640&HEI=480&RGN=%5B0.1%2C%200.2%2C%200.3%2C%200.4%5D&QLT=75&CNT=1.2&ROT=90&GAM=2.2&CMP=COLD&PFL=5%3A10%2C10-20%2C20&CTW=%5B0.1%2C0.2%2C0.3%3B0.4%2C0.5%2C0.6%3B0.7%2C0.8%2C0.9%5D&INV&COL=grey&CVT=jpeg"
         );

         try (ConfigAdjuster ignored = new ConfigAdjuster("image_server_param.yaml")) {
             List<URI> requestedURIs = collectProxyURIs(() -> {
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

    @Test
    public void dziPathTemplate() {
        final List<String> IDS = List.of(
                "bar.jpg.dzi",
                "foo/bar.png.tif");
        final List<String> EXPECTED = List.of(
                "http://example.com/bar.jpg.dzi",
                "http://example.com/foo/bar.png.tif.dzi");

        try (ConfigAdjuster ignored = new ConfigAdjuster("image_server_path.yaml")) {
            List<URI> requestedURIs = collectProxyURIs(() -> {
                for (String id : IDS) {
                    IIPFacade.getInstance().getDeepzoomDZI(SOURCE, id, null, null);
                }
            });
            assertEquals(EXPECTED.toString(), requestedURIs.toString());
        }
    }

    @Test
    public void dziPatternTemplate() {
        final List<String> IDS = List.of(
                "bar.jpg.dzi",
                "foo/bar.png.tif");
        final List<String> EXPECTED = List.of(
                "http://example.com/iipsrv/iipsrv.fcgi?DeepZoom=bar.jpg.dzi",
                "http://example.com/iipsrv/iipsrv.fcgi?DeepZoom=foo/bar.png.tif.dzi");

        try (ConfigAdjuster ignored = new ConfigAdjuster("image_server_param.yaml")) {
            List<URI> requestedURIs = collectProxyURIs(() -> {
                for (String id : IDS) {
                    IIPFacade.getInstance().getDeepzoomDZI(SOURCE, id, null, null);
                }
            });
            assertEquals(EXPECTED.toString(), requestedURIs.toString());
        }
    }

    @Test
    public void tileDeepZoomPathTemplate() {
        final List<String> EXPECTED = List.of(
                "http://example.com/foo.jpg_files/11/2_4.jpg",
                "http://example.com/bar/foo.jpg_files/11/2_4.jpg?GAM=1.2&INV",
                "http://example.com/bar/foo.jpg_files/11/2_4.jpg?INV",
                "http://example.com/bar/foo.jpg_files/11/2_4.jpg?CNT=1.0?GAM=1.1?CMP=COLD?CTW=%5B0.1%2C0.2%2C0.3%3B0.4%2C0.5%2C0.6%3B0.7%2C0.8%2C0.9%5D&INV?COL=grey"
        );

        try (ConfigAdjuster ignored = new ConfigAdjuster("image_server_path.yaml")) {
            List<URI> requestedURIs = collectProxyURIs(() -> {
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

    @Test
    public void tileDeepZoomParamTemplate() {
        final List<String> EXPECTED = List.of(
                "http://example.com/iipsrv/iipsrv.fcgi?DeepZoom=foo.jpg/11/2_4.jpg",
                "http://example.com/iipsrv/iipsrv.fcgi?DeepZoom=bar/foo.jpg/11/2_4.jpg&GAM=1.2&INV",
                "http://example.com/iipsrv/iipsrv.fcgi?DeepZoom=bar/foo.jpg/11/2_4.jpg&INV",
                "http://example.com/iipsrv/iipsrv.fcgi?DeepZoom=bar/foo.jpg/11/2_4.jpg&CNT=1.0&GAM=1.1&CMP=COLD&CTW=%5B0.1%2C0.2%2C0.3%3B0.4%2C0.5%2C0.6%3B0.7%2C0.8%2C0.9%5D&INV&COL=grey"
        );

        try (ConfigAdjuster ignored = new ConfigAdjuster("image_server_param.yaml")) {
            List<URI> requestedURIs = collectProxyURIs(() -> {
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

    /**
     * Mocks {@link ProxyHelper#proxy} to collect all given URIs and write the byte 87 as the answer.
     * @param runnable action that triggers one or more calls to the proxy method.
     * @return a list of the URIs that were supposed to be proxied.
     */
    private List<URI> collectProxyURIs(Runnable runnable) {
        // https://www.baeldung.com/mockito-mock-static-methods
        List<URI> requestedURIs = new ArrayList<>();
        try (MockedStatic<ProxyHelper> mockProxy = Mockito.mockStatic(ProxyHelper.class)) {
            // Mock the three overloads to the proxy method
            mockProxy.when(() -> ProxyHelper.proxy(anyString(), any(URI.class), any(URI.class),
                            any(), any()))
                    .thenAnswer((Answer<StreamingOutput>) invocation -> {
                        requestedURIs.add(invocation.getArgument(1)); // The URI to proxy
                        return writer -> writer.write(87);
                    });
            mockProxy.when(() -> ProxyHelper.proxy(anyString(), any(URI.class), any(URI.class), any()))
                    .thenAnswer((Answer<StreamingOutput>) invocation -> {
                        requestedURIs.add(invocation.getArgument(1)); // The URI to proxy
                        return writer -> writer.write(87);
                    });
            mockProxy.when(() -> ProxyHelper.proxy(anyString(), any(String.class), any(URI.class), any()))
                    .thenAnswer((Answer<StreamingOutput>) invocation -> {
                        URI uri;
                        try {
                            uri = new URI(invocation.getArgument(1));  // The URI to proxy
                        } catch (URISyntaxException e) {
                            throw new RuntimeException(
                                    "Exception converting '" + invocation.getArgument(1) + "' to URI", e);
                        }
                        requestedURIs.add(uri);
                        return writer -> writer.write(87);
                    });

            // Mockito.mockStatic(ProxyHelper.class, Answers.CALLS_REAL_METHODS) calls the original methods
            // AND the mocked version, resulting in a lot of exceptions.
            // To avoid this all non-mocked methods muct be mocked to call the real methods.
            mockProxy.when(() -> ProxyHelper.addIfPresent(any(URIBuilder.class), anyString(), any()))
                    .thenCallRealMethod();
            mockProxy.when(() -> ProxyHelper.addIfPresent(any(UriTemplate.class), anyString(), any(List.class)))
                    .thenCallRealMethod();
            mockProxy.when(() -> ProxyHelper.addIfPresent(any(UriTemplate.class), anyString(), any()))
                    .thenCallRealMethod();

            // Run the proxy-using code
            runnable.run();
        }
        return requestedURIs;
    }

    /**
     * Helper class for temporarily changing the application config.
     * Use the auto-closing try-catch mechanism around the test code using the temporary config:
     * <pre>
     *
     * </pre>
     */
    public static class ConfigAdjuster implements Closeable {
        private static ServiceConfig oldConfig;

        public ConfigAdjuster(String temporaryConfigSource) {
            try {
                oldConfig = ServiceConfig.getInstance();
                ServiceConfig tempConf = new ServiceConfig();
                tempConf.initialize(temporaryConfigSource);
                ServiceConfig.setInstance(tempConf);
            } catch (IOException e) {
                throw new RuntimeException("Exception creating temporary ServiceConfig", e);
            }
        }

        @Override
        public void close() {
            ServiceConfig.setInstance(oldConfig);
        }
    }
}