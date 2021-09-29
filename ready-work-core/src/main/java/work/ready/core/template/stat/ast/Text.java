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

package work.ready.core.template.stat.ast;

import work.ready.core.template.EngineConfig;
import work.ready.core.template.Env;
import work.ready.core.template.TemplateException;
import work.ready.core.template.io.IWritable;
import work.ready.core.template.io.Writer;
import work.ready.core.template.stat.Compressor;
import work.ready.core.template.stat.Scope;

import java.io.IOException;
import java.nio.charset.Charset;

public class Text extends Stat implements IWritable {

	private StringBuilder content;
	private Charset charset;
	private byte[] bytes;
	private char[] chars;

	public Text(StringBuilder content, EngineConfig ec) {
		Compressor c = ec.getCompressor();
		this.content = (c != null ? c.compress(content) : content);
		this.charset = Charset.forName(ec.getEncoding());
		this.bytes = null;
		this.chars = null;
	}

	public void exec(Env env, Scope scope, Writer writer) {
		try {
			writer.write(this);
		} catch (IOException e) {
			throw new TemplateException(e.getMessage(), location, e);
		}
	}

	public byte[] getBytes() {
		if (bytes != null) {
			return bytes;
		}

		synchronized (this) {
			if (bytes != null) {
				return bytes;
			}

			if (content != null) {
				bytes = content.toString().getBytes(charset);
				content = null;
				return bytes;
			} else {
				bytes = new String(chars).getBytes(charset);
				return bytes;
			}
		}
	}

	public char[] getChars() {
		if (chars != null) {
			return chars;
		}

		synchronized (this) {
			if (chars != null) {
				return chars;
			}

			if (content != null) {
				char[] charsTemp = new char[content.length()];
				content.getChars(0, content.length(), charsTemp, 0);
				chars = charsTemp;
				content = null;
				return chars;
			} else {
				String strTemp = new String(bytes, charset);
				char[] charsTemp = new char[strTemp.length()];
				strTemp.getChars(0, strTemp.length(), charsTemp, 0);
				chars = charsTemp;
				return chars;
			}
		}
	}

	public boolean isEmpty() {
		if (content != null) {
			return content.length() == 0;
		} else if (bytes != null) {
			return bytes.length == 0;
		} else {
			return chars.length == 0;
		}
	}

	public String toString() {
		if (bytes != null) {
			return new String(bytes, charset);
		} else if (chars != null) {
			return new String(chars);
		} else {
			return content.toString();
		}
	}
}

