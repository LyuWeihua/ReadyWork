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

import work.ready.core.template.expr.ast.Arith;
import work.ready.core.template.expr.ast.ExprList;
import work.ready.core.template.expr.ast.SharedMethodKit;
import work.ready.core.template.ext.directive.*;
import work.ready.core.template.ext.sharedmethod.SharedMethodLib;
import work.ready.core.template.io.EncoderFactory;
import work.ready.core.template.io.WriterBuffer;
import work.ready.core.template.source.*;
import work.ready.core.template.stat.Compressor;
import work.ready.core.template.stat.Location;
import work.ready.core.template.stat.OutputDirectiveFactory;
import work.ready.core.template.stat.Parser;
import work.ready.core.template.stat.ast.Define;
import work.ready.core.template.stat.ast.Output;
import work.ready.core.tools.StrUtil;

import java.lang.reflect.Method;
import java.math.RoundingMode;
import java.util.*;
import java.util.Map.Entry;

public class EngineConfig {

	public static final String DEFAULT_ENCODING = "UTF-8";

	WriterBuffer writerBuffer = new WriterBuffer();

	Compressor compressor = null;

	private Map<String, Define> sharedFunctionMap = createSharedFunctionMap();		
	private List<TemplateSource> sharedFunctionSourceList = new ArrayList<TemplateSource>();		

	Map<String, Object> sharedObjectMap = null;

	private OutputDirectiveFactory outputDirectiveFactory = OutputDirectiveFactory.me;
	private ISourceFactory sourceFactory = new FileSourceFactory();
	private Map<String, Class<? extends Directive>> directiveMap = new HashMap<String, Class<? extends Directive>>(64, 0.5F);
	private SharedMethodKit sharedMethodKit = new SharedMethodKit();

	private Set<String> keepLineBlankDirectives = new HashSet<>();

	private boolean devMode = false;
	private boolean reloadModifiedSharedFunctionInDevMode = true;
	private String baseTemplatePath = null;
	private String encoding = DEFAULT_ENCODING;
	private String datePattern = "yyyy-MM-dd HH:mm";

	private RoundingMode roundingMode = RoundingMode.HALF_UP;

	public EngineConfig() {
		
		setKeepLineBlank("output", true);
		setKeepLineBlank("include", true);

		addDirective("render", RenderDirective.class, true);
		addDirective("date", DateDirective.class, true);
		addDirective("escape", EscapeDirective.class, true);
		addDirective("random", RandomDirective.class, true);
		addDirective("number", NumberDirective.class, true);

		addDirective("call", CallDirective.class, false);
		addDirective("string", StringDirective.class, false);

		addSharedMethod(new SharedMethodLib());
	}

	public void addSharedFunction(String fileName) {
		fileName = fileName.replace("\\", "/");
		
		TemplateSource source = sourceFactory.getSource(baseTemplatePath, fileName, encoding);
		doAddSharedFunction(source, fileName);
	}

	private synchronized void doAddSharedFunction(TemplateSource source, String fileName) {
		Env env = new Env(this);
		new Parser(env, source.getContent(), fileName).parse();
		addToSharedFunctionMap(sharedFunctionMap, env);
		if (devMode) {
			sharedFunctionSourceList.add(source);
			env.addSource(source);
		}
	}

	public void addSharedFunction(String... fileNames) {
		for (String fileName : fileNames) {
			addSharedFunction(fileName);
		}
	}

	public void addSharedFunctionByString(String content) {

		StringSource stringSource = new StringSource(content, false);
		doAddSharedFunction(stringSource, null);
	}

	public void addSharedFunction(TemplateSource source) {
		String fileName = source instanceof FileSource ? ((FileSource)source).getFileName() : null;
		doAddSharedFunction(source, fileName);
	}

