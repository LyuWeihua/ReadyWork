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

import work.ready.core.template.EngineConfig;
import work.ready.core.template.Env;
import work.ready.core.template.expr.ast.Assign;
import work.ready.core.template.expr.ast.Const;
import work.ready.core.template.expr.ast.Expr;
import work.ready.core.template.expr.ast.ExprList;
import work.ready.core.template.io.Writer;
import work.ready.core.template.source.TemplateSource;
import work.ready.core.template.stat.*;

public class Include extends Stat {

	private Assign[] assignArray;
	private Stat stat;

	public Include(Env env, ExprList exprList, String parentFileName, Location location) {
		int len = exprList.length();
		if (len == 0) {
			throw new ParseException("The parameter of #include directive can not be blank", location);
		}
		
		Expr expr = exprList.getExpr(0);
		if (expr instanceof Const && ((Const)expr).isStr()) {
		} else {
			throw new ParseException("The first parameter of #include directive must be String, or use the #render directive", location);
		}
		
		if (len > 1) {
			for (int i = 1; i < len; i++) {
				if (!(exprList.getExpr(i) instanceof Assign)) {
					throw new ParseException("The " + (i + 1) + "th parameter of #include directive must be an assignment expression", location);
				}
			}
		}

		parseSubTemplate(env, ((Const)expr).getStr(), parentFileName, location);
		getAssignExpression(exprList);
	}

	private void parseSubTemplate(Env env, String fileName, String parentFileName, Location location) {
		String subFileName = getSubFileName(fileName, parentFileName);
		EngineConfig config = env.getEngineConfig();
		
		TemplateSource fileSource = config.getSourceFactory().getSource(config.getBaseTemplatePath(), subFileName, config.getEncoding());
		try {
			Parser parser = new Parser(env, fileSource.getContent(), subFileName);
			if (config.isDevMode()) {
				env.addSource(fileSource);
			}
			this.stat = parser.parse().getActualStat();
		} catch (Exception e) {
			
			throw new ParseException(e.getMessage(), location, e);
		}
	}

	public static String getSubFileName(String fileName, String parentFileName) {
		if (parentFileName == null) {
			return fileName;
		}
		if (fileName.startsWith("/")) {
			return fileName;
		}
		int index = parentFileName.lastIndexOf('/');
		if (index == -1) {
			return fileName;
		}
		return parentFileName.substring(0, index + 1) + fileName;
	}

	private void getAssignExpression(ExprList exprList) {
		int len = exprList.length();
		if (len > 1) {
			assignArray = new Assign[len - 1];
			for (int i = 0; i < assignArray.length; i++) {
				assignArray[i] = (Assign)exprList.getExpr(i + 1);
			}
		} else {
			assignArray = null;
		}
	}

	public void exec(Env env, Scope scope, Writer writer) {
		scope = new Scope(scope);
		if (assignArray != null) {
			evalAssignExpression(scope);
		}
		stat.exec(env, scope, writer);
		scope.getCtrl().setJumpNone();
	}

	private void evalAssignExpression(Scope scope) {
		Ctrl ctrl = scope.getCtrl();
		try {
			ctrl.setLocalAssignment();
			for (Assign assign : assignArray) {
				assign.eval(scope);
			}
		} finally {
			ctrl.setWisdomAssignment();
		}
	}
}

