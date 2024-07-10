package dk.kb.oauth;

import dk.kb.oauth.config.ServiceConfig;
import dk.kb.util.Resolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import java.io.IOException;
import java.nio.file.Path;


import static org.junit.jupiter.api.Assertions.assertEquals;

public class EncryptionHelperTest {

    @BeforeAll
    static void initConfig() throws IOException {
        Path knownFile = Path.of(Resolver.resolveURL("bff-test.yaml").getPath());
        ServiceConfig.getInstance().initialize(knownFile.toString());
    }

    @Test
    public void testEncryptDecrypt()  {
        String testString = "abcdefg12345678";
        String encryptedString =  EncryptionHelper.encryptString(testString);
        String decryptedString = EncryptionHelper.decryptString(encryptedString);
        assertEquals(testString,decryptedString);
    }

}