	private void addToSharedFunctionMap(Map<String, Define> sharedFunctionMap, Env env) {
		Map<String, Define> funcMap = env.getFunctionMap();
		for (Entry<String, Define> e : funcMap.entrySet()) {
			if (sharedFunctionMap.containsKey(e.getKey())) {
				throw new IllegalArgumentException("Template function already exists : " + e.getKey());
			}
			Define func = e.getValue();
			if (devMode) {
				func.setEnvForDevMode(env);
			}
			sharedFunctionMap.put(e.getKey(), func);
		}
	}

	Define getSharedFunction(String functionName) {
		Define func = sharedFunctionMap.get(functionName);
		if (func == null) {
			
			return null;
		}

		if (devMode && reloadModifiedSharedFunctionInDevMode) {
			if (func.isSourceModifiedForDevMode()) {
				synchronized (this) {
					func = sharedFunctionMap.get(functionName);
					if (func.isSourceModifiedForDevMode()) {
						reloadSharedFunctionSourceList();
						func = sharedFunctionMap.get(functionName);
					}
				}
			}
		}
		return func;
	}

	private synchronized void reloadSharedFunctionSourceList() {
		Map<String, Define> newMap = createSharedFunctionMap();
		for (int i = 0, size = sharedFunctionSourceList.size(); i < size; i++) {
			TemplateSource source = sharedFunctionSourceList.get(i);
			String fileName = source instanceof FileSource ? ((FileSource)source).getFileName() : null;

			Env env = new Env(this);
			new Parser(env, source.getContent(), fileName).parse();
			addToSharedFunctionMap(newMap, env);
			if (devMode) {
				env.addSource(source);
			}
		}
		this.sharedFunctionMap = newMap;
	}

	private Map<String, Define> createSharedFunctionMap() {
		return new HashMap<String, Define>(512, 0.25F);
	}

	public synchronized void addSharedObject(String name, Object object) {
		if (sharedObjectMap == null) {
			sharedObjectMap = new HashMap<String, Object>(64, 0.25F);
		} else if (sharedObjectMap.containsKey(name)) {
			throw new IllegalArgumentException("Shared object already exists: " + name);
		}
		sharedObjectMap.put(name, object);
	}

	public Map<String, Object> getSharedObjectMap() {
		return sharedObjectMap;
	}

	public synchronized void removeSharedObject(String name) {
		if (sharedObjectMap != null) {
			sharedObjectMap.remove(name);
		}
	}

	public void setOutputDirectiveFactory(OutputDirectiveFactory outputDirectiveFactory) {
		if (outputDirectiveFactory == null) {
			throw new IllegalArgumentException("outputDirectiveFactory can not be null");
		}
		this.outputDirectiveFactory = outputDirectiveFactory;
	}

	public Output getOutputDirective(ExprList exprList, Location location) {
		return outputDirectiveFactory.getOutputDirective(exprList, location);
	}

	void setDevMode(boolean devMode) {
		this.devMode = devMode;
	}

	public boolean isDevMode() {
		return devMode;
	}

	void setSourceFactory(ISourceFactory sourceFactory) {
		if (sourceFactory == null) {
			throw new IllegalArgumentException("sourceFactory can not be null");
		}
		this.sourceFactory = sourceFactory;
	}

	public ISourceFactory getSourceFactory() {
		return sourceFactory;
	}

	public void setBaseTemplatePath(String baseTemplatePath) {
		
		if (baseTemplatePath == null) {
			this.baseTemplatePath = null;
			return ;
		}
		if (StrUtil.isBlank(baseTemplatePath)) {
			throw new IllegalArgumentException("baseTemplatePath can not be blank");
		}
		baseTemplatePath = baseTemplatePath.trim();
		baseTemplatePath = baseTemplatePath.replace("\\", "/");
		if (baseTemplatePath.length() > 1) {
			if (baseTemplatePath.endsWith("/")) {
				baseTemplatePath = baseTemplatePath.substring(0, baseTemplatePath.length() - 1);
			}
		}
		this.baseTemplatePath = baseTemplatePath;
	}

	public String getBaseTemplatePath() {
		return baseTemplatePath;
	}

