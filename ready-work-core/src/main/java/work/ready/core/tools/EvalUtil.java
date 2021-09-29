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

package work.ready.core.tools;

import work.ready.core.template.Directive;
import work.ready.core.template.Engine;
import work.ready.core.template.Env;
import work.ready.core.template.Template;
import work.ready.core.template.io.Writer;
import work.ready.core.template.stat.Scope;
import work.ready.core.tools.define.Kv;

import java.util.Map;

public class EvalUtil {

	private static Engine engine = new Engine();
	private static final String RETURN_VALUE_KEY = "_RETURN_VALUE_";

	static {
		engine.addDirective("eval", InnerEvalDirective.class);
	}

	public static Engine getEngine() {
		return engine;
	}

	public static <T> T eval(String expr) {
		return eval(expr, Kv.create());
	}

	@SuppressWarnings("unchecked")
	public static <T> T eval(String expr, Map<?, ?> data) {
		String stringTemplate = "#eval(" + expr + ")";
		Template template = engine.getTemplateByString(stringTemplate);
		template.render(data, (java.io.Writer)null);
		return (T)data.get(RETURN_VALUE_KEY);
	}

	public static class InnerEvalDirective extends Directive {
		public void exec(Env env, Scope scope, Writer writer) {
			Object value = exprList.eval(scope);
			scope.set(RETURN_VALUE_KEY, value);
		}
	}
}

