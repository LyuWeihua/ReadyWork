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

import work.ready.core.template.expr.ast.FieldGetters.*;
import work.ready.core.tools.define.SyncWriteMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class FieldKit {

	private static FieldGetter[] getters = init();

	private static final HashMap<Object, FieldGetter> fieldGetterCache = new SyncWriteMap<>(1024, 0.25F);

	private static FieldGetter[] init() {
		LinkedList<FieldGetter> ret = new LinkedList<FieldGetter>();

		ret.addLast(new GetterMethodFieldGetter(null));
		ret.addLast(new RealFieldGetter(null));
		ret.addLast(new ModelFieldGetter());
		ret.addLast(new RecordFieldGetter());
		ret.addLast(new MapFieldGetter());

		ret.addLast(new ArrayLengthGetter());

		return ret.toArray(new FieldGetter[ret.size()]);
	}

	public static FieldGetter getFieldGetter(Object key, Class<?> targetClass, String fieldName) {
		FieldGetter fieldGetter = fieldGetterCache.get(key);
		if (fieldGetter == null) {
			fieldGetter = doGetFieldGetter(targetClass, fieldName);	
			fieldGetterCache.putIfAbsent(key, fieldGetter);
		}
		return fieldGetter;
	}

	private static FieldGetter doGetFieldGetter(Class<?> targetClass, String fieldName) {
		FieldGetter ret;
		for (FieldGetter fieldGetter : getters) {
			ret = fieldGetter.takeOver(targetClass, fieldName);
			if (ret != null) {
				return ret;
			}
		}
		return NullFieldGetter.me;
	}

	public static void addFieldGetter(int index, FieldGetter fieldGetter) {
		addFieldGetter(fieldGetter, index, true);
	}

	public static void addFieldGetterToLast(FieldGetter fieldGetter) {
		addFieldGetter(fieldGetter, null, true);
	}

	public static void addFieldGetterToFirst(FieldGetter fieldGetter) {
		addFieldGetter(fieldGetter, null, false);
	}

	private static synchronized void addFieldGetter(FieldGetter fieldGetter, Integer index, boolean addLast) {
		checkParameter(fieldGetter);

		LinkedList<FieldGetter> ret = getCurrentFieldGetters();
		if (index != null) {
			ret.add(index, fieldGetter);
		} else {
			if (addLast) {
				ret.addLast(fieldGetter);
			} else {
				ret.addFirst(fieldGetter);
			}
		}
		getters = ret.toArray(new FieldGetter[ret.size()]);
	}

	private static LinkedList<FieldGetter> getCurrentFieldGetters() {
		LinkedList<FieldGetter> ret = new LinkedList<FieldGetter>();
		for (FieldGetter fieldGetter : getters) {
			ret.add(fieldGetter);
		}
		return ret;
	}

	private static void checkParameter(FieldGetter fieldGetter) {
		if (fieldGetter == null) {
			throw new IllegalArgumentException("The parameter fieldGetter can not be null");
		}
		for (FieldGetter fg : getters) {
			if (fg.getClass() == fieldGetter.getClass()) {
				throw new RuntimeException("FieldGetter already exists : " + fieldGetter.getClass().getName());
			}
		}
	}

	public static synchronized void removeFieldGetter(Class<? extends FieldGetter> fieldGetterClass) {
		LinkedList<FieldGetter> ret = getCurrentFieldGetters();

		for (Iterator<FieldGetter> it = ret.iterator(); it.hasNext();) {
			if (it.next().getClass() == fieldGetterClass) {
				it.remove();
			}
		}

		getters = ret.toArray(new FieldGetter[ret.size()]);
	}

	public static void clearCache() {
		fieldGetterCache.clear();
	}

	public static synchronized void setFastMode(boolean fastMode) {
		if (fastMode) {
			if ( !contains(FastFieldGetter.class) ) {
				addFieldGetterToFirst(new FastFieldGetter());
			}
		} else {
			if (contains(FastFieldGetter.class)) {
				removeFieldGetter(FastFieldGetter.class);
			}
		}
	}

	public static boolean contains(Class<? extends FieldGetter> fieldGetterClass) {
		for (FieldGetter fg : getters) {
			if (fg.getClass() == fieldGetterClass) {
				return true;
			}
		}
		return false;
	}
}

