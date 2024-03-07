package dk.kb.oauth;

import dk.kb.oauth.config.ServiceConfig;
import dk.kb.util.Resolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EncryptionHelperTest {

    @BeforeAll
    static void initConfig() throws IOException {
        Path knownFile = Path.of(Resolver.resolveURL("bff-test.yaml").getPath());
        ServiceConfig.getInstance().initialize(knownFile.toString());
    }

    @Test
    public void testEncryptDecrypt() throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, UnsupportedEncodingException {
        String testString = "abcdefg12345678";
        String encryptedString =  EncryptionHelper.encryptString(testString);
        String decryptedString = EncryptionHelper.decryptString(encryptedString);
        assertEquals(testString,decryptedString);
    }

}
