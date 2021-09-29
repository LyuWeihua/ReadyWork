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

package work.ready.core.template.ext.sharedmethod;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class SharedMethodLib {

	public Boolean isEmpty(Object v) {
		if (v == null) {
			return true;
		}

		if (v instanceof Collection) {
			return ((Collection<?>)v).isEmpty();
		}
		if (v instanceof Map) {
			return ((Map<?, ?>)v).isEmpty();
		}

		if (v.getClass().isArray()) {
			return Array.getLength(v) == 0;
		}

		if (v instanceof Iterator) {
			return ! ((Iterator<?>)v).hasNext();
		}
		if (v instanceof Iterable) {
			return ! ((Iterable<?>)v).iterator().hasNext();
		}

		throw new IllegalArgumentException("isEmpty(...) 方法只能接受 Collection、Map、数组、Iterator、Iterable 类型参数");
	}

	public Boolean notEmpty(Object v) {
		return !isEmpty(v);
	}
}

