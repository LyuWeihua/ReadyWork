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

import work.ready.core.exception.ApiException;
import work.ready.core.handler.Controller;
import work.ready.core.handler.action.Action;
import work.ready.core.service.status.Status;
import work.ready.core.tools.StrUtil;

public class ShortGetter extends ParamGetter<Short> {

	public ShortGetter(String parameterName, String defaultValue) {
		super(parameterName,defaultValue);
	}

	@Override
	public Short get(Action action, Controller c) {
		String value = c.getParam(this.getParameterName());
		try {
			if (StrUtil.isBlank(value))
				return this.getDefaultValue();
			return to(value.trim());
		}
		catch (Exception e) {
			throw new ApiException(new Status(PARAM_PARSE_EXCEPTION, value, Short.class));
		}
	}

	@Override
	protected Short to(String v) {
		if(StrUtil.notBlank(v)){
			return Short.parseShort(v);
		}
		return null;
	}

}
