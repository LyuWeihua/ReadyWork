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

package work.ready.core.template.source;

import work.ready.core.template.EngineConfig;
import work.ready.core.tools.HashUtil;
import work.ready.core.tools.StrUtil;

public class StringSource implements TemplateSource {

	private String cacheKey;
	private StringBuilder content;

	public StringSource(String content, boolean cache) {
		if (StrUtil.isBlank(content)) {
			throw new IllegalArgumentException("content can not be blank");
		}
		this.content = new StringBuilder(content);
		this.cacheKey = cache ? HashUtil.md5(content) : null;	
	}

	public StringSource(StringBuilder content, boolean cache) {
		if (content == null || content.length() == 0) {
			throw new IllegalArgumentException("content can not be blank");
		}
		this.content = content;
		this.cacheKey = cache ? HashUtil.md5(content.toString()) : null;	
	}

	public boolean isModified() {
		return false;
	}

	public String getCacheKey() {
		return cacheKey;
	}

	public StringBuilder getContent() {
		return content;
	}

	public String getEncoding() {
		return EngineConfig.DEFAULT_ENCODING;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("cacheKey : ").append(cacheKey).append("\n");
		sb.append("content : ").append(content).append("\n");
		return sb.toString();
	}
}

