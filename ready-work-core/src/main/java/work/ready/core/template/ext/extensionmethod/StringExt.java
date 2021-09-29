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

package work.ready.core.template.ext.extensionmethod;

import work.ready.core.tools.StrUtil;

public class StringExt {

	public Boolean toBoolean(String self) {
		if (StrUtil.isBlank(self)) {
			return null;	
		}

		String value = self.trim().toLowerCase();
		if ("true".equals(value) || "1".equals(value)) {	
			return Boolean.TRUE;
		} else if ("false".equals(value) || "0".equals(value)) {
			return Boolean.FALSE;
		} else {
			throw new RuntimeException("Can not parse to boolean type of value: \"" + self + "\"");
		}
	}

	public Integer toInt(String self) {
		return StrUtil.isBlank(self) ? null : Integer.parseInt(self);
	}

	public Long toLong(String self) {
		return StrUtil.isBlank(self) ? null : Long.parseLong(self);
	}

	public Float toFloat(String self) {
		return StrUtil.isBlank(self) ? null : Float.parseFloat(self);
	}

	public Double toDouble(String self) {
		return StrUtil.isBlank(self) ? null : Double.parseDouble(self);
	}

	public Short toShort(String self) {
		return StrUtil.isBlank(self) ? null : Short.parseShort(self);
	}

	public Byte toByte(String self) {
		return StrUtil.isBlank(self) ? null : Byte.parseByte(self);
	}
}

