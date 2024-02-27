package dk.kb.oauth;

import dk.kb.oauth.config.ServiceConfig;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class EncryptionHelper {

    private static final Logger log = LoggerFactory.getLogger(EncryptionHelper.class);


    public static String encryptString(String plain) {
        try {
            SecretKey key = generateKey(ServiceConfig.getConfig().getString("config.secretsalt"));
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(plain.getBytes());
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
            return new String(decryptedBytes);
        } catch (IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("decryption error ",e);
            throw new InternalServiceException("decryption error");
        }
    }

    private static SecretKey generateKey(String salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(salt.getBytes("UTF-8"));
        return new SecretKeySpec(keyBytes, "AES");
    }

}
