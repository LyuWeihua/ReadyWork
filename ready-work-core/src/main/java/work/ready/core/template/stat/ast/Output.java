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

package work.ready.core.template.stat.ast;

import work.ready.core.template.Env;
import work.ready.core.template.TemplateException;
import work.ready.core.template.expr.ast.Expr;
import work.ready.core.template.expr.ast.ExprList;
import work.ready.core.template.io.Writer;
import work.ready.core.template.stat.Location;
import work.ready.core.template.stat.ParseException;
import work.ready.core.template.stat.Scope;

public class Output extends Stat {

	private Expr expr;

	public Output(ExprList exprList, Location location) {
		if (exprList.length() == 0) {
			throw new ParseException("The expression of output directive like #(expression) can not be blank", location);
		}
		this.expr = exprList.getActualExpr();
	}

	public void exec(Env env, Scope scope, Writer writer) {
		try {
			Object value = expr.eval(scope);

			if (value instanceof String) {
				String str = (String)value;
				writer.write(str, 0, str.length());
			} else if (value instanceof Number) {
				Class<?> c = value.getClass();
				if (c == Integer.class) {
					writer.write((Integer)value);
				} else if (c == Long.class) {
					writer.write((Long)value);
				} else if (c == Double.class) {
					writer.write((Double)value);
				} else if (c == Float.class) {
					writer.write((Float)value);
				} else {
					writer.write(value.toString());
				}
			} else if (value != null) {
				writer.write(value.toString());
			}
		} catch(TemplateException | ParseException e) {
			throw e;
		} catch(Exception e) {
			throw new TemplateException(e.getMessage(), location, e);
		}
	}
}

