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
import work.ready.core.template.io.Writer;
import work.ready.core.template.stat.Scope;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class RandomDirective extends Directive {

	public void exec(Env env, Scope scope, Writer writer) {
		try {
			writer.write(ThreadLocalRandom.current().nextInt());
		} catch (IOException e) {
			throw new TemplateException(e.getMessage(), location, e);
		}
	}
}

