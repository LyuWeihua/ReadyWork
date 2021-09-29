/**
 *
 * Original work Copyright (c) 2015-2019, Michael Yang 杨福海 (fuhai999@gmail.com).
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

package work.ready.core.tools;

import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.ApplicationConfig;
import work.ready.core.server.Constant;
import work.ready.core.server.Ready;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class ClassScanner {
    private static final Log logger = LogFactory.getLog(ClassScanner.class);

    private final Set<Class<?>> applicationClassCache = new HashSet<>();
    private final Map<Class<?>, List<?>> scanCache = new HashMap<>();

    public final Set<String> includeJars = new HashSet<>();
    public final Set<String> excludeJars = new HashSet<>();
    public final Set<String> includePackages = new HashSet<>();
    public final Set<String> excludePackages = new HashSet<>();

    private static final String CLASS_EXT = ".class";
    
    private static final String JAR_FILE_EXT = ".jar";
    
    private static final String JAR_PATH_EXT = ".jar!";
    
    private static final String PATH_FILE_PRE = "file:";

    public ClassScanner(ApplicationConfig config){
        excludeJars.add("cglib-");
        excludeJars.add("byte-buddy-");
        excludeJars.add("jai-imageio-");
        excludeJars.add("apiguardian-");
        excludeJars.add("undertow-");
        excludeJars.add("xnio-");
        excludeJars.add("javax.");
        excludeJars.add("hikaricp-");
        excludeJars.add("druid-");
        excludeJars.add("mysql-");
        excludeJars.add("db2jcc-");
        excludeJars.add("db2jcc4-");
        excludeJars.add("ojdbc");
        excludeJars.add("junit-");
        excludeJars.add("org.junit");
        excludeJars.add("hamcrest-");
        excludeJars.add("jboss-");
        excludeJars.add("motan-");
        excludeJars.add("commons-pool");
        excludeJars.add("commons-pool2");
        excludeJars.add("commons-beanutils");
        excludeJars.add("commons-codec");
        excludeJars.add("commons-collections");
        excludeJars.add("commons-configuration");
        excludeJars.add("commons-lang");
        excludeJars.add("commons-logging");
        excludeJars.add("commons-io");
        excludeJars.add("commons-httpclient");
        excludeJars.add("commons-fileupload");
        excludeJars.add("commons-validator");
        excludeJars.add("commons-email");
        excludeJars.add("commons-text");
        excludeJars.add("commons-cli");
        excludeJars.add("commons-math");
        excludeJars.add("commons-jxpath");
        excludeJars.add("commons-compress");
        excludeJars.add("audience-");
        excludeJars.add("hessian-");
        excludeJars.add("metrics-");
        excludeJars.add("javapoet-");
        excludeJars.add("netty-");
        excludeJars.add("consul-");
        excludeJars.add("gson-");
        excludeJars.add("zookeeper-");
        excludeJars.add("slf4j-");
        excludeJars.add("fastjson-");
        excludeJars.add("guava-");
        excludeJars.add("failureaccess-");
        excludeJars.add("listenablefuture-");
        excludeJars.add("jsr305-");
        excludeJars.add("checker-qual-");
        excludeJars.add("error_prone_annotations-");
        excludeJars.add("j2objc-");
        excludeJars.add("animal-sniffer-");
        excludeJars.add("cron4j-");
        excludeJars.add("jedis-");
        excludeJars.add("lettuce-");
        excludeJars.add("reactor-");
        excludeJars.add("fst-");
        excludeJars.add("kryo-");
        excludeJars.add("jackson-");
        excludeJars.add("javassist-");
        excludeJars.add("objenesis-");
        excludeJars.add("reflectasm-");
        excludeJars.add("asm-");
        excludeJars.add("minlog-");
        excludeJars.add("jsoup-");
        excludeJars.add("ons-client-");
        excludeJars.add("amqp-client-");
        excludeJars.add("ehcache-");
        excludeJars.add("sharding-");
        excludeJars.add("snakeyaml-");
        excludeJars.add("groovy-");
        excludeJars.add("profiler-");
        excludeJars.add("joda-time-");
        excludeJars.add("shiro-");
        excludeJars.add("dubbo-");
        excludeJars.add("curator-");
        excludeJars.add("resteasy-");
        excludeJars.add("reactive-");
        excludeJars.add("validation-");
        excludeJars.add("httpclient-");
        excludeJars.add("httpcore-");
        excludeJars.add("jcip-");
        excludeJars.add("jcl-");
        excludeJars.add("microprofile-");
        excludeJars.add("org.osgi");
        excludeJars.add("zkclient-");
        excludeJars.add("jjwt-");
        excludeJars.add("okhttp-");
        excludeJars.add("okio-");
        excludeJars.add("zbus-");
        excludeJars.add("swagger-");
        excludeJars.add("j2cache-");
        excludeJars.add("caffeine-");
        excludeJars.add("jline-");
        excludeJars.add("qpid-");
        excludeJars.add("geronimo-");
        excludeJars.add("activation-");
        excludeJars.add("org.abego");
        excludeJars.add("antlr-");
        excludeJars.add("antlr4-");
        excludeJars.add("st4-");
        excludeJars.add("icu4j-");
        excludeJars.add("idea_rt");
        excludeJars.add("mrjtoolkit");
        excludeJars.add("logback-");
        excludeJars.add("log4j-");
        excludeJars.add("log4j2-");
        excludeJars.add("aliyun-java-sdk-");
        excludeJars.add("aliyun-sdk-");
        excludeJars.add("archaius-");
        excludeJars.add("aopalliance-");
        excludeJars.add("hdrhistogram-");
        excludeJars.add("jdom-");
        excludeJars.add("rxjava-");
        excludeJars.add("jersey-");
        excludeJars.add("stax-");
        excludeJars.add("stax2-");
        excludeJars.add("jettison-");
        excludeJars.add("commonmark-");
        excludeJars.add("jaxb-");
        excludeJars.add("json-20");
        excludeJars.add("jcseg-");
        excludeJars.add("lucene-");
        excludeJars.add("elasticsearch-");
        excludeJars.add("mapper-extras-client-");
        excludeJars.add("jopt-");
        excludeJars.add("httpasyncclient-");
        excludeJars.add("jna-");
        excludeJars.add("lang-mustache-client-");
        excludeJars.add("parent-join-client-");
        excludeJars.add("rank-eval-client-");
        excludeJars.add("aggs-matrix-stats-client-");
        excludeJars.add("t-digest-");
        excludeJars.add("compiler-");
        excludeJars.add("hppc-");
        excludeJars.add("libthrift-");
        excludeJars.add("seata-");
        excludeJars.add("eureka-");
        excludeJars.add("netflix-");
        excludeJars.add("nacos-");
        excludeJars.add("apollo-");
        excludeJars.add("guice-");
        excludeJars.add("servlet-");
        excludeJars.add("debugger-agent.jar");
        excludeJars.add("xpp3_min-");
        excludeJars.add("latency");
        excludeJars.add("micrometer-");
        excludeJars.add("xstream-");
        excludeJars.add("jsr311-");
        excludeJars.add("servo-");
        excludeJars.add("compactmap-");
        excludeJars.add("dexx-");
        excludeJars.add("xmlpull-");
        excludeJars.add("jose4j-");
        excludeJars.add("ignite-");
        excludeJars.add("jsqlparser-");
        excludeJars.add("pinyin4j-");
        excludeJars.add("javase-");
        excludeJars.add("jakarta.");
        excludeJars.add("h2-");
        excludeJars.add("annotations-");
        excludeJars.add("protobuf-");
        excludeJars.add("wildfly-");
        excludeJars.add("cache-api-");
        excludeJars.add("oshi-");
        excludeJars.add("woodstox-");
        excludeJars.add("mockito-");
        excludeJars.add("truth-");
        excludeJars.add("redis-");
        excludeJars.add("embedded-redis-");
        excludeJars.add("aspectj");
        excludeJars.add("jcommander-");
        excludeJars.add("core-");
        excludeJars.add("opentest4j-");
        excludeJars.add("ready-work-");

        excludePackages.add("org.aopalliance.");
        excludePackages.add("org.apache.");
        excludePackages.add("org.nustaq.");
        excludePackages.add("net.sf.");
        excludePackages.add("org.slf4j.");
        excludePackages.add("org.antlr.");
        excludePackages.add("org.jboss.");
        excludePackages.add("org.javassist.");
        excludePackages.add("org.hamcrest.");
        excludePackages.add("org.jsoup.");
        excludePackages.add("org.objenesis.");
        excludePackages.add("org.ow2.");
        excludePackages.add("org.reactivest.");
        excludePackages.add("org.yaml.");
        excludePackages.add("org.checker");
        excludePackages.add("org.codehaus");
        excludePackages.add("ch.qos.");
        excludePackages.add("com.alibaba.");
        excludePackages.add("com.aliyun.open");
        excludePackages.add("com.caucho");
        excludePackages.add("com.codahale");
        excludePackages.add("com.ctrip.framework.apollo");
        excludePackages.add("com.ecwid.");
        excludePackages.add("com.esotericsoftware.");
        excludePackages.add("com.fasterxml.");
        excludePackages.add("com.github.");
        excludePackages.add("com.google.");
        excludePackages.add("com.rabbitmq.");
        excludePackages.add("com.squareup.");
        excludePackages.add("com.typesafe.");
        excludePackages.add("com.weibo.");
        excludePackages.add("com.zaxxer.");
        excludePackages.add("com.mysql.");
        excludePackages.add("org.gjt.");
        excludePackages.add("io.dropwizard");
        excludePackages.add("io.jsonwebtoken");
        excludePackages.add("io.lettuce");
        excludePackages.add("reactor.adapter");
        excludePackages.add("io.prometheus");
        excludePackages.add("io.seata.");
        excludePackages.add("io.swagger.");
        excludePackages.add("io.undertow.");
        excludePackages.add("it.sauronsoftware");
        excludePackages.add("javax.");
        excludePackages.add("java.");
        excludePackages.add("junit.");
        excludePackages.add("jline.");
        excludePackages.add("redis.");
        excludePackages.add("net.oschina.j2cache");
        excludePackages.add("com.sun.");
        excludePackages.add("org.h2.");
        excludePackages.add("org.jose4j.");
        excludePackages.add("org.xnio.");
        excludePackages.add("org.wildfly.");
        excludePackages.add("com.ctc.");
        excludePackages.add("oshi.");
        excludePackages.add("net.sourceforge.");
        excludePackages.add("org.intellij.");
        excludePackages.add("org.jsr166.");
        excludePackages.add("com.hp.");
        excludePackages.add("org.jetbrains.");
        excludePackages.add("work.ready.core.");
        excludePackages.add("work.ready.cloud.");

        String scanJarPrefix = config.getScanJarPrefix();
        if (scanJarPrefix != null) {
            for (String prefix : scanJarPrefix.split(",")) addScanJarPrefix(prefix.trim());
        }

        String unScanJarPrefix = config.getSkipJarPrefix();
        if (unScanJarPrefix != null) {
            for (String prefix : unScanJarPrefix.split(",")) addUnscanJarPrefix(prefix.trim());
        }

        Ready.post(new GeneralEvent(Event.CLASS_SCANNER_CREATE, this));
    }

    public void addScanJarPrefix(String prefix) {
        includeJars.add(prefix.trim());
    }

    public void addScanJarPrefix(List<String> prefix){
        includeJars.addAll(prefix);
    }

    public void addUnscanJarPrefix(String prefix) {
        excludeJars.add(prefix.trim());
    }

    public void addUnscanJarPrefix(List<String> prefix){
        excludeJars.addAll(prefix);
    }

    public void addScanPackage(String prefix) {
        includePackages.add(prefix.trim());
    }

    public void addScanPackage(List<String> prefix) {
        includePackages.addAll(prefix);
    }

    public void addUnscanPackage(String prefix) {
        excludePackages.add(prefix.trim());
    }

    public void addUnscanPackage(List<String> prefix) {
        excludePackages.addAll(prefix);
    }

    public <T> List<Class<T>> scanSubClass(Class<T> pclazz) {
        return scanSubClass(pclazz, false);
    }

    public <T> List<Class<T>> scanSubClass(Class<T> pclazz, boolean isInstantiable) {
        initIfNecessary();
        if(scanCache.get(pclazz) == null) {
            List<Class<T>> classes = new ArrayList<>();
            findChildClasses(classes, pclazz, false);
            scanCache.put(pclazz, classes);
        }
        return (List<Class<T>>)scanCache.get(pclazz).stream().filter(clazz -> !isInstantiable || isInstantiable((Class<?>)clazz))
                .collect(Collectors.toList());
    }

    public List<Class<?>> scanClass() {
        return scanClass(false);
    }

    public List<Class<?>> scanClass(boolean isInstantiable) {

        initIfNecessary();

        if (!isInstantiable) {
            return new ArrayList<>(applicationClassCache);
        }

        return applicationClassCache.stream()
                .filter(this::isInstantiable)
                .collect(Collectors.toList());
    }

    public void clearClassCache() {
        applicationClassCache.clear();
        scanCache.clear();
    }

    private boolean isInstantiable(Class<?> clazz) {
        return !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers());
    }

    public List<Class<?>> scanClassByMethodAnnotation(Class<? extends Annotation> annotationClass, boolean isInstantiable) {
        initIfNecessary();
        if(scanCache.get(annotationClass) == null){
            List<Class<?>> list = new ArrayList<>();
            for (Class<?> clazz : applicationClassCache) {
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(annotationClass)) {
                        list.add(clazz);
                        break;
                    }
                }
            }
            scanCache.put(annotationClass, list);
        }
        return (List<Class<?>>) scanCache.get(annotationClass).stream().filter(clazz -> !isInstantiable || isInstantiable((Class<?>)clazz))
                .collect(Collectors.toList());
    }

    public List<Class<?>> scanClassByAnnotation(Class<? extends Annotation> annotationClass, boolean isInstantiable) {
        initIfNecessary();
        if(scanCache.get(annotationClass) == null) {
            List<Class<?>> list = new ArrayList<>();
            for (Class<?> clazz : applicationClassCache) {
                if (!clazz.isAnnotationPresent(annotationClass)) {
                    continue;
                }
                list.add(clazz);
            }
            scanCache.put(annotationClass, list);
        }
        return (List<Class<?>>) scanCache.get(annotationClass).stream().filter(clazz -> !isInstantiable || isInstantiable((Class<?>)clazz))
                .collect(Collectors.toList());
    }

    private void initIfNecessary() {
        if (applicationClassCache.isEmpty()) {
            initAppClasses();
        }
    }

    private <T> void findChildClasses(List<Class<T>> classes, Class<T> parent, boolean isInstantiable) {
        for (Class clazz : applicationClassCache) {

            if (!parent.isAssignableFrom(clazz)) {
                continue;
            }

            if (isInstantiable && !isInstantiable(clazz)) {
                continue;
            }

            classes.add(clazz);
        }
    }

    private void initAppClasses() {

        Set<String> jarPaths = new HashSet<>();
        Set<String> classPaths = new HashSet<>();
        
        findClassPathsAndJarsByClassloader(jarPaths, classPaths, Ready.getClassLoader());

        findClassPathsAndJarsByClassPath(jarPaths, classPaths);

        for (String classPath : classPaths) {
            logger.debug("ClassScanner scan classpath : " + classPath);
            addClassesFromClassPath(classPath);
        }

        for (String jarPath : jarPaths) {
            if (!isIncludeJar(jarPath)) {
                continue;
            }

            logger.debug("ClassScanner scan jar : " + jarPath);
            addClassesFromJar(jarPath);
        }
    }

    private static String removePrefix(String str, String prefix) {
        if (str != null && str.startsWith(prefix)) {
            return str.substring(prefix.length());
        }
        return str;
    }

    private void addClassesFromJar(String jarPath) {
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jarPath);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                String entryName = jarEntry.getName();
                int index = entryName.lastIndexOf(JAR_PATH_EXT);
                if (index != -1) {
                    
                    entryName = entryName.substring(0, index + JAR_FILE_EXT.length()); 
                    entryName = removePrefix(entryName, PATH_FILE_PRE); 
                    addClassesFromJar(entryName);
                }
                if (!jarEntry.isDirectory() && entryName.endsWith(CLASS_EXT)) {
                    String className = entryName.replace("/", ".").substring(0, entryName.length() - 6);
                    addClass(classForName(className));
                }
            }
        } catch (IOException e1) {
        } finally {
            if (jarFile != null)
                try {
                    jarFile.close();
                } catch (IOException e) {
                }
        }
    }

    private void addClassesFromClassPath(String classPath) {
        List<File> classFileList = new ArrayList<>();
        scanClassFile(classFileList, classPath);

        for (File file : classFileList) {

            int start = classPath.length();
            int end = file.toString().length() - ".class".length();

            String classFile = file.toString().substring(start + 1, end);
            String className = classFile.replace(File.separator, ".");

            addClass(classForName(className));
        }
    }

    private void addClass(Class<?> clazz) {
        if (clazz != null && isIncludeClass(clazz.getName())){
            applicationClassCache.add(clazz);
        }
    }

    private boolean isIncludeClass(String clazzName) {
        boolean result = includePackages.isEmpty();
        for (String prefix : includePackages) {
            if (clazzName.startsWith(prefix)) {
                result = true; break;
            }
        }
        for (String prefix : excludePackages) {
            if (clazzName.startsWith(prefix)) {
                result = false; break;
            }
        }
        return result;
    }

    private void findClassPathsAndJarsByClassloader(Set<String> jarPaths, Set<String> classPaths, ClassLoader classLoader) {
        try {
            URL[] urls = null;
            if (classLoader instanceof URLClassLoader) {
                URLClassLoader ucl = (URLClassLoader) classLoader;
                urls = ucl.getURLs();
            }
            if (urls != null) {
                for (URL url : urls) {
                    String path = url.getPath();
                    path = URLDecoder.decode(path, Constant.DEFAULT_CHARSET);

                    if (path.startsWith("/") && path.indexOf(":") == 2) {
                        path = path.substring(1);
                    }

                    if (!path.toLowerCase().endsWith(JAR_FILE_EXT)) {
                        classPaths.add(new File(path).getCanonicalPath().replace('\\', '/'));
                    } else {
                        jarPaths.add(path.replace('\\', '/'));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        ClassLoader parent = classLoader.getParent();
        if (parent != null) {
            findClassPathsAndJarsByClassloader(jarPaths, classPaths, parent);
        }
    }

    private void findClassPathsAndJarsByClassPath(Set<String> jarPaths, Set<String> classPaths) {
        String classPath = System.getProperty("java.class.path");
        if (classPath == null || classPath.trim().length() == 0) {
            return;
        }
        String[] classPathArray = classPath.split(File.pathSeparator);
        if (classPathArray == null || classPathArray.length == 0) {
            return;
        }
        for (String path : classPathArray) {
            path = path.trim();

            if (path.startsWith("./")) {
                path = path.substring(2);
            }

            if (path.startsWith("/") && path.indexOf(":") == 2) {
                path = path.substring(1);
            }

            if (!path.toLowerCase().endsWith(JAR_FILE_EXT) && !jarPaths.contains(path)) {
                try {
                    classPaths.add(new File(path).getCanonicalPath().replace('\\', '/'));
                } catch (IOException e) {
                }
            } else {
                jarPaths.add(path.replace('\\', '/'));
            }
        }
    }

    private boolean isIncludeJar(String path) {

        String jarName = new File(path).getName().toLowerCase();

        boolean result = includeJars.isEmpty();
        for (String include : includeJars) {
            if (jarName.startsWith(include)) {
                result = true; break;
            }
        }
        for (String exclude : excludeJars) {
            if (jarName.startsWith(exclude)) {
                result = false; break;
            }
        }

        if (path.contains("/jre/lib")
                || path.contains("\\jre\\lib")) {
            result = false;
        }

        if (getJavaHome() != null
                && path.startsWith(getJavaHome())) {
            result = false;
        }

        return result;
    }

    private Class<?> classForName(String className) {
        ClassLoader classLoader = Ready.getClassLoader();
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException ex){
            int lastDotIndex = className.lastIndexOf(".");
            if (lastDotIndex != -1) {
                String innerClassName =
                        className.substring(0, lastDotIndex) + '$' + className.substring(lastDotIndex + 1);
                try {
                    return Class.forName(innerClassName, false, classLoader);
                } catch (ClassNotFoundException ex2) {
                    
                }
            }
        } catch (Throwable ex) {
            
        }
        return null;
    }

    private void scanClassFile(List<File> fileList, String path) {
        File[] files = new File(path).listFiles();
        if (null == files || files.length == 0)
            return;
        for (File file : files) {
            if (file.isDirectory()) {
                scanClassFile(fileList, file.getAbsolutePath());
            } else if (file.getName().endsWith(".class")) {
                fileList.add(file);
            }
        }
    }

    private String javaHome;

    private String getJavaHome() {
        if (javaHome == null) {
            try {
                String javaHomeString = System.getProperty("java.home");
                if (javaHomeString != null && javaHomeString.trim().length() > 0) {
                    javaHome = new File(javaHomeString, "..").getCanonicalPath().replace('\\', '/');
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return javaHome;
    }

}
