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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static work.ready.core.aop.transformer.TransformerManager.ENHANCER;

public class ProxyClass implements SourceCode {

	private Class<?> target;

	private String pkg;						
	private String name;						
	private String sourceCode;				
	private Map<String, byte[]> byteCode;	
	private Class<?> clazz;					
	private List<ProxyMethod> proxyMethodList = new ArrayList<>();

	public ProxyClass(Class<?> target) {
		this.target = target;
		this.pkg = target.getPackage().getName();
		this.name = target.getSimpleName() + ENHANCER;
	}

	public boolean needProxy() {
		return proxyMethodList.size() > 0;
	}

	public Class<?> getTarget() {
		return target;
	}

	public String getPkg() {
		return pkg;
	}

	public void setPkg(String pkg) { this.pkg = pkg; }

	public String getName() {
		return name;
	}

	public void setName(String name) { this.name = name; }

	public String getSourceCode() {
		return sourceCode;
	}

	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}

	public Map<String, byte[]> getByteCode() {
		return byteCode;
	}

	public void setByteCode(Map<String, byte[]> byteCode) {
		this.byteCode = byteCode;
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public void setClazz(Class<?> clazz) {
		this.clazz = clazz;
	}

	public void addProxyMethod(ProxyMethod proxyMethod) {
		proxyMethodList.add(proxyMethod);
	}

	public List<ProxyMethod> getProxyMethodList() {
		return proxyMethodList;
	}
}

