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

import java.util.ArrayList;
import java.util.List;

public class IntegerArrayGetter extends ParamGetter<Integer[]> {

	public IntegerArrayGetter(String parameterName, String defaultValue) {
		super(parameterName,defaultValue);
	}

	@Override
	public Integer[] get(Action action, Controller c) {
		Integer[] ret = c.getParamValuesToInt(getParameterName());
		if( null == ret) {
			ret =  this.getDefaultValue();
		}
		return ret;
	}

	@Override
	protected Integer[] to(String v) {
		if(StrUtil.notBlank(v)){
			String[] ss = v.split(",");
			List<Integer> ls = new ArrayList<Integer>(ss.length);
			for(String s : ss){
				if(StrUtil.notBlank(s)){
					ls.add(Integer.parseInt(s.trim()));
				}
			}
			return ls.toArray(new Integer[0]);
		}
		return null;
	}
}
