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
import work.ready.core.template.expr.ast.Expr;
import work.ready.core.template.expr.ast.ForCtrl;
import work.ready.core.template.expr.ast.Logic;
import work.ready.core.template.io.Writer;
import work.ready.core.template.stat.Ctrl;
import work.ready.core.template.stat.Scope;

import java.util.Iterator;

public class For extends Stat {

	private ForCtrl forCtrl;
	private Stat stat;
	private Stat _else;

	public For(ForCtrl forCtrl, StatList statList, Stat _else) {
		this.forCtrl = forCtrl;
		this.stat = statList.getActualStat();
		this._else = _else;
	}

	public void exec(Env env, Scope scope, Writer writer) {
		scope = new Scope(scope);
		if (forCtrl.isIterator()) {
			forIterator(env, scope, writer);
		} else {
			forLoop(env, scope, writer);
		}
	}

	private void forIterator(Env env, Scope scope, Writer writer) {
		Ctrl ctrl = scope.getCtrl();
		Object outer = scope.get("for");
		ctrl.setLocalAssignment();
		ForIteratorStatus forIteratorStatus = new ForIteratorStatus(outer, forCtrl.getExpr().eval(scope), location);
		ctrl.setWisdomAssignment();
		scope.setLocal("for", forIteratorStatus);

		Iterator<?> it = forIteratorStatus.getIterator();
		String itemName = forCtrl.getId();
		while(it.hasNext()) {
			scope.setLocal(itemName, it.next());
			stat.exec(env, scope, writer);
			forIteratorStatus.nextState();

			if (ctrl.isJump()) {
				if (ctrl.isBreak()) {
					ctrl.setJumpNone();
					break ;
				} else if (ctrl.isContinue()) {
					ctrl.setJumpNone();
					continue ;
				} else {
					return ;
				}
			}
		}

		if (_else != null && forIteratorStatus.getIndex() == 0) {
			_else.exec(env, scope, writer);
		}
	}

	private void forLoop(Env env, Scope scope, Writer writer) {
		Ctrl ctrl = scope.getCtrl();
		Object outer = scope.get("for");
		ForLoopStatus forLoopStatus = new ForLoopStatus(outer);
		scope.setLocal("for", forLoopStatus);

		Expr init = forCtrl.getInit();
		Expr cond = forCtrl.getCond();
		Expr update = forCtrl.getUpdate();

		ctrl.setLocalAssignment();
		for (init.eval(scope); cond == null || Logic.isTrue(cond.eval(scope)); update.eval(scope)) {
			ctrl.setWisdomAssignment();
			stat.exec(env, scope, writer);
			ctrl.setLocalAssignment();
			forLoopStatus.nextState();

			if (ctrl.isJump()) {
				if (ctrl.isBreak()) {
					ctrl.setJumpNone();
					break ;
				} else if (ctrl.isContinue()) {
					ctrl.setJumpNone();
					continue ;
				} else {
					ctrl.setWisdomAssignment();
					return ;
				}
			}
		}

		ctrl.setWisdomAssignment();
		if (_else != null && forLoopStatus.getIndex() == 0) {
			_else.exec(env, scope, writer);
		}
	}
}

