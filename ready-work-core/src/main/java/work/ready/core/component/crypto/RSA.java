/**
 *
 * Original work Copyright core-ng
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package work.ready.core.component.crypto;

import work.ready.core.tools.HttpUtil;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSA {
    public static final String RSA_ALGORITHM = "RSA";
    public static final String UTF8 = "UTF-8";

    private PrivateKey privateKey;
    private PublicKey publicKey;

    public static KeyStore generateBase64KeyPair() {
        try {
            KeyPairGenerator keyPairGeno = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyPairGeno.initialize(2048);
            KeyPair keyPair = keyPairGeno.generateKeyPair();

            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

            KeyStore keyStore = new KeyStore();
            keyStore.setPublicKey(HttpUtil.base64(publicKey.getEncoded()));
            keyStore.setPrivateKey(HttpUtil.base64(privateKey.getEncoded()));
            return keyStore;
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            generator.initialize(2048);
            return generator.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }
    }

    public static RSAPublicKey getPublicKey(byte[] pubKeyData) throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pubKeyData);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    public static RSAPublicKey getPublicKey(String pubKey) throws Exception {
        return getPublicKey(HttpUtil.decodeBase64(pubKey));
    }

    public void setPublicKey(byte[] publicKeyValue) {
        try {
            var keySpec = new X509EncodedKeySpec(publicKeyValue);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            publicKey = keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new Error(e);
        }
    }

    public static RSAPrivateKey getPrivateKey(String priKey) throws Exception {
        return getPrivateKey(HttpUtil.decodeBase64(priKey));
    }

    public static RSAPrivateKey getPrivateKey(byte[] keyBytes) throws Exception {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    public void setPrivateKey(byte[] privateKeyValue) {
        try {
            var keySpec = new PKCS8EncodedKeySpec(privateKeyValue);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            privateKey = keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new Error(e);
        }
    }

    public byte[] decrypt(byte[] encryptedMessage) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(encryptedMessage);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
            throw new Error("failed to decrypt message, please check private key and message", e);
        } catch (InvalidKeyException e) {
            throw new Error(e);
        }
    }

    public byte[] encrypt(byte[] plainMessage) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(plainMessage);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            throw new Error(e);
        }
    }

    public static String encryptByPublicKey(String data, String publicKey) throws Exception {
        return encryptByPublicKey(data, getPublicKey(publicKey));
    }

    public static String encryptByPublicKey(String data, RSAPublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] bytes = cipher.doFinal(data.getBytes(UTF8));
        return HttpUtil.base64(bytes);
    }

    public static String decryptByPublicKey(String data, String rsaPublicKey) throws Exception {
        return decryptByPublicKey(data, getPublicKey(rsaPublicKey));
    }

    public static String decryptByPublicKey(String data, RSAPublicKey rsaPublicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, rsaPublicKey);
        byte[] inputData = HttpUtil.decodeBase64(data);
        byte[] bytes = cipher.doFinal(inputData);
        return new String(bytes, UTF8);
    }

    public static String encryptByPrivateKey(String data, String privateKey) throws Exception {
        return encryptByPrivateKey(data, getPrivateKey(privateKey));
    }

    public static String encryptByPrivateKey(String data, RSAPrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] bytes = cipher.doFinal(data.getBytes(UTF8));
        return HttpUtil.base64(bytes);
    }

    public static String decryptByPrivateKey(String data, String privateKey) throws Exception {
        return decryptByPrivateKey(data, getPrivateKey(privateKey));
    }

    public static String decryptByPrivateKey(String data, RSAPrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] inputData = HttpUtil.decodeBase64(data);
        byte[] bytes = cipher.doFinal(inputData);
        return new String(bytes, UTF8);
    }

    public static class KeyStore {
        private String publicKey;
        private String privateKey;

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }
    }
}
