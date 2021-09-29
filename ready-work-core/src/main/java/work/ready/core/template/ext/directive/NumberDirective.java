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

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class NumberDirective extends Directive {

	private Expr valueExpr;
	private Expr patternExpr;

	public void setExprList(ExprList exprList) {
		int paraNum = exprList.length();
		if (paraNum == 0) {
			throw new ParseException("The parameter of #number directive can not be blank", location);
		}
		if (paraNum > 2) {
			throw new ParseException("Wrong number parameter of #number directive, two parameters allowed at most", location);
		}

		valueExpr = exprList.getExpr(0);
		patternExpr = (paraNum == 1 ? null : exprList.getExpr(1));
	}

	public void exec(Env env, Scope scope, Writer writer) {
		Object value = valueExpr.eval(scope);
		if (value == null) {
			return ;
		}

		RoundingMode roundingMode = env.getEngineConfig().getRoundingMode();
		if (patternExpr == null) {
			outputWithoutPattern(value, roundingMode, writer);
		} else {
			outputWithPattern(value, roundingMode, scope, writer);
		}
	}

	private void outputWithoutPattern(Object value, RoundingMode roundingMode, Writer writer) {
		DecimalFormat df = new DecimalFormat();
		df.setRoundingMode(roundingMode);

		String ret = df.format(value);
		write(writer, ret);
	}

	private void outputWithPattern(Object value, RoundingMode roundingMode, Scope scope, Writer writer) {
		Object pattern = patternExpr.eval(scope);
		if ( !(pattern instanceof String) ) {
			throw new TemplateException("The sencond parameter pattern of #number directive must be String", location);
		}

		DecimalFormat df = new DecimalFormat((String)pattern);
		df.setRoundingMode(roundingMode);

		String ret = df.format(value);
		write(writer, ret);
	}
}

