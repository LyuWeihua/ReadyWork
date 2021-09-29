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

import work.ready.core.template.source.TemplateSource;
import work.ready.core.template.stat.Location;
import work.ready.core.template.stat.ParseException;
import work.ready.core.template.stat.ast.Define;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Env {

	protected EngineConfig engineConfig;
	protected Map<String, Define> functionMap = new HashMap<String, Define>(16, 0.5F);

	protected List<TemplateSource> sourceList = null;

	public Env(EngineConfig engineConfig) {
		this.engineConfig = engineConfig;
	}

	public EngineConfig getEngineConfig() {
		return engineConfig;
	}

	public boolean isDevMode() {
		return engineConfig.isDevMode();
	}

	public void addFunction(Define function) {
		String fn = function.getFunctionName();
		if (functionMap.containsKey(fn)) {
			Define previous = functionMap.get(fn);
			throw new ParseException(
				"Template function \"" + fn + "\" already defined in " +
				getAlreadyDefinedLocation(previous.getLocation()),
				function.getLocation()
			);
		}
		functionMap.put(fn, function);
	}

	private String getAlreadyDefinedLocation(Location loc) {
		StringBuilder buf = new StringBuilder();
		if (loc.getTemplateFile() != null) {
			buf.append(loc.getTemplateFile()).append(", line ").append(loc.getRow());
		} else {
			buf.append("string template line ").append(loc.getRow());
		}
		return buf.toString();
	}

	public Define getFunction(String functionName) {
		Define func = functionMap.get(functionName);
		return func != null ? func : engineConfig.getSharedFunction(functionName);
	}

	Map<String, Define> getFunctionMap() {
		return functionMap;
	}

	public boolean isSourceListModified() {
		if (sourceList != null) {
			for (int i = 0, size = sourceList.size(); i < size; i++) {
				if (sourceList.get(i).isModified()) {
					return true;
				}
			}
		}
		return false;
	}

	public void addSource(TemplateSource source) {
		if (sourceList == null) {
			sourceList = new ArrayList<TemplateSource>();
		}
		sourceList.add(source);
	}
}

