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
import work.ready.core.tools.converter.Converters;

import java.sql.Date;
import java.text.ParseException;

public class SqlDateGetter extends ParamGetter<Date> {
	private static Converters.SqlDateConverter converter = new Converters.SqlDateConverter();
	public SqlDateGetter(String parameterName, String defaultValue) {
		super(parameterName, defaultValue);
	}

	@Override
	public java.sql.Date get(Action action, Controller c) {
		String value = c.getParam(this.getParameterName());
		if(StrUtil.notBlank(value)){
			return to(value);
		}
		return this.getDefaultValue();
	}

	@Override
	protected java.sql.Date to(String v) {
		if(StrUtil.isBlank(v)){
			return null;
		}
		try {
			return converter.convert(v);
		} catch (ParseException e) {
			throw new ApiException(new Status(PARAM_PARSE_EXCEPTION, v, java.sql.Date.class));
		}
	}

}
