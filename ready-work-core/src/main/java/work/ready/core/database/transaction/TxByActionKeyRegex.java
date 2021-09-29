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
import work.ready.core.tools.StrUtil;

import java.util.regex.Pattern;

public class TxByActionKeyRegex implements Interceptor {

	private Pattern pattern;

	public TxByActionKeyRegex(String regex) {
		this(regex, true);
	}

	public TxByActionKeyRegex(String regex, boolean caseSensitive) {
		if (StrUtil.isBlank(regex)) {
			throw new IllegalArgumentException("regex can not be blank.");
		}
		pattern = caseSensitive ? Pattern.compile(regex) : Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
	}

	@Override
	public void intercept(final Invocation inv) throws Throwable {
		if (pattern.matcher(inv.getActionKey()).matches()) {
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

