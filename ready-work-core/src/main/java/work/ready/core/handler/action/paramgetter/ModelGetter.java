/**
 * Copyright (c) 2011-2017, 玛雅牛 (myaniu AT gmail.com).
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
package work.ready.core.handler.action.paramgetter;

import work.ready.core.handler.Controller;
import work.ready.core.handler.action.Action;

public class ModelGetter<T> extends ParamGetter<T> {

	private final Class<T> modelClass;
	public ModelGetter(Class<T> modelClass, String parameterName) {
		super(parameterName,null);
		this.modelClass = modelClass;
	}
	@Override
	public T get(Action action, Controller c) {
		return c.getModel(modelClass);
	}
	@Override
	protected T to(String v) {
		return null;
	}
}
