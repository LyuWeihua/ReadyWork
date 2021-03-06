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

public class ForCtrl extends Expr {

	private String id;
	private Expr expr;

	private Expr init;
	private Expr cond;
	private Expr update;

	public ForCtrl(Id id, Expr expr, Location location) {
		if (expr == null) {
			throw new ParseException("The iterator target of #for statement can not be null", location);
		}
		this.id = id.getId();
		this.expr = expr;
		this.init = null;
		this.cond = null;
		this.update = null;
		this.location = location;
	}

	public ForCtrl(ExprList init, Expr cond, ExprList update, Location location) {
		this.init = init.getActualExpr();
		this.cond = cond;
		this.update = update.getActualExpr();
		this.id = null;
		this.expr = null;
		this.location = location;
	}

	public boolean isIterator() {
		return id != null;
	}

	public String getId() {
		return id;
	}

	public Expr getExpr() {
		return expr;
	}

	public Expr getInit() {
		return init;
	}

	public Expr getCond() {
		return cond;
	}

	public Expr getUpdate() {
		return update;
	}

	public Object eval(Scope scope) {
		throw new TemplateException("The eval(Scope scope) method can not be invoked", location);
	}
}

