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

import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.module.CoreContext;
import work.ready.core.module.Initializer;
import work.ready.core.server.Ready;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unchecked")
public class ProxyManager {

	protected final ConcurrentHashMap<Class<?>, Class<?>> cache = new ConcurrentHashMap<>();
	protected final CoreContext context;
	protected ProxyGenerator proxyGenerator;
	protected ProxyCompiler proxyCompiler;
	protected ProxyClassLoader proxyClassLoader;
	protected ProxyMethodCache proxyMethodCache;
	protected List<Initializer<ProxyManager>> initializers = new ArrayList<>();

	public ProxyManager(CoreContext context){
		this.context = context;
		proxyGenerator = new ProxyGenerator(context);
		proxyCompiler = new ProxyCompiler();
		proxyClassLoader = new ProxyClassLoader();
		proxyMethodCache = new ProxyMethodCache();
		Ready.post(new GeneralEvent(Event.PROXY_MANAGER_CREATE, this));
	}

	public void addInitializer(Initializer<ProxyManager> initializer) {
		this.initializers.add(initializer);
		initializers.sort(Comparator.comparing(Initializer::order));
	}

	public void startInit() {
		try {
			for (Initializer<ProxyManager> i : initializers) {
				i.startInit(this);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void endInit() {
		try {
			for (Initializer<ProxyManager> i : initializers) {
				i.endInit(this);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public <T> Class<T> get(Class<T> target) {
		Class<T> ret = (Class<T>)cache.get(target);
		if (ret != null) {
			return ret;
		} else {
			return getProxyClass(target);
		}
	}

	protected <T> Class<T> getProxyClass(Class<T> target) {
		synchronized (target) {
			Class<T> ret = (Class<T>)cache.get(target);
			if (ret != null) {
				return ret;
			}

			int mod = target.getModifiers();
			if (!Modifier.isPublic(mod) || Modifier.isFinal(mod) || Modifier.isAbstract(mod)) {
				cache.put(target, target);
				return target;
			}

			ProxyClass proxyClass = proxyGenerator.generate(target);
			if (proxyClass.needProxy()) {
				proxyCompiler.compile(proxyClass);
				ret = (Class<T>)proxyClassLoader.loadProxyClass(proxyClass);
				proxyClass.setClazz(ret);

				cacheMethodProxy(proxyClass);

				cache.put(target, ret);
				return ret;
			} else {
				cache.put(target, target);		
				return target;
			}
		}
	}

	protected void cacheMethodProxy(ProxyClass proxyClass) {
		for (ProxyMethod m : proxyClass.getProxyMethodList()) {
			m.setProxyClass(proxyClass.getClazz());
			proxyMethodCache.put(m);
		}
	}

	public void addAutoCoder(JavaCoder coder){
		this.proxyGenerator.javaCoders.add(coder);
	}

	public void setProxyGenerator(ProxyGenerator proxyGenerator) {
		Objects.requireNonNull(proxyGenerator, "proxyGenerator can not be null");
		this.proxyGenerator = proxyGenerator;
	}

	public ProxyGenerator getProxyGenerator() {
		return proxyGenerator;
	}

	public void setProxyCompiler(ProxyCompiler proxyCompiler) {
		Objects.requireNonNull(proxyCompiler, "proxyCompiler can not be null");
		this.proxyCompiler = proxyCompiler;
	}

	public ProxyCompiler getProxyCompiler() {
		return proxyCompiler;
	}

	public void setProxyClassLoader(ProxyClassLoader proxyClassLoader) {
		Objects.requireNonNull(proxyClassLoader, "proxyClassLoader can not be null");
		this.proxyClassLoader = proxyClassLoader;
	}

	public ProxyClassLoader getProxyClassLoader() {
		return proxyClassLoader;
	}

	public ProxyMethodCache getProxyMethodCache() { return proxyMethodCache; }
}

