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

public class StaticMethod extends Expr {

	private Class<?> clazz;
	private String methodName;
	private ExprList exprList;

	public StaticMethod(String className, String methodName, Location location) {
		init(className, methodName, ExprList.NULL_EXPR_LIST, location);
	}

	public StaticMethod(String className, String methodName, ExprList exprList, Location location) {
		if (exprList == null || exprList.length() == 0) {
			throw new ParseException("exprList can not be blank", location);
		}
		init(className, methodName, exprList, location);
	}

	private void init(String className, String methodName, ExprList exprList, Location location) {
		try {
			this.clazz = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new ParseException("Class not found: " + className, location, e);
		} catch (Exception e) {
			throw new ParseException(e.getMessage(), location, e);
		}

		if (MethodKit.isForbiddenClass(this.clazz)) {
			throw new ParseException("Forbidden class: " + this.clazz.getName(), location);
		}
		if (MethodKit.isForbiddenMethod(methodName)) {
			throw new ParseException("Forbidden method: " + methodName, location);
		}

		this.methodName = methodName;
		this.exprList = exprList;
		this.location = location;
	}

	public Object eval(Scope scope) {
		Object[] argValues = exprList.evalExprList(scope);

		try {
			MethodInfo methodInfo = MethodKit.getMethod(clazz, methodName, argValues);

			if (methodInfo.notNull()) {
				if (methodInfo.isStatic()) {
					return methodInfo.invoke(null, argValues);
				} else {
					throw new TemplateException(Method.buildMethodNotFoundSignature("Not public static method: " + clazz.getName() + "::", methodName, argValues), location);
				}
			} else {

				throw new TemplateException(Method.buildMethodNotFoundSignature("public static method not found: " + clazz.getName() + "::", methodName, argValues), location);
			}

		} catch (TemplateException | ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new TemplateException(e.getMessage(), location, e);
		}
	}
}