	public void setEncoding(String encoding) {
		if (StrUtil.isBlank(encoding)) {
			throw new IllegalArgumentException("encoding can not be blank");
		}
		this.encoding = encoding;

		writerBuffer.setEncoding(encoding);		
	}

	public void setEncoderFactory(EncoderFactory encoderFactory) {
		writerBuffer.setEncoderFactory(encoderFactory);
		writerBuffer.setEncoding(encoding);		
	}

	public void setWriterBufferSize(int bufferSize) {
		writerBuffer.setBufferSize(bufferSize);
	}

	public void setWriterBuffer(WriterBuffer writerBuffer) {
		Objects.requireNonNull(writerBuffer, "writerBuffer can not be null");
		this.writerBuffer = writerBuffer;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setDatePattern(String datePattern) {
		if (StrUtil.isBlank(datePattern)) {
			throw new IllegalArgumentException("datePattern can not be blank");
		}
		this.datePattern = datePattern;
	}

	public String getDatePattern() {
		return datePattern;
	}

	public void setReloadModifiedSharedFunctionInDevMode(boolean reloadModifiedSharedFunctionInDevMode) {
		this.reloadModifiedSharedFunctionInDevMode = reloadModifiedSharedFunctionInDevMode;
	}

	public synchronized void addDirective(String directiveName, Class<? extends Directive> directiveClass, boolean keepLineBlank) {
		if (StrUtil.isBlank(directiveName)) {
			throw new IllegalArgumentException("directive name can not be blank");
		}
		if (directiveClass == null) {
			throw new IllegalArgumentException("directiveClass can not be null");
		}
		if (directiveMap.containsKey(directiveName)) {
			throw new IllegalArgumentException("directive already exists : " + directiveName);
		}

		directiveMap.put(directiveName, directiveClass);
		if (keepLineBlank) {
			keepLineBlankDirectives.add(directiveName);
		}
	}

	public void addDirective(String directiveName, Class<? extends Directive> directiveClass) {
		addDirective(directiveName, directiveClass, false);
	}

	public Class<? extends Directive> getDirective(String directiveName) {
		return directiveMap.get(directiveName);
	}

	public void removeDirective(String directiveName) {
		directiveMap.remove(directiveName);
		keepLineBlankDirectives.remove(directiveName);
	}

	public void setKeepLineBlank(String directiveName, boolean keepLineBlank) {
		if (keepLineBlank) {
			keepLineBlankDirectives.add(directiveName);
		} else {
			keepLineBlankDirectives.remove(directiveName);
		}
	}

	public Set<String> getKeepLineBlankDirectives() {
		return keepLineBlankDirectives;
	}

	public void addSharedMethod(Object sharedMethodFromObject) {
		sharedMethodKit.addSharedMethod(sharedMethodFromObject);
	}

	public void addSharedMethod(Class<?> sharedMethodFromClass) {
		sharedMethodKit.addSharedMethod(sharedMethodFromClass);
	}

	public void addSharedStaticMethod(Class<?> sharedStaticMethodFromClass) {
		sharedMethodKit.addSharedStaticMethod(sharedStaticMethodFromClass);
	}

	public void removeSharedMethod(String methodName) {
		sharedMethodKit.removeSharedMethod(methodName);
	}

	public void removeSharedMethod(Class<?> sharedClass) {
		sharedMethodKit.removeSharedMethod(sharedClass);
	}

	public void removeSharedMethod(Method method) {
		sharedMethodKit.removeSharedMethod(method);
	}

	public SharedMethodKit getSharedMethodKit() {
		return sharedMethodKit;
	}

	public void setCompressor(Compressor compressor) {
		this.compressor = compressor;
	}

	public Compressor getCompressor() {
		return compressor;
	}

	public void setRoundingMode(RoundingMode roundingMode) {
		this.roundingMode = roundingMode;
		Arith.setBigDecimalDivideRoundingMode(roundingMode);
	}

	public RoundingMode getRoundingMode() {
		return roundingMode;
	}
}

