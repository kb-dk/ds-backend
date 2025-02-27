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
package dk.kb.license.util;

import dk.kb.license.model.v1.CheckAccessForIdsInputDto;
import dk.kb.license.model.v1.CheckAccessForIdsOutputDto;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;

/**
 * Simple verification of client code generation.
 */
public class DsLicenseClientCacheTest {
    private static final Logger log = LoggerFactory.getLogger(DsLicenseClientCacheTest.class);

    // Reusable test requests & responses
    private CheckAccessForIdsInputDto request1 = new CheckAccessForIdsInputDto().accessIds(List.of("1", "one"));
    private CheckAccessForIdsInputDto request2 = new CheckAccessForIdsInputDto().accessIds(List.of("2", "two"));
    private CheckAccessForIdsInputDto resRequest1 = new CheckAccessForIdsInputDto().accessIds(List.of("r1", "rthree"));

    private CheckAccessForIdsOutputDto response1 = new CheckAccessForIdsOutputDto().query("1");
    private CheckAccessForIdsOutputDto response2 = new CheckAccessForIdsOutputDto().query("2");
    private CheckAccessForIdsOutputDto resResponse1 = new CheckAccessForIdsOutputDto().query("3");
    private CheckAccessForIdsOutputDto fail = new CheckAccessForIdsOutputDto().query("Should not be returned");

    // We cannot test usage as that would require a running instance of ds-license to connect to
    @Test
    public void testInstantiation() {
        String backendURIString = "htp://example.com/ds-license/v1";
        log.debug("Creating inactive client for ds-license with URI '{}'", backendURIString);
        new DsLicenseClient(backendURIString, 10, 5000);
    }
/*
    @Test
    public void testCaching() throws Exception {
        // Mock the DsLicenseClient
        DsLicenseClient clientSpy = Mockito.spy(
                new DsLicenseClient("http://localhost:9076/ds-license/v1", 10, 60000));
        doReturn(response1, fail).when(clientSpy).directCheckAccessForIds(eq(request1));
        doReturn(response2, fail).when(cdirectClientSpy).directCheckAccessForIds(eq(request2));
        doReturn(resResponse1, fail).when(clientSpy).directCheckAccessForResourceIds(eq(resRequest1));

        // Test basic caching
        assertEquals(response1, clientSpy.checkAccessForIds(request1),
                "First call with request 1 should return response1");
        assertEquals(response1, clientSpy.checkAccessForIds(request1),
                "Second call with request 1 should return the initial response1");

        assertEquals(response2, clientSpy.checkAccessForIds(request2),
                "First call with request 2 should return response2");
        assertEquals(response2, clientSpy.checkAccessForIds(request2),
                "Second call with request 2 should return the initial response2");

        assertEquals(resResponse1, clientSpy.checkAccessForResourceIds(resRequest1),
                "First call with resource request 1 should return resource response 1");
        assertEquals(resResponse1, clientSpy.checkAccessForResourceIds(resRequest1),
                "Second call with resource request 1 should return initial resource response 1");
    }

    @Test
    public void testCachingSize() throws Exception {
        // Mock the DsLicenseClient
        DsLicenseClient clientSpy = Mockito.spy(
                new DsLicenseClient("http://localhost:9076/ds-license/v1", 1, 60000));
        doReturn(response1, fail).when(clientSpy).directCheckAccessForIds(eq(request1));
        doReturn(response2, fail).when(clientSpy).directCheckAccessForIds(eq(request2));

        // Fill the cache beyond capacity
        assertEquals(response1, clientSpy.checkAccessForIds(request1),
                "First call with request 1 should return response1");
        assertEquals(response2, clientSpy.checkAccessForIds(request2),
                "First call with request 2 should return response2");

        // Actively force the cache to check constraints (max size + age). This should flush the oldest (request1)
        clientSpy.idcache.cleanUp();
        assertEquals(fail, clientSpy.checkAccessForIds(request1),
                "Fourth call with request 1 should miss the cache");
    }

    @Tag("slow")
    @Test
    public void testCachingTimeout() throws Exception, InterruptedException {
        // Mock the DsLicenseClient
        DsLicenseClient clientSpy = Mockito.spy(
                new DsLicenseClient("http://localhost:9076/ds-license/v1", 2, 1000));
        doReturn(response1, fail).when(clientSpy).directCheckAccessForIds(eq(request1));

        // Test basic caching
        assertEquals(response1, clientSpy.checkAccessForIds(request1),
                "First call with request 1 should return response1");
        assertEquals(response1, clientSpy.checkAccessForIds(request1),
                "Second call with request 1 should return the initial response1");

        // Actively force the cache to check constraints (max size + age). This should change nothing at this point
        clientSpy.idcache.cleanUp();

        assertEquals(response1, clientSpy.checkAccessForIds(request1),
                "Third call with request 1 should return the initial response1");

        // Sleep longer than the cache timeout period, then force a constraint check
        Thread.sleep(1001);
        clientSpy.idcache.cleanUp();
        assertEquals(fail, clientSpy.checkAccessForIds(request1),
                "Fourth call with request 1 should miss the cache");
    }
*/
}
