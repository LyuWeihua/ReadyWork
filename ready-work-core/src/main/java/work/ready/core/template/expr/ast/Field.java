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

import work.ready.core.template.TemplateException;
import work.ready.core.template.stat.Location;
import work.ready.core.template.stat.ParseException;
import work.ready.core.template.stat.Scope;
import work.ready.core.tools.HashUtil;
import work.ready.core.tools.StrUtil;

public class Field extends Expr {

	private Expr expr;
	private String fieldName;
	private String getterName;
	private long getterNameHash;

	public Field(Expr expr, String fieldName, Location location) {
		if (expr == null) {
			throw new ParseException("The object for field access can not be null", location);
		}
		this.expr = expr;
		this.fieldName = fieldName;
		this.getterName = "get" + StrUtil.firstCharToUpperCase(fieldName);

		this.getterNameHash = HashUtil.fnv1a64(getterName);
		this.location = location;
	}

	public Object eval(Scope scope) {
		Object target = expr.eval(scope);
		if (target == null) {
			if (scope.getCtrl().isNullSafe()) {
				return null;
			}
			if (expr instanceof Id) {
				String id = ((Id)expr).getId();
				throw new TemplateException("\"" + id + "\" can not be null for accessed by \"" + id + "." + fieldName + "\"", location);
			}
			throw new TemplateException("Can not accessed by \"" + fieldName + "\" field from null target", location);
		}

		try {
			Class<?> targetClass = target.getClass();
			Object key = FieldKeyBuilder.instance.getFieldKey(targetClass, getterNameHash);
			FieldGetter fieldGetter = FieldKit.getFieldGetter(key, targetClass, fieldName);
			if (fieldGetter.notNull()) {
				return fieldGetter.get(target, fieldName);
			}
		} catch (TemplateException | ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new TemplateException(e.getMessage(), location, e);
		}

		if (scope.getCtrl().isNullSafe()) {
			return null;
		}
		if (expr instanceof Id) {
			String id = ((Id)expr).getId();
			throw new TemplateException("public field not found: \"" + id + "." + fieldName + "\" and public getter method not found: \"" + id + "." + getterName + "()\"", location);
		}
		throw new TemplateException("public field not found: \"" + fieldName + "\" and public getter method not found: \"" + getterName + "()\"", location);
	}

}

