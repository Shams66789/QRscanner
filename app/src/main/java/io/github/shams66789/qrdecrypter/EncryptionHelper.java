package io.github.shams66789.qrdecrypter;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionHelper {

//    private static final String ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5Padding";
//    private static final String SECRET_KEY_ALGORITHM = "AES";
//    private static final String SECRET_KEY = "YourSecretKey123"; // Replace with your actual secret key
//    private static final String IV = "YourInitializationVector"; // Replace with your actual IV
//
//    public static String encrypt(String data) throws Exception {
//        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
//        SecretKey secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), SECRET_KEY_ALGORITHM);
//        IvParameterSpec ivParameterSpec = new IvParameterSpec(IV.getBytes());
//        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
//        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
//        return Base64.encodeBase64String(encryptedBytes);
//    }
//
//    public static String decrypt(String encryptedData) throws Exception {
//        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
//        SecretKey secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), SECRET_KEY_ALGORITHM);
//        IvParameterSpec ivParameterSpec = new IvParameterSpec(IV.getBytes());
//        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
//        byte[] decodedBytes = Base64.decodeBase64(encryptedData);
//        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
//        return new String(decryptedBytes);
//    }
//}

    private static final String KEY_ALIAS = "MyKeyAlias";

    public static String encryptData(String data) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            keyGenerator.init(new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());

            SecretKey key = keyGenerator.generateKey();

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] iv = cipher.getIV();
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to encrypted data
            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

            return Base64.encodeToString(combined, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decryptData(String encryptedData) {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            SecretKey key = (SecretKey) keyStore.getKey(KEY_ALIAS, null);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");

            byte[] combined = Base64.decode(encryptedData, Base64.DEFAULT);

            // Extract IV and encrypted data
            byte[] iv = Arrays.copyOfRange(combined, 0, 16);
            byte[] encryptedBytes = Arrays.copyOfRange(combined, 16, combined.length);

            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

