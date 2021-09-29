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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.undertow.client.*;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.*;
import work.ready.cloud.ReadyCloud;
import work.ready.cloud.client.ClientConfig;
import work.ready.cloud.client.CloudClient;
import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.registry.base.URL;
import work.ready.core.config.Config;
import work.ready.core.exception.ClientException;
import work.ready.core.handler.ContentType;
import work.ready.core.handler.RequestMethod;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.security.HttpAuth;
import work.ready.core.server.Constant;
import work.ready.core.service.result.Failure;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;
import work.ready.core.server.Ready;
import work.ready.core.service.status.Status;
import work.ready.core.tools.StrUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.UTF_8;

public class OauthHelper {
    private static final String GRANT_TYPE = "grant_type";
    private static final String CODE = "code";

    private static final String FAIL_TO_SEND_REQUEST = "ERROR10051";
    private static final String GET_TOKEN_ERROR = "ERROR10052";
    private static final String ESTABLISH_CONNECTION_ERROR = "ERROR10053";
    private static final String GET_TOKEN_TIMEOUT = "ERROR10054";
    public static final String STATUS_CLIENT_CREDENTIALS_TOKEN_NOT_AVAILABLE = "ERROR10009";

    private static final Log logger = LogFactory.getLog(OauthHelper.class);

    @Deprecated
    public static TokenResponse getToken(TokenRequest tokenRequest) throws ClientException {
        Result<TokenResponse> responseResult = getTokenResult(tokenRequest);
        if (responseResult.isSuccess()) {
            return responseResult.getResult();
        }
        throw new ClientException(responseResult.getError());
    }

    public static Result<TokenResponse> getTokenResult(TokenRequest tokenRequest) {
        return getTokenResult(tokenRequest, null);
    }

    public static Result<TokenResponse> getTokenResult(TokenRequest tokenRequest, String profile) {
        final AtomicReference<Result<TokenResponse>> reference = new AtomicReference<>();
        final CloudClient client = CloudClient.getInstance();
        if(tokenRequest.getServerUrl() != null) {
        } else if(tokenRequest.getServiceId() != null) {
            URL url = Cloud.discover("https", tokenRequest.getServiceId(), profile, null);
            if (url == null) {
                logger.error("Failed to discover token service with serviceId: %s, with profile: %s", tokenRequest.getServiceId(), profile);
                throw new ClientException(String.format("Failed to discover service with serviceId: %s, with profile: %s", tokenRequest.getServiceId(), profile));
            }
            tokenRequest.setServerUrl(url.getRequestUri());
        } else {
            
            logger.error("Error: both server_url and serviceId are not configured for " + tokenRequest.getClass());
            throw new ClientException("both server_url and serviceId are not configured for " + tokenRequest.getClass());
        }

        ClientRequestComposable requestComposer = ClientRequestComposerProvider.getInstance().getComposer(ClientRequestComposerProvider.ClientRequestComposers.CLIENT_CREDENTIAL_REQUEST_COMPOSER);
        new TokenRequestAction(tokenRequest, requestComposer, client).request(reference, 4000);

        return reference.get() == null ? Failure.of(new Status(GET_TOKEN_ERROR)) : reference.get();
    }

    public static Result<TokenResponse> getSignResult(SignRequest signRequest) {
        return getSignResult(signRequest, null);
    }

