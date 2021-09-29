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
package work.ready.cloud.client.oauth;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

public class CodeVerifierUtil {

    private static final Log logger = LogFactory.getLog(CodeVerifierUtil.class);

    public static final String CODE_CHALLENGE_METHOD_S256 = "S256";

    public static final String CODE_CHALLENGE_METHOD_PLAIN = "plain";

    public static final int MIN_CODE_VERIFIER_LENGTH = 43;

    public static final int MAX_CODE_VERIFIER_LENGTH = 128;

    public static final int DEFAULT_CODE_VERIFIER_ENTROPY = 64;

    public static final int MIN_CODE_VERIFIER_ENTROPY = 32;

    public static final int MAX_CODE_VERIFIER_ENTROPY = 96;

    public static final Pattern VALID_CODE_CHALLENGE_PATTERN = Pattern.compile("^[0-9a-zA-Z\\-\\.~_]+$");

    public static String generateRandomCodeVerifier() {
        return generateRandomCodeVerifier(new SecureRandom(), DEFAULT_CODE_VERIFIER_ENTROPY);
    }

    public static String generateRandomCodeVerifier(SecureRandom entropySource, int entropyBytes) {
        byte[] randomBytes = new byte[entropyBytes];
        entropySource.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public static String deriveCodeVerifierChallenge(String codeVerifier) {
        try {
            MessageDigest sha256Digester = MessageDigest.getInstance("SHA-256");
            sha256Digester.update(codeVerifier.getBytes("ISO_8859_1"));
            byte[] digestBytes = sha256Digester.digest();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digestBytes);
        } catch (NoSuchAlgorithmException e) {
            logger.warn(e,"SHA-256 is not supported on this device! Using plain challenge");
            return codeVerifier;
        } catch (UnsupportedEncodingException e) {
            logger.error(e,"ISO-8859-1 encoding not supported on this device!");
            throw new IllegalStateException("ISO-8859-1 encoding not supported", e);
        }
    }

    public static String getCodeVerifierChallengeMethod() {
        try {
            MessageDigest.getInstance("SHA-256");
            
            return CODE_CHALLENGE_METHOD_S256;
        } catch (NoSuchAlgorithmException e) {
            return CODE_CHALLENGE_METHOD_PLAIN;
        }
    }

}
