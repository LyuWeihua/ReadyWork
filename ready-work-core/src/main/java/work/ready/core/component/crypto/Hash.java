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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class Hash {
    private static final char[] HEX_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String md5Hex(String text) {
        return hash(text, "MD5");
    }

    public static String sha1Hex(String text) {
        return hash(text, "SHA1");
    }

    public static String sha256Hex(String text) {
        return hash(text, "SHA-256");
    }

    private static String hash(String text, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(text.getBytes(UTF_8));
            return hex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }
    }

    private static String hex(byte[] bytes) {
        char[] chars = new char[bytes.length << 1];
        int index = 0;
        for (byte b : bytes) {  
            chars[index++] = HEX_CHARS[(b >> 4) & 0xF];
            chars[index++] = HEX_CHARS[b & 0xF];
        }
        return new String(chars);
    }
}