    public static Result<TokenResponse> getSignResult(SignRequest signRequest, String profile) {
        final AtomicReference<Result<TokenResponse>> reference = new AtomicReference<>();
        final CloudClient client = CloudClient.getInstance();
        if(signRequest.getServerUrl() != null) {
        } else if(signRequest.getServiceId() != null) {
            URL url = Cloud.discover("https", signRequest.getServiceId(), profile, null);
            if (url == null) {
                logger.error("Failed to discover sign service with serviceId: %s, with profile: %s", signRequest.getServiceId(), profile);
                throw new ClientException(String.format("Failed to discover service with serviceId: %s, with profile: %s", signRequest.getServiceId(), profile));
            }
            signRequest.setServerUrl(url.getRequestUri());
        } else {
            
            logger.error("Error: both server_url and serviceId are not configured for " + signRequest.getClass());
            throw new ClientException("both server_url and serviceId are not configured for " + signRequest.getClass());
        }

        try {
            Map<String, Object> map = new HashMap<>();
            map.put("expires", signRequest.getExpires());
            map.put("payload", signRequest.getPayload());
            String requestBody = Ready.config().getJsonMapper().writeValueAsString(map);
            var builder = HttpRequest.newBuilder().method(RequestMethod.POST.name(), HttpRequest.BodyPublishers.ofString(requestBody, Constant.DEFAULT_CHARSET));
            builder.header(Headers.TRANSFER_ENCODING_STRING, "chunked");
            builder.header(Headers.CONTENT_TYPE_STRING, "application/x-www-form-urlencoded");
            builder.header(Headers.AUTHORIZATION_STRING, HttpAuth.getBasicAuthHeader(signRequest.getClientId(), signRequest.getClientSecret()));
            builder.uri(URI.create(signRequest.getServerUrl() + signRequest.getUri()));
            if (logger.isDebugEnabled()) {
                var request = builder.build();
                logger.debug("The request sent to the oauth server = request header(s): %s, request body: %s", request.headers().toString(), request.bodyPublisher().get());
            }
            int timeout = 4000;
            client.withTimeout(timeout).sendAsync(builder, res -> {
                if (logger.isDebugEnabled()) {
                    logger.debug("getToken responseCode = %s body = %s", res.statusCode(), res.body());
                }
                if (res.statusCode() == StatusCodes.OK) {
                    reference.set(handleResponse(getContentTypeFromHeaders(res.headers()), res.body()));
                } else {
                    reference.set(Failure.of(new Status(GET_TOKEN_ERROR, escapeBasedOnType(getContentTypeFromHeaders(res.headers()), res.body()))));
                }
            }, err -> {
                logger.error(err, "Exception: ");
                if (err instanceof TimeoutException || err instanceof HttpTimeoutException) {
                    reference.set(Failure.of(new Status(GET_TOKEN_TIMEOUT)));
                } else {
                    reference.set(Failure.of(new Status(FAIL_TO_SEND_REQUEST)));
                }
                return null;
            });
        } catch (Exception e) {
            logger.error(e, "Exception: ");
            reference.set(Failure.of(new Status(GET_TOKEN_ERROR)));
        }

        return reference.get() == null ? Failure.of(new Status(GET_TOKEN_ERROR)) : reference.get();
    }

    @Deprecated
    public static TokenResponse getTokenFromSaml(SAMLBearerRequest tokenRequest) throws ClientException {
        Result<TokenResponse> responseResult = getTokenFromSamlResult(tokenRequest);
        if (responseResult.isSuccess()) {
            return responseResult.getResult();
        }
        throw new ClientException(responseResult.getError());
    }

    public static Result<TokenResponse> getTokenFromSamlResult(SAMLBearerRequest tokenRequest) {
        return getTokenFromSamlResult(tokenRequest, null);
    }

    public static Result<TokenResponse> getTokenFromSamlResult(SAMLBearerRequest tokenRequest, String profile) {
        final AtomicReference<Result<TokenResponse>> reference = new AtomicReference<>();
        final CloudClient client = CloudClient.getInstance();
        if(tokenRequest.getServerUrl() != null) {
        } else if(tokenRequest.getServiceId() != null) {
            URL url = Cloud.discover("https", tokenRequest.getServiceId(), profile, null);
            if (url == null) {
                logger.error("Failed to discover saml token service with serviceId: %s, with profile: %s", tokenRequest.getServiceId(), profile);
                throw new ClientException(String.format("Failed to discover service with serviceId: %s, with profile: %s", tokenRequest.getServiceId(), profile));
            }
            tokenRequest.setServerUrl(url.getRequestUri());
        } else {
            
            logger.error("Error: both server_url and serviceId are not configured for " + tokenRequest.getClass());
            throw new ClientException("both server_url and serviceId are not configured for " + tokenRequest.getClass());
        }

        ClientRequestComposable requestComposer = ClientRequestComposerProvider.getInstance().getComposer(ClientRequestComposerProvider.ClientRequestComposers.SAML_BEARER_REQUEST_COMPOSER);
        new TokenRequestAction(tokenRequest, requestComposer, client).request(reference, 4000);

        return reference.get() == null ? Failure.of(new Status(GET_TOKEN_ERROR)) : reference.get();
    }

