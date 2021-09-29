/**
 * Copyright (c) 2011-2021, James Zhan 詹波 (jfinal@126.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package work.ready.core.tools.define;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class SyncWriteMap<K, V> extends HashMap<K, V> {

	private static final long serialVersionUID = -7287230891751869148L;

	public SyncWriteMap() {
	}

	public SyncWriteMap(int initialCapacity) {
		super(initialCapacity);
	}

	public SyncWriteMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public SyncWriteMap(Map<? extends K, ? extends V> m) {
		super(m);
	}

	@Override
	public V put(K key, V value) {
		synchronized (this) {
			return super.put(key, value);
		}
	}

	@Override
	public V replace(K key, V value) {
		synchronized (this) {
			return super.replace(key, value);
		}
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		synchronized (this) {
			return super.replace(key, oldValue, newValue);
		}
	}

	@Override
	public V putIfAbsent(K key, V value) {
		synchronized (this) {
			return super.putIfAbsent(key, value);
		}
	}

	@Override
	public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
		synchronized (this) {
			return super.computeIfAbsent(key, mappingFunction);
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		synchronized (this) {
			super.putAll(m);
		}
	}

	@Override
	public V remove(Object key) {
		synchronized (this) {
			return super.remove(key);
		}
	}

	@Override
	public void clear() {
		synchronized (this) {
			super.clear();
		}
	}
}

