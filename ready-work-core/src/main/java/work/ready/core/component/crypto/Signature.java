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

import java.io.ByteArrayInputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public final class Signature {
    private static final String ALGORITHM_SHA256_WITH_RSA = "SHA256withRSA";
    private PublicKey publicKey;
    private PrivateKey privateKey;

    public boolean verify(byte[] message, byte[] signatureValue) {
        try {
            java.security.Signature signature = java.security.Signature.getInstance(ALGORITHM_SHA256_WITH_RSA);
            signature.initVerify(publicKey);
            signature.update(message);
            return signature.verify(signatureValue);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new Error(e);
        }
    }

    public byte[] sign(byte[] message) {
        try {
            java.security.Signature signature = java.security.Signature.getInstance(ALGORITHM_SHA256_WITH_RSA);
            signature.initSign(privateKey);
            signature.update(message);
            return signature.sign();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new Error(e);
        }
    }

    public void certificate(byte[] certificateValue) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
            Certificate certificate = certificateFactory.generateCertificate(new ByteArrayInputStream(certificateValue));
            publicKey = certificate.getPublicKey();
        } catch (CertificateException e) {
            throw new Error(e);
        }
    }

    public void publicKey(byte[] publicKeyValue) {
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyValue);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA.RSA_ALGORITHM);
            publicKey = keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new Error(e);
        }
    }

    public void privateKey(byte[] privateKeyValue) {
        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyValue);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA.RSA_ALGORITHM);
            privateKey = keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new Error(e);
        }
    }
}