    private static class TokenRequestAction {
        private CloudClient client;
        private ClientRequestComposable requestComposer;
        private TokenRequest tokenRequest;

        TokenRequestAction(TokenRequest tokenRequest, ClientRequestComposable requestComposer, CloudClient client){
            this.tokenRequest = tokenRequest;
            this.client = client;
            this.requestComposer = requestComposer;
        }

        public void request(AtomicReference<Result<TokenResponse>> reference) {
            request(reference, 0);
        }

        public void request(AtomicReference<Result<TokenResponse>> reference, long timeout) {
            var builder = requestComposer.composeClientRequest(tokenRequest);
            if (logger.isDebugEnabled()) {
                final HttpRequest request = builder.build();
                logger.debug("The request sent to the oauth server = request header(s): %s, request body: %s", request.headers().toString(), request.bodyPublisher().get());
            }
            client.withTimeout(timeout).sendAsync(builder, res->{
                if (logger.isDebugEnabled()) {
                    logger.debug("getToken responseCode = %s body = %s", res.statusCode(), res.body());
                }
                if(res.statusCode() == StatusCodes.OK) {
                    reference.set(handleResponse(getContentTypeFromHeaders(res.headers()), res.body()));
                } else {
                    reference.set(Failure.of(new Status(GET_TOKEN_ERROR, escapeBasedOnType(getContentTypeFromHeaders(res.headers()), res.body()))));
                }
            }, err->{
                logger.error(err, "Exception: ");
                if(err instanceof TimeoutException || err instanceof HttpTimeoutException) {
                    reference.set(Failure.of(new Status(GET_TOKEN_TIMEOUT)));
                } else {
                    reference.set(Failure.of(new Status(FAIL_TO_SEND_REQUEST)));
                }
                return null;
            });
        }
    }

    public static String getKey(KeyRequest keyRequest) throws ClientException {
        return getKey(keyRequest, null);
    }

    public static String getKey(KeyRequest keyRequest, String profile) throws ClientException {
        final AtomicReference<Result<String>> reference = new AtomicReference<>();
        final CloudClient client = CloudClient.getInstance();
        if(keyRequest.getServerUrl() != null) {
        } else if(keyRequest.getServiceId() != null) {
            URL url = Cloud.discover("https", keyRequest.getServiceId(), profile, null);
            if (url == null) {
                logger.error("Failed to discover key distribution service with serviceId: %s, with profile: %s", keyRequest.getServiceId(), profile);
                throw new ClientException(String.format("Failed to discover service with serviceId: %s, with profile: %s", keyRequest.getServiceId(), profile));
            }
            keyRequest.setServerUrl(url.getRequestUri());
        } else {
            
            logger.error("Error: both server_url and serviceId are not configured for " + keyRequest.getClass());
            throw new ClientException("both server_url and serviceId are not configured for " + keyRequest.getClass());
        }

        var builder = HttpRequest.newBuilder().method(RequestMethod.GET.name(), HttpRequest.BodyPublishers.noBody());
        builder.header(Headers.AUTHORIZATION_STRING, HttpAuth.getBasicAuthHeader(keyRequest.getClientId(), keyRequest.getClientSecret()));
        builder.uri(URI.create(keyRequest.getServerUrl() + keyRequest.getUri()));
        if (logger.isDebugEnabled()) {
            final HttpRequest request = builder.build();
            logger.debug("The request sent to the oauth server = request header(s): %s", request.headers().toString());
        }

        int timeout = 4000;
        client.withTimeout(timeout).sendAsync(builder, res -> {
            if (logger.isDebugEnabled()) {
                logger.debug("getKey responseCode = %s body = %s", res.statusCode(), res.body());
            }
            if (res.statusCode() == StatusCodes.OK) {
                reference.set(Success.of(res.body()));
            } else {
                reference.set(Failure.of(new Status(GET_TOKEN_ERROR, res.body())));
            }
        }, err -> {
            logger.error(err, "Exception: ");
            if (err instanceof TimeoutException || err instanceof HttpTimeoutException) {
                reference.set(Failure.of(new Status(GET_TOKEN_TIMEOUT)));
            } else {
                reference.set(Failure.of(new Status(FAIL_TO_SEND_REQUEST)));
            }
            return null;
        });
        if(reference.get().isSuccess()) {
            return reference.get().getResult();
        } else {
            throw new ClientException(reference.get().getError());
        }
    }

