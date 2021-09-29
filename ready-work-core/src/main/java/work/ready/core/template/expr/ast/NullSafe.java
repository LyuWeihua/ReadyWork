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

import work.ready.core.template.stat.Ctrl;
import work.ready.core.template.stat.Location;
import work.ready.core.template.stat.ParseException;
import work.ready.core.template.stat.Scope;

public class NullSafe extends Expr {

	private Expr left;
	private Expr right;

	public NullSafe(Expr left, Expr right, Location location) {
		if (left == null) {
			throw new ParseException("The expression on the left side of null coalescing and safe access operator \"??\" can not be blank", location);
		}
		this.left = left;
		this.right = right;
		this.location = location;
	}

	public Object eval(Scope scope) {
		Ctrl ctrl = scope.getCtrl();
		boolean oldNullSafeValue = ctrl.isNullSafe();

		try {
			ctrl.setNullSafe(true);
			Object ret = left.eval(scope);
			if (ret != null) {
				return ret;
			}
		} finally {
			ctrl.setNullSafe(oldNullSafeValue);
		}

		return right != null ? right.eval(scope) : null;
	}
}

