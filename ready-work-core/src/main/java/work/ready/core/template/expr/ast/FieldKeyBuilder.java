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

public abstract class FieldKeyBuilder {

	public abstract Object getFieldKey(Class<?> targetClass, long fieldFnv1a64Hash);

	static FieldKeyBuilder instance = new StrictFieldKeyBuilder();

	public static FieldKeyBuilder getInstance() {
		return instance;
	}

	public static void setFastFieldKeyBuilder(boolean enable) {
		if (enable) {
			instance = new FastFieldKeyBuilder();
		} else {
			instance = new StrictFieldKeyBuilder();
		}
	}

	public static void setFieldKeyBuilder(FieldKeyBuilder fieldKeyBuilder) {
		if (fieldKeyBuilder == null) {
			throw new IllegalArgumentException("fieldKeyBuilder can not be null");
		}
		instance = fieldKeyBuilder;
	}

	public static class FastFieldKeyBuilder extends FieldKeyBuilder {
		public Object getFieldKey(Class<?> targetClass, long fieldFnv1a64Hash) {
			return targetClass.getName().hashCode() ^ fieldFnv1a64Hash;
		}
	}

	public static class StrictFieldKeyBuilder extends FieldKeyBuilder {
		public Object getFieldKey(Class<?> targetClass, long fieldFnv1a64Hash) {
			return new FieldKey(targetClass.getName().hashCode(), fieldFnv1a64Hash);
		}
	}

	public static class FieldKey {

		final int classHash;
		final long fieldHash;

		public FieldKey(int classHash, long fieldHash) {
			this.classHash = classHash;
			this.fieldHash = fieldHash;
		}

		public int hashCode() {
			return classHash ^ (int)fieldHash;
		}

		public boolean equals(Object fieldKey) {
			FieldKey fk = (FieldKey)fieldKey;
			return classHash == fk.classHash && fieldHash == fk.fieldHash;
		}

		public String toString() {
			return "classHash = " + classHash + "\nfieldHash = " + fieldHash;
		}
	}
}

