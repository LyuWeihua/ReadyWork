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
package work.ready.core.server;

import work.ready.core.aop.transformer.TransformerManager;
import work.ready.core.aop.InterceptorManager;
import work.ready.core.apm.ApmManager;
import work.ready.core.component.cache.CacheManager;
import work.ready.core.component.plugin.BaseCorePlugin;
import work.ready.core.component.proxy.ProxyManager;
import work.ready.core.component.snowflake.IdWorker;
import work.ready.core.component.time.TimeSupplier;
import work.ready.core.component.time.TimeWorker;
import work.ready.core.config.Config;
import work.ready.core.database.DatabaseManager;
import work.ready.core.event.Event;
import work.ready.core.event.EventManager;
import work.ready.core.event.GeneralEvent;
import work.ready.core.ioc.BeanManager;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.*;
import work.ready.core.tools.*;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static work.ready.core.tools.StrUtil.format;

public class Ready {
    public static final Log logger = LogFactory.getLog(Ready.class);
    public static final ShutdownHook shutdownHook = new ShutdownHook();
    private static final ClassScanner classScanner;
    private static ClassLoader classLoader = ClassUtil.getDefaultClassLoader();
    private static String[] cmdArgs;
    private static volatile StartStage startStage = StartStage.stop;

    private final static List<Class<? extends Application>> appClassList = new ArrayList<>();
    private final static Map<String, Application> appMap = new HashMap<>();
    private final static Map<String, Boolean> appAlive = new HashMap<>();
    private static String mainAppName;
    private static Application mainApp;
    private final static List<Class<? extends BaseCorePlugin>> pluginClassList = new ArrayList<>();
    private final static Map<String, BaseCorePlugin> pluginMap = new HashMap<>();

    private static final Config config = new Config();
    private static final EventManager eventManager = new EventManager();
    private static final Path root = locateRootDirectory();
    private static IdWorker idWorker;
    private static TimeWorker timeWorker;
    private static CoreContext context;

    static {
        disableAccessWarnings();
        
        ApplicationClassLoader appClassLoader = new ApplicationClassLoader(new URL[0], classLoader);
        try {
            appClassLoader.addURL(new File(root + File.separator + Constant.DEFAULT_CONFIG_FILE_DIR).toURI().toURL());
            appClassLoader.addURL(new File(root + File.separator + Constant.DEFAULT_LIB_FILE_DIR).toURI().toURL());
            appClassLoader.addURL(new File(root + File.separator + Constant.DEFAULT_PLUGIN_FILE_DIR).toURI().toURL());
            setClassLoader(appClassLoader);
        } catch (MalformedURLException e) {
        }
        config.reLocateConfigRepository();
        transformerManager().attach(getBootstrapConfig().getInterceptor());
        classScanner = new ClassScanner(getBootstrapConfig());
    }

    public static void setClassLoader(ClassLoader loader) {
        classLoader = loader;
    }

    public static ClassLoader getClassLoader() {
        return classLoader;
    }

    @SafeVarargs
    public static Ready For(Class<? extends Application>... appClass){
        if(appClass == null || appClass.length < 1) {
            throw new Error("app class is required");
        }
        appClassList.addAll(Arrays.asList(appClass));
        return new Ready();
    }

    public synchronized void Work(String[] args) {
        if (startStage.stage > 0) {
            return;
        }
        startStage = StartStage.begin;
        logger.info("Server is starting");
        Ready.cmdArgs = args;

        var watch = new StopWatch();
        try {
            LogFactory.applyConfig(getBootstrapConfig().getLog());
            config.parseCmdArgs(cmdArgs);
            post(new GeneralEvent(Event.READY_WORK_SERVER_START, this));

            System.out.println();
            System.out.println(Banner.getBanner(getBootstrapConfig().getBannerFile()));
            System.out.println();

            mainAppName = AppModule.getName(appClassList.get(0));
            
            for (var annotation : appClassList.get(0).getAnnotations()) {
                Method method = annotation.annotationType().getMethod("value");
                if(Class.class.equals(method.getReturnType())){
                    Class<?> clazz = (Class<?>) method.invoke(annotation);
                    if(BaseCorePlugin.class.isAssignableFrom(clazz)){
                        pluginClassList.add((Class<? extends BaseCorePlugin>)clazz);
                    }
                }
            }
            for (Class<? extends BaseCorePlugin> clazz : pluginClassList) {
                var plugin = clazz.getDeclaredConstructor().newInstance();
                pluginMap.put(plugin.getName(), plugin);
            }
            pluginMap.forEach((name,plugin) -> plugin.bootstrap());
            
            for (Class<? extends Application> clazz : appClassList) {
                var app = clazz.getDeclaredConstructor().newInstance();
                if(appMap.containsKey(app.getName())) {
                    throw new Error("app name " + app.getName() + " is exist already." );
                }
                appMap.put(app.getName(), app);
            }
            mainApp = getApp(mainAppName);

            new Configurer(pluginMap).config();
            
            scratchInit();

            context = new CoreContext(pluginMap);
            context.init();

            Ready.eventManager().addListener(this, "appStartListener",
                    event -> event.addName(Event.APP_STARTED));
            MainExecutor executor = new MainExecutor(1, "ApplicationRunner");
            appMap.values().forEach((app)-> {
                app.setCoreContext(context);
                executor.submit(app.getName() + " app", app::start);
            });

        } catch (Exception e) {
            
            if(logger.isErrorEnabled()) {
                logger.error(e, "Failed to start server! exception: ");
            } else {
                System.out.println("Failed to start server: " + e.getMessage());
            }
            
            System.exit(1);
        } finally {
            logger.info("Ready.Work server start elapsed=%s seconds.", watch.elapsedSeconds());
        }
    }

