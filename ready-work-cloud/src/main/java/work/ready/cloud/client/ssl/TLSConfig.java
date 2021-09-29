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

import work.ready.cloud.client.ClientConfig;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.tools.StrUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TLSConfig {
	private static final Log logger = LogFactory.getLog(TLSConfig.class);
	private static final Map<String, TLSConfig> memcache = new ConcurrentHashMap<>();

    private final boolean checkServerIdentify;
    private final Set<String> trustedNameSet;
    private final EndpointAlgorithm algorithm;

    private TLSConfig(boolean checkServerIdentify, Set<String> trustedNameSet) {
    	this.checkServerIdentify=checkServerIdentify;
    	this.trustedNameSet = Collections.unmodifiableSet(trustedNameSet);
    	this.algorithm = EndpointAlgorithm.select(checkServerIdentify, trustedNameSet);
    }

    public static TLSConfig create(final ClientConfig.TlsConfig config) {
    	return create(config, null);
    }

	public static TLSConfig create(final ClientConfig.TlsConfig config, final String trustedNameGroupKey) {
		String cacheKey = toCacheKey(config.isVerifyHostname(), trustedNameGroupKey);

    	return memcache.computeIfAbsent(cacheKey, key -> new TLSConfig(config.isVerifyHostname(),
    			resolveTrustedNames(config, trustedNameGroupKey)));
    }

	@SuppressWarnings("unchecked")
	public static Set<String> resolveTrustedNames(ClientConfig.TlsConfig config, String groupKey){
		if (StrUtil.isBlank(groupKey) 
				|| !config.isVerifyHostname()) {
			return Collections.EMPTY_SET;
		}

		String values = config.getTrustedNames().get(groupKey);
		if(values == null) throw new InvalidGroupKeyException(groupKey);

		return resolveTrustedNames(values);
	}

	public static Set<String> resolveTrustedNames(String trustedNames){
		Set<String> nameSet = Arrays.stream(StrUtil.trimToEmpty(trustedNames).split(","))
				.filter(StrUtil::notBlank)
				.collect(Collectors.toSet());

		if (logger.isDebugEnabled()) {
			logger.debug("trusted names %s", nameSet);
		}

		return nameSet;
	}

    public boolean getCheckServerIdentity() {
    	return checkServerIdentify;
    }

    public Set<String> getTrustedNameSet(){
    	return trustedNameSet;
    }

    public EndpointAlgorithm getEndpointAlgorithm() {
    	return algorithm;
    }

    private static String toCacheKey(boolean verifyHostName, String key) {
    	return String.format("%b-%s", verifyHostName, StrUtil.trimToEmpty(key));
    }

	@SuppressWarnings("serial")
	static class InvalidGroupKeyException extends IllegalArgumentException{
		InvalidGroupKeyException(String groupKey){
			super("Failed in resolving trustedNames. Invalid groupKey:" + groupKey);
		}
	}
}
