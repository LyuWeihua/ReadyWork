/**
 * Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
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

package work.ready.core.database;

import java.util.*;

public class CaseInsensitiveContainerFactory implements ContainerFactory {

	private static Boolean toLowerCase = null;

	public CaseInsensitiveContainerFactory() {
	}

	public CaseInsensitiveContainerFactory(boolean toLowerCase) {
		CaseInsensitiveContainerFactory.toLowerCase = toLowerCase;
	}

	public Map<String, Object> getAttrsMap() {
		return new CaseInsensitiveMap<>();
	}

	public Map<String, Object> getColumnsMap() {
		return new CaseInsensitiveMap<>();
	}

	public Set<String> getModifyFlagSet() {
		return new CaseInsensitiveSet();
	}

	private static String convertCase(String key) {
		if (toLowerCase != null) {
			return toLowerCase ? key.toLowerCase() : key.toUpperCase();
		} else {
			return key;
		}
	}

	public static class CaseInsensitiveSet extends TreeSet<String> {

		private static final long serialVersionUID = 6236541338642353211L;

		public CaseInsensitiveSet() {
			super(String.CASE_INSENSITIVE_ORDER);
		}

		public boolean add(String e) {
			return super.add(convertCase(e));
		}

		public boolean addAll(Collection<? extends String> c) {
			boolean modified = false;
			for (String o : c) {
				if (super.add(convertCase(o))) {
					modified = true;
				}
			}
			return modified;
		}
	}

	public static class CaseInsensitiveMap<V> extends TreeMap<String, V> {

		private static final long serialVersionUID = 7482853823611007217L;

		public CaseInsensitiveMap() {
			super(String.CASE_INSENSITIVE_ORDER);
		}

		public V put(String key, V value) {
			return super.put(convertCase(key), value);
		}

		public void putAll(Map<? extends String, ? extends V> map) {
			for (Map.Entry<? extends String, ? extends V> e : map.entrySet()) {
				super.put(convertCase(e.getKey()), e.getValue());
			}
		}
	}
}