    public void appStartListener(GeneralEvent event) {
        Application app = event.getSender();
        appAlive.put(app.getName(), true);
        if(appAlive.keySet().containsAll(appMap.keySet())) {
            Ready.post(new GeneralEvent(Event.READY_FOR_WORK, this));
            startStage = StartStage.end;
        }
    }

    public static Config config() {
        return config;
    }

    public static EventManager eventManager() { return eventManager; }

    public static void post(Object event) { Ready.eventManager.post(event);}

    public static boolean isStarted() {
        return startStage == StartStage.end;
    }
    public static StartStage getStartStage() { return startStage; }

    public static ClassScanner classScanner() { return classScanner; }

    public static TransformerManager transformerManager() { return TransformerManager.getInstance(); }

    public static ApmManager apmManager() { return ApmManager.getInstance(); }

    public static BeanManager beanManager() { return context.getBeanManager(); }

    public static DatabaseManager dbManager() { return context.getDbManager(); }

    public static ProxyManager proxyManager() { return context.getProxyManager(); }

    public static CacheManager cacheManager() { return context.getCacheManager(); }

    public static InterceptorManager interceptorManager() { return context.getInterceptorManager(); }

    public static boolean isMultiAppMode(){
        return appClassList.size() > 1;
    }

    public static void appDestroyed(Application app) {
        appAlive.remove(app.getName());
        appClassList.remove(app.getClass());
        appMap.remove(app.getName());
    }

    public static long getId() { return idWorker.getId(); }

    private static void scratchInit() {

        clockInit();
        
        idWorkerInit();
        
        ApmManager.getInstance();
    }

    private static void idWorkerInit() {
        String workerId = getBootstrapConfig().getUuidWorkerId();
        if(StrUtil.isNumbers(workerId)) {
            int id = Integer.parseInt(workerId);
            idWorker = new IdWorker(id);
        } else if("MAC".equals(workerId)) {
            idWorker = new IdWorker(IdWorker.getWorkerIdByMacAddress(18));
        } else if("IP".equals(workerId)) {
            idWorker = new IdWorker(IdWorker.getWorkerIdByIPV4(18));
        } else {
            throw new RuntimeException("invalid uuidWorkerId in bootstrap.yml, options are 'MAC', 'IP', or a positive number that is less than 262144.");
        }
    }

    private static boolean clockInit() {
        boolean result;
        TimeWorker worker = TimeWorker.getInstance().withConnectionTimeout(6000);
        result = worker.syncTime();
        timeWorker = worker;
        new TimeSupplier().start();
        Ready.post(new GeneralEvent(Event.READY_WORK_TIME_INIT, null, (Supplier<Long>) Ready::currentTimeMillis));
        return result;
    }

    public static synchronized boolean syncTime(){
        boolean result;
        if(timeWorker == null) {
            result = clockInit();
        } else {
            result = timeWorker.syncTime();
        }
        return result;
    }

    public static Date now() {
        return new Date(currentTimeMillis());
    }

    public static Instant instant() {
        return Instant.ofEpochMilli(currentTimeMillis());
    }

