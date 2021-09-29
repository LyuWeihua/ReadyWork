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

package work.ready.core.template.ext.directive;

import work.ready.core.template.Directive;
import work.ready.core.template.Env;
import work.ready.core.template.TemplateException;
import work.ready.core.template.expr.ast.Const;
import work.ready.core.template.expr.ast.Expr;
import work.ready.core.template.expr.ast.ExprList;
import work.ready.core.template.io.Writer;
import work.ready.core.template.stat.ParseException;
import work.ready.core.template.stat.Scope;
import work.ready.core.template.stat.ast.Define;

import java.util.ArrayList;

public class CallDirective extends Directive {

	protected Expr funcNameExpr;
	protected ExprList paraExpr;

	protected boolean nullSafe = false;

	public void setExprList(ExprList exprList) {
		int len = exprList.length();
		if (len == 0) {
			throw new ParseException("Template function name required", location);
		}

		int index = 0;
		Expr expr = exprList.getExpr(index);
		if (expr instanceof Const && ((Const)expr).isBoolean()) {
			if (len == 1) {
				throw new ParseException("Template function name required", location);
			}

			nullSafe = ((Const)expr).getBoolean();
			index++;
		}

		funcNameExpr = exprList.getExpr(index++);

		ArrayList<Expr> list = new ArrayList<Expr>();
		for (int i=index; i<len; i++) {
			list.add(exprList.getExpr(i));
		}
		paraExpr = new ExprList(list);
	}

	public void exec(Env env, Scope scope, Writer writer) {
		Object funcNameValue = funcNameExpr.eval(scope);
		if (funcNameValue == null) {
			if (nullSafe) {
				return ;
			}
			throw new TemplateException("Template function name can not be null", location);
		}

		if (!(funcNameValue instanceof String)) {
			throw new TemplateException("Template function name must be String", location);
		}

		Define func = env.getFunction(funcNameValue.toString());

		if (func == null) {
			if (nullSafe) {
				return ;
			}
			throw new TemplateException("Template function not found : " + funcNameValue, location);
		}

		func.call(env, scope, paraExpr, writer);
	}
}

