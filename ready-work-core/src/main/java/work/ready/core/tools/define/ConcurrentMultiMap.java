/**
 *
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
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

package work.ready.core.tools.define;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class ConcurrentMultiMap<K, V> {
	private transient final ConcurrentMap<K, List<V>> map;

	public ConcurrentMultiMap() {
		map = new ConcurrentHashMap<>();
	}

	private List<V> createList() {
		return new ArrayList<>();
	}

	public synchronized boolean put(K key, V value) {
		List<V> list = map.get(key);
		if (list == null) {
			list = createList();
			if (list.add(value)) {
				map.put(key, list);
				return true;
			} else {
				throw new AssertionError("New list violated the list spec");
			}
		} else if (list.add(value)) {
			return true;
		} else {
			return false;
		}
	}

	public boolean putAll(K key, List<V> list) {
		if (list == null) {
			return false;
		} else {
			map.put(key, list);
			return true;
		}
	}

	public List<V> get(K key) {
		return map.get(key);
	}

	public void clear() {
		map.clear();
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public List<V> computeIfAbsent(K key, Function<? super K, ? extends List<V>> mappingFunction) {
		return map.computeIfAbsent(key, mappingFunction);
	}

}
