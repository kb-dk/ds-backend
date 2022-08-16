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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Fake test class where the only purpose is to demonstrate test principles.
 * This class is deleted when the template is applied as a concrete project.
 */
public class DemoTest {
    private static final Logger log = LoggerFactory.getLogger(DemoTest.class);

    @Tag("fast")
    @Test
    @DisplayName("Hello World log")
    public void mrHyde() {
        log.info("Hello World");
        assertEquals(87, 12 + 75);
    }

    @Tag("fast")
    @Test
    @DisplayName("Mockito demonstration")
    public void mockito() throws MalformedURLException, ClassNotFoundException {
        // Mockito makes it easy to create mocked versions of classes.
        // Basically it creates an empty shell from the class and lets the test-writer fill in the methods
        // used by the test. The class constructor is bypassed. The class that is mocked here is ClassLoader,
        // to demonstrate that it is possible to mock very complex classes
        ClassLoader mockedLoader = mock(ClassLoader.class);

        // Methods that are not explicitly mocked returns null
        assertNull(mockedLoader.getName());

        // We can assign a result to a method
        when(mockedLoader.getName()).thenReturn("Mockito ClassLoader");
        assertEquals("Mockito ClassLoader", mockedLoader.getName());

        // We can assign conditionals to methods that takes arguments
        when(mockedLoader.getResource(eq("example"))).thenReturn(new URL("http://example.org/"));
        when(mockedLoader.getResource(eq("KB"))).thenReturn(new URL("https://www.kb.dk/"));
        assertNull(mockedLoader.getResource("Undefined"));
        assertEquals(new URL("http://example.org/"), mockedLoader.getResource("example"));
        assertEquals(new URL("https://www.kb.dk/"), mockedLoader.getResource("KB"));

        // Arguments need not be defined
        when(mockedLoader.getResource(anyString())).thenReturn(new URL("http://statsbiblioteket.dk/"));
        assertEquals(new URL("http://statsbiblioteket.dk/"), mockedLoader.getResource("randomString"));

        // The result can rely on the input
        when(mockedLoader.getResource(anyString())).thenAnswer(
                input -> new URL("http://" + input.getArgument(0) + ".example.org/"));
        assertEquals(new URL("http://foo.example.org/"), mockedLoader.getResource("foo"));
    }

    /**
     * Long running unit tests should be annotated as "slow". They won't be evaluated during a standard package,
     * but will be by Jenkins or if requested: {@code mvn package -PallTests}.
     */
    @Tag("slow")
    @Test
    public void sleeper() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(true);
    }
}
