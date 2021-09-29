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

import work.ready.core.server.Ready;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyClassLoader extends ClassLoader {

	protected Map<String, byte[]> byteCodeMap = new ConcurrentHashMap<>();

	static {
		registerAsParallelCapable();
	}

	public ProxyClassLoader() {
		super(getParentClassLoader());
	}

	protected static ClassLoader getParentClassLoader() {
		ClassLoader ret = Ready.getClassLoader();
		return ret != null ? ret : ProxyClassLoader.class.getClassLoader();
	}

	public Class<?> loadProxyClass(SourceCode proxyClass) {
		for (Entry<String, byte[]> e : proxyClass.getByteCode().entrySet()) {
			byteCodeMap.putIfAbsent(e.getKey(), e.getValue());
		}

		try {
			return loadClass(proxyClass.getPkg() + "." + proxyClass.getName());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		byte[] bytes = byteCodeMap.get(name);
		if (bytes != null) {
			Class<?> ret = defineClass(name, bytes, 0, bytes.length);
			byteCodeMap.remove(name);
			return ret;
		}

		return super.findClass(name);
	}
}

