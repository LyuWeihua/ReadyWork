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
import work.ready.core.template.expr.ast.Const;
import work.ready.core.template.expr.ast.Expr;
import work.ready.core.template.expr.ast.ExprList;
import work.ready.core.template.expr.ast.Id;
import work.ready.core.template.io.CharWriter;
import work.ready.core.template.io.FastStringWriter;
import work.ready.core.template.io.Writer;
import work.ready.core.template.stat.ParseException;
import work.ready.core.template.stat.Scope;

public class StringDirective extends Directive {

	private String name;
	private boolean isLocalAssignment = false;

	public void setExprList(ExprList exprList) {
		Expr[] exprArray = exprList.getExprArray();
		if (exprArray.length == 0) {
			throw new ParseException("#string directive parameter cant not be null", location);
		}
		if (exprArray.length > 2) {
			throw new ParseException("wrong number of #string directive parameter, two parameters allowed at most", location);
		}

		if (!(exprArray[0] instanceof Id)) {
			throw new ParseException("#string first parameter must be identifier", location);
		}
		this.name = ((Id)exprArray[0]).getId();
		if (exprArray.length == 2) {
			if (exprArray[1] instanceof Const) {
				if (((Const)exprArray[1]).isBoolean()) {
					this.isLocalAssignment = ((Const)exprArray[1]).getBoolean();
				} else {
					throw new ParseException("#string sencond parameter must be boolean", location);
				}
			}
		}
	}

	public void exec(Env env, Scope scope, Writer writer) {
		CharWriter charWriter = new CharWriter(64);
		FastStringWriter fsw = new FastStringWriter();
		charWriter.init(fsw);
		try {
			stat.exec(env, scope, charWriter);
		} finally {
			charWriter.close();
		}

		if (this.isLocalAssignment) {
			scope.setLocal(name, fsw.toString());
		} else {
			scope.set(name, fsw.toString());
		}
	}

	public boolean hasEnd() {
		return true;
	}
}