    public static ZonedDateTime zonedDateTime(ZoneId zoneId) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis()), zoneId);
    }

    public static ZonedDateTime zonedDateTime() {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis()), ZoneId.systemDefault());
    }

    public static LocalDateTime localDateTime(ZoneId zoneId){
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis()), zoneId);
    }

    public static LocalDateTime localDateTime(){
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis()), ZoneId.systemDefault());
    }

    public static long currentTimeMillis(){
        return timeWorker == null ? System.currentTimeMillis() : timeWorker.currentTimeMillis();
    }

    public static String[] getCmdArgs(){ return cmdArgs; }

    public static Map<String, BaseCorePlugin> getPluginMap(){
        return Map.copyOf(pluginMap);
    }

    public static BaseCorePlugin getPlugin(String pluginName){
        return pluginMap.get(pluginName);
    }

    public static ApplicationConfig getBootstrapConfig(){
        return config.getBootstrapConfig();
    }

    public static ApplicationConfig getApplicationConfig(String appName){
        return config.getApplicationConfig(appName);
    }

    public static ApplicationConfig getMainApplicationConfig(){
        return config.getApplicationConfig(mainAppName);
    }

    public static List<Class<? extends Application>> getAppClass(){
        return List.copyOf(appClassList);
    }

    public static void getApp(Consumer<Collection<Application>> consumer){
        consumer.accept(appMap.values());
    }

    public static Application getApp(String appName){
        return appMap.get(appName);
    }

    public static String getMainAppName(){
        return mainAppName;
    }

    public static Application getMainApp() {
        return mainApp;
    }

    public static Application getApp(Class<? extends Application> appClass){
        for(var app : appMap.values()){
            if(app.getClass().equals(appClass)) return app;
        }
        return null;
    }

    static public void stop(){
        stop(true);
    }

    static public synchronized void stop(boolean exit) {
        if (startStage.stage > 0) {
            startStage = StartStage.stop;
        } else {
            return ;
        }
        var watch = new StopWatch();
        try {
            if(exit){
                System.out.println("\nShutdown server ......");
                shutdownHook.run();
                System.out.println("Shutdown complete in " + watch.elapsedSeconds() + " seconds.\n");
            } else {
                System.out.println("\nShutdown applications ......");
                
                appMap.values().forEach(Application::stop);
                appMap.clear();
                appAlive.clear();
                appClassList.clear();
                System.out.println("Applications shutdown complete in " + watch.elapsedSeconds() + " seconds.\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(exit)
            System.exit(0);
        }
    }

    private static Path locateRootDirectory() {
        String value = Ready.getProperty(Constant.READY_WORK_ROOT_PATH_PROPERTY);
        if (value != null) {
            Path path = Paths.get(value).toAbsolutePath();
            if (Files.isDirectory(path)) {
                logger.info("found work directory provided by -D" + Constant.READY_WORK_ROOT_PATH_PROPERTY + " as root path, path=" + path);
                return path;
            } else {
                if(path.toFile().mkdirs()) {
                    logger.info("created root path provided by -D" + Constant.READY_WORK_ROOT_PATH_PROPERTY + ", path=" + path);
                    return path;
                }
            }
        }
        value = getBootstrapConfig().getRootPath();
        if (value != null) {
            Path path = Paths.get(value).toAbsolutePath();
            if (Files.isDirectory(path)) {
                logger.info("found rootPath defined in bootstrap config as root path, path=" + path);
                return path;
            } else {
                if(path.toFile().mkdirs()) {
                    logger.info("created root path defined in bootstrap config, path=" + path);
                    return path;
                }
            }
        }
        Path path = Paths.get(Constant.DEFAULT_BASE_WORKING_DIR);
        if (Files.isDirectory(path)) {
            logger.info("found work directory in current path as root path, path=" + path);
            return path.toAbsolutePath();
        } else {
            if(path.toFile().mkdirs()) {
                logger.info("created a work directory in current path as root path, path=" + path);
                return path;
            }
        }
        value = System.getProperty("user.home");
        if (value != null) {
            path = Paths.get(value + File.separator + Constant.DEFAULT_BASE_WORKING_DIR).toAbsolutePath();
            if (Files.isDirectory(path)) {
                logger.info("found work directory in user.home as root path, path=" + path);
                return path.toAbsolutePath();
            } else {
                if(path.toFile().mkdirs()) {
                    logger.info("created a work directory in current user.home as root path, path=" + path);
                    return path;
                }
            }
        }
        throw new RuntimeException("Can not locate work directory");
    }

    public static Path path(String path) {
        if (!path.startsWith("/")) throw new Error(format("path must start with '/', path=%s", path));
        return root().resolve(path.substring(1)).toAbsolutePath();
    }

    public static boolean rootExist(){
        return root != null && Files.exists(root);
    }

    public static Path root() {
        if (!rootExist())
            throw new Error(format("Can not find work path, please create a a work directory in project path %s for local dev env, or check -D" + Constant.READY_WORK_ROOT_PATH_PROPERTY + " for server env.", System.getProperty("user.dir")));
        return root;
    }

    public static String getProperty(String property) {
        return getProperty(property, null);
    }

    public static String getProperty(String property, String defaultValue) {
        String value = System.getProperty(property);
        if(value == null) value = System.getenv(property.replace('.','_'));
        return value == null ? defaultValue : value;
    }

    @SuppressWarnings("unchecked")
    public static void disableAccessWarnings() {
        try {
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);

            Method putObjectVolatile = unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
            Method staticFieldOffset = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);

            Class loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field loggerField = loggerClass.getDeclaredField("logger");
            Long offset = (Long) staticFieldOffset.invoke(unsafe, loggerField);
            putObjectVolatile.invoke(unsafe, loggerClass, offset, null);
        } catch (Exception ignored) {
        }
    }

    public enum StartStage {
        stop(0),begin(1),end(99);

        int stage;
        StartStage(int stage){
            this.stage = stage;
        }

        public int getStage(){
            return stage;
        }
    }
}
