/**
 *
 * Original work Copyright (c) 2016 Network New Technologies Inc.
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
package work.ready.core.security.cloud;

import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.lang.JoseException;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.security.SecurityConfig;
import work.ready.core.server.Ready;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;

public class JwtIssuer {
    private static final Log logger = LogFactory.getLog(JwtIssuer.class);
    private final SecurityConfig config;

    private static class LazyHolder{
        static final JwtIssuer instance = new JwtIssuer(Ready.getMainApplicationConfig().getSecurity());
    }

    public static JwtIssuer getInstance(){
        return LazyHolder.instance;
    }

    public JwtIssuer(SecurityConfig config){
        this.config = config;
    }

    public String getJwt(JwtClaims claims) throws JoseException {
        String jwt;
        RSAPrivateKey privateKey;
        if(config.getKey() != null && config.getKey().validate() && config.getJwtPrivateKeyPassword() != null) {
            privateKey = (RSAPrivateKey) getPrivateKey(
                    config.getKey().getFilename(), config.getJwtPrivateKeyPassword(), config.getKey().getKeyName());
        } else {
            throw new RuntimeException("Private Key settings for JWT component is invalid in security section of application config");
        }

        JsonWebSignature jws = new JsonWebSignature();

        jws.setPayload(claims.toJson());

        jws.setKey(privateKey);

        String provider_id = "";
        if (config.getProviderId() != null) {
            provider_id = config.getProviderId();
            if (provider_id.length() == 1) {
                provider_id = "0" + provider_id;
            } else if (provider_id.length() > 2) {
                logger.error("provider_id defined in the security.yml file is invalid; the length should be 2");
                provider_id = provider_id.substring(0, 2);
            }
        }
        jws.setKeyIdHeaderValue(provider_id + config.getKey().getKid());

        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

        jwt = jws.getCompactSerialization();
        return jwt;
    }

    public JwtClaims getDefaultJwtClaims(){ return getJwtClaimsWithExpiresIn(null); }

    public JwtClaims getJwtClaimsWithExpiresIn(Integer expiresInSeconds) {

        JwtClaims claims = new JwtClaims();

        claims.setIssuer(config.getIssuer());
        claims.setAudience(config.getAudience());
        int expiresInMinutes = expiresInSeconds == null ? config.getExpiredInMinutes() : expiresInSeconds / 60;
        
        claims.setExpirationTime(NumericDate.fromMilliseconds(Ready.currentTimeMillis() + expiresInMinutes * 60 * 1000L));
        claims.setGeneratedJwtId(); 
        claims.setIssuedAt(NumericDate.fromMilliseconds(Ready.currentTimeMillis()));  
        claims.setNotBeforeMinutesInThePast(2); 
        claims.setClaim("version", config.getVersion());
        return claims;
    }

    private PrivateKey getPrivateKey(String filename, String password, String key) {
        if(logger.isDebugEnabled()) logger.debug("filename = " + filename + " key = " + key);
        PrivateKey privateKey = null;

        try {
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(Ready.config().getInputStreamFromFile(filename),
                    password.toCharArray());

            privateKey = (PrivateKey) keystore.getKey(key,
                    password.toCharArray());
        } catch (Exception e) {
            logger.error(e,"Exception: ");
        }

        if (privateKey == null) {
            logger.error("Failed to retrieve private key from keystore");
        }

        return privateKey;
    }

}
