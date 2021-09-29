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

import org.apache.ignite.IgniteCache;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.NearCacheConfiguration;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.*;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.jose4j.keys.resolvers.X509VerificationKeyResolver;
import work.ready.cloud.ReadyCloud;
import work.ready.cloud.client.oauth.OauthHelper;
import work.ready.cloud.client.oauth.SignKeyRequest;
import work.ready.cloud.client.oauth.TokenKeyRequest;
import work.ready.cloud.cluster.Cloud;
import work.ready.core.exception.ExpiredTokenException;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.security.SecurityConfig;
import work.ready.core.server.Ready;
import work.ready.core.tools.HashUtil;
import work.ready.core.tools.HttpUtil;
import work.ready.core.tools.StrUtil;

import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

public class JwtVerifier {
    private static final Log logger = LogFactory.getLog(JwtVerifier.class);

    private static final int CACHE_EXPIRED_IN_MINUTES = 15;

    public static final String JWT_KEY_RESOLVER = "keyResolver";
    public static final String JWT_KEY_RESOLVER_X509CERT = "X509Certificate";
    public static final String JWT_KEY_RESOLVER_JWKS = "JsonWebKeySet";

    private SecurityConfig config;

    private IgniteCache<String, JwtClaims> cache;
    private Map<String, X509Certificate> certMap;
    private Map<String, List<JsonWebKey>> jwksMap;
    private List<String> fingerPrints;

    private static class LazyHolder{
        static final JwtVerifier instance = new JwtVerifier(Ready.getMainApplicationConfig().getSecurity());
    }

    public static JwtVerifier getInstance(){
        return LazyHolder.instance;
    }

    public JwtVerifier(SecurityConfig config) {
        this.config = config;
        if(config.isEnableJwtCache() && ReadyCloud.isReady()) {
            CacheConfiguration<String, JwtClaims> cfg = ReadyCloud.getInstance().newCacheConfig("ready.work:oauth2:jwt");
            NearCacheConfiguration<String, JwtClaims> nearCfg = new NearCacheConfiguration<>();
            cache = Cloud.getOrCreateCache(cfg, nearCfg);
        }
        switch (config.getKeyResolver()) {
            case JWT_KEY_RESOLVER_JWKS:
                jwksMap = new HashMap<>();
                break;
            case JWT_KEY_RESOLVER_X509CERT:
                
                if(!config.isBootstrapFromKeyService()) {
                    certMap = new HashMap<>();
                    fingerPrints = new ArrayList<>();
                    Map<String, Object> keyMap = Optional.ofNullable(config.getCertificate()).orElse(new HashMap<>());
                    for(String kid: keyMap.keySet()) {
                        X509Certificate cert = null;
                        try {
                            cert = readCertificate((String)keyMap.get(kid));
                        } catch (Exception e) {
                            logger.error(e,"Exception: ");
                        }
                        certMap.put(kid, cert);
                        fingerPrints.add(HashUtil.getCertFingerPrint(cert));
                    }
                }
                break;
            default:
                logger.info("%s not found or not recognized in SecurityConfig section of bootstrap config. Use %s as default %s",
                        JWT_KEY_RESOLVER, JWT_KEY_RESOLVER_X509CERT, JWT_KEY_RESOLVER);
        }
    }

