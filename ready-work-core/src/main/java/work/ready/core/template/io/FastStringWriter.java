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

package work.ready.core.template.io;

import java.io.IOException;
import java.io.Writer;

public class FastStringWriter extends Writer {

	private char[] value;
	private int len;

	private static int MAX_BUFFER_SIZE = 1024 * 512;

	public static void setMaxBufferSize(int maxBufferSize) {
		int min = 256;
		if (maxBufferSize < min) {
			throw new IllegalArgumentException("maxBufferSize must be greater than " + min);
		}
		MAX_BUFFER_SIZE = maxBufferSize;
	}

	@Override
	public void close()  {
		len = 0;

		if (value.length > MAX_BUFFER_SIZE) {
			value = new char[Math.max(256, MAX_BUFFER_SIZE / 2)];
		}
	}

	public String toString() {
		return new String(value, 0, len);
	}

	public StringBuilder toStringBuilder() {
		return new StringBuilder(len + 64).append(value, 0, len);
	}

	public FastStringWriter(int capacity) {
		value = new char[capacity];
	}

	public FastStringWriter() {
		this(128);
	}

	protected void expandCapacity(int newLen) {
		int newCapacity = Math.max(newLen, value.length * 2);
		char[] newValue = new char[newCapacity];

		if (len > 0) {
			System.arraycopy(value, 0, newValue, 0, len);
		}
		value = newValue;
	}

	@Override
	public void write(char buffer[], int offset, int len) throws IOException {
		int newLen = this.len + len;
		if (newLen > value.length) {
			expandCapacity(newLen);
		}

		System.arraycopy(buffer, offset, value, this.len, len);
		this.len = newLen;
	}

	@Override
	public void write(String str, int offset, int len) throws IOException {
		int newLen = this.len + len;
		if (newLen > value.length) {
			expandCapacity(newLen);
		}

		str.getChars(offset, (offset + len), value, this.len);
		this.len = newLen;
	}

	@Override
	public void write(int c) throws IOException {
		char[] buffer = {(char)c};
		write(buffer, 0, 1);
	}

	@Override
	public void write(char buffer[]) throws IOException {
		write(buffer, 0, buffer.length);
	}

	@Override
	public void write(String str) throws IOException {
		write(str, 0, str.length());
	}

	@Override
	public Writer append(CharSequence csq) throws IOException {
		if (csq instanceof String) {
			String str = (String)csq;
			write(str, 0, str.length());
			return this;
		}

		if (csq == null)
			write("null");
		else
			write(csq.toString());
		return this;
	}

	@Override
	public Writer append(CharSequence csq, int start, int end) throws IOException {
		if (csq instanceof String) {
			String str = (String)csq;
			write(str, start, (end - start));
			return this;
		}

		CharSequence cs = (csq == null ? "null" : csq);
		write(cs.subSequence(start, end).toString());
		return this;
	}

	@Override
	public Writer append(char c) throws IOException {
		char[] buffer = {c};
		write(buffer, 0, 1);
		return this;
	}

	@Override
	public void flush() throws IOException {

	}
}

