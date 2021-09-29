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

import work.ready.core.aop.AopComponent;
import work.ready.core.aop.annotation.Before;
import work.ready.core.aop.annotation.Clear;
import work.ready.core.aop.annotation.GlobalInterceptor;
import work.ready.core.aop.transformer.TransformerManager;
import work.ready.core.config.ConfigInjector;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.CoreContext;
import work.ready.core.template.Engine;
import work.ready.core.template.Template;
import work.ready.core.tools.EvalUtil;
import work.ready.core.tools.define.Kv;
import work.ready.core.tools.StrUtil;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class ProxyGenerator {

	private static final Log logger = LogFactory.getLog(ProxyGenerator.class);

	protected CoreContext context;
	protected Engine engine = new Engine("forProxy").setToClassPathSourceFactory();
	protected Template template = engine.getTemplate("work/ready/core/component/proxy/proxy_class_template.tpl");
	protected List<JavaCoder> javaCoders = new ArrayList<>();
	protected boolean printGeneratedClassToLogger = false;

	public ProxyGenerator(CoreContext context){
		this.context = context;
	}

	public ProxyClass generate(Class<?> target) {
		ProxyClass proxyClass = new ProxyClass(target);

		Kv classData = Kv.create();
		classData.set("pkg", proxyClass.getPkg());
		classData.set("name", proxyClass.getName());
		classData.set("targetName", getTargetName(target));

		@SuppressWarnings("rawtypes")
		TypeVariable[] tvs = target.getTypeParameters();
		classData.set("classTypeVars", getTypeVars(tvs));
		classData.set("targetTypeVars", getTargetTypeVars(tvs));

		List<Class<?>> globalAndClassLevelInterceptors = getGlobalAndClassLevelInterceptors(proxyClass);

		List<Kv> methodList = new ArrayList<>();
		classData.set("methodList", methodList);
		Method[] methodArray = target.getMethods();
		Constructor<?>[] constructors = target.getDeclaredConstructors();
		List<Kv> constructorList = new ArrayList<>();
		classData.set("constructorList", constructorList);
		for(var constructor : constructors){
			if(Modifier.isPrivate(constructor.getModifiers())) continue;
			Kv methodData = Kv.create();
			Parameter[] params = constructor.getParameters();
			methodData.set("paramTypes", getParameterTypes(params));
			if (params.length == 1) {
				if (params[0].getType().isArray() || params[0].isVarArgs()) {
					methodData.set("singleArrayParam", true);
				}
			}
			methodData.set("modifier", Modifier.toString(constructor.getModifiers()));
			constructorList.add(methodData);
		}

		for (Method method : methodArray) {
			if (isSkipMethod(method)) {
				continue ;
			}

			List<JavaCoder> matchedCoders = getMatchedAutoCoders(target, method);

			Kv methodData = Kv.create();
			if ( hasInterceptor(globalAndClassLevelInterceptors, proxyClass, method) ) {
				methodData.set("hasInterceptor", true);
			} else {
				if(matchedCoders.size() == 0) continue;
				methodData.set("hasInterceptor", false);
			}

			methodData.set("autoCoders", matchedCoders);
			methodData.set("methodTypeVars", getTypeVars(method.getTypeParameters()));
			methodData.set("returnType", getReturnType(method));
			methodData.set("returnTypeForVar", (method.getReturnType().isPrimitive() && !void.class.equals(method.getReturnType()))
												? primitiveToWrappedType(method.getReturnType()) : getReturnType(method));
			methodData.set("name", method.getName());
			methodData.set("throws", getThrows(method));

			Parameter[] params = method.getParameters();
			methodData.set("paramTypes", getParameterTypes(params));

			Long proxyMethodKey = context.getProxyManager().proxyMethodCache.generateKey();
			methodData.set("proxyMethodKey", proxyMethodKey);

			if (params.length == 1) {
				if (params[0].getType().isArray() || params[0].isVarArgs()) {
					methodData.set("singleArrayParam", true);
				}
			}

			if (method.getReturnType() != void.class) {
				methodData.set("frontReturn", "return ");
			} else {
				methodData.set("backReturn", "return null;");
			}

			matchedCoders.sort(Comparator.comparingInt(JavaCoder::getOrder));
			matchedCoders.forEach(coder->{
				LinkedList<CodeGenerator> list = (LinkedList<CodeGenerator>)methodData.get("codeGenerator");
				if(list == null) list = new LinkedList<>();
				var generator = coder.getGenerator().generatorCode(target, method, classData, methodData);
				list.add(generator);
				methodData.set("codeGenerator", list);
				if(generator.isReplace()){
					methodData.set("hasReplace", true);
				}
			});

			methodList.add(methodData);

			ProxyMethod proxyMethod = new ProxyMethod(context);
			proxyClass.addProxyMethod(proxyMethod);
			proxyMethod.setKey(proxyMethodKey);
			proxyMethod.setTargetClass(target);
			proxyMethod.setMethod(method);
		}

		if (proxyClass.needProxy()) {

			String sourceCode = template.renderToString(classData);
			proxyClass.setSourceCode(sourceCode);

			if (printGeneratedClassToLogger && logger.isDebugEnabled()) {
				String msg = "\nGenerate proxy class \"" + proxyClass.getPkg() + "." + proxyClass.getName() + "\":";
				logger.debug(msg + sourceCode);
			}
		}

		return proxyClass;
	}

	private List<String> getParameterTypes(Parameter[] Parameters){
		return Arrays.asList(Parameters).stream().map(
				x -> {

					StringBuilder sb = new StringBuilder();
					Type type = x.getParameterizedType();
					String typename = type.getTypeName();

					if(x.isVarArgs()) {
						sb.append(typename.replaceFirst("\\[\\]$", "..."));
					} else {
						sb.append(typename);
					}

					return sb.toString();
				}
		).collect(Collectors.toList());
	}

	protected String getTargetName(Class<?> target) {
		if (Modifier.isStatic(target.getModifiers())) {

			String ret = target.getName();
			int index = ret.lastIndexOf('$');
			return ret.substring(0, index) + "." + ret.substring(index + 1);
		} else {
			return target.getSimpleName();
		}
	}

	protected String getReturnType(Method method) {

		return method.getGenericReturnType().getTypeName();
	}

	protected String primitiveToWrappedType(Class<?> baseType){
		if(int.class.equals(baseType)) return Integer.class.getCanonicalName();
		if(long.class.equals(baseType)) return Long.class.getCanonicalName();
		if(boolean.class.equals(baseType)) return Boolean.class.getCanonicalName();
		if(short.class.equals(baseType)) return Short.class.getCanonicalName();
		if(float.class.equals(baseType)) return Float.class.getCanonicalName();
		if(double.class.equals(baseType)) return Double.class.getCanonicalName();
		if(char.class.equals(baseType)) return Character.class.getCanonicalName();
		if(byte.class.equals(baseType)) return Byte.class.getCanonicalName();
		return null;
	}

	@SuppressWarnings("rawtypes")
	protected String getTypeVars(TypeVariable[] typeVars) {
		if (typeVars == null|| typeVars.length == 0) {
			return null;
		}

		StringBuilder ret = new StringBuilder();

		ret.append('<');
		for (int i=0; i<typeVars.length; i++) {
			TypeVariable tv = typeVars[i];
			if (i > 0) {
				ret.append(", ");
			}

			ret.append(tv.getName());

			Type[] bounds = tv.getBounds();
			if (bounds.length == 1) {
				if (bounds[0] != Object.class) {
					ret.append(" extends ").append(bounds[0].getTypeName());
					continue ;
				}
			} else {
				for (int j=0; j<bounds.length; j++) {
					String tn = bounds[j].getTypeName();
					if (j > 0) {
						ret.append(" & ").append(tn);
					} else {
						ret.append(" extends ").append(tn);
					}
				}
			}
		}

		return ret.append('>').toString();
	}

	@SuppressWarnings("rawtypes")
	protected String getTargetTypeVars(TypeVariable[] typeVars) {
		if (typeVars == null|| typeVars.length == 0) {
			return null;
		}

		StringBuilder ret = new StringBuilder();
		ret.append('<');
		for (int i=0; i<typeVars.length; i++) {
			TypeVariable tv = typeVars[i];
			if (i > 0) {
				ret.append(", ");
			}
			ret.append(tv.getName());
		}
		return ret.append('>').toString();
	}

	protected String getThrows(Method method) {
		Class<?>[] throwTypes = method.getExceptionTypes();
		if (throwTypes == null || throwTypes.length == 0) {
			return null;
		}

		StringBuilder ret = new StringBuilder().append("throws ");
		for (int i=0; i<throwTypes.length; i++) {
			if (i > 0) {
				ret.append(", ");
			}
			ret.append(throwTypes[i].getName());
		}
		return ret.append(' ').toString();
	}

	protected boolean isSkipMethod(Method method) {
		int mod = method.getModifiers();
		if ( ! Modifier.isPublic(mod) ) {
			return true;
		}

		if (Modifier.isFinal(mod) || Modifier.isStatic(mod) || Modifier.isAbstract(mod)) {
			return true;
		}

		String n = method.getName();
		if (n.equals("toString") || n.equals("hashCode") || n.equals("equals") || n.startsWith(TransformerManager.METHOD_PREFIX)) {
			return true;
		}

		return false;
	}

	protected List<Class<?>> getGlobalAndClassLevelInterceptors(ProxyClass proxyClass) {
		List<Class<?>> ret;

		Clear clearOnClass = proxyClass.getTarget().getAnnotation(Clear.class);
		if (clearOnClass != null) {
			Class<?>[] clearInterceptorsOnClass = clearOnClass.value();
			if (clearInterceptorsOnClass.length != 0) {	
				ret = context.getInterceptorManager().getGlobalServiceInterceptorClasses();
				removeInterceptor(ret, clearInterceptorsOnClass);
			} else {
				ret = new ArrayList<>(3);
			}
		} else {
			ret = context.getInterceptorManager().getGlobalServiceInterceptorClasses();
		}

		Iterator<Class<?>> it = ret.iterator();
		while(it.hasNext()){
			Class<?> clazz = it.next();
			var annotation  = clazz.getAnnotation(GlobalInterceptor.class);
			if(annotation == null) continue;
			String match = ConfigInjector.getStringValue(annotation.match());
			if (StrUtil.notBlank(match)) {
				boolean elPass = EvalUtil.eval(match, Kv.by("name", proxyClass.getTarget().getCanonicalName()));
				if(logger.isDebugEnabled())
					logger.debug("GlobalInterceptor [" + clazz.getCanonicalName() + "] on " + proxyClass.getTarget().getCanonicalName() + ": match=[" + match + "] result=[" + elPass + "]");
				if (!elPass) {
					it.remove();
				}
			} else {
				it.remove();
			}
		}

		Before beforeOnClass = proxyClass.getTarget().getAnnotation(Before.class);
		if (beforeOnClass != null) {
			Class<?>[] classInterceptors = beforeOnClass.value();
			ret.addAll(Arrays.asList(classInterceptors));
		}

		for(AopComponent component : context.getInterceptorManager().getAopComponents()){
			if(proxyClass.getTarget().isAnnotationPresent(component.getAnnotation())){
				ret.add(component.getInterceptorClass());
			}
		}

		return ret;
	}

	protected void removeInterceptor(List<Class<?>> target, Class<?>[] clearInterceptors) {
		if (target.isEmpty() || clearInterceptors.length == 0) {
			return ;
		}

		for (Iterator<Class<?>> it = target.iterator(); it.hasNext();) {
			Class<?> interClass = it.next();
			for (Class<?> c : clearInterceptors) {
				if (c == interClass) {
					it.remove();
					break ;
				}
			}
		}
	}

	protected boolean hasInterceptor(List<Class<?>> globalAndClassLevelInterceptors, ProxyClass proxyClass, Method method) {

		Before beforeOnMethod = method.getAnnotation(Before.class);
		if (beforeOnMethod != null && beforeOnMethod.value().length != 0) {
			return true;
		}

		for(AopComponent component : context.getInterceptorManager().getAopComponents()){
			if(method.isAnnotationPresent(component.getAnnotation())){
				return true;
			}
		}

		List<Class<?>> ret;
		Clear clearOnMethod = method.getAnnotation(Clear.class);
		if (clearOnMethod != null) {
			Class<?>[] clearInterceptorsOnMethod = clearOnMethod.value();
			if (clearInterceptorsOnMethod.length != 0) {

				ret = copyInterceptors(globalAndClassLevelInterceptors);
				removeInterceptor(ret, clearInterceptorsOnMethod);
			} else {
				ret = null;
			}
		} else {
			ret = globalAndClassLevelInterceptors;
		}

		return ret != null && ret.size() > 0;
	}

	protected List<Class<?>> copyInterceptors(List<Class<?>> methodInterceptors) {
		return new ArrayList<>(methodInterceptors);
	}

	private List<JavaCoder> getMatchedAutoCoders(Class<?> target, Method method) {
		List<JavaCoder> matchedCoders = new ArrayList<>();
		for(JavaCoder coder : javaCoders){
			if(coder.getAnnotation() == null || coder.getGenerator() == null) continue;
			boolean matched = false;
			if(coder.getAssignableFrom() != null && coder.getAssignableFrom().isAssignableFrom(target)){
				matched = true;
			} else if (StrUtil.notBlank(coder.getNameContains()) && target.getCanonicalName().contains(ConfigInjector.getStringValue(coder.getNameContains()))){
				matched = true;
			}
			if(matched && (target.getClass().isAnnotationPresent(coder.getAnnotation()) || method.isAnnotationPresent(coder.getAnnotation()))){
				matchedCoders.add(coder);
			}
		}
		return matchedCoders;
	}

	public void setPrintGeneratedClassToLogger(boolean printGeneratedClassToLogger) {
		this.printGeneratedClassToLogger = printGeneratedClassToLogger;
	}

	public void setProxyClassTemplate(String proxyClassTemplate) {
		template = engine.getTemplate(proxyClassTemplate);
	}
}

