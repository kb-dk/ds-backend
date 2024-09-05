package dk.kb.oauth;

import dk.kb.oauth.config.ServiceConfig;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.Response;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class EncryptionHelper {
    //TODO: should these be moved to config
    private static final int AES_KEY_SIZE = 256;
    private static final int INITIALIZATION_VECTOR_LENGTH = 16;
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final String FACTORY_INSTANCE = "PBKDF2WithHmacSHA512";

    private static final Logger log = LoggerFactory.getLogger(EncryptionHelper.class);

    private static final SecretKeyFactory keyFactory;

    static {
        try {
            keyFactory = SecretKeyFactory.getInstance(FACTORY_INSTANCE);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Encrypt a string using the configured secret salt
     *
     * @param plain text to be encrypted (i.e. the value of the BFF cookie)
     * @return The encrypted string base64 encoded
     */
    public static String encryptString(String plain) {
        try {
            SecretKey key = generateKey(ServiceConfig.getConfig().getString("secretSalt"));
            byte[] iv = getRandomBytes(INITIALIZATION_VECTOR_LENGTH);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(INITIALIZATION_VECTOR_LENGTH * 8, iv));
            byte[] encryptedBytes = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] cipherBytes = ByteBuffer.allocate(iv.length+encryptedBytes.length)
                .put(iv).put(encryptedBytes).array();
            return Base64.getEncoder().encodeToString(cipherBytes);
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException |
                 InvalidKeyException | InvalidKeySpecException | InvalidAlgorithmParameterException e) {
            log.error("encryption error ",e);
            throw new InternalServiceException("encryption error");
        }
    }

    /**
     * Decrypts an encrypted string using the same encryption method as encrypt
     *
     * @param encryptedString base64 encoded encrypted String the string to decrypt
     * @return the decrypted string.
     */
    public static String decryptString(String encryptedString){
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedString);
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedBytes);
            byte[] iv = new byte[INITIALIZATION_VECTOR_LENGTH];
            byteBuffer.get(iv);
            byte[] content = new byte[byteBuffer.remaining()];
            byteBuffer.get(content);

            SecretKey key = generateKey(ServiceConfig.getConfig().getString("secretSalt"));
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(INITIALIZATION_VECTOR_LENGTH * 8, iv));
            byte[] decryptedBytes = cipher.doFinal(content);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            log.debug("Bad tag (invalid salt)");
            throw new ServiceException("Invalid salt", Response.Status.UNAUTHORIZED);
        } catch (InvalidAlgorithmParameterException  |InvalidKeySpecException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("decryption error ",e);
            throw new InternalServiceException("decryption error");
        }
    }

    private static SecretKey generateKey(String secretSalt) throws InvalidKeySpecException, NoSuchAlgorithmException {
        PBEKeySpec pbeKeySpec = new PBEKeySpec(secretSalt.toCharArray(), secretSalt.getBytes(StandardCharsets.UTF_8), 65536, AES_KEY_SIZE);
        SecretKey pbeKey = keyFactory.generateSecret(pbeKeySpec);
        return new SecretKeySpec(pbeKey.getEncoded(), KEY_ALGORITHM);
    }

    private static byte[] getRandomBytes(int lengh) {
        byte[] bytes = new byte[lengh];
        SecureRandom random = new SecureRandom();
        random.nextBytes(bytes);
        return bytes;
    }

}
