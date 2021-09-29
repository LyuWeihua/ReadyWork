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

import java.math.BigDecimal;
import java.math.BigInteger;

public class Unary extends Expr {

	private Sym op;
	private Expr expr;

	public Unary(Sym op, Expr expr, Location location) {
		if (expr == null) {
			throw new ParseException("The parameter of \"" + op.value() + "\" operator can not be blank", location);
		}
		this.op = op;
		this.expr = expr;
		this.location = location;
	}

	public Object eval(Scope scope) {
		Object value = expr.eval(scope);
		if (value == null) {
			if (scope.getCtrl().isNullSafe()) {
				return null;
			}
			throw new TemplateException("The parameter of \"" + op.value() + "\" operator can not be blank", location);
		}
		if (! (value instanceof Number) ) {
			throw new TemplateException(op.value() + " operator only support int long float double short byte BigDecimal BigInteger type", location);
		}

		switch (op) {
		case ADD:
			return value;
		case SUB:
			Number n = (Number)value;
			if (n instanceof Integer) {
                return Integer.valueOf(-n.intValue());
            }
			if (n instanceof Long) {
                return Long.valueOf(-n.longValue());
            }
			if (n instanceof Float) {
                return Float.valueOf(-n.floatValue());
            }
			if (n instanceof Double) {
                return Double.valueOf(-n.doubleValue());
            }
			if (n instanceof BigDecimal) {
            	return ((BigDecimal)n).negate();
			}
			if (n instanceof BigInteger) {
				return ((BigInteger)n).negate();
			}
			if (n instanceof Short || n instanceof Byte) {

				return Integer.valueOf(-((Number)n).intValue());
			}

			throw new TemplateException("Unsupported data type: " + n.getClass().getName(), location);
		default :
			throw new TemplateException("Unsupported operator: " + op.value(), location);
		}
	}

	public Expr toConstIfPossible() {
		if (expr instanceof Const && (op == Sym.SUB || op == Sym.ADD || op == Sym.NOT)) {
		} else {
			return this;
		}

		Expr ret = this;
		Const c = (Const)expr;
		if (op == Sym.SUB) {
			if (c.isInt()) {
				ret = new Const(Sym.INT, -c.getInt());
			} else if (c.isLong()) {
				ret = new Const(Sym.LONG, -c.getLong());
			} else if (c.isFloat()) {
				ret = new Const(Sym.FLOAT, -c.getFloat());
			} else if (c.isDouble()) {
				ret = new Const(Sym.DOUBLE, -c.getDouble());
			}
		} else if (op == Sym.ADD) {
			if (c.isNumber()) {
				ret = c;
			}
		} else if (op == Sym.NOT) {
			if (c.isBoolean()) {
				ret = c.isTrue() ? Const.FALSE : Const.TRUE;
			}
		}

		return ret;
	}

	public String toString() {
		return op.toString() + expr.toString();
	}
}

