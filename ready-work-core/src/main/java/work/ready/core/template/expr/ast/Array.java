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

import java.util.ArrayList;
import java.util.List;

public class Array extends Expr {

	private Expr[] exprList;

	public Array(Expr[] exprList, Location location) {
		if (exprList == null) {
			throw new ParseException("exprList can not be null", location);
		}
		this.exprList = exprList;
	}

	public Object eval(Scope scope) {
		List<Object> array = new ArrayListExt(exprList.length);
		for (Expr expr : exprList) {
			array.add(expr.eval(scope));
		}
		return array;
	}

	@SuppressWarnings("serial")
	public static class ArrayListExt extends ArrayList<Object> {

		public ArrayListExt(int initialCapacity) {
			super(initialCapacity);
		}

		public Integer getLength() {
			return size();
		}
	}
}

