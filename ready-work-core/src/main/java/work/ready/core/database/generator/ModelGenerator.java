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

import work.ready.core.tools.PathUtil;
import work.ready.core.tools.define.Kv;
import work.ready.core.tools.StrUtil;
import work.ready.core.template.Engine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

public class ModelGenerator {

	protected Engine engine;
	protected String template = getClass().getPackageName().replace('.','/') + "/model_template.tpl";

	protected String modelPackageName;
	protected String baseModelPackageName;
	protected String modelOutputDir;
	protected boolean generateDaoInModel = false;

	public ModelGenerator(String modelPackageName, String baseModelPackageName) {
		if (StrUtil.isBlank(modelPackageName)) {
			throw new IllegalArgumentException("modelPackageName can not be blank.");
		}
		if (modelPackageName.contains("/") || modelPackageName.contains("\\")) {
			throw new IllegalArgumentException("modelPackageName error : " + modelPackageName);
		}
		if (StrUtil.isBlank(baseModelPackageName)) {
			throw new IllegalArgumentException("baseModelPackageName can not be blank.");
		}
		if (baseModelPackageName.contains("/") || baseModelPackageName.contains("\\")) {
			throw new IllegalArgumentException("baseModelPackageName error : " + baseModelPackageName);
		}

		this.modelPackageName = modelPackageName;
		this.baseModelPackageName = baseModelPackageName;
		this.modelOutputDir = buildOutPutDir();

		initEngine();
	}

	public ModelGenerator(String modelPackageName, String baseModelPackageName, String modelOutputDir) {
		if (StrUtil.isBlank(modelPackageName)) {
			throw new IllegalArgumentException("modelPackageName can not be blank.");
		}
		if (modelPackageName.contains("/") || modelPackageName.contains("\\")) {
			throw new IllegalArgumentException("modelPackageName error : " + modelPackageName);
		}
		if (StrUtil.isBlank(baseModelPackageName)) {
			throw new IllegalArgumentException("baseModelPackageName can not be blank.");
		}
		if (baseModelPackageName.contains("/") || baseModelPackageName.contains("\\")) {
			throw new IllegalArgumentException("baseModelPackageName error : " + baseModelPackageName);
		}
		if (StrUtil.isBlank(modelOutputDir)) {
			throw new IllegalArgumentException("modelOutputDir can not be blank.");
		}

		this.modelPackageName = modelPackageName;
		this.baseModelPackageName = baseModelPackageName;
		this.modelOutputDir = modelOutputDir;

		initEngine();
	}

	private String buildOutPutDir() {
		return PathUtil.getProjectRootPath() + "/src/main/java/" + modelPackageName.replace(".", "/");
	}

	protected void initEngine() {
		engine = new Engine();
		engine.setToClassPathSourceFactory();
		engine.addSharedMethod(new StrUtil());
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public void setGenerateDaoInModel(boolean generateDaoInModel) {
		this.generateDaoInModel = generateDaoInModel;
	}

	public void generate(List<TableMeta> tableMetas) {
		System.out.println("Generate model ...");
		System.out.println("Model Output Dir: " + modelOutputDir);

		for (TableMeta tableMeta : tableMetas) {
			genModelContent(tableMeta);
		}
		writeToFile(tableMetas);
	}

	protected void genModelContent(TableMeta tableMeta) {
		Kv data = Kv.by("modelPackageName", modelPackageName);
		data.set("baseModelPackageName", baseModelPackageName);
		data.set("generateDaoInModel", generateDaoInModel);
		data.set("tableMeta", tableMeta);

		String ret = engine.getTemplate(template).renderToString(data);
		tableMeta.modelContent = ret;
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
		File dir = new File(modelOutputDir);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		String target = modelOutputDir + File.separator + tableMeta.modelName + ".java";

		File file = new File(target);
		if (file.exists()) {
			return ;	
		}

		OutputStreamWriter osw = null;
		try {
			osw = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
			osw.write(tableMeta.modelContent);
		}
		finally {
			if (osw != null) {
				osw.close();
			}
		}
	}

	public String getModelPackageName() {
		return modelPackageName;
	}

	public String getBaseModelPackageName() {
		return baseModelPackageName;
	}

	public String getModelOutputDir() {
		return modelOutputDir;
	}
}

