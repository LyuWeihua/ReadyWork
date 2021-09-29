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

package work.ready.core.template;

import work.ready.core.template.io.ByteWriter;
import work.ready.core.template.io.CharWriter;
import work.ready.core.template.io.FastStringWriter;
import work.ready.core.template.stat.Scope;
import work.ready.core.template.stat.ast.Stat;

import java.io.*;
import java.util.Map;

public class Template {

	private Env env;
	private Stat ast;

	public Template(Env env, Stat ast) {
		if (env == null || ast == null) {
			throw new IllegalArgumentException("env and ast can not be null");
		}
		this.env = env;
		this.ast = ast;
	}

	public void render(Map<?, ?> data, OutputStream outputStream) {
		ByteWriter byteWriter = env.engineConfig.writerBuffer.getByteWriter(outputStream);
		try {
			ast.exec(env, new Scope(data, env.engineConfig.sharedObjectMap), byteWriter);
		} finally {
			byteWriter.close();
		}
	}

	public void render(OutputStream outputStream) {
		render(null, outputStream);
	}

	public void render(Map<?, ?> data, Writer writer) {
		CharWriter charWriter = env.engineConfig.writerBuffer.getCharWriter(writer);
		try {
			ast.exec(env, new Scope(data, env.engineConfig.sharedObjectMap), charWriter);
		} finally {
			charWriter.close();
		}
	}

	public void render(Writer writer) {
		render(null, writer);
	}

	public String renderToString(Map<?, ?> data) {
		FastStringWriter fsw = env.engineConfig.writerBuffer.getFastStringWriter();
		try {
			render(data, fsw);
			return fsw.toString();
		} finally {
			fsw.close();
		}
	}

	public String renderToString() {
		return renderToString(null);
	}

	public StringBuilder renderToStringBuilder(Map<?, ?> data) {
		FastStringWriter fsw = new FastStringWriter();
		render(data, fsw);
		return fsw.toStringBuilder();
	}

	public void render(Map<?, ?> data, File file) {
		try (FileOutputStream fos = new FileOutputStream(file)) {
			render(data, fos);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void render(Map<?, ?> data, String fileName) {
		render(data, new File(fileName));
	}

	public boolean isModified() {
		return env.isSourceListModified();
	}

	@FunctionalInterface
	public interface Func<T> {
		void call(Stat ast, Env env, Scope scope, T t);
	}

	public String renderToString(Map<?, ?> data, Func<CharWriter> func) {
		FastStringWriter fsw = env.engineConfig.writerBuffer.getFastStringWriter();
		try {

			CharWriter charWriter = env.engineConfig.writerBuffer.getCharWriter(fsw);
			try {
				func.call(ast, env, new Scope(data, env.engineConfig.sharedObjectMap), charWriter);
			} finally {
				charWriter.close();
			}

			return fsw.toString();
		} finally {
			fsw.close();
		}
	}

	public void render(Map<?, ?> data, OutputStream outputStream, Func<ByteWriter> func) {
		ByteWriter byteWriter = env.engineConfig.writerBuffer.getByteWriter(outputStream);
		try {
			func.call(ast, env, new Scope(data, env.engineConfig.sharedObjectMap), byteWriter);
		} finally {
			byteWriter.close();
		}
	}

	public void render(Map<?, ?> data, Writer writer, Func<CharWriter> func) {
		CharWriter charWriter = env.engineConfig.writerBuffer.getCharWriter(writer);
		try {
			func.call(ast, env, new Scope(data, env.engineConfig.sharedObjectMap), charWriter);
		} finally {
			charWriter.close();
		}
	}

	public void render(Map<?, ?> data, File file, Func<ByteWriter> func) {
		try (FileOutputStream fos = new FileOutputStream(file)) {

			ByteWriter byteWriter = env.engineConfig.writerBuffer.getByteWriter(fos);
			try {
				func.call(ast, env, new Scope(data, env.engineConfig.sharedObjectMap), byteWriter);
			} finally {
				byteWriter.close();
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

