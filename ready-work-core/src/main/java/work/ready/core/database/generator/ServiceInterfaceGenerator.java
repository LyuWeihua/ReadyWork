/**
 * Copyright (c) 2015-2020, Michael Yang 杨福海 (fuhai999@gmail.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import work.ready.core.tools.StrUtil;
import work.ready.core.tools.define.Kv;

import java.io.*;
import java.util.List;

public class ServiceInterfaceGenerator {

    protected Engine engine;
    protected String template = getClass().getPackageName().replace('.','/') + "/service_template.tpl";

    protected JavaKeyword javaKeyword = JavaKeyword.INSTANCE;
    protected boolean generateChainSetter = false;
    protected String serviceOutputDir;
    private String modelPackageName;
    private String servicePackageName;

    public ServiceInterfaceGenerator(String servicePackageName, String modelPackageName) {
        if (StrUtil.isBlank(servicePackageName)) {
            throw new IllegalArgumentException("servicePackageName can not be blank.");
        }
        if (servicePackageName.contains("/") || servicePackageName.contains("\\")) {
            throw new IllegalArgumentException("servicePackageName error : " + servicePackageName);
        }
        if (StrUtil.isBlank(modelPackageName)) {
            throw new IllegalArgumentException("modelPackageName can not be blank.");
        }
        if (modelPackageName.contains("/") || modelPackageName.contains("\\")) {
            throw new IllegalArgumentException("modelPackageName error : " + servicePackageName);
        }
        this.servicePackageName = servicePackageName;
        this.modelPackageName = modelPackageName;
        this.serviceOutputDir = buildOutPutDir();
        initEngine();
    }

    public ServiceInterfaceGenerator(String servicePackageName, String modelPackageName, String serviceOutputDir) {
        if (StrUtil.isBlank(servicePackageName)) {
            throw new IllegalArgumentException("servicePackageName can not be blank.");
        }
        if (servicePackageName.contains("/") || servicePackageName.contains("\\")) {
            throw new IllegalArgumentException("baseModelPackageName error : " + servicePackageName);
        }
        if (StrUtil.isBlank(serviceOutputDir)) {
            throw new IllegalArgumentException("service serviceOutputDir can not be blank.");
        }
        if (StrUtil.isBlank(modelPackageName)) {
            throw new IllegalArgumentException("modelPackageName can not be blank.");
        }
        if (modelPackageName.contains("/") || modelPackageName.contains("\\")) {
            throw new IllegalArgumentException("modelPackageName error : " + modelPackageName);
        }
        this.servicePackageName = servicePackageName;
        this.serviceOutputDir = serviceOutputDir;
        this.modelPackageName = modelPackageName;
        initEngine();
    }

    private String buildOutPutDir() {
        return PathUtil.getProjectRootPath() + "/src/main/java/" + servicePackageName.replace(".", "/");
    }

    protected void initEngine() {
        engine = Engine.create("forService");
        engine.setToClassPathSourceFactory();	
        engine.addSharedMethod(new StrUtil());
        engine.addSharedObject("getterTypeMap", Generator.getterTypeMap);
        engine.addSharedObject("javaKeyword", javaKeyword);
    }

    public void generate(List<TableMeta> tableMetas) {
        System.out.println("Generate base model ...");
        System.out.println("Service Output Dir: " + serviceOutputDir);

        for (TableMeta tableMeta : tableMetas) {
            genServiceContent(tableMeta);
        }
        writeToFile(tableMetas);
    }

    protected void genServiceContent(TableMeta tableMeta) {
        Kv data = Kv.by("tableMeta", tableMeta);
        data.set("generateChainSetter", generateChainSetter);
        data.set("modelPackageName", modelPackageName);
        data.set("servicePackageName", servicePackageName);

        tableMeta.serviceInterfaceContent = engine.getTemplate(template).renderToString(data);
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
        File dir = new File(serviceOutputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String target = serviceOutputDir + File.separator + tableMeta.modelName + "Service" + ".java";

        File targetFile = new File(target);
        if (targetFile.exists()) {
            return;
        }

        OutputStreamWriter osw = null;
        try {
            osw = new OutputStreamWriter(new FileOutputStream(targetFile), "UTF-8");
            osw.write(tableMeta.serviceInterfaceContent);
        }
        finally {
            if (osw != null) {
                osw.close();
            }
        }
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public void setGenerateChainSetter(boolean generateChainSetter) {
        this.generateChainSetter = generateChainSetter;
    }

    public String getServicePackageName() {
        return servicePackageName;
    }

    public String getServiceOutputDir() {
        return serviceOutputDir;
    }
}