    public static String derefToken(DerefRequest derefRequest) throws ClientException {
        return derefToken(derefRequest, null);
    }

    public static String derefToken(DerefRequest derefRequest, String profile) throws ClientException {
        final AtomicReference<Result<String>> reference = new AtomicReference<>();
        final CloudClient client = CloudClient.getInstance();
        if(derefRequest.getServerUrl() != null) {
        } else if(derefRequest.getServiceId() != null) {
            URL url = Cloud.discover("https", derefRequest.getServiceId(), profile, null);
            if (url == null) {
                logger.error("Failed to discover de-reference service with serviceId: %s, with profile: %s", derefRequest.getServiceId(), profile);
                throw new ClientException(String.format("Failed to discover service with serviceId: %s, with profile: %s", derefRequest.getServiceId(), profile));
            }
            derefRequest.setServerUrl(url.getRequestUri());
        } else {
            
            logger.error("Error: both server_url and serviceId are not configured for " + derefRequest.getClass());
            throw new ClientException("both server_url and serviceId are not configured for " + derefRequest.getClass());
        }

        var builder = HttpRequest.newBuilder().method(RequestMethod.GET.name(), HttpRequest.BodyPublishers.noBody());
        builder.header(Headers.AUTHORIZATION_STRING, HttpAuth.getBasicAuthHeader(derefRequest.getClientId(), derefRequest.getClientSecret()));
        builder.uri(URI.create(derefRequest.getServerUrl() + derefRequest.getUri()));
        if (logger.isDebugEnabled()) {
            final HttpRequest request = builder.build();
            logger.debug("The request sent to the oauth server = request header(s): %s", request.headers().toString());
        }

        int timeout = 4000;
        client.withTimeout(timeout).sendAsync(builder, res -> {
            if (logger.isDebugEnabled()) {
                logger.debug("getToken responseCode = %s body = %s", res.statusCode(), res.body());
            }
            if (res.statusCode() == StatusCodes.OK) {
                reference.set(Success.of(res.body()));
            } else {
                reference.set(Failure.of(new Status(GET_TOKEN_ERROR, res.body())));
            }
        }, err -> {
            logger.error(err, "Exception: ");
            if (err instanceof TimeoutException || err instanceof HttpTimeoutException) {
                reference.set(Failure.of(new Status(GET_TOKEN_TIMEOUT)));
            } else {
                reference.set(Failure.of(new Status(FAIL_TO_SEND_REQUEST)));
            }
            return null;
        });
        if(reference.get().isSuccess()) {
            return reference.get().getResult();
        } else {
            throw new ClientException(reference.get().getError());
        }
    }

    public static String getEncodedString(TokenRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put(GRANT_TYPE, request.getGrantType());
        if(ClientConfig.AUTHORIZATION_CODE.equals(request.getGrantType())) {
            params.put(CODE, ((AuthorizationCodeRequest)request).getAuthCode());
            
            if(((AuthorizationCodeRequest)request).getRedirectUri() != null) {
                params.put(ClientConfig.REDIRECT_URI, ((AuthorizationCodeRequest)request).getRedirectUri());
            }
            String csrf = request.getCsrf();
            if(csrf != null) {
                params.put(ClientConfig.CSRF, csrf);
            }
        }
        if(ClientConfig.REFRESH_TOKEN.equals(request.getGrantType())) {
            params.put(ClientConfig.REFRESH_TOKEN, ((RefreshTokenRequest)request).getRefreshToken());
            String csrf = request.getCsrf();
            if(csrf != null) {
                params.put(ClientConfig.CSRF, csrf);
            }
        }
        if(request.getScope() != null) {
            params.put(ClientConfig.SCOPE, String.join(" ", request.getScope()));
        }
        return CloudClient.getFormDataString(params);
    }

