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
import work.ready.core.tools.HttpUtil;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;

import static java.lang.System.exit;

public class AESEncryptor {
    final static String CONFIG_PASSWORD_PROPERTY = "ready.config.decryptor_password";

    public static void main(String [] args) {
        if(args.length == 0) {
            System.out.println("Please provide plain text to encrypt!");
            exit(0);
        }
        AESEncryptor encryptor = new AESEncryptor();
        System.out.println(encryptor.encrypt(args[0]));
    }

    private static final int ITERATIONS = 65536;
    private static final int KEY_SIZE = 128;
    private static final byte[] SALT = { (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0 };
    private static final String STRING_ENCODING = "UTF-8";
    private SecretKeySpec secret;
    private Cipher cipher;

    public AESEncryptor() {
        try {
           
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec;

            spec = new PBEKeySpec(getPassword(), SALT, ITERATIONS, KEY_SIZE);
            SecretKey tmp = factory.generateSecret(spec);
            secret = new SecretKeySpec(tmp.getEncoded(), "AES");

            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize", e);
        }
    }

    public String encrypt(String input)
    {
        try
        {
            byte[] inputBytes = input.getBytes(STRING_ENCODING);

            cipher.init(Cipher.ENCRYPT_MODE, secret);
            AlgorithmParameters params = cipher.getParameters();
            byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
            byte[] ciphertext = cipher.doFinal(inputBytes);
            byte[] out = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ciphertext, 0, out, iv.length, ciphertext.length);
            return Decryptor.CRYPT_PREFIX + ":" + HttpUtil.base64(out);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException("Unable to encrypt", e);
        } catch (BadPaddingException e) {
            throw new RuntimeException("Unable to encrypt", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Unable to encrypt", e);
        } catch (InvalidParameterSpecException e) {
            throw new RuntimeException("Unable to encrypt", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to encrypt", e);
        }
    }

    protected char[] getPassword() {
        String passwordStr = getProperty(CONFIG_PASSWORD_PROPERTY, null);
        if (passwordStr == null || passwordStr.trim().equals("")) {
            passwordStr = Constant.FRAMEWORK_NAME;
        }
        return passwordStr.toCharArray();
    }

    static String getProperty(String property, String defaultValue) {
        String value = System.getProperty(property);
        if(value == null) value = System.getenv(property.replace('.','_'));
        return value == null ? defaultValue : value;
    }
}