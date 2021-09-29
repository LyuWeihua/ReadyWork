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
import work.ready.core.template.expr.ast.Expr;
import work.ready.core.template.expr.ast.ExprList;
import work.ready.core.template.io.Writer;
import work.ready.core.template.stat.ParseException;
import work.ready.core.template.stat.Scope;

import java.time.temporal.Temporal;
import java.util.Date;

public class DateDirective extends Directive {

	private Expr dateExpr;
	private Expr patternExpr;

	public void setExprList(ExprList exprList) {
		int paraNum = exprList.length();
		if (paraNum == 0) {
			this.dateExpr = null;
			this.patternExpr = null;
		} else if (paraNum == 1) {
			this.dateExpr = exprList.getExpr(0);
			this.patternExpr = null;
		} else if (paraNum == 2) {
			this.dateExpr = exprList.getExpr(0);
			this.patternExpr = exprList.getExpr(1);
		} else {
			throw new ParseException("Wrong number parameter of #date directive, two parameters allowed at most", location);
		}
	}

	public void exec(Env env, Scope scope, Writer writer) {
		Object date;
		String pattern;

		if (dateExpr != null) {
			date = dateExpr.eval(scope);
		} else {
			date = new Date();
		}

		if (patternExpr != null) {
			Object temp = patternExpr.eval(scope);
			if (temp instanceof String) {
				pattern = (String)temp;
			} else {
				throw new TemplateException("The second parameter datePattern of #date directive must be String", location);
			}
		} else {
			pattern = env.getEngineConfig().getDatePattern();
		}

		write(date, pattern, writer);
	}

	private void write(Object date, String pattern, Writer writer) {
		try {

			if (date instanceof Date) {
				writer.write((Date)date, pattern);
			} else if (date instanceof Temporal) {		
				writer.write((Temporal)date, pattern);
			} else if (date != null) {
				throw new TemplateException("The first parameter of #date directive can not be " + date.getClass().getName(), location);
			}

		} catch (TemplateException e) {
			throw e;
		} catch (Exception e) {
			throw new TemplateException(e.getMessage(), location, e);
		}
	}
}

