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

import work.ready.core.template.stat.Scope;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class Map extends Expr {

	private LinkedHashMap<Object, Expr> map;

	public Map(LinkedHashMap<Object, Expr> map) {
		this.map = map;
	}

	public Object eval(Scope scope) {
		LinkedHashMap<Object, Object> valueMap = new LinkedHashMap<Object, Object>(map.size());
		for (Entry<Object, Expr> e : map.entrySet()) {
			valueMap.put(e.getKey(), e.getValue().eval(scope));
		}
		return valueMap;
	}
}

