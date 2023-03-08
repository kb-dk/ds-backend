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
package dk.kb.license;

import dk.kb.license.client.v1.DsLicenseApi;
import dk.kb.license.invoker.v1.ApiClient;
import dk.kb.license.invoker.v1.Configuration;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * How to use the ds-license client demonstration (and instantiation unit test).
 *
 * When used from another project, add pomx.ml dependency with
 * <pre>
 * <dependency>
 *     <groupId>dk.kb.license</groupId>
 *     <artifactId>ds-license</artifactId>
 *     <version>1.0-SNAPSHOT</version>
 *     <type>jar</type>
 *     <classifier>classes</classifier>
 *     <exclusions>
 *         <exclusion>
 *             <groupId>*</groupId>
 *             <artifactId>*</artifactId>
 *         </exclusion>
 *     </exclusions>
 * </dependency>
 * </pre>
 * During development, a SNAPSHOT dependency can be installed locally by running
 * {@code mvn install} in the {@code ds-license} checkout.
 */
public class ClientInstantiationTest {
    private static final Logger log = LoggerFactory.getLogger(ClientInstantiationTest.class);

    // We cannot test usage as that would require a running instance of ds-license to connect to
    @Test
    public void testInstantiation() {
        String backendURIString = "htp://example.com/ds-license/v1";
        log.debug("Creating inactive client for ds-license with URI '{}'", backendURIString);

        URI serviceURI = URI.create(backendURIString);

        // No mechanism for just providing the full URI. We have to deconstruct it
        ApiClient client = Configuration.getDefaultApiClient();
        client.setScheme(serviceURI.getScheme());
        client.setHost(serviceURI.getHost());
        client.setPort(serviceURI.getPort());
        client.setBasePath(serviceURI.getRawPath());

        // Note that the import is dk.kb.license.client.v1.DsLicenseApi
        new DsLicenseApi(client);
    }
}
