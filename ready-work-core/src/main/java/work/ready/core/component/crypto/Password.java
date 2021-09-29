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

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

public final class Password {
    public static String encrypt(String plainText, String publicKey) {
        var rsa = new RSA();
        rsa.setPublicKey(PEM.fromPEM(publicKey));
        byte[] encryptedBytes = rsa.encrypt(plainText.getBytes(StandardCharsets.UTF_8));
        return HttpUtil.base64(encryptedBytes);
    }

    public static String decrypt(String encryptedText, String privateKey) {
        var rsa = new RSA();
        rsa.setPrivateKey(PEM.fromPEM(privateKey));
        byte[] encryptedBytes = HttpUtil.decodeBase64(encryptedText);
        byte[] plainText = rsa.decrypt(encryptedBytes);
        return new String(plainText, StandardCharsets.UTF_8);
    }

    public static String[] generateKeyPair() {
        KeyPair keyPair = RSA.generateKeyPair();
        String publicKey = PEM.toPEM("RSA PUBLIC KEY", keyPair.getPublic().getEncoded());
        String privateKey = PEM.toPEM("RSA PRIVATE KEY", keyPair.getPrivate().getEncoded());
        return new String[]{publicKey, privateKey};
    }
}
