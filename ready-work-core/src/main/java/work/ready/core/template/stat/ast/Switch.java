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
import work.ready.core.template.expr.ast.Expr;
import work.ready.core.template.expr.ast.ExprList;
import work.ready.core.template.io.Writer;
import work.ready.core.template.stat.Location;
import work.ready.core.template.stat.ParseException;
import work.ready.core.template.stat.Scope;

public class Switch extends Stat implements CaseSetter {

	private Expr expr;
	private Case nextCase;
	private Default _default;

	public Switch(ExprList exprList, Location location) {
		if (exprList.length() == 0) {
			throw new ParseException("The parameter of #switch directive can not be blank", location);
		}
		this.expr = exprList.getActualExpr();
	}

	public void setNextCase(Case nextCase) {
		this.nextCase = nextCase;
	}

	public void setDefault(Default _default, Location location) {
		if (this._default != null) {
			throw new ParseException("The #default case of #switch is already defined", location);
		}
		this._default = _default;
	}

	public void exec(Env env, Scope scope, Writer writer) {
		Object switchValue = expr.eval(scope);

		if (nextCase != null && nextCase.execIfMatch(switchValue, env, scope, writer)) {
			return ;
		}

		if (_default != null) {
			_default.exec(env, scope, writer);
		}
	}

	public boolean hasEnd() {
		return true;
	}
}