    private static Result<TokenResponse> handleResponse(ContentType contentType, String responseBody) {
        TokenResponse tokenResponse;
        Result<TokenResponse> result;
        try {
            
            if(!contentType.equals(ContentType.APPLICATION_JSON)) {
                return Failure.of(new Status(GET_TOKEN_ERROR, escapeBasedOnType(contentType, responseBody)));
            }
            if (responseBody != null && responseBody.length() > 0) {
                tokenResponse = Ready.config().getJsonMapper().readValue(responseBody, TokenResponse.class);
                
                if(tokenResponse != null && tokenResponse.getAccessToken() != null) {
                    result = Success.of(tokenResponse);
                } else {
                    result = Failure.of(new Status(tokenResponse.getHttpCode(), tokenResponse.getCode(), tokenResponse.getMessage(), tokenResponse.getDescription(), tokenResponse.getSeverity()));
                }
            } else {
                result = Failure.of(new Status(GET_TOKEN_ERROR, "no auth server response"));
                logger.error("Error in token retrieval, response = " + responseBody);
            }
        } catch (UnrecognizedPropertyException e) {
            
            result = Failure.of(new Status(GET_TOKEN_ERROR, escapeBasedOnType(contentType, responseBody)));
        } catch (IOException | RuntimeException e) {
            result = Failure.of(new Status(GET_TOKEN_ERROR, e.getMessage()));
            logger.error(e,"Error in token retrieval");
        }
        return result;
    }

    @Deprecated
    public static void sendStatusToResponse(HttpServerExchange exchange, Status status) {
        exchange.setStatusCode(status.getHttpCode());
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(status.toString());
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        logger.error(status.toString() + " at " + elements[2].getClassName() + "." + elements[2].getMethodName() + "(" + elements[2].getFileName() + ":" + elements[2].getLineNumber() + ")");
    }

    public static Result<Jwt> populateCCToken(final Jwt jwt) {
        boolean isInRenewWindow = jwt.getExpire() - Ready.currentTimeMillis() < Jwt.getTokenRenewBeforeExpired();
        logger.trace("isInRenewWindow = " + isInRenewWindow);
        
        if(!isInRenewWindow) { return Success.of(jwt); }
        
        synchronized (jwt) {
            
            if(jwt.getExpire() <= Ready.currentTimeMillis()) {
                Result<Jwt> result = renewCCTokenSync(jwt);
                if(logger.isTraceEnabled()) logger.trace("Check secondary token is done!");
                return result;
            } else {
                
                renewCCTokenAsync(jwt);
                if(logger.isTraceEnabled()) logger.trace("Check secondary token is done!");
                return Success.of(jwt);
            }
        }
    }

    private static Result<Jwt> renewCCTokenSync(final Jwt jwt) {
        
        logger.trace("In renew window and token is already expired.");
        
        if (!jwt.isRenewing() || Ready.currentTimeMillis() > jwt.getExpiredRetryTimeout()) {
            jwt.setRenewing(true);
            jwt.setEarlyRetryTimeout(Ready.currentTimeMillis() + Jwt.getExpiredRefreshRetryDelay());
            Result<Jwt> result = getCCTokenRemotely(jwt);
            
            jwt.setRenewing(false);
            return result;
        } else {
            if(logger.isTraceEnabled()) logger.trace("Circuit breaker is tripped and not timeout yet!");
            
            return Failure.of(new Status(STATUS_CLIENT_CREDENTIALS_TOKEN_NOT_AVAILABLE));
        }
    }

    private static void renewCCTokenAsync(final Jwt jwt) {
        
        logger.trace("In renew window but token is not expired yet.");
        if(!jwt.isRenewing() || Ready.currentTimeMillis() > jwt.getEarlyRetryTimeout()) {
            jwt.setRenewing(true);
            jwt.setEarlyRetryTimeout(Ready.currentTimeMillis() + Jwt.getEarlyRefreshRetryDelay());
            logger.trace("Retrieve token async is called while token is not expired yet");

            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

            executor.schedule(() -> {
                Result<Jwt> result = getCCTokenRemotely(jwt);
                if(result.isFailure()) {
                    
                    logger.error("Async retrieve token error with status: %s", result.getError().toString());
                }
                
                jwt.setRenewing(false);
            }, 50, TimeUnit.MILLISECONDS);
            executor.shutdown();
        }
    }

