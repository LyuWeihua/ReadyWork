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
import work.ready.core.template.expr.ast.Logic;
import work.ready.core.template.io.Writer;
import work.ready.core.template.stat.Location;
import work.ready.core.template.stat.ParseException;
import work.ready.core.template.stat.Scope;

public class ElseIf extends Stat {

	private Expr cond;
	private Stat stat;
	private Stat elseIfOrElse;

	public ElseIf(ExprList cond, StatList statList, Location location) {
		if (cond.length() == 0) {
			throw new ParseException("The condition expression of #else if statement can not be blank", location);
		}
		this.cond = cond.getActualExpr();
		this.stat = statList.getActualStat();
	}

	public void setStat(Stat elseIfOrElse) {
		this.elseIfOrElse = elseIfOrElse;
	}

	public void exec(Env env, Scope scope, Writer writer) {
		if (Logic.isTrue(cond.eval(scope))) {
			stat.exec(env, scope, writer);
		} else if (elseIfOrElse != null) {
			elseIfOrElse.exec(env, scope, writer);
		}
	}
}

