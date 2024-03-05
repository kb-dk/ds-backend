package dk.kb.oauth;

import dk.kb.oauth.config.ServiceConfig;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.*;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptionHelper {

    private static final Logger log = LoggerFactory.getLogger(EncryptionHelper.class);


    public static String encryptString(String plain) {
        try {
            SecretKey key = generateKey(ServiceConfig.getConfig().getString("config.secretsalt"));
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (UnsupportedEncodingException | NoSuchPaddingException | IllegalBlockSizeException |
                 NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
            log.error("encryption error ",e);
            throw new InternalServiceException("encryption error");
        }
    }

    public static String decryptString(String encryptedString){
        try {
            SecretKey key = generateKey(ServiceConfig.getConfig().getString("config.secretsalt"));
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedString);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes,StandardCharsets.UTF_8);
        } catch (IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("decryption error ",e);
            throw new InternalServiceException("decryption error");
        }
    }

    private static SecretKey generateKey(String salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(salt.getBytes(StandardCharsets.UTF_8));
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256,secureRandom);
        return keyGenerator.generateKey();
    }

}
