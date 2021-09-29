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

import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.module.ApplicationContext;
import work.ready.core.module.Initializer;
import work.ready.core.server.ApplicationConfig;
import work.ready.core.server.Ready;
import work.ready.core.template.Engine;
import work.ready.core.tools.PathUtil;
import work.ready.core.tools.StrUtil;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class RenderManager {

	private final ApplicationContext context;
	private final ApplicationConfig config;
	private Engine engine = null;
	private RenderFactory renderFactory = null;
	private String baseDownloadPath;
	private String viewPath;
	public final String viewFileExt;
	protected List<Initializer<RenderManager>> initializers = new ArrayList<>();

	public RenderManager(ApplicationContext context) {
		this.context = context;
		this.config = Ready.getApplicationConfig(context.application.getName());
		viewFileExt = config.getViewFileExt();
		Ready.post(new GeneralEvent(Event.RENDER_MANAGER_CREATE, this));

		ServiceLoader<RenderFactory> factoryServiceLoader = ServiceLoader.load(RenderFactory.class, RenderManager.class.getClassLoader());
		renderFactory = factoryServiceLoader.findFirst().orElse(Ready.beanManager().get(DefaultRenderFactory.class));
		renderFactory.setRenderManager(this);
	}

	public void addInitializer(Initializer<RenderManager> initializer) {
		this.initializers.add(initializer);
		initializers.sort(Comparator.comparing(Initializer::order));
	}

	public void startInit() {
		try {
			for (Initializer<RenderManager> i : initializers) {
				i.startInit(this);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void endInit() {
		try {
			for (Initializer<RenderManager> i : initializers) {
				i.endInit(this);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public RenderFactory getRenderFactory() {
		return renderFactory;
	}

	public void setRenderFactory(RenderFactory renderFactory) {
		if (renderFactory == null) {
			throw new IllegalArgumentException("renderFactory can not be null");
		}
		this.renderFactory = renderFactory;
	}

	public String getBaseDownloadPath(){
		if(baseDownloadPath == null) {
			String downloadPath = config.getDownloadPath();
			if(StrUtil.notBlank(downloadPath))
				this.baseDownloadPath = pathAssembly(downloadPath);
		}
		return baseDownloadPath;
	}

	public String getViewPath(){
		if(viewPath == null) {
			String viewPath = config.getViewPath();
			if(StrUtil.notBlank(viewPath))
				this.viewPath = pathAssembly(viewPath);
		}
		return viewPath;
	}

	private String pathAssembly(String path){
		path = path.trim();
		path = path.replaceAll("\\\\", "/");

		String basePath;
		
		if (PathUtil.isAbsolutePath(path)) {
			basePath = path.startsWith("@") ? path.substring(1) : path;
		} else {
			if(!config.isViewInJar() && Ready.rootExist() && Files.isDirectory(Ready.root().resolve(path))){
				basePath = Ready.root().resolve(path).toAbsolutePath().toString();
			} else {
				config.setViewInJar(true);
				basePath = Paths.get(PathUtil.getRootClassPath()).resolve(path).toString();
			}
		}

		if (basePath.equals("/") == false) {
			if (basePath.endsWith("/")) {
				basePath = basePath.substring(0, basePath.length() - 1);
			}
		}
		return basePath;
	}

	public synchronized Engine getEngine() {
		if(engine == null) {
			engine = Ready.beanManager().get(Engine.class);
			if(getViewPath() == null || config.isViewInJar()) {
				
				engine.setBaseTemplatePath(getViewPath());
				engine.setToClassPathSourceFactory();
			} else {
				engine.setDevMode(Ready.getBootstrapConfig().isDevMode());
				engine.setBaseTemplatePath(getViewPath());
			}
		}
		return engine;
	}

	private Map<Integer, String> errorViewMapping = new HashMap<Integer, String>();
	
	public void setError404View(String error404View) {
		errorViewMapping.put(404, error404View);
	}

	public void setError500View(String error500View) {
		errorViewMapping.put(500, error500View);
	}

	public void setError401View(String error401View) {
		errorViewMapping.put(401, error401View);
	}

	public void setError403View(String error403View) {
		errorViewMapping.put(403, error403View);
	}

	public void setErrorView(int errorCode, String errorView) {
		errorViewMapping.put(errorCode, errorView);
	}

	public String getErrorView(int errorCode) {
		return errorViewMapping.get(errorCode);
	}

}

