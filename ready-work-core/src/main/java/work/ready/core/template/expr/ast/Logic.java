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
import work.ready.core.template.expr.Sym;
import work.ready.core.template.stat.Location;
import work.ready.core.template.stat.ParseException;
import work.ready.core.template.stat.Scope;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class Logic extends Expr {

	private Sym op;
	private Expr left;		
	private Expr right;

	public Logic(Sym op, Expr left, Expr right, Location location) {
		if (left == null) {
			throw new ParseException("The target of \"" + op.value() + "\" operator on the left side can not be blank", location);
		}
		if (right == null) {
			throw new ParseException("The target of \"" + op.value() + "\" operator on the right side can not be blank", location);
		}
		this.op = op;
		this.left = left;
		this.right = right;
		this.location = location;
	}

	public Logic(Sym op, Expr right, Location location) {
		if (right == null) {
			throw new ParseException("The target of \"" + op.value() + "\" operator on the right side can not be blank", location);
		}
		this.op = op;
		this.left = null;
		this.right = right;
		this.location = location;
	}

	public Object eval(Scope scope) {
		switch (op) {
		case NOT:
			return evalNot(scope);
		case AND:
			return evalAnd(scope);
		case OR:
			return evalOr(scope);
		default:
			throw new TemplateException("Unsupported operator: " + op.value(), location);
		}
	}

	Object evalNot(Scope scope) {
		return ! isTrue(right.eval(scope));
	}

	Object evalAnd(Scope scope) {
		return isTrue(left.eval(scope)) && isTrue(right.eval(scope));
	}

	Object evalOr(Scope scope) {
		return isTrue(left.eval(scope)) || isTrue(right.eval(scope));
	}

	public static boolean isTrue(Object v) {
		if (v == null) {
			return false;
		}

		if (v instanceof Boolean) {
			return (Boolean)v;
		}

		if (v instanceof CharSequence) {
			return ((CharSequence)v).length() > 0;
		}

		return true;
	}

	public static boolean isFalse(Object v) {
		return !isTrue(v);
	}
}

