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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import work.ready.core.config.Config;
import work.ready.core.server.Ready;
import work.ready.core.tools.DateUtil;

public class Jackson extends Json {

	private static boolean defaultGenerateNullValue = true;

	protected Boolean generateNullValue = null;

	protected ObjectMapper objectMapper;

	public Jackson() {
		objectMapper = Ready.config().getJsonMapper();
		config();
	}

	@SuppressWarnings("deprecation")
	protected void config() {
		objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
		objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	}

	public static void setDefaultGenerateNullValue(boolean defaultGenerateNullValue) {
		Jackson.defaultGenerateNullValue = defaultGenerateNullValue;
	}

	public Jackson setGenerateNullValue(boolean generateNullValue) {
		this.generateNullValue = generateNullValue;
		return this;
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public static Jackson getJson() {
		return new Jackson();
	}

	@Override
	public String toJson(Object object) {
		try {

			String dp = datePattern != null ? datePattern : getDefaultDatePattern();
			if (dp != null) {
				objectMapper.setDateFormat(DateUtil.getSimpleDateFormat(dp));
			}

			Boolean pnv = generateNullValue != null ? generateNullValue : defaultGenerateNullValue;
			if (pnv == false) {
				objectMapper.setSerializationInclusion(Include.NON_NULL);
			}

			return objectMapper.writeValueAsString(object);
		} catch (Exception e) {
			throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
		}
	}

	@Override
	public <T> T parse(String jsonString, Class<T> type) {
		try {
			return objectMapper.readValue(jsonString, type);
		} catch (Exception e) {
			throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
		}
	}
}

