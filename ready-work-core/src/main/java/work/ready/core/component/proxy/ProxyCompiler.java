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

package work.ready.core.component.proxy;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.Map.Entry;

public class ProxyCompiler {

	private static final Log logger = LogFactory.getLog(ProxyCompiler.class);

	protected volatile List<String> options = null;

	protected List<String> getOptions() {
		if (options != null) {
			return options;
		}

		synchronized (this) {
			if (options != null) {
				return options;
			}

			List<String> ret = new ArrayList<>();
			ret.add("-target");
			ret.add("11");

			String cp = getClassPath();
			if (cp != null && cp.trim().length() != 0) {
				ret.add("-classpath");
				ret.add(cp);
			}

			options = ret;
			return options;
		}
	}

	protected String getClassPath() {
		URLClassLoader classLoader = getURLClassLoader();
		if (classLoader == null) {
			return null;
		}

		int index = 0;
		boolean isWindows = isWindows();
		StringBuilder ret = new StringBuilder();
		for (URL url : classLoader.getURLs()) {
			if (index++ > 0) {
				ret.append(File.pathSeparator);
			}

			String path = url.getFile();

			if (isWindows && path.startsWith("/")) {
				path = path.substring(1);
			}

			if (path.length() > 1 && (path.endsWith("/") || path.endsWith(File.separator))) {
				path = path.substring(0, path.length() - 1);
			}

			ret.append(path);
		}

		return ret.toString();
	}

	protected boolean isWindows() {
		String osName = System.getProperty("os.name", "unknown");
		return osName.toLowerCase().indexOf("windows") != -1;
	}

	protected URLClassLoader getURLClassLoader() {
		ClassLoader ret = Thread.currentThread().getContextClassLoader();
		if (ret == null) {
			ret = ProxyCompiler.class.getClassLoader();
		}
		return (ret instanceof URLClassLoader) ? (URLClassLoader)ret : null;
	}

	public void compile(SourceCode proxyClass) {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			throw new RuntimeException("Can not get javax.tools.JavaCompiler, check whether \"tools.jar\" is in the environment variable CLASSPATH");
		}

		DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
		try (MyJavaFileManager javaFileManager = new MyJavaFileManager(compiler.getStandardFileManager(collector, null, null))) {

			MyJavaFileObject javaFileObject = new MyJavaFileObject(proxyClass.getName(), proxyClass.getSourceCode());
			Boolean result = compiler.getTask(null, javaFileManager, collector, getOptions(), null, Arrays.asList(javaFileObject)).call();
			outputCompileError(result, collector);

			Map<String, byte[]> ret = new HashMap<>();
			for (Entry<String, MyJavaFileObject> e : javaFileManager.fileObjects.entrySet()) {
				ret.put(e.getKey(), e.getValue().getByteCode());
			}

			proxyClass.setByteCode(ret);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected void outputCompileError(Boolean result, DiagnosticCollector<JavaFileObject> collector) {
		if (! result) {
			collector.getDiagnostics().forEach(item -> logger.error(item.toString()));
		}
	}

	public ProxyCompiler setCompileOptions(List<String> options) {
		Objects.requireNonNull(options, "options can not be null");
		this.options = options;
		return this;
	}

	public ProxyCompiler addCompileOption(String option) {
		Objects.requireNonNull(option, "option can not be null");
		options.add(option);
		return this;
	}

	public static class MyJavaFileObject extends SimpleJavaFileObject {

		private String source;
		private ByteArrayOutputStream outPutStream;

		public MyJavaFileObject(String name, String source) {
			super(URI.create("String:///" + name + Kind.SOURCE.extension), Kind.SOURCE);
			this.source = source;
		}

		public MyJavaFileObject(String name, Kind kind) {
			super(URI.create("String:///" + name + kind.extension), kind);
			source = null;
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			if (source == null) {
				throw new IllegalStateException("source field can not be null");
			}
			return source;
		}

		@Override
		public OutputStream openOutputStream() throws IOException {
			outPutStream = new ByteArrayOutputStream();
			return outPutStream;
		}

		public byte[] getByteCode() {
			return outPutStream.toByteArray();
		}
	}

	public static class MyJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

		public Map<String, MyJavaFileObject> fileObjects = new HashMap<>();

		public MyJavaFileManager(JavaFileManager fileManager) {
			super(fileManager);
		}

		@Override
		public JavaFileObject getJavaFileForOutput(Location location, String qualifiedClassName, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
			MyJavaFileObject javaFileObject = new MyJavaFileObject(qualifiedClassName, kind);
			fileObjects.put(qualifiedClassName, javaFileObject);
			return javaFileObject;
		}

		@Override
		public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind) throws IOException {
			JavaFileObject javaFileObject = fileObjects.get(className);
			if (javaFileObject == null) {
				javaFileObject = super.getJavaFileForInput(location, className, kind);
			}
			return javaFileObject;
		}
	}
}

