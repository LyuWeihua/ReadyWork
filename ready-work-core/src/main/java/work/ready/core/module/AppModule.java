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

package work.ready.core.module;

import work.ready.core.aop.InterceptorManager;
import work.ready.core.aop.transformer.TransformerManager;
import work.ready.core.component.cache.CacheManager;
import work.ready.core.component.proxy.ProxyManager;
import work.ready.core.database.*;
import work.ready.core.event.Event;
import work.ready.core.event.EventManager;
import work.ready.core.event.GeneralEvent;
import work.ready.core.handler.HandlerManager;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.handler.route.RouteManager;
import work.ready.core.handler.websocket.WebSocketManager;
import work.ready.core.ioc.BeanManager;
import work.ready.core.ioc.annotation.*;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.scheduler.SchedulerConfig;
import work.ready.core.render.RenderManager;
import work.ready.core.security.SecurityManager;
import work.ready.core.server.*;
import work.ready.core.tools.ClassUtil;
import work.ready.core.tools.StrUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AppModule {
    static { TransformerManager.getInstance().init(); } 
    private static final Log logger = LogFactory.getLog(AppModule.class);
    private String name; 
    private String version; 
    private String apiVersion; 
    protected ApplicationContext context;
    protected Application app;
    protected AppModule parent;
    protected List<AppModule> children = new ArrayList<>();
    private List<Class<? extends AppModule>> registry;
    private boolean loadExternalModuleConfig = false;

    public static void registerModules(List<Class<? extends AppModule>> moduleList){  

    }

    protected static void invokeRegisterModules(Class<? extends AppModule> clazz, final List<Class<? extends AppModule>> list) {
        try {
            Method method = clazz.getMethod("registerModules", List.class);
            method.invoke(clazz, list);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            
        }
    }

    protected List<Class<? extends AppModule>> getRegistry(){
        if(registry == null) {
            registry = new LinkedList<>();
            final List<Class<? extends AppModule>> list = new LinkedList<>();
            invokeRegisterModules(this.getClass(), list);
            registry.addAll(list);
        }
        return registry;
    }

    public void registerModule(Class<? extends AppModule> module) {
        if(!getRegistry().contains(module)) {
            getRegistry().add(module);
        }
    }

    protected void load(Class<? extends AppModule> clazz){
        load(context.coreContext.beanManager.get(clazz));
    }

    protected void load(AppModule appModule) {
        Class<?> clazz = ClassUtil.getUserClass(appModule.getClass());
        if(Application.class.isAssignableFrom(clazz)){
            throw new RuntimeException("Module " + clazz + " cannot be loaded, it is an application, not a module.");
        }

        boolean found = false;
        for(Class<? extends AppModule> registered : getRegistry()){
            if(registered.equals(clazz) || registered.isAssignableFrom(clazz)){ 
                found = true; break;
            }
        }
        if(!found){
            throw new RuntimeException("Module " + clazz + " should be register first by registerModules static method.");
        }

        logger.info("Load %s appModule, appModule=%s", getName(), appModule.getClass().getName());
        appModule.context = this.context;
        appModule.app = this.app;
        appModule.parent = this;
        children.add(appModule);

        if(!appModule.verifyInstallation()) appModule.install();

        appModule.initBegin();
        appModule.initHandlers();
        appModule.initSecurity();
        appModule.initRoutes();
        appModule.initRender();
        appModule.initWebSocket();
        appModule.initWebServer();
        appModule.initEnd();

        appModule.initialize();
        appModule.loadModules();
    }

    protected void loadModules(){
        for(Class<? extends AppModule> registered : getRegistry()){
            boolean isLoaded = false;
            for(AppModule module : children){
                Class<?> clazz = ClassUtil.getUserClass(module.getClass());
                if(registered.equals(clazz) || registered.isAssignableFrom(clazz)){ 
                    isLoaded = true; break;
                }
            }
            if(!isLoaded){
                load(registered);
            }
        }
    }

    protected void unload(AppModule appModule){
        children.remove(appModule);
        appModule.destroy();
        logger.info("Unload %s appModule, appModule=%s", getName(), appModule.getClass().getName());
    }

    protected boolean verifyInstallation(){
        return true;
    }

    protected void install(){
        logger.info("Install %s appModule, appModule=%s", getName(), this.getClass().getName());
    }

    protected void uninstall(){

    }

    protected void upgrade(){

    }

    protected void rollback(){

    }

    public String getName(){
        if(name == null) {
            name = getName(this.getClass());
        }
        return name;
    }

    public String getVersion(){
        if(version == null) {
            version = getVersion(this.getClass());
        }
        return version;
    }

    public String getApiVersion(){
        if(apiVersion == null) {
            apiVersion = getApiVersion(this.getClass());
        }
        return apiVersion;
    }

    public Version getFullVersion(){
        String[] ver = StrUtil.split(getVersion(), '.');
        return new Version(Byte.parseByte(ver[0]), Byte.parseByte(ver[1]), Byte.parseByte(ver[2]), ver[3], getApiVersion(), "work.ready", "ready-work-core", "WeiHua Lyu", "ready work framework, http://ready.work");
    }

    public Version getFullVersion(String groupId, String artifactId, String author, String description){
        String[] ver = StrUtil.split(getVersion(), '.');
        return new Version(Byte.parseByte(ver[0]), Byte.parseByte(ver[1]), Byte.parseByte(ver[2]), ver[3], getApiVersion(), groupId, artifactId, author, description);
    }

    public static String getName(Class<? extends AppModule> clazz){
        try {
            Field field = clazz.getField("name");
            if (Modifier.isStatic(field.getModifiers()) && String.class.equals(field.getType())) {
                field.setAccessible(true);
                return (String)field.get(clazz);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) { }
        return clazz.getSimpleName();
    }

    public static String getVersion(Class<? extends AppModule> clazz){
        try {
            Field field = clazz.getField("version");
            if (Modifier.isStatic(field.getModifiers()) && String.class.equals(field.getType())) {
                field.setAccessible(true);
                String version = (String)field.get(clazz);
                if(StrUtil.isValidVersion(version)){
                    return version;
                } else {
                    throw new RuntimeException("invalid version definition in " + clazz.getCanonicalName());
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) { }
        return Constant.DEFAULT_VERSION;
    }

    public static String getApiVersion(Class<? extends AppModule> clazz){
        try {
            Field field = clazz.getField("apiVersion");
            if (Modifier.isStatic(field.getModifiers()) && String.class.equals(field.getType())) {
                field.setAccessible(true);
                return (String)field.get(clazz);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) { }
        return Constant.DEFAULT_API_VERSION;
    }

    public AppModule getParent() {
        return parent;
    }

    public boolean isApp(){
        return this instanceof Application;
    }

    public Application getApp() {
        return app;
    }

    public List<AppModule> getChildren() {
        return children;
    }

    public boolean isLoadExternalModuleConfig() {
        return loadExternalModuleConfig;
    }

    public void setLoadExternalModuleConfig(boolean loadExternalModuleConfig) {
        this.loadExternalModuleConfig = loadExternalModuleConfig;
    }

    public void onShutdown(Task task) {
        context.shutdownHook.add(ShutdownHook.STAGE_5, timeout -> task.execute());
    }

    public void onStartup(Task task) {
        context.startupHook.add(task);
    }

    public BackgroundTaskExecutor backgroundTask(){
        return context.backgroundTask();
    }

    public Map<String, Object> config(){
        if(loadExternalModuleConfig){
            return Ready.config().getMapConfig(StrUtil.firstCharToLowerCase(getName()));
        }
        if(Ready.getApplicationConfig(app.getName()).getAppModule() != null) {
            return Ready.getApplicationConfig(app.getName()).getAppModule().getModuleConfig(StrUtil.firstCharToLowerCase(this.getClass().getSimpleName()));
        }
        return null;
    }

    public ProxyManager proxy() { return context.coreContext.proxyManager; }

    public void proxy(Consumer<ProxyManager> consumer) { consumer.accept(context.coreContext.proxyManager); }

    public BeanManager bean() { return context.coreContext.beanManager; }

    public void bean(Consumer<BeanManager> consumer) { consumer.accept(context.coreContext.beanManager); }

    public InterceptorManager interceptor() { return context.coreContext.interceptorManager; }

    public void interceptor(Consumer<InterceptorManager> consumer) { consumer.accept(context.coreContext.interceptorManager); }

    public EventManager event() { return Ready.eventManager(); }

    public void event(Consumer<EventManager> consumer) { consumer.accept(Ready.eventManager()); }

    public RenderManager render() { return context.renderManager; }

    public void render(Consumer<RenderManager> consumer) { consumer.accept(context.renderManager); }

    public HandlerManager handler() { return context.handlerManager; }

    public void handler(Consumer<HandlerManager> consumer) { consumer.accept(context.handlerManager); }

    public SecurityManager security() { return context.securityManager; }

    public void security(Consumer<SecurityManager> consumer) { consumer.accept(context.securityManager); }

    public RouteManager route() { return context.routeManager; }

    public void route(Consumer<RouteManager> consumer) { consumer.accept(context.routeManager); }

    public CacheManager cache() { return context.coreContext.cacheManager; }

    public void cache(Consumer<CacheManager> consumer) { consumer.accept(context.coreContext.cacheManager); }

    public DatabaseManager dbManager() { return context.coreContext.dbManager; }

    public void dbManager(Consumer<DatabaseManager> consumer) { consumer.accept(context.coreContext.dbManager); }

    public WebSocketManager webSocket() {
        return context.webSocketManager;
    }

    public void webSocket(Consumer<WebSocketManager> consumer) { consumer.accept(context.webSocketManager); }

    public WebServer webServer() {
        return context.webServer;
    }

    public void webServer(Consumer<WebServer> consumer) { consumer.accept(context.webServer); }

    public ApplicationConfig applicationConfig() {
        return Ready.getApplicationConfig(app.getName());
    }

    public ServerConfig serverConfig() {
        return applicationConfig().getServer();
    }

    public SchedulerConfig localSchedule() {
        return new SchedulerConfig(context);
    }

    public ExecutorConfig executorConfig() {
        return new ExecutorConfig(context);
    }

    protected void initBegin(){
        if(!isApp()){
            Ready.post(new GeneralEvent(Event.MODULE_FRAMEWORK_INIT_BEGIN, this));
        }
    }

    protected void initHandlers(){
        List<Class<?>> handlers = Ready.classScanner().scanClassByAnnotation(HttpHandler.class, true);
        classPicker(handlers, clazz->handler().addHandlerByAnnotation(clazz));
        if(!isApp()){
            Ready.post(new GeneralEvent(Event.MODULE_INIT_HANDLER, this));
        }
    }

    protected void initSecurity(){

        if(!isApp()){
            Ready.post(new GeneralEvent(Event.MODULE_INIT_SECURITY, this));
        }
    }

    protected void initRoutes(){
        List<Class<?>> controllers = Ready.classScanner().scanClassByAnnotation(RequestMapping.class, true);
        classPicker(controllers, clazz->route().addMappingByAnnotation(clazz));
        if(!isApp()){
            Ready.post(new GeneralEvent(Event.MODULE_INIT_ROUTE, this));
        }
    }

    protected void initRender(){

        if(!isApp()){
            Ready.post(new GeneralEvent(Event.MODULE_INIT_RENDER, this));
        }
    }

    protected void initWebSocket(){
        List<Class<?>> listeners = Ready.classScanner().scanClassByAnnotation(WebSocketListener.class, true);
        classPicker(listeners, clazz->webSocket().addListenerByAnnotation(clazz));
        if(!isApp()){
            Ready.post(new GeneralEvent(Event.MODULE_INIT_WEB_SOCKET, this));
        }
    }

    protected void initWebServer(){

        if(!isApp()){
            Ready.post(new GeneralEvent(Event.MODULE_INIT_WEB_SERVER, this));
        }
    }

    protected void initEnd(){

        if(!isApp()){
            Ready.post(new GeneralEvent(Event.MODULE_FRAMEWORK_INIT_END, this));
        }
    }

    private void classPicker(List<Class<?>> classList, Consumer<Class<?>> action){
        if(isApp()) {
            var thisApp = (Application) this;
            for (Class<?> clazz : classList) {
                if (thisApp.checkPackage(clazz)) { 
                    boolean belongsToModule = false;
                    for (String pkg : thisApp.modulePackage) {
                        if (clazz.getPackageName().startsWith(pkg)) {
                            belongsToModule = true;
                            break;
                        }
                    }
                    if (belongsToModule) continue; 
                    action.accept(clazz);
                }
            }
        } else {
            String modulePkg = this.getClass().getPackageName();
            for(Class<?> clazz : classList){
                if(clazz.getPackageName().startsWith(modulePkg)){
                    action.accept(clazz);
                }
            }
        }
    }

    protected abstract void initialize();

    protected void destroy(){};
}
