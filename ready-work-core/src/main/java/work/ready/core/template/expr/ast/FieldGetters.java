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

import work.ready.core.database.Model;
import work.ready.core.database.Record;
import work.ready.core.tools.StrUtil;

import java.lang.reflect.Array;

public class FieldGetters {

	public static class NullFieldGetter extends FieldGetter {

		public static final NullFieldGetter me = new NullFieldGetter();

		public boolean notNull() {
			return false;
		}

		public FieldGetter takeOver(Class<?> targetClass, String fieldName) {
			throw new RuntimeException("The method takeOver(Class, String) of NullFieldGetter should not be invoked");
		}

		public Object get(Object target, String fieldName) throws Exception {
			throw new RuntimeException("The method get(Object, String) of NullFieldGetter should not be invoked");
		}
	}

	public static class GetterMethodFieldGetter extends FieldGetter {

		protected java.lang.reflect.Method getterMethod;

		public GetterMethodFieldGetter(java.lang.reflect.Method getterMethod) {
			this.getterMethod = getterMethod;
		}

		public FieldGetter takeOver(Class<?> targetClass, String fieldName) {
			if (MethodKit.isForbiddenClass(targetClass)) {
				throw new RuntimeException("Forbidden class: " + targetClass.getName());
			}

			String getterName = "get" + StrUtil.firstCharToUpperCase(fieldName);
			java.lang.reflect.Method[] methodArray = targetClass.getMethods();
			for (java.lang.reflect.Method method : methodArray) {
				if (method.getName().equals(getterName) && method.getParameterCount() == 0) {

					return new GetterMethodFieldGetter(method);
				}
			}

			return null;
		}

		public Object get(Object target, String fieldName) throws Exception {
			return getterMethod.invoke(target, ExprList.NULL_OBJECT_ARRAY);
		}

		public String toString() {
			return getterMethod.toString();
		}
	}

	public static class IsMethodFieldGetter extends FieldGetter {

		protected java.lang.reflect.Method isMethod;

		public IsMethodFieldGetter() {
		}

		public IsMethodFieldGetter(java.lang.reflect.Method isMethod) {
			this.isMethod = isMethod;
		}

		public FieldGetter takeOver(Class<?> targetClass, String fieldName) {
			if (MethodKit.isForbiddenClass(targetClass)) {
				throw new RuntimeException("Forbidden class: " + targetClass.getName());
			}

			String isMethodName = "is" + StrUtil.firstCharToUpperCase(fieldName);
			java.lang.reflect.Method[] methodArray = targetClass.getMethods();
			for (java.lang.reflect.Method method : methodArray) {
				if (method.getName().equals(isMethodName) && method.getParameterCount() == 0) {
					Class<?> returnType = method.getReturnType();
					if (returnType == Boolean.class || returnType == boolean.class) {
						return new IsMethodFieldGetter(method);
					}
				}
			}

			return null;
		}

		public Object get(Object target, String fieldName) throws Exception {
			return isMethod.invoke(target, ExprList.NULL_OBJECT_ARRAY);
		}

		public String toString() {
			return isMethod.toString();
		}
	}

	public static class ModelFieldGetter extends FieldGetter {

		static final ModelFieldGetter singleton = new ModelFieldGetter();

		public FieldGetter takeOver(Class<?> targetClass, String fieldName) {
			if (Model.class.isAssignableFrom(targetClass)) {
				return singleton;
			} else {
				return null;
			}
		}

		public Object get(Object target, String fieldName) throws Exception {
			return ((Model<?>)target).get(fieldName);
		}
	}

	public static class RecordFieldGetter extends FieldGetter {

		static final RecordFieldGetter singleton = new RecordFieldGetter();

		public FieldGetter takeOver(Class<?> targetClass, String fieldName) {
			if (Record.class.isAssignableFrom(targetClass)) {
				return singleton;
			} else {
				return null;
			}
		}

		public Object get(Object target, String fieldName) throws Exception {
			return ((Record)target).get(fieldName);
		}
	}

	public static class MapFieldGetter extends FieldGetter {

		static final MapFieldGetter singleton = new MapFieldGetter();

		public FieldGetter takeOver(Class<?> targetClass, String fieldName) {
			if (java.util.Map.class.isAssignableFrom(targetClass)) {
				return singleton;
			} else {
				return null;
			}
		}

		public Object get(Object target, String fieldName) throws Exception {
			return ((java.util.Map<?, ?>)target).get(fieldName);
		}
	}

	public static class RealFieldGetter extends FieldGetter {

		protected java.lang.reflect.Field field;

		public RealFieldGetter(java.lang.reflect.Field field) {
			this.field = field;
		}

		public FieldGetter takeOver(Class<?> targetClass, String fieldName) {
			java.lang.reflect.Field[] fieldArray = targetClass.getFields();
			for (java.lang.reflect.Field field : fieldArray) {
				if (field.getName().equals(fieldName)) {
					return new RealFieldGetter(field);
				}
			}

			return null;
		}

		public Object get(Object target, String fieldName) throws Exception {
			return field.get(target);
		}

		public String toString() {
			return field.toString();
		}
	}

	public static class ArrayLengthGetter extends FieldGetter {

		static final ArrayLengthGetter singleton = new ArrayLengthGetter();

		public FieldGetter takeOver(Class<?> targetClass, String fieldName) {
			if ("length".equals(fieldName) && targetClass.isArray()) {
				return singleton;
			} else {
				return null;
			}
		}

		public Object get(Object target, String fieldName) throws Exception {
			return Array.getLength(target);
		}
	}
}

