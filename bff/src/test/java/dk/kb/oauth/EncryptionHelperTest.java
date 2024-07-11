package dk.kb.oauth;

import dk.kb.oauth.config.ServiceConfig;
import dk.kb.util.Resolver;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EncryptionHelperTest {

    @BeforeAll
    static void initConfig() throws IOException {
        Path knownFile = Path.of(Resolver.resolveURL("bff-test.yaml").getPath());
        ServiceConfig.getInstance().initialize(knownFile.toString());
    }

    @Test
    public void testEncryptDecrypt() throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException {
        String testString = "abcdefg12345678";
        String encryptedString =  EncryptionHelper.encryptString(testString);
        String decryptedString = EncryptionHelper.decryptString(encryptedString);
        assertEquals(testString,decryptedString);
    }

    @Test
    public void testSaltChange() throws IOException {
        String testString = "abcdefg12345678";
        String encryptedString =  EncryptionHelper.encryptString(testString);
        String decryptedString = EncryptionHelper.decryptString(encryptedString);
        assertEquals(testString,decryptedString);
        Path knownFile = Path.of(Resolver.resolveURL("bff-test-newsalt.yaml").getPath());
        ServiceConfig.getInstance().initialize(knownFile.toString());
        assertEquals(testString,decryptedString);
        assertThrows(InternalServiceException.class, () ->
            EncryptionHelper.decryptString(encryptedString));
    }

}
