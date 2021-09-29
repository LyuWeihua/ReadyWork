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

package work.ready.core.template.expr.ast;

import work.ready.core.tools.HashUtil;

public abstract class MethodKeyBuilder {

	public abstract Long getMethodKey(Class<?> targetClass, String methodName, Class<?>[] argTypes);

	static MethodKeyBuilder instance = new FastMethodKeyBuilder();

	public static MethodKeyBuilder getInstance() {
		return instance;
	}

	public static void setToStrictMethodKeyBuilder() {
		instance = new StrictMethodKeyBuilder();
	}

	public static void setMethodKeyBuilder(MethodKeyBuilder methodKeyBuilder) {
		if (methodKeyBuilder == null) {
			throw new IllegalArgumentException("methodKeyBuilder can not be null");
		}
		instance = methodKeyBuilder;
	}

	public static class FastMethodKeyBuilder extends MethodKeyBuilder {
		public Long getMethodKey(Class<?> targetClass, String methodName, Class<?>[] argTypes) {
			long hash = HashUtil.FNV_OFFSET_BASIS_64;
			hash ^= targetClass.getName().hashCode();
			hash *= HashUtil.FNV_PRIME_64;

			hash ^= methodName.hashCode();
			hash *= HashUtil.FNV_PRIME_64;

			if (argTypes != null) {
				for (int i=0; i<argTypes.length; i++) {
					Class<?> type = argTypes[i];
					if (type != null) {
						hash ^= type.getName().hashCode();
						hash *= HashUtil.FNV_PRIME_64;
					} else {
						hash ^= "null".hashCode();
						hash *= HashUtil.FNV_PRIME_64;
					}
				}
			}
			return hash;
		}
	}

	public static class StrictMethodKeyBuilder extends MethodKeyBuilder {
		public Long getMethodKey(Class<?> targetClass, String methodName, Class<?>[] argTypes) {
			long hash = HashUtil.FNV_OFFSET_BASIS_64;

			hash = fnv1a64(hash, targetClass.getName());
			hash = fnv1a64(hash, methodName);
			if (argTypes != null) {
				for (int i=0; i<argTypes.length; i++) {
					Class<?> type = argTypes[i];
					if (type != null) {
						hash = fnv1a64(hash, type.getName());
					} else {
						hash = fnv1a64(hash, "null");
					}
				}
			}

			return hash;
		}

		private long fnv1a64(long offsetBasis, String key) {
			long hash = offsetBasis;
			for(int i=0, size=key.length(); i<size; i++) {
				hash ^= key.charAt(i);
				hash *= HashUtil.FNV_PRIME_64;
			}
			return hash;
		}
	}
}

