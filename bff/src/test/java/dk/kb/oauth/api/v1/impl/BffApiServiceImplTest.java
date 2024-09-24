package dk.kb.oauth.api.v1.impl;

import dk.kb.oauth.config.ServiceConfig;
import dk.kb.util.Resolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BffApiServiceImplTest {

    @BeforeAll
    static void setup() {
        try {
            Path file = Path.of(Resolver.resolveURL("bff-test.yaml").getPath());
            ServiceConfig.getInstance().initialize(file.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testMessages() {
        BffApiServiceImpl service = new BffApiServiceImpl();
        Map<String, String> messages = service.getMessages();
        assertEquals(2, messages.size());
        assertEquals("text1",messages.get("msg1"));
        assertEquals("text2",messages.get("msg2"));
    }


}
