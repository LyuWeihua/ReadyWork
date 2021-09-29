/**
 * Copyright (c) 2011-2019, 玛雅牛 (myaniu AT gmail.com).
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

public class ParamProcessor implements IParamGetter<Object[]> {

	private int fileParamIndex = -1;
	private IParamGetter<?>[] paramGetters;

	public ParamProcessor(int paramCount) {
		paramGetters = paramCount > 0 ? new IParamGetter<?>[paramCount] : null;
	}

	public void addParamGetter(int index, IParamGetter<?> paramGetter) {
		
		if (	fileParamIndex == -1 &&
				(paramGetter instanceof FileGetter || paramGetter instanceof UploadFileGetter)) {
			fileParamIndex = index;
		}

		paramGetters[index] = paramGetter;
	}

	@Override
	public Object[] get(Action action, Controller c) {
		int len = paramGetters.length;
		Object[] ret = new Object[len];

		if (fileParamIndex == -1) {
			for (int i=0; i<len; i++) {
				ret[i] = paramGetters[i].get(action, c);
			}
			return ret;
		}

		Object fileRet = paramGetters[fileParamIndex].get(action, c);
		for (int i=0; i<len; i++) {
			if (i != fileParamIndex) {
				ret[i] = paramGetters[i].get(action, c);
			} else {
				ret[i] = fileRet;
			}
		}
		return ret;
	}
}

