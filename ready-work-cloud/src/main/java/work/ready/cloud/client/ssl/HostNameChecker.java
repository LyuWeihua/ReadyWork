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

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Set;

public class HostNameChecker {
	private static final Log logger = LogFactory.getLog(HostNameChecker.class);
	private static final TrustHostnameVerifier verifier = new TrustHostnameVerifier();

	public static void verifyAndThrow(final Set<String> nameSet, final X509Certificate cert) throws CertificateException{
		if (!verify(nameSet, cert)) {
			throw new CertificateException("No name matching " + nameSet + " found");
		}
	}

	public static void verifyAndThrow(final String name, final X509Certificate cert) throws CertificateException{
		if (!verify(name, cert)) {
			throw new CertificateException("No name matching " + name + " found");
		}
	}

	public static boolean verify(final Set<String> nameSet, final X509Certificate cert)  {
		if (null!=nameSet && !nameSet.isEmpty()) {
			return nameSet.stream().filter(name->verify(name, cert)).findAny().isPresent();
		}

		return false;
	}

    public static boolean verify(final String name, final X509Certificate cert) {
        try {
        	verifier.verify(name, cert);
            return true;
        } catch (final SSLException ex) {

            logger.error(ex, "SSL exception");

            return false;
        }
    }
}
