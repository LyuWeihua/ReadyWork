/**
 *
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
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

import work.ready.core.tools.StrUtil;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.security.NoSuchAlgorithmException;

import static work.ready.core.tools.HttpUtil.base64;
import static work.ready.core.tools.HttpUtil.decodeBase64;

public class AES {
    private static final String UTF8 = "UTF-8";
    private static final String ALGORITHM = "AES";

    private static final String ALGORITHM_CIPHER = "AES/ECB/PKCS5Padding";

    private static final int LIMIT_LEN = 16;

    public static byte[] generateKey(int keySize) {
        try {
            KeyGenerator generator = KeyGenerator.getInstance(ALGORITHM);
            generator.init(keySize);
            return generator.generateKey().getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }
    }

    public static SecretKey getSecretKey(String password) {
        byte[] passwordData = password.getBytes();
        if(passwordData.length > LIMIT_LEN) {
            throw new IllegalArgumentException("password length is limited within " + LIMIT_LEN);
        }

        byte[] keyData = new byte[16];
        System.arraycopy(passwordData, 0, keyData, 0, passwordData.length);

        return new SecretKeySpec(keyData, ALGORITHM);
    }

    public static byte[] encrypt(byte[] data, String password) throws Exception {
        SecretKey secretKey = getSecretKey(password);
        Cipher cipher = Cipher.getInstance(ALGORITHM_CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] data, String password) throws Exception {
        SecretKey secretKey = getSecretKey(password);
        Cipher cipher = Cipher.getInstance(ALGORITHM_CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    public static String encryptToBase64(String content, String password) throws Exception {
        byte[] data = content.getBytes(UTF8);
        byte[] result = encrypt(data, password);
        return base64(result);
    }

    public static String decryptFromBase64(String base64String, String password) throws Exception {
        byte[] data = decodeBase64(base64String);
        byte[] contentData = decrypt(data, password);
        return new String(contentData, UTF8);
    }

    public static String encryptToHex(String content, String password) throws Exception {
        byte[] data = content.getBytes(UTF8);
        byte[] result = encrypt(data, password);
        return StrUtil.toHexString(result);
    }

    public static String decryptFromHex(String hex, String password) throws Exception {
        byte[] data = StrUtil.fromHexString(hex);
        byte[] contentData = decrypt(data, password);
        return new String(contentData,UTF8);
    }

}
