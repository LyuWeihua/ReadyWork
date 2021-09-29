/**
 *
 * Original work copyright (c) 2016 Network New Technologies Inc.
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

package work.ready.core.component.decrypt;

import work.ready.core.server.Constant;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.KeySpec;

import static work.ready.core.component.decrypt.AESEncryptor.CONFIG_PASSWORD_PROPERTY;
import static work.ready.core.component.decrypt.AESEncryptor.getProperty;
import static work.ready.core.tools.HttpUtil.decodeBase64;

public class AESDecryptor implements Decryptor {
    private static final int ITERATIONS = 65536;

    private static final String STRING_ENCODING = Constant.DEFAULT_ENCODING;

    private static final int KEY_SIZE = 128;

    private static final byte[] SALT = { (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0 };

    private SecretKeySpec secret;

    private Cipher cipher;

    public AESDecryptor() {
        try {
            
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec;

            spec = new PBEKeySpec(getPassword(), SALT, ITERATIONS, KEY_SIZE);
            SecretKey tmp = factory.generateSecret(spec);
            secret = new SecretKeySpec(tmp.getEncoded(), "AES");

            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize " + this.getClass().getName(), e);
        }
    }

    @Override
    public String decrypt(String input) {
        if (!input.startsWith(CRYPT_PREFIX)) {
            throw new RuntimeException("Unable to decrypt, input string does not start with 'CRYPT'.");
        }

        try {
            String encodedValue = input.substring(6, input.length());
            byte[] data = decodeBase64(encodedValue);
            int keylen = KEY_SIZE / 8;
            byte[] iv = new byte[keylen];
            System.arraycopy(data, 0, iv, 0, keylen);
            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
            return new String(cipher.doFinal(data, keylen, data.length - keylen), STRING_ENCODING);
        } catch (Exception e) {
            throw new RuntimeException("Unable to decrypt because the decrypted password is incorrect.", e);
        }
    }

    protected char[] getPassword() {
        String passwordStr = getProperty(CONFIG_PASSWORD_PROPERTY, null);
        if (passwordStr == null || passwordStr.trim().equals("")) {
            passwordStr = Constant.FRAMEWORK_NAME;
        }
        return passwordStr.toCharArray();
    }

}
