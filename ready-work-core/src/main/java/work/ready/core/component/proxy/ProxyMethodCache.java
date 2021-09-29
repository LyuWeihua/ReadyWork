/**
 *
 * Original work Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
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

package work.ready.core.component.proxy;

import work.ready.core.tools.define.SyncWriteMap;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class ProxyMethodCache {

	private final AtomicLong atomicLong = new AtomicLong();
	private final Map<Long, ProxyMethod> cache = new SyncWriteMap<>(2048, 0.25F);

	public Long generateKey() {
		return atomicLong.incrementAndGet();
	}

	public void put(ProxyMethod proxyMethod) {
		Objects.requireNonNull(proxyMethod, "proxyMethod can not be null");
		Objects.requireNonNull(proxyMethod.getKey(), "the key of proxyMethod can not be null");
		if (cache.containsKey(proxyMethod.getKey())) {
			throw new RuntimeException("the key of proxyMethod already exists");
		}

		cache.putIfAbsent(proxyMethod.getKey(), proxyMethod);
	}

	public ProxyMethod get(Long key) {
		return cache.get(key);
	}
}

