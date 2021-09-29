package work.ready.core.template.expr.ast;

import work.ready.core.component.proxy.ProxyClassLoader;
import work.ready.core.tools.StrUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static work.ready.core.aop.transformer.TransformerManager.ENHANCER;

public class FastFieldGetter extends FieldGetter {

	protected static ProxyGenerator generator = new ProxyGenerator();
	protected static ProxyCompiler compiler = new ProxyCompiler();
	protected static ProxyClassLoader classLoader = new ProxyClassLoader();
	protected static Map<Class<?>, Proxy> cache = new ConcurrentHashMap<>(512, 0.25F);

	protected static boolean outputCompileError = false;

	protected Proxy proxy;
	protected java.lang.reflect.Method getterMethod;

	public FastFieldGetter(Proxy proxy, java.lang.reflect.Method getterMethod) {
		this.proxy = proxy;
		this.getterMethod = getterMethod;
	}

	public FastFieldGetter() {
		this(null, null);
	}

	public FieldGetter takeOver(Class<?> targetClass, String fieldName) {
		if (MethodKit.isForbiddenClass(targetClass)) {
			throw new RuntimeException("Forbidden class: " + targetClass.getName());
		}

		String getterName = "get" + StrUtil.firstCharToUpperCase(fieldName);
		java.lang.reflect.Method[] methodArray = targetClass.getMethods();
		for (java.lang.reflect.Method method : methodArray) {
			if (method.getName().equals(getterName) && method.getParameterCount() == 0) {

				Proxy proxy = cache.get(targetClass);
				if (proxy == null) {
					synchronized (targetClass) {
						proxy = cache.get(targetClass);
						if (proxy == null) {
							try {
								proxy = createProxy(targetClass, fieldName);
							} catch (Throwable e) {
								return null;
							}
							cache.putIfAbsent(targetClass, proxy);
						}
					}
				}

				return new FastFieldGetter(proxy, method);
			}
		}

		return null;
	}

	public Object get(Object target, String fieldName) throws Exception {

		return proxy.getValue(target, fieldName);
	}

	protected Proxy createProxy(Class<?> targetClass, String fieldName) {
		ProxyClass proxyClass = new ProxyClass(targetClass);
		String sourceCode = generator.generate(proxyClass);

		proxyClass.setSourceCode(sourceCode);
		compiler.compile(proxyClass);
		Class<?> retClass = classLoader.loadProxyClass(proxyClass);
		proxyClass.setClazz(retClass);
		try {
			return (Proxy)retClass.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public String toString() {
		return getterMethod.toString();
	}

	public static interface Proxy {
		public Object getValue(Object target, String fieldName);
	}

	static class ProxyClass extends work.ready.core.component.proxy.ProxyClass {

		private String name;

		public ProxyClass(Class<?> target) {
			super(target);

			name = target.getSimpleName() + ENHANCER + "$$FieldGetter";
		}

		public String getName() {
			return name;
		}
	}

	static class ProxyGenerator {

		String generate(ProxyClass proxyClass) {
			StringBuilder ret = new StringBuilder(1024);

			Class<?> targetClass = proxyClass.getTarget();
			String className = proxyClass.getName();

			ret.append("package ").append(proxyClass.getPkg()).append(";\n\n");
			ret.append("import work.ready.core.template.expr.ast.FastFieldGetter.Proxy;\n\n");
			ret.append("public class ").append(className).append(" implements Proxy {\n\n");
			ret.append("\tpublic Object getValue(Object target, String fieldName) {\n");
			ret.append("\t\tint hash = fieldName.hashCode();\n");
			ret.append("\t\tswitch (hash) {\n");

			java.lang.reflect.Method[] methodArray = targetClass.getMethods();
			for (java.lang.reflect.Method method : methodArray) {
				String mn = method.getName();
				if (method.getParameterCount() == 0 && mn.startsWith("get") && (!mn.equals("getClass"))) {
					String fieldName = StrUtil.firstCharToLowerCase(mn.substring(3));
					ret.append("\t\tcase ").append(fieldName.hashCode()).append(" :\n");
					ret.append("\t\t\treturn ((").append(targetClass.getName()).append(")target).").append(mn).append("();\n");
				}
			}

			ret.append("\t\tdefault :\n");
			ret.append("\t\t\tthrow new RuntimeException(\"Can not access the field \\\"\" + target.getClass().getName() + \".\" + fieldName + \"\\\"\");\n");

			ret.append("\t\t}\n");
			ret.append("\t}\n");
			ret.append("}\n");

			return ret.toString();
		}
	}

	public static void setOutputCompileError(boolean outputCompileError) {
		FastFieldGetter.outputCompileError = outputCompileError;
	}

	static class ProxyCompiler extends work.ready.core.component.proxy.ProxyCompiler {
		@Override
		protected void outputCompileError(Boolean result, javax.tools.DiagnosticCollector<javax.tools.JavaFileObject> collector) {
			if (outputCompileError) {
				super.outputCompileError(result, collector);
			}
		}
	}
}

