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

import work.ready.core.tools.StrUtil;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import java.net.Socket;
import java.util.Set;

public enum EndpointAlgorithm {
	HTTPS,
	LDAPS,
	APIS;

	public static EndpointAlgorithm select(boolean checkIdentity, Set<String> trustedNameSet) {
		if (checkIdentity) {
			if (trustedNameSet.isEmpty()) {
				return EndpointAlgorithm.HTTPS;
			} else {
				return EndpointAlgorithm.APIS;
			}
		}

		return null;
	}

	public static void setup(SSLEngine engine, EndpointAlgorithm identityAlg) {
		if (null!=engine
				&& null!= identityAlg
				&& EndpointAlgorithm.APIS!=identityAlg) {
			SSLParameters parameters = engine.getSSLParameters();
			String existingAlgorithm = parameters.getEndpointIdentificationAlgorithm();

			if (StrUtil.isBlank(existingAlgorithm)) {
				parameters.setEndpointIdentificationAlgorithm(identityAlg.name());
				engine.setSSLParameters(parameters);
			}
		}
	}

	public static void setup(Socket socket, EndpointAlgorithm identityAlg) {
		if (null!=socket && socket.isConnected() && socket instanceof SSLSocket
				&& null!=identityAlg
				&& EndpointAlgorithm.APIS!=identityAlg) {
			SSLSocket sslSocket = (SSLSocket)socket;

			SSLParameters parameters = sslSocket.getSSLParameters();
			String existingAlgorithm = parameters.getEndpointIdentificationAlgorithm();

			if (StrUtil.isBlank(existingAlgorithm)) {
				parameters.setEndpointIdentificationAlgorithm(identityAlg.name());
				sslSocket.setSSLParameters(parameters);
			}
		}
	}
}
