/**
 *
 * Original work Copyright apache, Spring
 * Modified Copyright (c) 2020 WeiHua Lyu [ready.work]
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
package work.ready.core.tools;

import work.ready.core.aop.transformer.TransformerManager;
import work.ready.core.server.Ready;
import work.ready.core.tools.converter.TypeConverter;
import work.ready.core.tools.define.ConcurrentReferenceHashMap;
import work.ready.core.tools.validator.Assert;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@SuppressWarnings("all")
public abstract class ClassUtil {

	public static final String ARRAY_SUFFIX = "[]";

	private static final String INTERNAL_ARRAY_PREFIX = "[";

	private static final String NON_PRIMITIVE_ARRAY_PREFIX = "[L";

	private static final char PACKAGE_SEPARATOR = '.';

	private static final char PATH_SEPARATOR = '/';

	private static final char INNER_CLASS_SEPARATOR = '$';

	public static final String PROXY_CLASS_SEPARATOR = TransformerManager.PROXY_CLASS_SEPARATOR;

	public static final String CLASS_FILE_SUFFIX = ".class";

	private static final Method[] NO_METHODS = {};
	private static final Field[] NO_FIELDS = {};

	private static final Map<Class<?>, Class<?>> primitiveWrapperTypeMap = new IdentityHashMap<Class<?>, Class<?>>(8);

	private static final Map<Class<?>, Class<?>> primitiveTypeToWrapperMap = new IdentityHashMap<Class<?>, Class<?>>(8);

	private static final Map<String, Class<?>> primitiveTypeNameMap = new HashMap<String, Class<?>>(32);

	private static final Map<String, Class<?>> commonClassCache = new HashMap<String, Class<?>>(32);

	private static ConcurrentHashMap<String, Class<?>> classExistCache = new ConcurrentHashMap<>();

	private static Map<Class<?>, Map<String, Object>> uniqueMethodCache = new ConcurrentHashMap<>();

	private static Map<Class<?>, Map<String, Method>> classReadMethods = new ConcurrentHashMap<>();
	private static Map<Class<?>, Map<String, Method>> classWriteMethods = new ConcurrentHashMap<>();

	private static final Map<Class<?>, Method[]> publicMethodsCache =
			new ConcurrentReferenceHashMap<Class<?>, Method[]>(256);

	private static final Map<Class<?>, Method[]> declaredMethodsCache =
			new ConcurrentReferenceHashMap<Class<?>, Method[]>(256);

	private static final Map<Class<?>, Field[]> declaredFieldsCache =
			new ConcurrentReferenceHashMap<Class<?>, Field[]>(256);

	static {
		primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
		primitiveWrapperTypeMap.put(Byte.class, byte.class);
		primitiveWrapperTypeMap.put(Character.class, char.class);
		primitiveWrapperTypeMap.put(Double.class, double.class);
		primitiveWrapperTypeMap.put(Float.class, float.class);
		primitiveWrapperTypeMap.put(Integer.class, int.class);
		primitiveWrapperTypeMap.put(Long.class, long.class);
		primitiveWrapperTypeMap.put(Short.class, short.class);

		for (Map.Entry<Class<?>, Class<?>> entry : primitiveWrapperTypeMap.entrySet()) {
			primitiveTypeToWrapperMap.put(entry.getValue(), entry.getKey());
			registerCommonClasses(entry.getKey());
		}

		Set<Class<?>> primitiveTypes = new HashSet<Class<?>>(32);
		primitiveTypes.addAll(primitiveWrapperTypeMap.values());
		primitiveTypes.addAll(Arrays.asList(new Class<?>[] {
				boolean[].class, byte[].class, char[].class, double[].class,
				float[].class, int[].class, long[].class, short[].class}));
		primitiveTypes.add(void.class);
		for (Class<?> primitiveType : primitiveTypes) {
			primitiveTypeNameMap.put(primitiveType.getName(), primitiveType);
		}

		registerCommonClasses(Boolean[].class, Byte[].class, Character[].class, Double[].class,
				Float[].class, Integer[].class, Long[].class, Short[].class);
		registerCommonClasses(Number.class, Number[].class, String.class, String[].class,
				Object.class, Object[].class, Class.class, Class[].class);
		registerCommonClasses(Throwable.class, Exception.class, RuntimeException.class,
				Error.class, StackTraceElement.class, StackTraceElement[].class);
	}

	public static Object newInstance(Class<?> clazz) {
		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isSimpleType(Class clazz) {
		return clazz == String.class
				|| clazz == Integer.class
				|| clazz == int.class
				|| clazz == Long.class
				|| clazz == long.class
				|| clazz == Double.class
				|| clazz == double.class
				|| clazz == Float.class
				|| clazz == float.class
				|| clazz == Boolean.class
				|| clazz == boolean.class
				|| clazz == Character.class
				|| clazz == char.class
				|| clazz == Byte.class
				|| clazz == byte.class
				|| clazz == Short.class
				|| clazz == short.class
				|| clazz == BigDecimal.class
				|| clazz == BigInteger.class
				|| clazz == java.util.Date.class
				|| clazz == java.sql.Date.class
				|| clazz == java.sql.Timestamp.class
				|| clazz == java.sql.Time.class
				|| clazz == java.time.LocalDate.class
				|| clazz == java.time.LocalDateTime.class;
	}

	public static final Object typeCast(Class<?> type, String value) throws ParseException {
		return TypeConverter.getInstance().convert(type, value);
	}

	public static boolean isBasicType(Class clazz){
		return 	clazz == int.class
				|| clazz == long.class
				|| clazz == double.class
				|| clazz == float.class
				|| clazz == boolean.class
				|| clazz == char.class
				|| clazz == byte.class
				|| clazz == short.class;
	}

	public static boolean isWrapped(java.lang.reflect.Type ct) {
		if (ct == null) {
			return false;
		} else {
			return ct == String.class || ct == Byte.class || ct == Short.class || ct == Integer.class || ct == Long.class
					|| ct == Float.class || ct == Double.class || ct == Character.class || ct == Boolean.class;
		}
	}

	public static String filterComments(String str) {
		
		String s = str.replaceAll("//(.)+\\n", "");
		s = s.replaceAll("//(.)?\\n", "");
		
		s = s.replaceAll("/\\*[\\s\\S]*?\\*/", "");
		return s;
	}

	public static Class<?> getGenericType(Type givenType){
		if(givenType instanceof ParameterizedType) {
			ParameterizedType type = (ParameterizedType) givenType;
			return (type.getActualTypeArguments()[0] instanceof Class) ? (Class)type.getActualTypeArguments()[0] : null;
		} else {
			return null;
		}
	}

	public static void getGenericType(Type givenType, Collection<Class<?>> typeList, boolean wildCard){
		if(givenType instanceof ParameterizedType) {
			ParameterizedType type = (ParameterizedType) givenType;
			typeList.add((Class<?>) type.getRawType());
			for(Type thisType : type.getActualTypeArguments()){
				getGenericType(thisType, typeList, wildCard);
			}
		} else if(wildCard && givenType instanceof WildcardType){
			WildcardType holder = (WildcardType)givenType;
			typeList.add((Class<?>) holder.getUpperBounds()[0]); 
		} else if(givenType instanceof Class){
			typeList.add((Class<?>)givenType);
		}
	}

	public static void getAllGenericType(Type givenType, Collection<Type> typeList, boolean wildCard){
		if(givenType instanceof ParameterizedType) {
			ParameterizedType type = (ParameterizedType) givenType;
			typeList.add((Class<?>) type.getRawType());
			for(Type thisType : type.getActualTypeArguments()){
				getAllGenericType(thisType, typeList, wildCard);
			}
		} else if(wildCard && givenType instanceof WildcardType){
			WildcardType holder = (WildcardType)givenType;
			typeList.add(givenType);
			for(Type type : holder.getLowerBounds()) {
				typeList.add(type);
			}
			for(Type type : holder.getUpperBounds()) {
				typeList.add(type);
			}
		} else {
			typeList.add(givenType);
		}
	}

	public static String convertToString(Object object) {
		if (object == null) {
			return "null";
		}
		if (!isSimpleType(object.getClass())) {
			return null;
		}

		if (object instanceof java.util.Date) {
			return String.valueOf(((java.util.Date) object).getTime());
		}

		return String.valueOf(object);
	}

	public static String getMethodSignature(Method method) {
		return getMethodSignature(method, true);
	}

	private static Pattern classNameShorten = Pattern.compile("(\\w+\\.)+");
	public static String getMethodSignature(Method method, boolean withClassName) {
		StringBuilder ret = new StringBuilder();
		if(withClassName) {
			ret.append(method.getDeclaringClass().getName()).append(".");
		}
		ret.append(method.getName()).append("(");
		int index = 0;
		Parameter[] params = method.getParameters();
		for (Parameter p : params) {
			if (index++ > 0) {
				ret.append(", ");
			}
			ret.append(classNameShorten.matcher(p.getParameterizedType().getTypeName()).replaceAll(""));
		}

		return ret.append(")").toString();
	}

	public static Class getGenericClass(Class<?> clazz){
		Type type = getUserClass(clazz).getGenericSuperclass();
		if(type instanceof ParameterizedType) return (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
		else{
			type = getUserClass(clazz).getSuperclass().getGenericSuperclass();
		}
		return (type instanceof ParameterizedType) ? (Class) ((ParameterizedType) type).getActualTypeArguments()[0] : null;
	}

	private static void registerCommonClasses(Class<?>... commonClasses) {
		for (Class<?> clazz : commonClasses) {
			commonClassCache.put(clazz.getName(), clazz);
		}
	}

	public static ClassLoader getDefaultClassLoader() {
		ClassLoader cl = null;
		try {
			cl = Thread.currentThread().getContextClassLoader();
		}
		catch (Throwable ex) {
			
		}
		if (cl == null) {
			
			cl = Ready.class.getClassLoader();
			if (cl == null) {
				
				try {
					cl = ClassLoader.getSystemClassLoader();
				}
				catch (Throwable ex) {
					
				}
			}
		}
		return cl;
	}

	public static ClassLoader overrideThreadContextClassLoader(ClassLoader classLoaderToUse) {
		Thread currentThread = Thread.currentThread();
		ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
		if (classLoaderToUse != null && !classLoaderToUse.equals(threadContextClassLoader)) {
			currentThread.setContextClassLoader(classLoaderToUse);
			return threadContextClassLoader;
		}
		else {
			return null;
		}
	}

	public static Class<?> forName(String name, ClassLoader classLoader) throws ClassNotFoundException, LinkageError {
		Assert.that(name).notNull("Name must not be null");

		Class<?> clazz = resolvePrimitiveClassName(name);
		if (clazz == null) {
			clazz = commonClassCache.get(name);
		}
		if (clazz != null) {
			return clazz;
		}

		if (name.endsWith(ARRAY_SUFFIX)) {
			String elementClassName = name.substring(0, name.length() - ARRAY_SUFFIX.length());
			Class<?> elementClass = forName(elementClassName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

		if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
			String elementName = name.substring(NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1);
			Class<?> elementClass = forName(elementName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

		if (name.startsWith(INTERNAL_ARRAY_PREFIX)) {
			String elementName = name.substring(INTERNAL_ARRAY_PREFIX.length());
			Class<?> elementClass = forName(elementName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

		ClassLoader clToUse = classLoader;
		if (clToUse == null) {
			clToUse = getDefaultClassLoader();
		}
		try {
			return (clToUse != null ? clToUse.loadClass(name) : Class.forName(name));
		}
		catch (ClassNotFoundException ex) {
			int lastDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR);
			if (lastDotIndex != -1) {
				String innerClassName =
						name.substring(0, lastDotIndex) + INNER_CLASS_SEPARATOR + name.substring(lastDotIndex + 1);
				try {
					return (clToUse != null ? clToUse.loadClass(innerClassName) : Class.forName(innerClassName));
				}
				catch (ClassNotFoundException ex2) {
					
				}
			}
			throw ex;
		}
	}

	public static Class<?> forName(String name, boolean returnNullIfException) {
		try {
			return forName(name, Ready.getClassLoader());
		}catch (Exception e){
			return null;
		}
	}

	public static Class<?> resolveClassName(String className, ClassLoader classLoader) throws IllegalArgumentException {
		try {
			return forName(className, classLoader);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException("Cannot find class [" + className + "]", ex);
		}
		catch (LinkageError ex) {
			throw new IllegalArgumentException(
					"Error loading class [" + className + "]: problem with class file or dependent class.", ex);
		}
	}

	public static Class<?> resolvePrimitiveClassName(String name) {
		Class<?> result = null;

		if (name != null && name.length() <= 8) {
			
			result = primitiveTypeNameMap.get(name);
		}
		return result;
	}

	public static boolean isPresent(String className, ClassLoader classLoader) {
		try {
			forName(className, classLoader);
			return true;
		}
		catch (Throwable ex) {
			
			return false;
		}
	}

	public static Class<?> getUserClass(Object instance) {
		Assert.that(instance).notNull("Instance must not be null");
		return getUserClass(instance.getClass());
	}

	public static Class<?> getUserClass(Class<?> clazz) {
		if (clazz != null && clazz.getName().contains(PROXY_CLASS_SEPARATOR)) {
			Class<?> superclass = clazz.getSuperclass();
			if (superclass != null && Object.class != superclass) {
				return superclass;
			}
		}
		return clazz;
	}

	public static boolean isCacheSafe(Class<?> clazz, ClassLoader classLoader) {
		Assert.that(clazz).notNull("Class must not be null");
		try {
			ClassLoader target = clazz.getClassLoader();
			if (target == null) {
				return true;
			}
			ClassLoader cur = classLoader;
			if (cur == target) {
				return true;
			}
			while (cur != null) {
				cur = cur.getParent();
				if (cur == target) {
					return true;
				}
			}
			return false;
		}
		catch (SecurityException ex) {
			
			return true;
		}
	}

	public static String getShortName(String className) {
		Assert.that(className).hasLength("Class name must not be empty");
		int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		int nameEndIndex = className.indexOf(PROXY_CLASS_SEPARATOR);
		if (nameEndIndex == -1) {
			nameEndIndex = className.length();
		}
		String shortName = className.substring(lastDotIndex + 1, nameEndIndex);
		shortName = shortName.replace(INNER_CLASS_SEPARATOR, PACKAGE_SEPARATOR);
		return shortName;
	}

	public static String getShortName(Class<?> clazz) {
		return getShortName(getQualifiedName(clazz));
	}

	public static String getShortNameAsProperty(Class<?> clazz) {
		String shortName = getShortName(clazz);
		int dotIndex = shortName.lastIndexOf(PACKAGE_SEPARATOR);
		shortName = (dotIndex != -1 ? shortName.substring(dotIndex + 1) : shortName);
		return Introspector.decapitalize(shortName);
	}

	public static String getClassFileName(Class<?> clazz) {
		Assert.that(clazz).notNull("Class must not be null");
		String className = clazz.getName();
		int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		return className.substring(lastDotIndex + 1) + CLASS_FILE_SUFFIX;
	}

	public static String getPackageName(Class<?> clazz) {
		Assert.that(clazz).notNull("Class must not be null");
		return getPackageName(clazz.getName());
	}

	public static String getPackageName(String fqClassName) {
		Assert.that(fqClassName).notNull("Class name must not be null");
		int lastDotIndex = fqClassName.lastIndexOf(PACKAGE_SEPARATOR);
		return (lastDotIndex != -1 ? fqClassName.substring(0, lastDotIndex) : "");
	}

	public static String getQualifiedName(Class<?> clazz) {
		Assert.that(clazz).notNull("Class must not be null");
		if (clazz.isArray()) {
			return getQualifiedNameForArray(clazz);
		}
		else {
			return clazz.getName();
		}
	}

	private static String getQualifiedNameForArray(Class<?> clazz) {
		StringBuilder result = new StringBuilder();
		while (clazz.isArray()) {
			clazz = clazz.getComponentType();
			result.append(ARRAY_SUFFIX);
		}
		result.insert(0, clazz.getName());
		return result.toString();
	}

	public static String getQualifiedMethodName(Method method) {
		return getQualifiedMethodName(method, null);
	}

	public static String getQualifiedMethodName(Method method, Class<?> clazz) {
		Assert.that(method).notNull("Method must not be null");
		return (clazz != null ? clazz : method.getDeclaringClass()).getName() + '.' + method.getName();
	}

	public static String getDescriptiveType(Object value) {
		if (value == null) {
			return null;
		}
		Class<?> clazz = value.getClass();
		if (Proxy.isProxyClass(clazz)) {
			StringBuilder result = new StringBuilder(clazz.getName());
			result.append(" implementing ");
			Class<?>[] ifcs = clazz.getInterfaces();
			for (int i = 0; i < ifcs.length; i++) {
				result.append(ifcs[i].getName());
				if (i < ifcs.length - 1) {
					result.append(',');
				}
			}
			return result.toString();
		}
		else if (clazz.isArray()) {
			return getQualifiedNameForArray(clazz);
		}
		else {
			return clazz.getName();
		}
	}

	public static boolean matchesTypeName(Class<?> clazz, String typeName) {
		return (typeName != null &&
				(typeName.equals(clazz.getName()) || typeName.equals(clazz.getSimpleName()) ||
				(clazz.isArray() && typeName.equals(getQualifiedNameForArray(clazz)))));
	}

	public static boolean hasConstructor(Class<?> clazz, Class<?>... paramTypes) {
		return (getConstructorIfAvailable(clazz, paramTypes) != null);
	}

	public static <T> Constructor<T> getConstructorIfAvailable(Class<T> clazz, Class<?>... paramTypes) {
		Assert.that(clazz).notNull("Class must not be null");
		try {
			return clazz.getConstructor(paramTypes);
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}

	public static boolean hasMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		return (getMethodIfAvailable(clazz, methodName, paramTypes) != null);
	}

	public static Method getMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		Assert.that(clazz).notNull("Class must not be null");
		Assert.that(methodName).notNull("Method name must not be null");
		if (paramTypes != null) {
			try {
				return clazz.getMethod(methodName, paramTypes);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException("Expected method not found: " + ex);
			}
		}
		else {
			Set<Method> candidates = new HashSet<Method>(1);
			Method[] methods = clazz.getMethods();
			for (Method method : methods) {
				if (methodName.equals(method.getName())) {
					candidates.add(method);
				}
			}
			if (candidates.size() == 1) {
				return candidates.iterator().next();
			}
			else if (candidates.isEmpty()) {
				throw new IllegalStateException("Expected method not found: " + clazz.getName() + '.' + methodName);
			}
			else {
				throw new IllegalStateException("No unique method found: " + clazz.getName() + '.' + methodName);
			}
		}
	}

	public static Method getMethodIfAvailable(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		Assert.that(clazz).notNull("Class must not be null");
		Assert.that(methodName).notNull("Method name must not be null");
		if (paramTypes != null) {
			try {
				return clazz.getMethod(methodName, paramTypes);
			}
			catch (NoSuchMethodException ex) {
				return null;
			}
		}
		else {
			Set<Method> candidates = new HashSet<Method>(1);
			Method[] methods = clazz.getMethods();
			for (Method method : methods) {
				if (methodName.equals(method.getName())) {
					candidates.add(method);
				}
			}
			if (candidates.size() == 1) {
				return candidates.iterator().next();
			}
			return null;
		}
	}

	public static int getMethodCountForName(Class<?> clazz, String methodName) {
		Assert.that(clazz).notNull("Class must not be null");
		Assert.that(methodName).notNull("Method name must not be null");
		int count = 0;
		Method[] declaredMethods = clazz.getDeclaredMethods();
		for (Method method : declaredMethods) {
			if (methodName.equals(method.getName())) {
				count++;
			}
		}
		Class<?>[] ifcs = clazz.getInterfaces();
		for (Class<?> ifc : ifcs) {
			count += getMethodCountForName(ifc, methodName);
		}
		if (clazz.getSuperclass() != null) {
			count += getMethodCountForName(clazz.getSuperclass(), methodName);
		}
		return count;
	}

	public static boolean hasAtLeastOneMethodWithName(Class<?> clazz, String methodName) {
		Assert.that(clazz).notNull("Class must not be null");
		Assert.that(methodName).notNull("Method name must not be null");
		Method[] declaredMethods = clazz.getDeclaredMethods();
		for (Method method : declaredMethods) {
			if (method.getName().equals(methodName)) {
				return true;
			}
		}
		Class<?>[] ifcs = clazz.getInterfaces();
		for (Class<?> ifc : ifcs) {
			if (hasAtLeastOneMethodWithName(ifc, methodName)) {
				return true;
			}
		}
		return (clazz.getSuperclass() != null && hasAtLeastOneMethodWithName(clazz.getSuperclass(), methodName));
	}

	public static Method getMostSpecificMethod(Method method, Class<?> targetClass) {
		if (method != null && isOverridable(method, targetClass) &&
				targetClass != null && targetClass != method.getDeclaringClass()) {
			try {
				if (Modifier.isPublic(method.getModifiers())) {
					try {
						return targetClass.getMethod(method.getName(), method.getParameterTypes());
					}
					catch (NoSuchMethodException ex) {
						return method;
					}
				}
				else {
					Method specificMethod = findMethod(targetClass, method.getName(), method.getParameterTypes());
					return (specificMethod != null ? specificMethod : method);
				}
			}
			catch (SecurityException ex) {
				
			}
		}
		return method;
	}

	public static boolean isUserLevelMethod(Method method) {
		Assert.that(method).notNull("Method must not be null");
		return (method.isBridge() || (!method.isSynthetic() && !isGroovyObjectMethod(method)));
	}

	private static boolean isGroovyObjectMethod(Method method) {
		return method.getDeclaringClass().getName().equals("groovy.lang.GroovyObject");
	}

	private static boolean isOverridable(Method method, Class<?> targetClass) {
		if (Modifier.isPrivate(method.getModifiers())) {
			return false;
		}
		if (Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers())) {
			return true;
		}
		return getPackageName(method.getDeclaringClass()).equals(getPackageName(targetClass));
	}

	public static Method getStaticMethod(Class<?> clazz, String methodName, Class<?>... args) {
		Assert.that(clazz).notNull("Class must not be null");
		Assert.that(methodName).notNull("Method name must not be null");
		try {
			Method method = clazz.getMethod(methodName, args);
			return Modifier.isStatic(method.getModifiers()) ? method : null;
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}

	public static boolean isPrimitiveWrapper(Class<?> clazz) {
		Assert.that(clazz).notNull("Class must not be null");
		return primitiveWrapperTypeMap.containsKey(clazz);
	}

	public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
		Assert.that(clazz).notNull("Class must not be null");
		return (clazz.isPrimitive() || isPrimitiveWrapper(clazz));
	}

	public static boolean isPrimitiveArray(Class<?> clazz) {
		Assert.that(clazz).notNull("Class must not be null");
		return (clazz.isArray() && clazz.getComponentType().isPrimitive());
	}

	public static boolean isPrimitiveWrapperArray(Class<?> clazz) {
		Assert.that(clazz).notNull("Class must not be null");
		return (clazz.isArray() && isPrimitiveWrapper(clazz.getComponentType()));
	}

	public static Class<?> resolvePrimitiveIfNecessary(Class<?> clazz) {
		Assert.that(clazz).notNull("Class must not be null");
		return (clazz.isPrimitive() && clazz != void.class ? primitiveTypeToWrapperMap.get(clazz) : clazz);
	}

	public static boolean isAssignable(Type lhsType, Type rhsType) {
		Assert.that(lhsType).notNull("Left-hand side type must not be null");
		Assert.that(rhsType).notNull("Right-hand side type must not be null");

		if (lhsType.equals(rhsType) || Object.class == lhsType) {
			return true;
		}

		if (lhsType instanceof Class) {
			Class<?> lhsClass = (Class<?>) lhsType;

			if (rhsType instanceof Class) {
				return isAssignable(lhsClass, (Class<?>) rhsType);
			}

			if (rhsType instanceof ParameterizedType) {
				Type rhsRaw = ((ParameterizedType) rhsType).getRawType();

				if (rhsRaw instanceof Class) {
					return isAssignable(lhsClass, (Class<?>) rhsRaw);
				}
			}
			else if (lhsClass.isArray() && rhsType instanceof GenericArrayType) {
				Type rhsComponent = ((GenericArrayType) rhsType).getGenericComponentType();

				return isAssignable(lhsClass.getComponentType(), rhsComponent);
			}
		}

		if (lhsType instanceof ParameterizedType) {
			if (rhsType instanceof Class) {
				Type lhsRaw = ((ParameterizedType) lhsType).getRawType();

				if (lhsRaw instanceof Class) {
					return isAssignable((Class<?>) lhsRaw, (Class<?>) rhsType);
				}
			}
			else if (rhsType instanceof ParameterizedType) {
				return isAssignable((ParameterizedType) lhsType, (ParameterizedType) rhsType);
			}
		}

		if (lhsType instanceof GenericArrayType) {
			Type lhsComponent = ((GenericArrayType) lhsType).getGenericComponentType();

			if (rhsType instanceof Class) {
				Class<?> rhsClass = (Class<?>) rhsType;

				if (rhsClass.isArray()) {
					return isAssignable(lhsComponent, rhsClass.getComponentType());
				}
			}
			else if (rhsType instanceof GenericArrayType) {
				Type rhsComponent = ((GenericArrayType) rhsType).getGenericComponentType();

				return isAssignable(lhsComponent, rhsComponent);
			}
		}

		if (lhsType instanceof WildcardType) {
			return isAssignable((WildcardType) lhsType, rhsType);
		}

		return false;
	}

	private static boolean isAssignable(ParameterizedType lhsType, ParameterizedType rhsType) {
		if (lhsType.equals(rhsType)) {
			return true;
		}

		Type[] lhsTypeArguments = lhsType.getActualTypeArguments();
		Type[] rhsTypeArguments = rhsType.getActualTypeArguments();

		if (lhsTypeArguments.length != rhsTypeArguments.length) {
			return false;
		}

		for (int size = lhsTypeArguments.length, i = 0; i < size; ++i) {
			Type lhsArg = lhsTypeArguments[i];
			Type rhsArg = rhsTypeArguments[i];

			if (!lhsArg.equals(rhsArg) &&
					!(lhsArg instanceof WildcardType && isAssignable((WildcardType) lhsArg, rhsArg))) {
				return false;
			}
		}

		return true;
	}

	private static boolean isAssignable(WildcardType lhsType, Type rhsType) {
		Type[] lUpperBounds = lhsType.getUpperBounds();

		if (lUpperBounds.length == 0) {
			lUpperBounds = new Type[] { Object.class };
		}

		Type[] lLowerBounds = lhsType.getLowerBounds();

		if (lLowerBounds.length == 0) {
			lLowerBounds = new Type[] { null };
		}

		if (rhsType instanceof WildcardType) {

			WildcardType rhsWcType = (WildcardType) rhsType;
			Type[] rUpperBounds = rhsWcType.getUpperBounds();

			if (rUpperBounds.length == 0) {
				rUpperBounds = new Type[] { Object.class };
			}

			Type[] rLowerBounds = rhsWcType.getLowerBounds();

			if (rLowerBounds.length == 0) {
				rLowerBounds = new Type[] { null };
			}

			for (Type lBound : lUpperBounds) {
				for (Type rBound : rUpperBounds) {
					if (!isAssignableBound(lBound, rBound)) {
						return false;
					}
				}

				for (Type rBound : rLowerBounds) {
					if (!isAssignableBound(lBound, rBound)) {
						return false;
					}
				}
			}

			for (Type lBound : lLowerBounds) {
				for (Type rBound : rUpperBounds) {
					if (!isAssignableBound(rBound, lBound)) {
						return false;
					}
				}

				for (Type rBound : rLowerBounds) {
					if (!isAssignableBound(rBound, lBound)) {
						return false;
					}
				}
			}
		}
		else {
			for (Type lBound : lUpperBounds) {
				if (!isAssignableBound(lBound, rhsType)) {
					return false;
				}
			}

			for (Type lBound : lLowerBounds) {
				if (!isAssignableBound(rhsType, lBound)) {
					return false;
				}
			}
		}

		return true;
	}

	public static boolean isAssignableBound(Type lhsType, Type rhsType) {
		if (rhsType == null) {
			return true;
		}
		if (lhsType == null) {
			return false;
		}
		return isAssignable(lhsType, rhsType);
	}

	public static boolean isAssignable(Class<?> lhsType, Class<?> rhsType) {
		Assert.that(lhsType).notNull("Left-hand side type must not be null");
		Assert.that(rhsType).notNull("Right-hand side type must not be null");
		if (lhsType.isAssignableFrom(rhsType)) {
			return true;
		}
		if (lhsType.isPrimitive()) {
			Class<?> resolvedPrimitive = primitiveWrapperTypeMap.get(rhsType);
			if (lhsType == resolvedPrimitive) {
				return true;
			}
		}
		else {
			Class<?> resolvedWrapper = primitiveTypeToWrapperMap.get(rhsType);
			if (resolvedWrapper != null && lhsType.isAssignableFrom(resolvedWrapper)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isAssignableValue(Class<?> type, Object value) {
		Assert.that(type).notNull("Type must not be null");
		return (value != null ? isAssignable(type, value.getClass()) : !type.isPrimitive());
	}

	public static String convertResourcePathToClassName(String resourcePath) {
		Assert.that(resourcePath).notNull("Resource path must not be null");
		return resourcePath.replace(PATH_SEPARATOR, PACKAGE_SEPARATOR);
	}

	public static String convertClassNameToResourcePath(String className) {
		Assert.that(className).notNull("Class name must not be null");
		return className.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
	}

	public static String addResourcePathToPackagePath(Class<?> clazz, String resourceName) {
		Assert.that(resourceName).notNull("Resource name must not be null");
		if (!resourceName.startsWith("/")) {
			return classPackageAsResourcePath(clazz) + '/' + resourceName;
		}
		return classPackageAsResourcePath(clazz) + resourceName;
	}

	public static String classPackageAsResourcePath(Class<?> clazz) {
		if (clazz == null) {
			return "";
		}
		String className = clazz.getName();
		int packageEndIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		if (packageEndIndex == -1) {
			return "";
		}
		String packageName = className.substring(0, packageEndIndex);
		return packageName.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
	}

	public static String classNamesToString(Class<?>... classes) {
		return classNamesToString(Arrays.asList(classes));
	}

	public static String classNamesToString(Collection<Class<?>> classes) {
		if (CollectionUtil.isEmpty(classes)) {
			return "[]";
		}
		StringBuilder sb = new StringBuilder("[");
		for (Iterator<Class<?>> it = classes.iterator(); it.hasNext(); ) {
			Class<?> clazz = it.next();
			sb.append(clazz.getName());
			if (it.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append("]");
		return sb.toString();
	}

	public static Class<?>[] toClassArray(Collection<Class<?>> collection) {
		if (collection == null) {
			return null;
		}
		return collection.toArray(new Class<?>[collection.size()]);
	}

	public static Class<?>[] getAllInterfaces(Object instance) {
		Assert.that(instance).notNull("Instance must not be null");
		return getAllInterfacesForClass(instance.getClass());
	}

	public static Class<?>[] getAllInterfacesForClass(Class<?> clazz) {
		return getAllInterfacesForClass(clazz, null);
	}

	public static Class<?>[] getAllInterfacesForClass(Class<?> clazz, ClassLoader classLoader) {
		Set<Class<?>> ifcs = getAllInterfacesForClassAsSet(clazz, classLoader);
		return ifcs.toArray(new Class<?>[ifcs.size()]);
	}

	public static Set<Class<?>> getAllInterfacesAsSet(Object instance) {
		Assert.that(instance).notNull("Instance must not be null");
		return getAllInterfacesForClassAsSet(instance.getClass());
	}

	public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz) {
		return getAllInterfacesForClassAsSet(clazz, null);
	}

	public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz, ClassLoader classLoader) {
		Assert.that(clazz).notNull("Class must not be null");
		if (clazz.isInterface() && isVisible(clazz, classLoader)) {
			return Collections.<Class<?>>singleton(clazz);
		}
		Set<Class<?>> interfaces = new LinkedHashSet<Class<?>>();
		while (clazz != null) {
			Class<?>[] ifcs = clazz.getInterfaces();
			for (Class<?> ifc : ifcs) {
				interfaces.addAll(getAllInterfacesForClassAsSet(ifc, classLoader));
			}
			clazz = clazz.getSuperclass();
		}
		return interfaces;
	}

	public static Object createProxyByInterface(Class<?>[] interfaces, InvocationHandler handler, ClassLoader classLoader) {
		Assert.that(interfaces).notEmpty("Interfaces must not be empty");
		return Proxy.newProxyInstance(
				classLoader != null ? classLoader : Ready.getClassLoader(),
				interfaces,
				handler);
	}

	public static Class<?> determineCommonAncestor(Class<?> clazz1, Class<?> clazz2) {
		if (clazz1 == null) {
			return clazz2;
		}
		if (clazz2 == null) {
			return clazz1;
		}
		if (clazz1.isAssignableFrom(clazz2)) {
			return clazz1;
		}
		if (clazz2.isAssignableFrom(clazz1)) {
			return clazz2;
		}
		Class<?> ancestor = clazz1;
		do {
			ancestor = ancestor.getSuperclass();
			if (ancestor == null || Object.class == ancestor) {
				return null;
			}
		}
		while (!ancestor.isAssignableFrom(clazz2));
		return ancestor;
	}

	public static boolean isVisible(Class<?> clazz, ClassLoader classLoader) {
		if (classLoader == null) {
			return true;
		}
		try {
			Class<?> actualClass = classLoader.loadClass(clazz.getName());
			return (clazz == actualClass);
			
		}
		catch (ClassNotFoundException ex) {
			
			return false;
		}
	}

	public static boolean isProxy(Object object) {
		return isProxyClass(object.getClass());
	}

	public static boolean isProxyClass(Class<?> clazz) {
		return (clazz != null && isProxyClassName(clazz.getName()));
	}

	public static boolean isProxyClassName(String className) {
		return (className != null && className.contains(PROXY_CLASS_SEPARATOR));
	}

	public static Class<?> checkClassExist(String className) {
		Class<?> result = classExistCache.get(className);
		if (result != null) {
			if (Object.class.equals(result))
				return null;
			else
				return result;
		}
		try {
			result = Class.forName(className);
			if (result != null)
				classExistCache.put(className, result);
			else
				classExistCache.put(className, Object.class);
			return result;
		} catch (Exception e) {
			classExistCache.put(className, Object.class);
			return null;
		}
	}

	public static void registerClass(Class<?> clazz) {
		classExistCache.put(clazz.getName(), clazz);
	}

	public static Method checkMethodExist(Class<?> clazz, String uniqueMethodName) {
		if (clazz == null || StrUtil.isBlank(uniqueMethodName))
			return null;
		Map<String, Object> methodMap = uniqueMethodCache.get(clazz);
		if (methodMap != null && !methodMap.isEmpty()) {
			Object result = methodMap.get(uniqueMethodName);
			if (result != null) {
				if (Object.class.equals(result))
					return null;
				else
					return (Method) result;
			}
		}
		if (methodMap == null) {
			methodMap = new HashMap<>();
			uniqueMethodCache.put(clazz, methodMap);
		}
		Method[] methods = clazz.getMethods();
		for (Method method : methods)
			if (uniqueMethodName != null && uniqueMethodName.equals(method.getName())) {
				methodMap.put(uniqueMethodName, method);
				return method;
			}
		methodMap.put(uniqueMethodName, Object.class);
		return null;
	}

	private static LinkedHashMap<String, Method> sortMap(Map<String, Method> map) {
		List<Map.Entry<String, Method>> list = new ArrayList<Map.Entry<String, Method>>(map.entrySet());
		Collections.sort(list, Comparator.comparing(Map.Entry::getKey));
		LinkedHashMap<String, Method> result = new LinkedHashMap<String, Method>();
		for (Map.Entry<String, Method> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	public static synchronized void cacheReadWriteMethodsAndBoxField(Class<?> clazz) {
		BeanInfo beanInfo = null;
		PropertyDescriptor[] pds = null;
		try {
			beanInfo = Introspector.getBeanInfo(clazz);
			pds = beanInfo.getPropertyDescriptors();
		} catch (Exception e) {
			throw new RuntimeException("Class '" + clazz + "' can not get bean info", e);
		}

		Map<String, Method> readMethods = new HashMap<>();
		Map<String, Method> writeMethods = new HashMap<>();
		for (PropertyDescriptor pd : pds) {
			String fieldName = pd.getName();
			if ("class".equals(fieldName) || "simpleName".equals(fieldName) || "canonicalName".equals(fieldName)
					|| "box".equals(fieldName))
				continue;
			Method readMtd = pd.getReadMethod();
			readMethods.put(fieldName, readMtd);
			Method writeMtd = pd.getWriteMethod();
			if (writeMtd == null) {
				writeMtd = findMethod(clazz, "set" + StrUtil.firstCharToUpperCase(fieldName),
						readMtd.getReturnType());
			}
			writeMethods.put(fieldName, writeMtd);
		}
		classReadMethods.put(clazz, sortMap(readMethods));
		classWriteMethods.put(clazz, sortMap(writeMethods));
	}

	public static Map<String, Method> getClassReadMethods(Class<?> clazz) {
		Map<String, Method> readMethods = classReadMethods.get(clazz);
		if (readMethods == null) {
			cacheReadWriteMethodsAndBoxField(clazz);
			return classReadMethods.get(clazz);
		} else
			return readMethods;
	}

	public static Method getClassFieldReadMethod(Class<?> clazz, String fieldName) {
		return getClassReadMethods(clazz).get(fieldName);
	}

	public static Map<String, Method> getClassWriteMethods(Class<?> clazz) {
		Map<String, Method> writeMethods = classWriteMethods.get(clazz);
		if (writeMethods == null) {
			cacheReadWriteMethodsAndBoxField(clazz);
			return classWriteMethods.get(clazz);
		} else
			return writeMethods;
	}

	public static Method getClassFieldWriteMethod(Class<?> clazz, String fieldName) {
		return getClassWriteMethods(clazz).get(fieldName);
	}

	public static Object readValueFromBeanField(Object entityBean, String fieldName) {
		Method readMethod = getClassFieldReadMethod(entityBean.getClass(), fieldName);
		if (readMethod == null) {
			throw new RuntimeException(
					"No mapping found for field '" + fieldName + "' in '" + entityBean.getClass() + "'");
		} else
			try {
				return readMethod.invoke(entityBean);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
	}

	public static void writeValueToBeanField(Object entityBean, String fieldName, Object value) {
		Method writeMethod = getClassFieldWriteMethod(entityBean.getClass(), fieldName);
		if (writeMethod == null) {
			throw new RuntimeException("Can not find Java bean read method '" + fieldName + "'");
		} else
			try {
				writeMethod.invoke(entityBean, value);
			} catch (Exception e) {
				throw new RuntimeException("FieldName '" + fieldName + "' can not write with value '" + value + "'", e);
			}
	}

	public static Field findField(Class<?> clazz, String name) {
		return findField(clazz, name, null);
	}

	public static Field findField(Class<?> clazz, String name, Class<?> type) {
		Assert.that(clazz).notNull("Class must not be null");
		Assert.that(name != null || type != null).isTrue("Either name or type of the field must be specified");
		Class<?> searchType = clazz;
		while (Object.class != searchType && searchType != null) {
			Field[] fields = getDeclaredFields(searchType);
			for (Field field : fields) {
				if ((name == null || name.equals(field.getName())) &&
						(type == null || type.equals(field.getType()))) {
					return field;
				}
			}
			searchType = searchType.getSuperclass();
		}
		return null;
	}

	public static void setField(Field field, Object target, Object value) {
		try {
			field.set(target, value);
		}
		catch (IllegalAccessException ex) {
			handleReflectionException(ex);
			throw new IllegalStateException(
					"Unexpected reflection exception - " + ex.getClass().getName() + ": " + ex.getMessage());
		}
	}

	public static Object getField(Field field, Object target) {
		try {
			return field.get(target);
		}
		catch (IllegalAccessException ex) {
			handleReflectionException(ex);
			throw new IllegalStateException(
					"Unexpected reflection exception - " + ex.getClass().getName() + ": " + ex.getMessage());
		}
	}

	public static Method findMethod(Class<?> clazz, String name) {
		return findMethod(clazz, name, new Class<?>[0]);
	}

	public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
		Assert.that(clazz).notNull("Class must not be null");
		Assert.that(name).notNull("Method name must not be null");
		Class<?> searchType = clazz;
		while (searchType != null) {
			Method[] methods = (searchType.isInterface() ? searchType.getMethods() : getDeclaredMethods(searchType));
			for (Method method : methods) {
				if (name.equals(method.getName()) &&
						(paramTypes == null || Arrays.equals(paramTypes, method.getParameterTypes()))) {
					return method;
				}
			}
			searchType = searchType.getSuperclass();
		}
		return null;
	}

	public static Object invokeMethod(Method method, Object target) {
		return invokeMethod(method, target, new Object[0]);
	}

	public static Object invokeMethod(Method method, Object target, Object... args) {
		try {
			return method.invoke(target, args);
		}
		catch (Exception ex) {
			handleReflectionException(ex);
		}
		throw new IllegalStateException("Should never get here");
	}

	public static Object invokeJdbcMethod(Method method, Object target) throws SQLException {
		return invokeJdbcMethod(method, target, new Object[0]);
	}

	public static Object invokeJdbcMethod(Method method, Object target, Object... args) throws SQLException {
		try {
			return method.invoke(target, args);
		}
		catch (IllegalAccessException ex) {
			handleReflectionException(ex);
		}
		catch (InvocationTargetException ex) {
			if (ex.getTargetException() instanceof SQLException) {
				throw (SQLException) ex.getTargetException();
			}
			handleInvocationTargetException(ex);
		}
		throw new IllegalStateException("Should never get here");
	}

	public static void handleReflectionException(Exception ex) {
		if (ex instanceof NoSuchMethodException) {
			throw new IllegalStateException("Method not found: " + ex.getMessage());
		}
		if (ex instanceof IllegalAccessException) {
			throw new IllegalStateException("Could not access method: " + ex.getMessage());
		}
		if (ex instanceof InvocationTargetException) {
			handleInvocationTargetException((InvocationTargetException) ex);
		}
		if (ex instanceof RuntimeException) {
			throw (RuntimeException) ex;
		}
		throw new UndeclaredThrowableException(ex);
	}

	public static void handleInvocationTargetException(InvocationTargetException ex) {
		rethrowRuntimeException(ex.getTargetException());
	}

	public static void rethrowRuntimeException(Throwable ex) {
		if (ex instanceof RuntimeException) {
			throw (RuntimeException) ex;
		}
		if (ex instanceof Error) {
			throw (Error) ex;
		}
		throw new UndeclaredThrowableException(ex);
	}

	public static void rethrowException(Throwable ex) throws Exception {
		if (ex instanceof Exception) {
			throw (Exception) ex;
		}
		if (ex instanceof Error) {
			throw (Error) ex;
		}
		throw new UndeclaredThrowableException(ex);
	}

	public static boolean declaresException(Method method, Class<?> exceptionType) {
		Assert.that(method).notNull("Method must not be null");
		Class<?>[] declaredExceptions = method.getExceptionTypes();
		for (Class<?> declaredException : declaredExceptions) {
			if (declaredException.isAssignableFrom(exceptionType)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isPublicStaticFinal(Field field) {
		int modifiers = field.getModifiers();
		return (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers));
	}

	public static boolean isEqualsMethod(Method method) {
		if (method == null || !method.getName().equals("equals")) {
			return false;
		}
		Class<?>[] paramTypes = method.getParameterTypes();
		return (paramTypes.length == 1 && paramTypes[0] == Object.class);
	}

	public static boolean isHashCodeMethod(Method method) {
		return (method != null && method.getName().equals("hashCode") && method.getParameterTypes().length == 0);
	}

	public static boolean isToStringMethod(Method method) {
		return (method != null && method.getName().equals("toString") && method.getParameterTypes().length == 0);
	}

	public static boolean isObjectMethod(Method method) {
		if (method == null) {
			return false;
		}
		try {
			Object.class.getDeclaredMethod(method.getName(), method.getParameterTypes());
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}

	public static void makeAccessible(Field field) {
		if ((!Modifier.isPublic(field.getModifiers()) ||
				!Modifier.isPublic(field.getDeclaringClass().getModifiers()) ||
				Modifier.isFinal(field.getModifiers()))) {
			field.setAccessible(true);
		}
	}

	public static void makeAccessible(Method method) {
		if ((!Modifier.isPublic(method.getModifiers()) ||
				!Modifier.isPublic(method.getDeclaringClass().getModifiers()))) {
			method.setAccessible(true);
		}
	}

	public static void makeAccessible(Constructor<?> ctor) {
		if ((!Modifier.isPublic(ctor.getModifiers()) ||
				!Modifier.isPublic(ctor.getDeclaringClass().getModifiers()))) {
			ctor.setAccessible(true);
		}
	}

	public static void doWithLocalMethods(Class<?> clazz, MethodCallback mc) {
		Method[] methods = getDeclaredMethods(clazz);
		for (Method method : methods) {
			try {
				mc.doWith(method);
			}
			catch (IllegalAccessException ex) {
				throw new IllegalStateException("Not allowed to access method '" + method.getName() + "': " + ex);
			}
		}
	}

	public static void doWithMethods(Class<?> clazz, MethodCallback mc) {
		doWithMethods(clazz, mc, null);
	}

	public static void doWithMethods(Class<?> clazz, MethodCallback mc, MethodFilter mf) {
		
		Method[] methods = getDeclaredMethods(clazz);
		for (Method method : methods) {
			if (mf != null && !mf.matches(method)) {
				continue;
			}
			try {
				mc.doWith(method);
			}
			catch (IllegalAccessException ex) {
				throw new IllegalStateException("Not allowed to access method '" + method.getName() + "': " + ex);
			}
		}
		if (clazz.getSuperclass() != null) {
			doWithMethods(clazz.getSuperclass(), mc, mf);
		}
		else if (clazz.isInterface()) {
			for (Class<?> superIfc : clazz.getInterfaces()) {
				doWithMethods(superIfc, mc, mf);
			}
		}
	}

	public static Method[] getAllDeclaredMethods(Class<?> leafClass) {
		final List<Method> methods = new ArrayList<Method>(32);
		doWithMethods(leafClass, new MethodCallback() {
			@Override
			public void doWith(Method method) {
				methods.add(method);
			}
		});
		return methods.toArray(new Method[methods.size()]);
	}

	public static Method[] getUniqueDeclaredMethods(Class<?> leafClass) {
		final List<Method> methods = new ArrayList<Method>(32);
		doWithMethods(leafClass, new MethodCallback() {
			@Override
			public void doWith(Method method) {
				boolean knownSignature = false;
				Method methodBeingOverriddenWithCovariantReturnType = null;
				for (Method existingMethod : methods) {
					if (method.getName().equals(existingMethod.getName()) &&
							Arrays.equals(method.getParameterTypes(), existingMethod.getParameterTypes())) {
						
						if (existingMethod.getReturnType() != method.getReturnType() &&
								existingMethod.getReturnType().isAssignableFrom(method.getReturnType())) {
							methodBeingOverriddenWithCovariantReturnType = existingMethod;
						}
						else {
							knownSignature = true;
						}
						break;
					}
				}
				if (methodBeingOverriddenWithCovariantReturnType != null) {
					methods.remove(methodBeingOverriddenWithCovariantReturnType);
				}
				if (!knownSignature) {
					methods.add(method);
				}
			}
		});
		return methods.toArray(new Method[methods.size()]);
	}

	public static Method[] getMethods(Class<?> clazz) {
		Assert.that(clazz).notNull("Class must not be null");
		Method[] result = publicMethodsCache.get(clazz);
		if (result == null) {
			result = clazz.getMethods();
			publicMethodsCache.put(clazz, (result.length == 0 ? NO_METHODS : result));
		}
		return result;
	}

	public static Method[] getDeclaredMethods(Class<?> clazz) {
		Assert.that(clazz).notNull("Class must not be null");
		Method[] result = declaredMethodsCache.get(clazz);
		if (result == null) {
			Method[] declaredMethods = clazz.getDeclaredMethods();
			List<Method> defaultMethods = findConcreteMethodsOnInterfaces(clazz);
			if (defaultMethods != null) {
				result = new Method[declaredMethods.length + defaultMethods.size()];
				System.arraycopy(declaredMethods, 0, result, 0, declaredMethods.length);
				int index = declaredMethods.length;
				for (Method defaultMethod : defaultMethods) {
					result[index] = defaultMethod;
					index++;
				}
			}
			else {
				result = declaredMethods;
			}
			declaredMethodsCache.put(clazz, (result.length == 0 ? NO_METHODS : result));
		}
		return result;
	}

	private static List<Method> findConcreteMethodsOnInterfaces(Class<?> clazz) {
		List<Method> result = null;
		for (Class<?> ifc : clazz.getInterfaces()) {
			for (Method ifcMethod : ifc.getMethods()) {
				if (!Modifier.isAbstract(ifcMethod.getModifiers())) {
					if (result == null) {
						result = new LinkedList<Method>();
					}
					result.add(ifcMethod);
				}
			}
		}
		return result;
	}

	public static void doWithLocalFields(Class<?> clazz, FieldCallback fc) {
		for (Field field : getDeclaredFields(clazz)) {
			try {
				fc.doWith(field);
			}
			catch (IllegalAccessException ex) {
				throw new IllegalStateException("Not allowed to access field '" + field.getName() + "': " + ex);
			}
		}
	}

	public static void doWithFields(Class<?> clazz, FieldCallback fc) {
		doWithFields(clazz, fc, null);
	}

	public static void doWithFields(Class<?> clazz, FieldCallback fc, FieldFilter ff) {
		
		Class<?> targetClass = clazz;
		do {
			Field[] fields = getDeclaredFields(targetClass);
			for (Field field : fields) {
				if (ff != null && !ff.matches(field)) {
					continue;
				}
				try {
					fc.doWith(field);
				}
				catch (IllegalAccessException ex) {
					throw new IllegalStateException("Not allowed to access field '" + field.getName() + "': " + ex);
				}
			}
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);
	}

	public static Field[] getDeclaredFields(Class<?> clazz) {
		Assert.that(clazz).notNull("Class must not be null");
		Field[] result = declaredFieldsCache.get(clazz);
		if (result == null) {
			result = clazz.getDeclaredFields();
			declaredFieldsCache.put(clazz, (result.length == 0 ? NO_FIELDS : result));
		}
		return result;
	}

	public static void shallowCopyFieldState(final Object src, final Object dest) {
		if (src == null) {
			throw new IllegalArgumentException("Source for field copy cannot be null");
		}
		if (dest == null) {
			throw new IllegalArgumentException("Destination for field copy cannot be null");
		}
		if (!src.getClass().isAssignableFrom(dest.getClass())) {
			throw new IllegalArgumentException("Destination class [" + dest.getClass().getName() +
					"] must be same or subclass as source class [" + src.getClass().getName() + "]");
		}
		doWithFields(src.getClass(), new FieldCallback() {
			@Override
			public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
				makeAccessible(field);
				Object srcValue = field.get(src);
				field.set(dest, srcValue);
			}
		}, COPYABLE_FIELDS);
	}

	public static void clearCache() {
		publicMethodsCache.clear();
		declaredMethodsCache.clear();
		declaredFieldsCache.clear();
	}

	public interface MethodCallback {

		void doWith(Method method) throws IllegalArgumentException, IllegalAccessException;
	}

	public interface MethodFilter {

		boolean matches(Method method);
	}

	public interface FieldCallback {

		void doWith(Field field) throws IllegalArgumentException, IllegalAccessException;
	}

	public interface FieldFilter {

		boolean matches(Field field);
	}

	public static final FieldFilter COPYABLE_FIELDS = new FieldFilter() {

		@Override
		public boolean matches(Field field) {
			return !(Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers()));
		}
	};

	public static final MethodFilter NON_BRIDGED_METHODS = new MethodFilter() {

		@Override
		public boolean matches(Method method) {
			return !method.isBridge();
		}
	};

	public static final MethodFilter USER_DECLARED_METHODS = new MethodFilter() {

		@Override
		public boolean matches(Method method) {
			return (!method.isBridge() && method.getDeclaringClass() != Object.class);
		}
	};

}
