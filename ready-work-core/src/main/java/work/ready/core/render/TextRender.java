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
package work.ready.core.render;

import io.undertow.util.StatusCodes;
import work.ready.core.handler.ContentType;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

public class TextRender extends Render {
	private static final Log logger = LogFactory.getLog(TextRender.class);

	protected String text;
	protected ContentType contentType = ContentType.create("txt");

	public TextRender(String text) {
		this.text = text;
	}

	public TextRender(String text, String contentType) {
		this.text = text;
		this.contentType = ContentType.create(contentType);
	}

	public TextRender(String text, ContentType contentType) {
		this.text = text;
		this.contentType = contentType;
	}

	public void render() {
		try {
			response.setContentType(contentType);
			response.setStatus(StatusCodes.OK);
			logger.info("response: %s", text);
			response.send(text);
		} catch (Exception e) {
			throw new RenderException(e);
		}
	}

	public String getText() {
		return text;
	}

	public ContentType getContentType() {
		return contentType;
	}
}

