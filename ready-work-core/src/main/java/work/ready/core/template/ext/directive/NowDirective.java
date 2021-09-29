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
import work.ready.core.template.TemplateException;
import work.ready.core.template.expr.ast.ExprList;
import work.ready.core.template.io.Writer;
import work.ready.core.template.stat.ParseException;
import work.ready.core.template.stat.Scope;

import java.util.Date;

public class NowDirective extends Directive {

	public void setExprList(ExprList exprList) {
		if (exprList.length() > 1) {
			throw new ParseException("#now directive support one parameter only", location);
		}
		super.setExprList(exprList);
	}

	public void exec(Env env, Scope scope, Writer writer) {
		String datePattern;
		if (exprList.length() == 0) {
			datePattern = env.getEngineConfig().getDatePattern();
		} else {
			Object dp = exprList.eval(scope);
			if (dp instanceof String) {
				datePattern = (String)dp;
			} else {
				throw new TemplateException("The parameter of #now directive must be String", location);
			}
		}

		try {
			writer.write(new Date(), datePattern);
		} catch (Exception e) {
			throw new TemplateException(e.getMessage(), location, e);
		}
	}
}

