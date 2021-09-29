/**
 *
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
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

package work.ready.core.aop.transformer;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import work.ready.core.aop.transformer.enhance.*;
import work.ready.core.aop.transformer.match.InterceptorPoint;
import work.ready.core.apm.model.SpiDefine;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.io.*;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.*;

import static work.ready.core.tools.ClassUtil.getDefaultClassLoader;

public class TransformerManager {
    private static final Log logger = LogFactory.getLog(TransformerManager.class);

    private static List<Interceptor> INTERCEPTORS;
    public static final AbstractInterceptorChainFactory<MethodInterceptor> METHOD
            = new MethodInterceptorChainFactory();
    public static final AbstractInterceptorChainFactory<ConstructorInterceptor> CONSTRUCTOR
            = new ConstructorInterceptorChainFactory();

    private static final TransformerManager INSTANCE = new TransformerManager();
    public static final String PROXY_CLASS_SEPARATOR = "$$";
    public static final String ENHANCER = PROXY_CLASS_SEPARATOR + "EnhancerByReadyWork";
    public static final String METHOD_PREFIX = "ready$";
    private static boolean initialized = false;
    private AgentBuilder agentBuilder;
    private ResettableClassFileTransformer reSetter;

    public static TransformerManager getInstance() {
        return INSTANCE;
    }

    private TransformerManager() {
        ByteBuddyAgent.install();
    }

    public synchronized void init() {
        if(initialized) { return; }
        Map<String, Object> interceptorList = load("META-INF/aop.properties");
        List<Interceptor> interceptors = new ArrayList<>();
        for (var interceptor : interceptorList.values()) {
            if(!(interceptor instanceof MethodInterceptor) && !(interceptor instanceof ConstructorInterceptor)) {
                logger.warn("invalid Interceptor %s.", interceptor.getClass().getName());
                continue;
            }
            if(interceptors.contains(interceptor)) continue;
            try {
                logger.info("initializing interceptor %s.", interceptor.getClass().getName());
                interceptors.add((Interceptor)interceptor);
            } catch (Exception e) {
                logger.error(e, "initialize %s failed!", interceptor.getClass().getName());
            }
        }

        INTERCEPTORS = Collections.unmodifiableList(interceptors);
        METHOD.addInterceptors(interceptors);
        CONSTRUCTOR.addInterceptors(interceptors);

        ElementMatcher<? super ClassLoader> classLoaderMatcher = ElementMatchers.any();
        
        this.agentBuilder = new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(AgentBuilder.TypeStrategy.Default.REBASE)
                .with(RedefinitionStrategy.RETRANSFORMATION)
                
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                
                .with(new AgentBuilder.LocationStrategy.Simple(ClassFileLocator.ForClassLoader.of(getClass().getClassLoader())))
                .ignore(TypeMatcher.INSTANCE.ignoreRule(), classLoaderMatcher)
                .type(TypeMatcher.INSTANCE.passRule(interceptors), classLoaderMatcher)
                .transform(new AgentBuilder.Transformer.ForAdvice())
                .transform(BuilderDecorator::decorate)
                .with(new ByteBuddy().with(MethodGraph.Compiler.ForDeclaredMethods.INSTANCE).with(new NamingStrategy.AbstractBase() {
                    @Override
                    protected String name(TypeDescription typeDescription) {
                        return typeDescription.getCanonicalName() + ENHANCER + PROXY_CLASS_SEPARATOR + System.currentTimeMillis();
                    }
                }))
                .enableNativeMethodPrefix(METHOD_PREFIX) 
                .with(new DefaultAgentListener());
        this.reSetter = this.agentBuilder.installOnByteBuddyAgent();
        initialized = true;
        logger.info("TransformerManager is ready!");
    }

    public static List<Interceptor> getAllInterceptors(){
        return Collections.unmodifiableList(INTERCEPTORS);
    }

    public static Interceptor getInterceptor(Class<?> type){
        for(var interceptor : INTERCEPTORS) {
            if(interceptor.getClass().equals(type)) {
                return interceptor;
            }
        }
        return null;
    }

    public static Interceptor getInterceptor(String type){
        for(var interceptor : INTERCEPTORS) {
            if(interceptor.getClass().getCanonicalName().equals(type)) {
                return interceptor;
            }
        }
        return null;
    }

    public void attach(Map<String, InterceptorPoint> interceptorPoint) {
        if(interceptorPoint == null || interceptorPoint.isEmpty()) {
            return;
        }
        List<Interceptor> interceptors = new ArrayList<>();
        for(Map.Entry<String, InterceptorPoint> entry : interceptorPoint.entrySet()) {
            InterceptorPoint config = entry.getValue();
            EasyInterceptor easyInterceptor = null;
            Interceptor userInterceptor = null;
            if(!config.isEnabled() || config.getInterceptor() == null) continue;
            try {
                Class<?> clazz = Class.forName(config.getInterceptor(), true, getDefaultClassLoader());
                if(EasyInterceptor.class.isAssignableFrom(clazz)) {
                    easyInterceptor = (EasyInterceptor) clazz.getDeclaredConstructor().newInstance();;
                    logger.info("initializing EasyInterceptor %s.", clazz.getName());
                } else if(Interceptor.class.isAssignableFrom(clazz)) {
                    try {
                        Constructor<?> constructor = clazz.getDeclaredConstructor(Map.class, Map.class, Map.class, Map.class);
                        userInterceptor = (Interceptor)
                                constructor.newInstance(config.getTypeInclude(), config.getTypeExclude(), config.getMethodInclude(), config.getMethodExclude());
                        logger.info("initializing Interceptor %s.", clazz.getName());
                    } catch (NoSuchMethodException e) {
                        logger.error("invalid configurable Interceptor %s. ", clazz.getName());
                    }
                } else {
                    logger.warn("invalid Interceptor %s.", clazz.getName());
                    continue;
                }
            } catch (Throwable t) {
                logger.error(t,"load " + config.getInterceptor());
            }
            if(easyInterceptor != null) {
                
                var interceptor = new EasyInterceptorHandler(easyInterceptor, config.getTypeInclude(), config.getTypeExclude(), config.getMethodInclude(), config.getMethodExclude());
                interceptors.add(interceptor);
            } else if(userInterceptor != null) {
                interceptors.add(userInterceptor);
            }
        }
        internalAttach(interceptors);
    }

    public void attach(AgentBuilder builder){
        this.reSetter = builder.patchOnByteBuddyAgent(this.reSetter);
    }

    public void attach(List<String> interceptorClassList) {
        List<Interceptor> interceptors = new ArrayList<>();
        for(String interceptor : interceptorClassList) {
            try {
                Object loaded = Class.forName(interceptor, true, getDefaultClassLoader()).getDeclaredConstructor().newInstance();
                if(!(loaded instanceof MethodInterceptor) && !(loaded instanceof ConstructorInterceptor)) {
                    logger.warn("invalid Interceptor %s.", loaded.getClass().getName());
                    continue;
                }
                if(interceptors.contains(loaded)) continue;
                logger.info("initializing interceptor %s.", loaded.getClass().getName());
                interceptors.add((Interceptor)loaded);
            } catch (Throwable t) {
                logger.error(t,"load " + interceptor);
            }
        }
        internalAttach(interceptors);
    }

    public void attach(List<String> interceptorClassList, List<String> ignorePrefix) {
        attach(interceptorClassList);
        if(ignorePrefix != null && !ignorePrefix.isEmpty()) {
            TypeMatcher.INSTANCE.ignoreRule(ignorePrefix);
        }
    }

    private void internalAttach(List<Interceptor> interceptors) {
        if(!initialized) {
            init();
        }
        Set<Interceptor> set = new HashSet<>(INTERCEPTORS);
        set.addAll(interceptors);
        INTERCEPTORS = List.copyOf(set);
        METHOD.addInterceptors(interceptors);
        CONSTRUCTOR.addInterceptors(interceptors);

        TypeMatcher.INSTANCE.passRule(interceptors);
    }

    public void reset() {
        this.reSetter.reset(ByteBuddyAgent.getInstrumentation(),
                RedefinitionStrategy.RETRANSFORMATION);
    }

    public void reApply() {
        this.reSetter = this.agentBuilder.installOn(ByteBuddyAgent.
                getInstrumentation());
    }

    private static void config() {
    }

    public static boolean isInitialized() {
        return initialized;
    }

    static class DefaultAgentListener implements AgentBuilder.Listener {

        @Override
        public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule javaModule, boolean b) {
        }

        @Override
        public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
                                     boolean loaded, DynamicType dynamicType) {
        }

        @Override
        public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
                              boolean loaded) {
        }

        @Override
        public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded,
                            Throwable throwable) {
            logger.error(throwable, "%s load %s error.", classLoader, typeName);
        }

        @Override
        public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
        }
    }

    public static <T> Map<String, T> load(String fileName) {
        Map<String, T> spiMap = new HashMap<String, T>(8);
        List<SpiDefine> spiDefList = new ArrayList<SpiDefine>(8);
        List<URL> resources = getResources(getDefaultClassLoader(), fileName);
        if(resources == null || resources.isEmpty()) return spiMap;
        for (URL url : resources) {
            try {
                readSpiDefine(url.openStream(), spiDefList);
            } catch (Throwable t) {
                logger.error(t,"read " + fileName);
            }
        }

        for (SpiDefine define : spiDefList) {
            try {
                T spi = (T) Class.forName(define.clazz, true, getDefaultClassLoader()).getDeclaredConstructor().newInstance();
                spiMap.put(define.name, spi);
            } catch (Throwable t) {
                logger.error(t,"load " + define.clazz);
            }
        }
        return spiMap;
    }

    private static List<URL> getResources(ClassLoader classLoader, String fileName) {
        List<URL> cfgUrlPaths = new ArrayList<URL>();
        Enumeration<URL> urls;
        try {
            urls = classLoader.getResources(fileName);
            while (urls.hasMoreElements()) {
                URL pluginUrl = urls.nextElement();
                cfgUrlPaths.add(pluginUrl);
            }
            return cfgUrlPaths;
        } catch (IOException e) {
            logger.error(e, "load " + fileName);
        }
        return null;
    }

    private static void readSpiDefine(InputStream input, List<SpiDefine> spiDefList) throws IOException {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String define;
            while ((define = reader.readLine()) != null) {
                define = define.trim();
                try {
                    if (define.isEmpty() || define.startsWith("#")) {
                        continue;
                    }
                    define = define.trim();
                    String[] defs = define.split("=");
                    if (defs.length != 2) {
                        continue;
                    }
                    spiDefList.add(new SpiDefine(defs[0].trim(), defs[1].trim()));
                } catch (Exception e) {
                    logger.error(e, "invalid spi " + define);
                }
            }
        } finally {
            input.close();
        }
    }
}
