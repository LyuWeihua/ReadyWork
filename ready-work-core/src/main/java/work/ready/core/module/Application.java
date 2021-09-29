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

import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.system.SystemModule;
import work.ready.core.server.ApplicationConfig;
import work.ready.core.server.Ready;
import work.ready.core.tools.NetUtil;

import java.net.InetAddress;
import java.util.*;

public abstract class Application extends AppModule {
    private static final Log logger = LogFactory.getLog(Application.class);
    private CoreContext coreContext;
    protected List<String> packagePrefix = new ArrayList<>();
    protected List<String> modulePackage = new ArrayList<>();
    protected List<String> exceptPackage = new ArrayList<>();
    protected List<Class<? extends AppModule>> allModules = new ArrayList<>();
    protected Map<Class<? extends AppModule>, String> moduleInJar = new HashMap<>();

    public final void start() {
        logger.info("=== " + getName() + " is starting ===");
        Ready.beanManager().setCurrentApplication(this);
        boolean failed = false;
        this.app = this;
        try {
            currentHost();
            initApplication();
            for (Task task : context.startupHook) {
                task.execute();
            }
        } catch (Throwable e) {
            logger.error(e,"" + getName() + " failed to start, error=%s", e.getMessage());
            failed = true;
        } finally {
            logger.info("=== " + getName() + " is started ===");
            Ready.post(new GeneralEvent(Event.APP_STARTED, this));
        }
        if (failed) {
            System.exit(1);
        }
    }

    public final void stop(){
        context.shutdownHook.run();
    }

    void currentHost() {
        Runtime runtime = Runtime.getRuntime();
        logger.info("cpu %s", runtime.availableProcessors());
        logger.info("max_memory %s", runtime.maxMemory());
        InetAddress inetAddress = NetUtil.getLocalAddress();
        logger.info("host %s", inetAddress.getHostAddress());
    }

    private void configurePackage(){
        Ready.getAppClass().forEach(c -> {
            if (c.equals(this.getClass()) || c.getPackageName().startsWith(this.getClass().getPackageName())) {
                addPackagePrefix(c.getPackageName());
            } else {
                addExceptPackage(c.getPackageName());
            }
        });
        allModules.addAll(getRegistry());
        registerModule(getRegistry());
    }

    private void registerModule(List<Class<? extends AppModule>> parentRegistry){
        for(Class<? extends AppModule> moduleClazz : parentRegistry) {
            addModulePackage(moduleClazz.getPackageName());
            moduleInJar.putIfAbsent(moduleClazz, moduleClazz.getProtectionDomain().getCodeSource().getLocation().getFile());
            final List<Class<? extends AppModule>> moduleList = new ArrayList<>();
            invokeRegisterModules(moduleClazz, moduleList);
            allModules.addAll(moduleList);
            registerModule(moduleList);
        }
    }

    public final void initApplication() {
        if(!verifyInstallation()) install();

        configurePackage();

        Ready.config().addStatusConfig(this.getName() + "-status");

        logger.info(getName() + " is initializing");
        context = new ApplicationContext(coreContext,this);

        initBegin();
        initHandlers();
        initSecurity();
        initRoutes();
        initRender();
        initWebSocket();
        initWebServer();
        initEnd();

        initialize();

        if(Ready.getApplicationConfig(getName()).getAppModule().isEnableSystemModule() && Ready.getAppClass().get(0).equals(getClass())) {
            getRegistry().add(SystemModule.class);
            allModules.add(SystemModule.class);
            load(SystemModule.class);
        }
        loadModules();
        context.validate();
    }

    public void setCoreContext(CoreContext coreContext) {
        this.coreContext = coreContext;
    }

    public List<String> getPackagePrefix() {
        return packagePrefix;
    }

    public void setPackagePrefix(List<String> packagePrefix) {
        this.packagePrefix = packagePrefix;
    }

    public void addPackagePrefix(String prefix){
        if(!this.packagePrefix.contains(prefix))
        this.packagePrefix.add(prefix);
    }

