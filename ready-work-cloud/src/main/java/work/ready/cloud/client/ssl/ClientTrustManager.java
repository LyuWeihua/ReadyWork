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
package work.ready.cloud.client.ssl;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import javax.net.ssl.*;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ClientTrustManager extends X509ExtendedTrustManager implements X509TrustManager {
	private static final Log logger = LogFactory.getLog(ClientTrustManager.class);

	private final X509TrustManager trustManager;
	private final EndpointAlgorithm algorithm;
	private final Set<String> trustedNameSet = new HashSet<>();

	public ClientTrustManager(X509TrustManager trustManager, TLSConfig tlsConfig) {
		this.trustManager = Objects.requireNonNull(trustManager);
		this.algorithm = tlsConfig.getEndpointAlgorithm();
		this.trustedNameSet.addAll(tlsConfig.getTrustedNameSet());
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return trustManager.getAcceptedIssuers();
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		checkClientTrusted(chain, authType, (Socket)null);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		checkServerTrusted(chain, authType, (Socket)null);
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
		try {
			EndpointAlgorithm.setup(socket, algorithm);

			if (trustManager instanceof X509ExtendedTrustManager) {
				((X509ExtendedTrustManager)trustManager).checkClientTrusted(chain, authType, socket);
			} else {
				trustManager.checkClientTrusted(chain, authType);
				checkIdentity(socket, chain[0]);
			}

		} catch (Throwable t) {
			handleTrustValidationErrors(t);
		}
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
		try {
			EndpointAlgorithm.setup(socket, algorithm);

			if (trustManager instanceof X509ExtendedTrustManager) {
				((X509ExtendedTrustManager)trustManager).checkServerTrusted(chain, authType, socket);
			}else {
				trustManager.checkServerTrusted(chain, authType);
				checkIdentity(socket, chain[0]);
			}

			doCustomServerIdentityCheck(chain[0]);
		} catch (Throwable t) {
			handleTrustValidationErrors(t);
		}
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
		try {
			EndpointAlgorithm.setup(engine, algorithm);

			if (trustManager instanceof X509ExtendedTrustManager) {
				((X509ExtendedTrustManager)trustManager).checkClientTrusted(chain, authType, engine);
			}else {
				trustManager.checkClientTrusted(chain, authType);
				checkIdentity(engine, chain[0]);
			}

		} catch (Throwable t) {
			handleTrustValidationErrors(t);
		}
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
		try {
			EndpointAlgorithm.setup(engine, algorithm);

			if (trustManager instanceof X509ExtendedTrustManager) {
				((X509ExtendedTrustManager)trustManager).checkServerTrusted(chain, authType, engine);
			}else {
				trustManager.checkServerTrusted(chain, authType);
				checkIdentity(engine, chain[0]);
			}

			doCustomServerIdentityCheck(chain[0]);
		} catch (Throwable t) {
			handleTrustValidationErrors(t);
		}
	}

	private void doCustomServerIdentityCheck(X509Certificate cert) throws CertificateException{
		if (EndpointAlgorithm.APIS == algorithm) {
			HostNameChecker.verifyAndThrow(trustedNameSet, cert);
		}
	}

	private void checkIdentity(SSLEngine engine, X509Certificate cert)  throws CertificateException{
		if (null!=engine) {
			SSLSession session = engine.getHandshakeSession();
			checkIdentity(session, cert);
		}
	}

	private void checkIdentity(Socket socket, X509Certificate cert) throws CertificateException {
		if (socket != null && socket.isConnected() && socket instanceof SSLSocket) {
			SSLSocket sslSocket = (SSLSocket) socket;
			SSLSession session = sslSocket.getHandshakeSession();

			checkIdentity(session, cert);
		}
	}

	private void checkIdentity(SSLSession session, X509Certificate cert) throws CertificateException {
		if (session == null) {
			throw new CertificateException("No handshake session");
		}

		if (EndpointAlgorithm.HTTPS == algorithm) {
			String hostname = session.getPeerHost();
			HostNameChecker.verifyAndThrow(hostname, cert);
		}
	}

	public static TrustManager[] decorate(TrustManager[] trustManagers, TLSConfig tlsConfig) {
		if (null!=trustManagers && trustManagers.length>0) {
			TrustManager[] decoratedTrustManagers = new TrustManager[trustManagers.length];

			for (int i=0; i<trustManagers.length; ++i) {
				TrustManager trustManager = trustManagers[i];

				if (trustManager instanceof X509TrustManager){
					decoratedTrustManagers[i] = new ClientTrustManager((X509TrustManager)trustManager, tlsConfig);
				}else {
					decoratedTrustManagers[i] = trustManager;
				}
			}

			return decoratedTrustManagers;
		}

		return trustManagers;
	}

	public static void handleTrustValidationErrors(Throwable t) throws CertificateException{
		logger.error(t, "TrustValidation exception");

		if (t instanceof CertificateException) {
			throw (CertificateException)t;
		}

		throw new CertificateException(t);
	}
}
