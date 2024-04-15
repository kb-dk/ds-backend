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

import com.damnhandy.uri.template.UriTemplate;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Hacked unit test as it expects www.kb.dk to be available.
 */
class ProxyHelperTest {

    /**
     * Mocks {@link ProxyHelper#proxy} to collect all given URIs and write the byte 87 as the answer.
     * @param runnable action that triggers one or more calls to the proxy method.
     * @return a list of the URIs that were supposed to be proxied.
     */
    public static List<URI> collectProxyURIs(Runnable runnable) {
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
            mockProxy.when(() -> ProxyHelper.addIfPresent(any(UriTemplate.class), anyString(), any(List.class)))
                    .thenCallRealMethod();
            mockProxy.when(() -> ProxyHelper.addIfPresent(any(UriTemplate.class), anyString(), any()))
                    .thenCallRealMethod();

            // Run the proxy-using code
            runnable.run();
        }
        return requestedURIs;
    }

    @Test
    void anyBytes() throws IOException, URISyntaxException {
        try (ByteArrayOutputStream content = new ByteArrayOutputStream()) {
            ProxyHelper.proxy("KB", new URI("https://www.kb.dk/"), new URI("http://example.com/irrelevantfortesting"),null).write(content);
            assertTrue(content.size() >= 1, "The resulting content should have at least 1 byte");
        }
    }

    @Test
    void contentType() throws IOException, URISyntaxException {
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);

        try (ByteArrayOutputStream content = new ByteArrayOutputStream()) {
            ProxyHelper.proxy("KB",
                              new URI("https://www.kb.dk/"),
                              new URI("http://example.com/irrelevantfortesting"),
                              servletResponse,null).
                    write(content);
            assertTrue(content.size() >= 1, "The resulting content should have at least 1 byte");
        }

        // We know the response from www.kb.dk starts with text/html
        verify(servletResponse).setContentType(ArgumentMatchers.startsWith("text/html"));
    }
}