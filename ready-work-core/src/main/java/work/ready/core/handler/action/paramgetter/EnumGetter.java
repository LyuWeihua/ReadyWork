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
import work.ready.core.tools.StrUtil;

@SuppressWarnings({"unchecked", "rawtypes"})
public class EnumGetter<T extends Enum> extends ParamGetter<T> {
	private final Class<T> enumType;
	public EnumGetter(Class<T> enumType, String parameterName, String defaultValue) {
		super(parameterName, defaultValue);
		this.enumType = enumType;
	}

	@Override
	public T get(Action action, Controller c) {
		String value = c.getParam(this.getParameterName());
		if(StrUtil.notBlank(value)){
			return to(value);
		}
		return this.getDefaultValue();
	}

	@Override
	protected T to(String v) {
		if(StrUtil.notBlank(v)){
			try{
				return (T) Enum.valueOf(this.enumType, v.trim());
			}catch(Exception e){
				return null;
			}
		}
		return null;
	}
}
