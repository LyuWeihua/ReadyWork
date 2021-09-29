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
import work.ready.core.template.EngineConfig;
import work.ready.core.template.Env;
import work.ready.core.template.TemplateException;
import work.ready.core.template.expr.ast.Assign;
import work.ready.core.template.expr.ast.ExprList;
import work.ready.core.template.io.Writer;
import work.ready.core.template.source.TemplateSource;
import work.ready.core.template.stat.Ctrl;
import work.ready.core.template.stat.ParseException;
import work.ready.core.template.stat.Parser;
import work.ready.core.template.stat.Scope;
import work.ready.core.template.stat.ast.Define;
import work.ready.core.template.stat.ast.Include;
import work.ready.core.template.stat.ast.Stat;
import work.ready.core.template.stat.ast.StatList;
import work.ready.core.tools.define.SyncWriteMap;

import java.util.Map;

public class RenderDirective extends Directive {

	private String parentFileName;
	private Map<String, SubStat> subStatCache = new SyncWriteMap<String, SubStat>(16, 0.5F);

	public void setExprList(ExprList exprList) {
		int len = exprList.length();
		if (len == 0) {
			throw new ParseException("The parameter of #render directive can not be blank", location);
		}
		if (len > 1) {
			for (int i = 1; i < len; i++) {
				if (!(exprList.getExpr(i) instanceof Assign)) {
					throw new ParseException("The " + (i + 1) + "th parameter of #render directive must be an assignment expression", location);
				}
			}
		}

		this.parentFileName = location.getTemplateFile();
		this.exprList = exprList;
	}

	private Object evalAssignExpressionAndGetFileName(Scope scope) {
		Ctrl ctrl = scope.getCtrl();
		try {
			ctrl.setLocalAssignment();
			return exprList.evalExprList(scope)[0];
		} finally {
			ctrl.setWisdomAssignment();
		}
	}

	public void exec(Env env, Scope scope, Writer writer) {
		
		scope = new Scope(scope);

		Object value = evalAssignExpressionAndGetFileName(scope);
		if (!(value instanceof String)) {
			throw new TemplateException("The parameter value of #render directive must be String", location);
		}

		String subFileName = Include.getSubFileName((String)value, parentFileName);
		SubStat subStat = subStatCache.get(subFileName);
		if (subStat == null) {
			subStat = parseSubStat(env, subFileName);
			subStatCache.put(subFileName, subStat);
		} else if (env.isDevMode()) {
			
			if (subStat.source.isModified() || subStat.env.isSourceListModified()) {
				subStat = parseSubStat(env, subFileName);
				subStatCache.put(subFileName, subStat);
			}
		}

		subStat.exec(null, scope, writer);	

		scope.getCtrl().setJumpNone();
	}

	private SubStat parseSubStat(Env env, String subFileName) {
		EngineConfig config = env.getEngineConfig();
		
		TemplateSource subFileSource = config.getSourceFactory().getSource(config.getBaseTemplatePath(), subFileName, config.getEncoding());

		try {
			SubEnv subEnv = new SubEnv(env);
			StatList subStatList = new Parser(subEnv, subFileSource.getContent(), subFileName).parse();
			return new SubStat(subEnv, subStatList.getActualStat(), subFileSource);
		} catch (Exception e) {
			throw new ParseException(e.getMessage(), location, e);
		}
	}

	public static class SubStat extends Stat {
		public SubEnv env;
		public Stat stat;
		public TemplateSource source;

		public SubStat(SubEnv env, Stat stat, TemplateSource source) {
			this.env = env;
			this.stat = stat;
			this.source = source;
		}

		@Override
		public void exec(Env env, Scope scope, Writer writer) {
			stat.exec(this.env, scope, writer);
		}
	}

	public static class SubEnv extends Env {
		public Env parentEnv;

		public SubEnv(Env parentEnv) {
			super(parentEnv.getEngineConfig());
			this.parentEnv = parentEnv;
		}

		@Override
		public Define getFunction(String functionName) {
			Define func = functionMap.get(functionName);
			return func != null ? func : parentEnv.getFunction(functionName);
		}
	}
}