    public List<String> getExceptPackage() {
        return exceptPackage;
    }

    public void setExceptPackage(List<String> exceptPackage) {
        this.exceptPackage = exceptPackage;
    }

    public void addExceptPackage(String exceptPackage){
        if(!this.exceptPackage.contains(exceptPackage))
        this.exceptPackage.add(exceptPackage);
    }

    private void addModulePackage(String packageName){
        if(!this.modulePackage.contains(packageName))
            this.modulePackage.add(packageName);
    }

    public List<String> getModulePackage(){
        return modulePackage;
    }

    public boolean checkPackage(Class<?> clazz){
        boolean matched = false;
        if(getPackagePrefix().size() > 0) {
            for (String prefix : getPackagePrefix()) {
                if(clazz.getPackageName().startsWith(prefix)){
                    matched = true;
                    break;
                }
            }
        } else {
            matched = true;
        }
        if(getExceptPackage().size() > 0){
            for (String prefix : getExceptPackage()) {
                if(clazz.getPackageName().startsWith(prefix)){
                    matched = false;
                    break;
                }
            }
        }
        return matched;
    }

    public static String getApplicationName(Class<? extends Application> clazz){
        return getName(clazz);
    }

    public static String getApplicationVersion(Class<? extends Application> clazz){
        return getVersion(clazz);
    }

    public static boolean moduleExists(Class<? extends Application> clazz, Class<? extends AppModule> module) {
        final List<Class<? extends AppModule>> list = new LinkedList<>();
        invokeRegisterModules(clazz, list);
        return list.contains(module);
    }

    @Override
    protected void initBegin(){
        super.initBegin();
        Ready.post(new GeneralEvent(Event.APP_FRAMEWORK_INIT_BEGIN, this));

        coreContext.pluginMap.forEach((name,plugin) -> {
            plugin.appInitBegin(this);
        });
    }

    @Override
    protected void initHandlers() {
        handler().startInit();
        super.initHandlers();
        handler().addHandlerByConfig(handler().getConfig());
        handler().endInit();
        Ready.post(new GeneralEvent(Event.HANDLER_MANAGER_AFTER_INIT, this));
    }

    @Override
    protected void initSecurity(){
        security().startInit();
        super.initSecurity();
        security().endInit();
        Ready.post(new GeneralEvent(Event.SECURITY_MANAGER_AFTER_INIT, this));
    }

    @Override
    protected void initRoutes() {
        route().startInit();
        super.initRoutes();
        
        route().add(route().getConfig());
        route().endInit();
        Ready.post(new GeneralEvent(Event.ROUTE_MANAGER_AFTER_INIT, this));
    }

    @Override
    protected void initRender(){
        render().startInit();
        super.initRender();
        render().endInit();
        Ready.post(new GeneralEvent(Event.RENDER_MANAGER_AFTER_INIT, this));
    }

    @Override
    protected void initWebSocket() {
        webSocket().startInit();
        super.initWebSocket();
        webSocket().endInit();
        Ready.post(new GeneralEvent(Event.WEB_SOCKET_MANAGER_AFTER_INIT, this));
    }

    @Override
    protected void initWebServer(){
        webServer().startInit();
        super.initWebServer();
        webServer().endInit();
        Ready.post(new GeneralEvent(Event.WEB_SERVER_AFTER_INIT, this));
    }

    @Override
    protected void initEnd(){
        super.initEnd();
        coreContext.pluginMap.forEach((name,plugin) -> {
            plugin.appInitEnd(this);
        });
        Ready.post(new GeneralEvent(Event.APP_FRAMEWORK_INIT_END, this));
    }

    protected void coreInitBegin(){

    }

    protected void coreInitEnd(){

    }

    protected void globalConfig(ApplicationConfig config) {

    }

    protected void appConfig(ApplicationConfig config) {

    }

    @Override
    protected void initialize() {

    }
}
