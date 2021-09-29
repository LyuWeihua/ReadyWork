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

package work.ready.core.database.sql;

import work.ready.core.database.SqlParam;
import work.ready.core.template.Engine;
import work.ready.core.template.Template;
import work.ready.core.template.source.FileSourceFactory;
import work.ready.core.template.source.TemplateSource;
import work.ready.core.tools.StrUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unchecked", "rawtypes"})
public class SqlKit {

	static final String SQL_TEMPLATE_MAP_KEY = "_SQL_TEMPLATE_MAP_";
	static final String SQL_PARAM_KEY = "_SQL_PARAM_";
	static final String PARAM_ARRAY_KEY = "_PARAM_ARRAY_"; 

	private String configName;
	private boolean devMode;
	private Engine engine;
	private List<SqlTemplateSource> sqlSourceList = new ArrayList<>();
	private Map<String, Template> sqlTemplateMap;

	public SqlKit(String configName, boolean devMode) {
		this.configName = configName;
		this.devMode = devMode;

		engine = new Engine(configName);
		engine.setDevMode(devMode);
		engine.setToClassPathSourceFactory();

		engine.addDirective("namespace", NameSpaceDirective.class);
		engine.addDirective("sql", SqlDirective.class);

		engine.addDirective("param", ParamDirective.class, true);
		engine.addDirective("p", ParamDirective.class, true);		
	}

	public SqlKit(String configName) {
		this(configName, false);
	}

	public Engine getEngine() {
		return engine;
	}

	public void setDevMode(boolean devMode) {
		this.devMode = devMode;
		engine.setDevMode(devMode);
	}

	public void setBaseSqlTemplatePath(String baseSqlTemplatePath) {
		engine.setBaseTemplatePath(baseSqlTemplatePath);
		if(StrUtil.notBlank(baseSqlTemplatePath)){
			engine.setSourceFactory(new FileSourceFactory());
		}
	}

	public void addSqlTemplate(String sqlTemplate) {
		if (StrUtil.isBlank(sqlTemplate)) {
			throw new IllegalArgumentException("sqlTemplate can not be blank");
		}
		sqlSourceList.add(new SqlTemplateSource(sqlTemplate));
		if(sqlTemplateMap != null) reloadModifiedSqlTemplate();
	}

	public void addSqlTemplate(TemplateSource sqlTemplate) {
		if (sqlTemplate == null) {
			throw new IllegalArgumentException("sqlTemplate can not be null");
		}
		sqlSourceList.add(new SqlTemplateSource(sqlTemplate));
		if(sqlTemplateMap != null) reloadModifiedSqlTemplate();
	}

	public synchronized void parseSqlTemplate() {
		Map<String, Template> sqlTemplateMap = new HashMap<String, Template>(512, 0.5F);
		for (SqlTemplateSource ss : sqlSourceList) {
			Template template = ss.isFile() ? engine.getTemplate(ss.file) : engine.getTemplate(ss.source);
			Map<Object, Object> data = new HashMap<Object, Object>();
			data.put(SQL_TEMPLATE_MAP_KEY, sqlTemplateMap);
			template.renderToString(data);
		}
		this.sqlTemplateMap = sqlTemplateMap;
	}

	private void reloadModifiedSqlTemplate() {
		engine.removeAllTemplateCache();	
		parseSqlTemplate();
	}

	private boolean isSqlTemplateModified() {
		for (Template template : sqlTemplateMap.values()) {
			if (template.isModified()) {
				return true;
			}
		}
		return false;
	}

	private Template getSqlTemplate(String key) {
		Template template = sqlTemplateMap.get(key);
		if (template == null) {	
			if ( !devMode ) {
				return null;
			}
			if (isSqlTemplateModified()) {
				synchronized (this) {
					if (isSqlTemplateModified()) {
						reloadModifiedSqlTemplate();
						template = sqlTemplateMap.get(key);
					}
				}
			}
			return template;
		}

		if (devMode && template.isModified()) {
			synchronized (this) {
				template = sqlTemplateMap.get(key);
				if (template.isModified()) {
					reloadModifiedSqlTemplate();
					template = sqlTemplateMap.get(key);
				}
			}
		}
		return template;
	}

	public String getSql(String key) {
		Template template = getSqlTemplate(key);
		return template != null ? template.renderToString(null) : null;
	}

	public SqlParam getSqlParam(String key, Map data) {
		Template template = getSqlTemplate(key);
		if (template == null) {
			return null;
		}

		SqlParam sqlParam = new SqlParam();
		data.put(SQL_PARAM_KEY, sqlParam);
		sqlParam.setSql(template.renderToString(data));
		data.remove(SQL_PARAM_KEY);	
		return sqlParam;
	}

	public SqlParam getSqlParam(String key, Object... params) {
		Template template = getSqlTemplate(key);
		if (template == null) {
			return null;
		}

		SqlParam sqlParam = new SqlParam();
		Map data = new HashMap();
		data.put(SQL_PARAM_KEY, sqlParam);
		data.put(PARAM_ARRAY_KEY, params);
		sqlParam.setSql(template.renderToString(data));
		
		return sqlParam;
	}

	public java.util.Set<Map.Entry<String, Template>> getSqlMapEntrySet() {
		return sqlTemplateMap.entrySet();
	}

	@Override
	public String toString() {
		return "SqlKit for config : " + configName;
	}

	public SqlParam getSqlParamByString(String content, Map data) {
		Template template = engine.getTemplateByString(content);

		SqlParam sqlParam = new SqlParam();
		data.put(SQL_PARAM_KEY, sqlParam);
		sqlParam.setSql(template.renderToString(data));
		data.remove(SQL_PARAM_KEY);	
		return sqlParam;
	}

	public SqlParam getSqlParamByString(String content, Object... params) {
		Template template = engine.getTemplateByString(content);

		SqlParam sqlParam = new SqlParam();
		Map data = new HashMap();
		data.put(SQL_PARAM_KEY, sqlParam);
		data.put(PARAM_ARRAY_KEY, params);
		sqlParam.setSql(template.renderToString(data));
		
		return sqlParam;
	}
}

