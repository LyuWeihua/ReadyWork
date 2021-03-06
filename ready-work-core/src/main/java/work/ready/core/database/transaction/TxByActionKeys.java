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

package work.ready.core.database.transaction;

import work.ready.core.aop.Interceptor;
import work.ready.core.aop.Invocation;
import work.ready.core.server.Ready;

import java.util.HashSet;
import java.util.Set;

public class TxByActionKeys implements Interceptor {

	private Set<String> actionKeySet = new HashSet<String>();

	public TxByActionKeys(String... actionKeys) {
		if (actionKeys == null || actionKeys.length == 0) {
			throw new IllegalArgumentException("actionKeys can not be blank.");
		}
		for (String actionKey : actionKeys) {
			actionKeySet.add(actionKey.trim());
		}
	}

	@Override
	public void intercept(final Invocation inv) throws Throwable {
		if (actionKeySet.contains(inv.getActionKey())) {
			Ready.dbManager().getDb().transaction(() -> {
				inv.invoke();
				return true;
			});
		}
		else {
			inv.invoke();
		}
	}
}

