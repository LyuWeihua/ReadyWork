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

package work.ready.core.database.generator;

import work.ready.core.template.Engine;
import work.ready.core.tools.JavaKeyword;
import work.ready.core.tools.PathUtil;
import work.ready.core.tools.define.Kv;
import work.ready.core.tools.StrUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseModelGenerator {

	protected Engine engine;
	protected String template = getClass().getPackageName().replace('.','/') + "/base_model_template.tpl";

	protected String baseModelPackageName;
	protected String baseModelOutputDir;
	protected boolean generateChainSetter = false;

	protected JavaKeyword javaKeyword = JavaKeyword.INSTANCE;

	public BaseModelGenerator(String baseModelPackageName) {
		if (StrUtil.isBlank(baseModelPackageName)) {
			throw new IllegalArgumentException("baseModelPackageName can not be blank.");
		}
		if (baseModelPackageName.contains("/") || baseModelPackageName.contains("\\")) {
			throw new IllegalArgumentException("baseModelPackageName error : " + baseModelPackageName);
		}

		this.baseModelPackageName = baseModelPackageName;
		this.baseModelOutputDir = buildOutPutDir();

		initEngine();
	}

	public BaseModelGenerator(String baseModelPackageName, String baseModelOutputDir) {
		if (StrUtil.isBlank(baseModelPackageName)) {
			throw new IllegalArgumentException("baseModelPackageName can not be blank.");
		}
		if (baseModelPackageName.contains("/") || baseModelPackageName.contains("\\")) {
			throw new IllegalArgumentException("baseModelPackageName error : " + baseModelPackageName);
		}
		if (StrUtil.isBlank(baseModelOutputDir)) {
			throw new IllegalArgumentException("baseModelOutputDir can not be blank.");
		}

		this.baseModelPackageName = baseModelPackageName;
		this.baseModelOutputDir = baseModelOutputDir;

		initEngine();
	}

	private String buildOutPutDir() {
		return	PathUtil.getProjectRootPath() + "/src/main/java/" + baseModelPackageName.replace(".", "/");
	}

	protected void initEngine() {
		engine = new Engine();
		engine.setToClassPathSourceFactory();	
		engine.addSharedMethod(new StrUtil());
		engine.addSharedObject("getterTypeMap", Generator.getterTypeMap);
		engine.addSharedObject("javaKeyword", javaKeyword);
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public void setGenerateChainSetter(boolean generateChainSetter) {
		this.generateChainSetter = generateChainSetter;
	}

	public void generate(List<TableMeta> tableMetas) {
		System.out.println("Generate base model ...");
		System.out.println("Base Model Output Dir: " + baseModelOutputDir);

		for (TableMeta tableMeta : tableMetas) {
			genBaseModelContent(tableMeta);
		}
		writeToFile(tableMetas);
	}

	protected void genBaseModelContent(TableMeta tableMeta) {
		Kv data = Kv.by("baseModelPackageName", baseModelPackageName);
		data.set("generateChainSetter", generateChainSetter);
		data.set("tableMeta", tableMeta);

		tableMeta.baseModelContent = engine.getTemplate(template).renderToString(data);
	}

	protected void writeToFile(List<TableMeta> tableMetas) {
		try {
			for (TableMeta tableMeta : tableMetas) {
				writeToFile(tableMeta);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected void writeToFile(TableMeta tableMeta) throws IOException {
		File dir = new File(baseModelOutputDir);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		String target = baseModelOutputDir + File.separator + tableMeta.baseModelName + ".java";
		OutputStreamWriter osw = null;
		try {
			osw = new OutputStreamWriter(new FileOutputStream(target), "UTF-8");
			osw.write(tableMeta.baseModelContent);
		}
		finally {
			if (osw != null) {
				osw.close();
			}
		}
	}

	public String getBaseModelPackageName() {
		return baseModelPackageName;
	}

	public String getBaseModelOutputDir() {
		return baseModelOutputDir;
	}
}

