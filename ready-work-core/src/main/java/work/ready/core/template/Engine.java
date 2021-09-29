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

import work.ready.core.template.expr.ast.FieldGetter;
import work.ready.core.template.expr.ast.FieldKeyBuilder;
import work.ready.core.template.expr.ast.FieldKit;
import work.ready.core.template.expr.ast.MethodKit;
import work.ready.core.template.io.EncoderFactory;
import work.ready.core.template.source.ClassPathSourceFactory;
import work.ready.core.template.source.TemplateSource;
import work.ready.core.template.source.ISourceFactory;
import work.ready.core.template.source.StringSource;
import work.ready.core.template.stat.Compressor;
import work.ready.core.template.stat.OutputDirectiveFactory;
import work.ready.core.template.stat.Parser;
import work.ready.core.template.stat.ast.Stat;
import work.ready.core.tools.HashUtil;
import work.ready.core.tools.StrUtil;
import work.ready.core.tools.define.SyncWriteMap;

import java.lang.reflect.Method;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class Engine {

	public static final String MAIN_ENGINE_NAME = "main";

	private static Engine MAIN_ENGINE;
	private static Map<String, Engine> engineMap = new HashMap<String, Engine>(32, 0.5F);

	private String name;
	private boolean devMode = false;
	private boolean cacheStringTemplate = false;
	private EngineConfig config = new EngineConfig();
	private ISourceFactory sourceFactory = config.getSourceFactory();

	private Map<String, Template> templateCache = new SyncWriteMap<String, Template>(1024, 0.5F);

	public Engine() {
		this.name = "Ready.Work";
	}

	public Engine(String engineName) {
		this.name = engineName;
	}

	public static Engine use() {
		if(MAIN_ENGINE == null){
			setMainEngine(new Engine(MAIN_ENGINE_NAME));
		}
		return MAIN_ENGINE;
	}

	public static Engine use(String engineName) {
		return engineMap.get(engineName);
	}

	public synchronized static Engine create(String engineName) {
		if (StrUtil.isBlank(engineName)) {
			throw new IllegalArgumentException("Engine name can not be blank");
		}
		engineName = engineName.trim();
		if (engineMap.containsKey(engineName)) {
			throw new IllegalArgumentException("Engine already exists : " + engineName);
		}
		Engine newEngine = new Engine(engineName);
		engineMap.put(engineName, newEngine);
		return newEngine;
	}

	public synchronized static Engine remove(String engineName) {
		Engine removed = engineMap.remove(engineName);
		if (removed != null && MAIN_ENGINE_NAME.equals(removed.name)) {
			Engine.MAIN_ENGINE = null;
		}
		return removed;
	}

	public synchronized static void setMainEngine(Engine engine) {
		if (engine == null) {
			throw new IllegalArgumentException("Engine can not be null");
		}
		engine.name = Engine.MAIN_ENGINE_NAME;
		engineMap.put(Engine.MAIN_ENGINE_NAME, engine);
		Engine.MAIN_ENGINE = engine;
	}

	public Template getTemplate(String fileName) {
		if (fileName.charAt(0) != '/') {
			char[] arr = new char[fileName.length() + 1];
			fileName.getChars(0, fileName.length(), arr, 1);
			arr[0] = '/';
			fileName = new String(arr);
		}

		Template template = templateCache.get(fileName);
		if (template == null) {
			template = buildTemplateBySourceFactory(fileName);
			templateCache.put(fileName, template);
		} else if (devMode) {
			if (template.isModified()) {
				template = buildTemplateBySourceFactory(fileName);
				templateCache.put(fileName, template);
			}
		}
		return template;
	}

	private Template buildTemplateBySourceFactory(String fileName) {
		
		TemplateSource source = sourceFactory.getSource(config.getBaseTemplatePath(), fileName, config.getEncoding());
		Env env = new Env(config);
		Parser parser = new Parser(env, source.getContent(), fileName);
		if (devMode) {
			env.addSource(source);
		}
		Stat stat = parser.parse();
		Template template = new Template(env, stat);
		return template;
	}

	public Template getTemplateByString(String content) {
		return getTemplateByString(content, cacheStringTemplate);
	}

	public Template getTemplateByString(String content, boolean cache) {
		if (!cache) {
			return buildTemplateBySource(new StringSource(content, cache));
		}

		String cacheKey = HashUtil.md5(content);
		Template template = templateCache.get(cacheKey);
		if (template == null) {
			template = buildTemplateBySource(new StringSource(content, cache));
			templateCache.put(cacheKey, template);
		} else if (devMode) {
			if (template.isModified()) {
				template = buildTemplateBySource(new StringSource(content, cache));
				templateCache.put(cacheKey, template);
			}
		}
		return template;
	}

	public Template getTemplate(TemplateSource source) {
		String cacheKey = source.getCacheKey();
		if (cacheKey == null) {	
			return buildTemplateBySource(source);
		}

		Template template = templateCache.get(cacheKey);
		if (template == null) {
			template = buildTemplateBySource(source);
			templateCache.put(cacheKey, template);
		} else if (devMode) {
			if (template.isModified()) {
				template = buildTemplateBySource(source);
				templateCache.put(cacheKey, template);
			}
		}
		return template;
	}

	private Template buildTemplateBySource(TemplateSource source) {
		Env env = new Env(config);
		Parser parser = new Parser(env, source.getContent(), null);
		if (devMode) {
			env.addSource(source);
		}
		Stat stat = parser.parse();
		Template template = new Template(env, stat);
		return template;
	}

	public Engine addSharedFunction(String fileName) {
		config.addSharedFunction(fileName);
		return this;
	}

	public Engine addSharedFunction(TemplateSource source) {
		config.addSharedFunction(source);
		return this;
	}

	public Engine addSharedFunction(String... fileNames) {
		config.addSharedFunction(fileNames);
		return this;
	}

	public Engine addSharedFunctionByString(String content) {
		config.addSharedFunctionByString(content);
		return this;
	}

	public Engine addSharedObject(String name, Object object) {
		config.addSharedObject(name, object);
		return this;
	}

	public Engine removeSharedObject(String name) {
		config.removeSharedObject(name);
		return this;
	}

	public Engine addEnum(Class<? extends Enum<?>> enumClass) {
		Map<String, Enum<?>> map = new java.util.LinkedHashMap<>();
		Enum<?>[] es = enumClass.getEnumConstants();
		for (Enum<?> e : es) {
			map.put(e.name(), e);
		}
		return addSharedObject(enumClass.getSimpleName(), map);
	}

	public Engine setOutputDirectiveFactory(OutputDirectiveFactory outputDirectiveFactory) {
		config.setOutputDirectiveFactory(outputDirectiveFactory);
		return this;
	}

	public Engine addDirective(String directiveName, Class<? extends Directive> directiveClass, boolean keepLineBlank) {
		config.addDirective(directiveName, directiveClass, keepLineBlank);
		return this;
	}

	public Engine addDirective(String directiveName, Class<? extends Directive> directiveClass) {
		config.addDirective(directiveName, directiveClass);
		return this;
	}

	public Engine removeDirective(String directiveName) {
		config.removeDirective(directiveName);
		return this;
	}

	public Engine addSharedMethod(Object sharedMethodFromObject) {
		config.addSharedMethod(sharedMethodFromObject);
		return this;
	}

	public Engine addSharedMethod(Class<?> sharedMethodFromClass) {
		config.addSharedMethod(sharedMethodFromClass);
		return this;
	}

	public Engine addSharedStaticMethod(Class<?> sharedStaticMethodFromClass) {
		config.addSharedStaticMethod(sharedStaticMethodFromClass);
		return this;
	}

	public Engine removeSharedMethod(String methodName) {
		config.removeSharedMethod(methodName);
		return this;
	}

	public Engine removeSharedMethod(Class<?> clazz) {
		config.removeSharedMethod(clazz);
		return this;
	}

	public Engine removeSharedMethod(Method method) {
		config.removeSharedMethod(method);
		return this;
	}

	public void removeTemplateCache(String cacheKey) {
		templateCache.remove(cacheKey);
	}

	public void removeAllTemplateCache() {
		templateCache.clear();
	}

	public int getTemplateCacheSize() {
		return templateCache.size();
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return "Template Engine: " + name;
	}

	public EngineConfig  getEngineConfig() {
		return config;
	}

	public Engine setDevMode(boolean devMode) {
		this.devMode = devMode;
		this.config.setDevMode(devMode);
		if (this.devMode) {
			removeAllTemplateCache();
		}
		return this;
	}

	public boolean getDevMode() {
		return devMode;
	}

	public Engine setCacheStringTemplate(boolean cacheStringTemplate) {
		this.cacheStringTemplate = cacheStringTemplate;
		return this;
	}

	public Engine setSourceFactory(ISourceFactory sourceFactory) {
		this.config.setSourceFactory(sourceFactory);	
		this.sourceFactory = sourceFactory;
		return this;
	}

	public Engine setToClassPathSourceFactory() {
		return setSourceFactory(new ClassPathSourceFactory());
	}

	public ISourceFactory getSourceFactory() {
		return sourceFactory;
	}

	public Engine setBaseTemplatePath(String baseTemplatePath) {
		config.setBaseTemplatePath(baseTemplatePath);
		return this;
	}

	public String getBaseTemplatePath() {
		return config.getBaseTemplatePath();
	}

	public Engine setDatePattern(String datePattern) {
		config.setDatePattern(datePattern);
		return this;
	}

	public String getDatePattern() {
		return config.getDatePattern();
	}

	public Engine setEncoding(String encoding) {
		config.setEncoding(encoding);
		return this;
	}

	public String getEncoding() {
		return config.getEncoding();
	}

	public Engine setRoundingMode(RoundingMode roundingMode) {
		config.setRoundingMode(roundingMode);
		return this;
	}

	public Engine setEncoderFactory(EncoderFactory encoderFactory) {
		config.setEncoderFactory(encoderFactory);
		return this;
	}

	public Engine setToJdkEncoderFactory() {
		config.setEncoderFactory(new work.ready.core.template.io.JdkEncoderFactory());
		return this;
	}

	public Engine setWriterBufferSize(int bufferSize) {
		config.setWriterBufferSize(bufferSize);
		return this;
	}

	public Engine setCompressorOn(char separator) {
		return setCompressor(new Compressor(separator));
	}

	public Engine setCompressorOn() {
		return setCompressor(new Compressor());
	}

	public Engine setCompressor(Compressor compressor) {
		config.setCompressor(compressor);
		return this;
	}

	public Engine setReloadModifiedSharedFunctionInDevMode(boolean reloadModifiedSharedFunctionInDevMode) {
		config.setReloadModifiedSharedFunctionInDevMode(reloadModifiedSharedFunctionInDevMode);
		return this;
	}

	public static void addExtensionMethod(Class<?> targetClass, Object objectOfExtensionClass) {
		MethodKit.addExtensionMethod(targetClass, objectOfExtensionClass);
	}

	public static void addExtensionMethod(Class<?> targetClass, Class<?> extensionClass) {
		MethodKit.addExtensionMethod(targetClass, extensionClass);
	}

	public static void removeExtensionMethod(Class<?> targetClass, Object objectOfExtensionClass) {
		MethodKit.removeExtensionMethod(targetClass, objectOfExtensionClass);
	}

	public static void removeExtensionMethod(Class<?> targetClass, Class<?> extensionClass) {
		MethodKit.removeExtensionMethod(targetClass, extensionClass);
	}

	public static void addFieldGetter(int index, FieldGetter fieldGetter) {
		FieldKit.addFieldGetter(index, fieldGetter);
	}

	public static void addFieldGetterToLast(FieldGetter fieldGetter) {
		FieldKit.addFieldGetterToLast(fieldGetter);
	}

	public static void addFieldGetterToFirst(FieldGetter fieldGetter) {
		FieldKit.addFieldGetterToFirst(fieldGetter);
	}

	public static void removeFieldGetter(Class<? extends FieldGetter> fieldGetterClass) {
		FieldKit.removeFieldGetter(fieldGetterClass);
	}

	public static void setFastFieldKeyBuilder(boolean enable) {
		FieldKeyBuilder.setFastFieldKeyBuilder(enable);
	}

	public static void setFastMode(boolean fastMode) {
		FieldKit.setFastMode(fastMode);
		FieldKeyBuilder.setFastFieldKeyBuilder(fastMode);
	}
}

