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

public class IntegerExt {

	public Boolean toBoolean(Integer self) {
		return self != 0;
	}

	public Integer toInt(Integer self) {
		return self;
	}

	public Long toLong(Integer self) {
		return self.longValue();
	}

	public Float toFloat(Integer self) {
		return self.floatValue();
	}

	public Double toDouble(Integer self) {
		return self.doubleValue();
	}

	public Short toShort(Integer self) {
		return self.shortValue();
	}

	public Byte toByte(Integer self) {
		return self.byteValue();
	}
}

