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

package work.ready.core.database.sql;

import work.ready.core.database.SqlParam;
import work.ready.core.template.Directive;
import work.ready.core.template.Env;
import work.ready.core.template.TemplateException;
import work.ready.core.template.expr.ast.Const;
import work.ready.core.template.expr.ast.Expr;
import work.ready.core.template.expr.ast.ExprList;
import work.ready.core.template.expr.ast.Id;
import work.ready.core.template.io.Writer;
import work.ready.core.template.stat.ParseException;
import work.ready.core.template.stat.Scope;

public class ParamDirective extends Directive {

	private int index = -1;
	private String paramName = null;
	private static boolean checkParamAssigned = true;

	public static void setCheckParamAssigned(boolean checkParamAssigned) {
		ParamDirective.checkParamAssigned = checkParamAssigned;
	}

	@Override
	public void setExprList(ExprList exprList) {
		if (exprList.length() == 0) {
			throw new ParseException("The parameter of #param directive can not be blank", location);
		}

		if (exprList.length() == 1) {
			Expr expr = exprList.getExpr(0);
			if (expr instanceof Const && ((Const)expr).isInt()) {
				index = ((Const)expr).getInt();
				if (index < 0) {
					throw new ParseException("The index of param array must be greater than -1", location);
				}
			}
		}

		if (checkParamAssigned && exprList.getLastExpr() instanceof Id) {
			Id id = (Id)exprList.getLastExpr();
			paramName = id.getId();
		}

		this.exprList = exprList;
	}

	@Override
	public void exec(Env env, Scope scope, Writer writer) {
		SqlParam sqlParam = (SqlParam)scope.get(SqlKit.SQL_PARAM_KEY);
		if (sqlParam == null) {
			throw new TemplateException("#param directive invoked by getSqlParam(...) method only", location);
		}

		write(writer, "?");
		if (index == -1) {

			if (checkParamAssigned && paramName != null && !scope.exists(paramName)) {
				throw new TemplateException("The parameter \""+ paramName +"\" must be assigned", location);
			}

			sqlParam.addParam(exprList.eval(scope));
		} else {
			Object[] params = (Object[])scope.get(SqlKit.PARAM_ARRAY_KEY);
			if (params == null) {
				throw new TemplateException("The #param(" + index + ") directive must invoked by getSqlParam(String, Object...) method", location);
			}
			if (index >= params.length) {
				throw new TemplateException("The index of #param directive is out of bounds: " + index, location);
			}
			sqlParam.addParam(params[index]);
		}
	}
}

