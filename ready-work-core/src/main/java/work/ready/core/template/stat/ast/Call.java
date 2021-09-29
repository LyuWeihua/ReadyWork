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
import work.ready.core.template.TemplateException;
import work.ready.core.template.expr.ast.ExprList;
import work.ready.core.template.io.Writer;
import work.ready.core.template.stat.Scope;

public class Call extends Stat {

	private String funcName;
	private ExprList exprList;
	private boolean callIfDefined;

	public Call(String funcName, ExprList exprList, boolean callIfDefined) {
		this.funcName = funcName;
		this.exprList = exprList;
		this.callIfDefined = callIfDefined;
	}

	public void exec(Env env, Scope scope, Writer writer) {
		Define function = env.getFunction(funcName);
		if (function != null) {
			function.call(env, scope, exprList, writer);	
		} else if (callIfDefined) {
			return ;
		} else {
			throw new TemplateException("Template function not defined: " + funcName, location);
		}
	}
}

