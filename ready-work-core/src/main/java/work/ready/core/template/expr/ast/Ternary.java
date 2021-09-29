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

import work.ready.core.template.stat.Location;
import work.ready.core.template.stat.ParseException;
import work.ready.core.template.stat.Scope;

public class Ternary extends Expr {

	private Expr cond;
	private Expr exprOne;
	private Expr exprTwo;

	public Ternary(Expr cond, Expr exprOne, Expr exprTwo, Location location) {
		if (cond == null || exprOne == null || exprTwo == null) {
			throw new ParseException("The parameter of ternary expression can not be blank", location);
		}
		this.cond = cond;
		this.exprOne = exprOne;
		this.exprTwo = exprTwo;
		this.location = location;
	}

	public Object eval(Scope scope) {
		return Logic.isTrue(cond.eval(scope)) ? exprOne.eval(scope) : exprTwo.eval(scope);
	}
}

