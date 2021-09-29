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

import java.lang.reflect.InvocationTargetException;

public class Method extends Expr {

	private Expr expr;
	private String methodName;
	private ExprList exprList;

	public Method(Expr expr, String methodName, ExprList exprList, Location location) {
		if (exprList == null || exprList.length() == 0) {
			throw new ParseException("The parameter of method can not be blank", location);
		}
		init(expr, methodName, exprList, location);
	}

	public Method(Expr expr, String methodName, Location location) {
		init(expr, methodName, ExprList.NULL_EXPR_LIST, location);
	}

	private void init(Expr expr, String methodName, ExprList exprList, Location location) {
		if (expr == null) {
			throw new ParseException("The target for method invoking can not be blank", location);
		}
		if (MethodKit.isForbiddenMethod(methodName)) {
			throw new ParseException("Forbidden method: " + methodName, location);
		}
		this.expr = expr;
		this.methodName = methodName;
		this.exprList = exprList;
		this.location = location;
	}

	public Object eval(Scope scope) {
		Object target = expr.eval(scope);
		if (target == null) {
			if (scope.getCtrl().isNullSafe()) {
				return null;
			}
			throw new TemplateException("The target for method invoking can not be null, method name: " + methodName, location);
		}

		Object[] argValues = exprList.evalExprList(scope);
		try {

			MethodInfo methodInfo = MethodKit.getMethod(target.getClass(), methodName, argValues);
			if (methodInfo.notNull()) {
				return methodInfo.invoke(target, argValues);
			}

			if (scope.getCtrl().isNullSafe()) {
				return null;
			}
			throw new TemplateException(buildMethodNotFoundSignature("public method not found: " + target.getClass().getName() + ".", methodName, argValues), location);

		} catch (TemplateException | ParseException e) {
			throw e;
		} catch (InvocationTargetException e) {
			Throwable t = e.getTargetException();
			if (t == null) {t = e;}
			throw new TemplateException(t.getMessage(), location, t);
		} catch (Exception e) {
			throw new TemplateException(e.getMessage(), location, e);
		}
	}

	static String buildMethodNotFoundSignature(String preMsg, String methodName, Object[] argValues) {
		StringBuilder ret = new StringBuilder().append(preMsg).append(methodName).append("(");
		if (argValues != null) {
			for (int i = 0; i < argValues.length; i++) {
				if (i > 0) {
					ret.append(", ");
				}
				ret.append(argValues[i] != null ? argValues[i].getClass().getName() : "null");
			}
		}
		return ret.append(")").toString();
	}

}