    public X509Certificate readCertificate(String filename) {
        InputStream inStream = null;
        X509Certificate cert = null;
        try {
            inStream = Ready.config().getInputStreamFromFile(filename);
            if (inStream != null) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                cert = (X509Certificate) cf.generateCertificate(inStream);
            } else {
                logger.info("Certificate " + filename + " not found.");
            }
        } catch (Exception e) {
            logger.error(e,"Exception: ");
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException ioe) {
                    logger.error(ioe,"Exception: ");
                }
            }
        }
        return cert;
    }

    public static String getJwtFromAuthorization(String authorization) {
        String jwt = null;
        if(authorization != null && authorization.length() > 10) {
            String bearer = authorization.substring(0, 7).toLowerCase();
            if(bearer.equals("bearer ")){
                String[] parts = StrUtil.split(authorization,' ');
                if (parts.length == 2) {
                    jwt = parts[1];
                }
            }
        }
        return jwt;
    }

    public JwtClaims verifyJwt(String jwt, boolean ignoreExpiry, boolean isToken) throws InvalidJwtException, ExpiredTokenException {
        return verifyJwt(jwt, ignoreExpiry, isToken, this::getKeyResolver);
    }

    public JwtClaims verifyJwt(String jwt, boolean ignoreExpiry, boolean isToken, BiFunction<String, Boolean, VerificationKeyResolver> getKeyResolver)
            throws InvalidJwtException, ExpiredTokenException {
        JwtClaims claims;

        if(config.isEnableJwtCache() && cache != null) {
            claims = cache.get(jwt);
            if(claims != null) {
                if(!ignoreExpiry) {
                    try {

                        if ((NumericDate.fromMilliseconds(Ready.currentTimeMillis()).getValue() - config.getClockSkewInSeconds()) >= claims.getExpirationTime().getValue())
                        {
                            logger.info("Cached jwt token is expired!");
                            throw new ExpiredTokenException("Token is expired");
                        }
                    } catch (MalformedClaimException e) {
                        
                        logger.error(e,"MalformedClaimException: ");
                    }
                }
                
                return claims;
            }
        }

        JwtConsumer consumer = new JwtConsumerBuilder()
                .setSkipAllValidators()
                .setDisableRequireSignature()
                .setSkipSignatureVerification()
                .build();

        JwtContext jwtContext = consumer.process(jwt);
        claims = jwtContext.getJwtClaims();
        JsonWebStructure structure = jwtContext.getJoseObjects().get(0);
        
        String kid = structure.getKeyIdHeaderValue();

        if(!ignoreExpiry) {
            try {
                if ((NumericDate.fromMilliseconds(Ready.currentTimeMillis()).getValue() - config.getClockSkewInSeconds()) >= claims.getExpirationTime().getValue())
                {
                    logger.info("jwt token is expired!");
                    throw new ExpiredTokenException("Token is expired");
                }
            } catch (MalformedClaimException e) {
                logger.error(e,"MalformedClaimException: ");
                throw new InvalidJwtException("MalformedClaimException", new ErrorCodeValidator.Error(ErrorCodes.MALFORMED_CLAIM, "Invalid ExpirationTime Format"), e, jwtContext);
            }
        }

        consumer = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(315360000) 
                .setSkipDefaultAudienceValidation()
                .setVerificationKeyResolver(getKeyResolver.apply(kid, isToken))
                .build();

        jwtContext = consumer.process(jwt);
        claims = jwtContext.getJwtClaims();
        if(config.isEnableJwtCache()) {
            var cache0 = cache.withExpiryPolicy(new CreatedExpiryPolicy(new Duration(TimeUnit.MINUTES, CACHE_EXPIRED_IN_MINUTES)));
            cache0.put(jwt, claims);
        }
        return claims;
    }

    private VerificationKeyResolver getKeyResolver(String kid, boolean isToken) {

        VerificationKeyResolver verificationKeyResolver = null;
        switch (config.getKeyResolver()) {
            default:
            case JWT_KEY_RESOLVER_X509CERT:

                X509Certificate certificate = certMap == null? null : certMap.get(kid);
                if(certificate == null) {
                    certificate = isToken? getCertForToken(kid) : getCertForSign(kid);
                    if(certMap == null) certMap = new HashMap<>();  
                    certMap.put(kid, certificate);
                } else {
                    logger.debug("Got raw certificate for kid: %s from local cache", kid);
                }
                X509VerificationKeyResolver x509VerificationKeyResolver = new X509VerificationKeyResolver(certificate);

                x509VerificationKeyResolver.setTryAllOnNoThumbHeader(true);

                verificationKeyResolver = x509VerificationKeyResolver;
                break;
            case JWT_KEY_RESOLVER_JWKS:
                List<JsonWebKey> jwkList = jwksMap == null ? null : jwksMap.get(kid);
                if (jwkList == null) {
                    jwkList = getJsonWebKeySetForToken(kid);
                    if (jwkList != null) {
                        if (jwksMap == null) jwksMap = new HashMap<>();  
                        jwksMap.put(kid, jwkList);
                    }
                } else {
                    logger.debug("Got Json web key set for kid: %s from local cache", kid);
                }
                if (jwkList != null) {
                    verificationKeyResolver = new JwksVerificationKeyResolver(jwkList);
                }
                break;
        }
        return verificationKeyResolver;
    }

    private List<JsonWebKey> getJsonWebKeySetForToken(String kid) {

        TokenKeyRequest keyRequest = new TokenKeyRequest(kid);
        try {
            logger.debug("Getting Json Web Key for kid: %s from %s", kid, keyRequest.getServerUrl());
            String key = OauthHelper.getKey(keyRequest);
            logger.debug("Got Json Web Key '%s' for kid: %s", key, kid);

            return new JsonWebKeySet(key).getJsonWebKeys();
        } catch (Exception e) {
            logger.error(e,"Exception: ");
            throw new RuntimeException(e);
        }

    }

    public X509Certificate getCertForToken(String kid) {
        X509Certificate certificate = null;
        TokenKeyRequest keyRequest = new TokenKeyRequest(kid);
        try {
            logger.warn("<Deprecated: use JsonWebKeySet instead> Getting raw certificate for key id: %s from %s", kid, keyRequest.getServerUrl());
            String key = OauthHelper.getKey(keyRequest);
            logger.warn("<Deprecated: use JsonWebKeySet instead> Got %s bytes of raw certificate %s for key id: %s", key.length(), key, kid);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(HttpUtil.decodeBase64(key)));
        } catch (Exception e) {
            logger.error(e,"Exception: ");
            throw new RuntimeException(e);
        }
        return certificate;
    }

    public X509Certificate getCertForSign(String kid) {
        X509Certificate certificate = null;
        SignKeyRequest keyRequest = new SignKeyRequest(kid);
        try {
            logger.warn("Getting raw certificate for key id: %s from %s", kid, keyRequest.getServerUrl());
            String key = OauthHelper.getKey(keyRequest);
            logger.warn("Got %s bytes of raw certificate %s for key id: %s", key.length(), key, kid);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(HttpUtil.decodeBase64(key)));
        } catch (Exception e) {
            logger.error(e,"Exception: ");
            throw new RuntimeException(e);
        }
        return certificate;
    }

    public List getFingerPrints() {
        return fingerPrints;
    }

}