    private static Result<Jwt> getCCTokenRemotely(final Jwt jwt) {
        TokenRequest tokenRequest = new ClientCredentialsRequest();
        
        setScope(tokenRequest, jwt);
        Result<TokenResponse> result = OauthHelper.getTokenResult(tokenRequest);
        if(result.isSuccess()) {
            TokenResponse tokenResponse = result.getResult();
            jwt.setJwt(tokenResponse.getAccessToken());
            
            jwt.setExpire(Ready.currentTimeMillis() + tokenResponse.getExpiresIn() * 1000);
            logger.info("Get client credentials token %s with expire_in %s seconds", jwt, tokenResponse.getExpiresIn());
            
            jwt.setScopes(tokenResponse.getScope());
            return Success.of(jwt);
        } else {
            logger.info("Get client credentials token fail with status: %s", result.getError().toString());
            return Failure.of(result.getError());
        }
    }

    private static void setScope(TokenRequest tokenRequest, Jwt jwt) {
        if(jwt.getKey() != null && !jwt.getKey().getScopes().isEmpty()) {
            tokenRequest.setScope(new ArrayList<String>() {{ addAll(jwt.getKey().getScopes()); }});
        }
    }

    public static ContentType getContentTypeFromHeaders(HttpHeaders headers) {
        String type = headers.firstValue(Headers.CONTENT_TYPE_STRING).orElse(null);
        return type == null ? ContentType.ANY_TYPE : ContentType.parse(type);
    }

    public static ContentType getContentTypeFromExchange(ClientExchange exchange) {
        HeaderValues headerValues = exchange.getResponse().getResponseHeaders().get(Headers.CONTENT_TYPE);
        return headerValues == null ? ContentType.ANY_TYPE : ContentType.parse(headerValues.getFirst());
    }

    private static String escapeBasedOnType(ContentType contentType, String responseBody) {
        if (ContentType.APPLICATION_JSON.equals(contentType)) {
            try {
                String escapedStr = Ready.config().getJsonMapper().writeValueAsString(responseBody);
                return escapedStr.substring(1, escapedStr.length() - 1);
            } catch (JsonProcessingException e) {
                logger.error("escape json response fails");
                return responseBody;
            }
        } else if(ContentType.TEXT_XML.equals(contentType)) {
            
            return escapeXml(responseBody);
        } else {
            return responseBody;
        }
    }

    private static String escapeXml (String nonEscapedXmlStr) {
        StringBuilder escapedXML = new StringBuilder();
        for (int i = 0; i < nonEscapedXmlStr.length(); i++) {
            char c = nonEscapedXmlStr.charAt(i);
            switch (c) {
                case '<':
                    escapedXML.append("&lt;");
                    break;
                case '>':
                    escapedXML.append("&gt;");
                    break;
                case '\"':
                    escapedXML.append("&quot;");
                    break;
                case '&':
                    escapedXML.append("&amp;");
                    break;
                case '\'':
                    escapedXML.append("&apos;");
                    break;
                default:
                    if (c > 0x7e) {
                        escapedXML.append("&#" + ((int) c) + ";");
                    } else {
                        escapedXML.append(c);
                    }
            }
        }
        return escapedXML.toString();
    }

    public static void adjustNoChunkedEncoding(ClientRequest request, String requestBody) {
        String fixedLengthString = request.getRequestHeaders().getFirst(Headers.CONTENT_LENGTH);
        String transferEncodingString = request.getRequestHeaders().getLast(Headers.TRANSFER_ENCODING);
        if(transferEncodingString != null) {
            request.getRequestHeaders().remove(Headers.TRANSFER_ENCODING);
        }
        
        if(fixedLengthString != null && Long.parseLong(fixedLengthString) > 0) {
            return;
        }
        if(!StrUtil.isEmpty(requestBody)) {
            long contentLength = requestBody.getBytes(UTF_8).length;
            request.getRequestHeaders().put(Headers.CONTENT_LENGTH, contentLength);
        }

    }
}
