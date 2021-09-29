/**
 *
 * Original work Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package work.ready.core.json;

import work.ready.core.tools.StrUtil;

public abstract class Json {
	private static Json defaultJson = null;

	private static String defaultDatePattern = "yyyy-MM-dd HH:mm:ss";

	protected String datePattern = null;

	static void setDefaultJson(Json json){
		if (json == null) {
			throw new IllegalArgumentException("defaultJson can not be null.");
		}
		Json.defaultJson = json;
	}

	static void setDefaultDatePattern(String defaultDatePattern) {
		if (StrUtil.isBlank(defaultDatePattern)) {
			throw new IllegalArgumentException("defaultDatePattern can not be blank.");
		}
		Json.defaultDatePattern = defaultDatePattern;
	}

	public Json setDatePattern(String datePattern) {
		if (StrUtil.isBlank(datePattern)) {
			throw new IllegalArgumentException("datePattern can not be blank.");
		}
		this.datePattern = datePattern;
		return this;
	}

	public String getDatePattern() {
		return datePattern;
	}

	public String getDefaultDatePattern() {
		return defaultDatePattern;
	}

	public static Json getJson() {
		if(defaultJson == null) {
			synchronized (Json.class) {
				defaultJson = defaultJson == null ? new Jackson() : defaultJson;
			}
		}
		return defaultJson;
	}

	public abstract String toJson(Object object);

	public abstract <T> T parse(String jsonString, Class<T> type);
}

