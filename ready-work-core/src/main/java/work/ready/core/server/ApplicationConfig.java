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

import work.ready.core.aop.transformer.match.InterceptorPoint;
import work.ready.core.apm.ApmConfig;
import work.ready.core.component.cache.CacheConfig;
import work.ready.core.config.BaseConfig;
import work.ready.core.database.DatabaseConfig;
import work.ready.core.handler.ServerModuleConfig;
import work.ready.core.handler.resource.StaticResourceConfig;
import work.ready.core.log.LogConfig;
import work.ready.core.module.AppModuleConfig;
import work.ready.core.tools.StrUtil;
import work.ready.core.security.SecurityConfig;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;

public class ApplicationConfig extends BaseConfig {

    private String project = Constant.DEFAULT_PROJECT;

    private String version = Constant.DEFAULT_VERSION;

    private String activeProfile = Constant.DEV_PROFILE;

    private String rootPath;

    private String configPath;

    private String assetsPath;

    private String downloadPath = Constant.DEFAULT_BASE_DOWNLOAD_PATH;

    private String uploadPath = Constant.DEFAULT_BASE_UPLOAD_PATH;

    private Integer maxUploadFileSize = Constant.DEFAULT_MAX_UPLOAD_FILE_SIZE;

    private boolean directlySaveUploadFile = false;

    private boolean viewInJar = false;

    private String viewPath = "view";

    private String viewFileExt;

    private LogConfig log = new LogConfig();
    private boolean printGeneratedClassToLogger = false;

    private boolean enableBanner = true;
    private String bannerFile = "banner.txt";

    private String mainNtpHost = "ntp6.aliyun.com";
    private List<String> fallbackNtpHost = Arrays.asList(new String[]{"cn.ntp.org.cn","ntp2.aliyun.com", "time2.cloud.tencent.com", "sgp.ntp.org.cn", "jp.ntp.org.cn"});

    private String uuidWorkerId = "MAC"; 

    private String scanJarPrefix;
    private String skipJarPrefix;
    private String hotSwapClassPrefix;

    private Map<String, Object> spi = new HashMap<>();

    private String encoding = Constant.DEFAULT_ENCODING;
    private String charset = Constant.DEFAULT_CHARSET.name();
    private Charset standardCharset = Constant.DEFAULT_CHARSET;

    private Map<String, InterceptorPoint> interceptor = new HashMap<>();
    private ApmConfig apm = new ApmConfig();
    private ServerConfig server = new ServerConfig();
    private SecurityConfig security = new SecurityConfig();

    private AppModuleConfig appModule = new AppModuleConfig();
    private ServerModuleConfig serverModule = new ServerModuleConfig();

    private boolean enableSession = true;
    private int sessionMaxInactiveInterval = 1800;

    private StaticResourceConfig staticResource = new StaticResourceConfig();

    private CacheConfig cache = new CacheConfig();

    private DatabaseConfig database = new DatabaseConfig();

    private List<String> configMergeByReplace = new ArrayList<>();

    public String getProject() {
        return project;
    }

    public ApplicationConfig setProject(String project) {
        this.project = project;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public ApplicationConfig setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getActiveProfile() {
        return activeProfile;
    }

    public ApplicationConfig setActiveProfile(String activeProfile) {
        this.activeProfile = activeProfile;
        return this;
    }

    public boolean isDevMode(){
        return Constant.DEV_PROFILE.equals(activeProfile) || activeProfile == null;
    }

    public boolean isEnableBanner() {
        return enableBanner;
    }

    public ApplicationConfig setEnableBanner(boolean enableBanner) {
        this.enableBanner = enableBanner;
        return this;
    }

    public String getBannerFile() {
        return bannerFile;
    }

    public ApplicationConfig setBannerFile(String bannerFile) {
        this.bannerFile = bannerFile;
        return this;
    }

    public String getAssetsPath() {
        return assetsPath;
    }

    public ApplicationConfig setAssetsPath(String assetsPath) {
        this.assetsPath = assetsPath;
        return this;
    }

    public String getDownloadPath() {
        return downloadPath;
    }

    public ApplicationConfig setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
        return this;
    }

    public String getUploadPath() {
        return uploadPath;
    }

    public ApplicationConfig setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
        return this;
    }

    public Integer getMaxUploadFileSize() {
        return maxUploadFileSize;
    }

    public ApplicationConfig setMaxUploadFileSize(Integer maxUploadFileSize) {
        this.maxUploadFileSize = maxUploadFileSize;
        return this;
    }

    public ApplicationConfig setDirectlySaveUploadFile(boolean directlySaveUploadFile) {
        this.directlySaveUploadFile = directlySaveUploadFile;
        return this;
    }

    public boolean isDirectlySaveUploadFile() {
        return directlySaveUploadFile;
    }

    public boolean isViewInJar() {
        return viewInJar;
    }

    public ApplicationConfig setViewInJar(boolean viewInJar) {
        this.viewInJar = viewInJar;
        return this;
    }

    public String getViewPath() {
        return viewPath;
    }

    public ApplicationConfig setViewPath(String viewPath) {
        this.viewPath = viewPath;
        return this;
    }

    public String getViewFileExt() {
        return StrUtil.notBlank(viewFileExt) ? viewFileExt : Constant.DEFAULT_VIEW_FILE_EXT;
    }

    public ApplicationConfig setViewFileExt(String viewFileExt) {
        this.viewFileExt = viewFileExt;
        return this;
    }

    public String getMainNtpHost() {
        return mainNtpHost;
    }

    public ApplicationConfig setMainNtpHost(String mainNtpHost) {
        this.mainNtpHost = mainNtpHost;
        return this;
    }

    public List<String> getFallbackNtpHost() {
        return fallbackNtpHost;
    }

    public ApplicationConfig setFallbackNtpHost(List<String> fallbackNtpHost) {
        this.fallbackNtpHost = fallbackNtpHost;
        return this;
    }

    public String getUuidWorkerId() {
        return uuidWorkerId;
    }

    public void setUuidWorkerId(String uuidWorkerId) {
        this.uuidWorkerId = uuidWorkerId;
    }

    public String getScanJarPrefix() {
        return scanJarPrefix;
    }

    public ApplicationConfig setScanJarPrefix(String scanJarPrefix) {
        this.scanJarPrefix = scanJarPrefix;
        return this;
    }

    public String getSkipJarPrefix() {
        return skipJarPrefix;
    }

    public ApplicationConfig setSkipJarPrefix(String skipJarPrefix) {
        this.skipJarPrefix = skipJarPrefix;
        return this;
    }

    public String getHotSwapClassPrefix() {
        return hotSwapClassPrefix;
    }

    public ApplicationConfig setHotSwapClassPrefix(String hotSwapClassPrefix) {
        this.hotSwapClassPrefix = hotSwapClassPrefix;
        return this;
    }

    public Map<String, Object> getSpi() {
        return spi;
    }

    public ApplicationConfig setSpi(Map<String, Object> spi) {
        this.spi = spi;
        return this;
    }

    public ApplicationConfig AddSpi(String className, Class<?> clazz){
        this.spi.put(className, clazz);
        return this;
    }

    public String getRootPath() {
        return rootPath;
    }

    public ApplicationConfig setRootPath(String rootPath) {
        this.rootPath = rootPath;
        return this;
    }

    public String getConfigPath() {
        return configPath;
    }

    public ApplicationConfig setConfigPath(String configPath) {
        this.configPath = configPath;
        return this;
    }

    public boolean isPrintGeneratedClassToLogger() {
        return printGeneratedClassToLogger;
    }

    public ApplicationConfig setPrintGeneratedClassToLogger(boolean printGeneratedClassToLogger) {
        this.printGeneratedClassToLogger = printGeneratedClassToLogger;
        return this;
    }

    public LogConfig getLog() {
        return log;
    }

    public ApplicationConfig setLog(LogConfig log) {
        this.log = log;
        return this;
    }

    public Map<String, InterceptorPoint> getInterceptor() {
        return interceptor;
    }

    public ApplicationConfig setInterceptor(Map<String, InterceptorPoint> interceptor) {
        this.interceptor = interceptor;
        return this;
    }

    public ApmConfig getApm() {
        return apm;
    }

    public ApplicationConfig setApm(ApmConfig apm) {
        this.apm = apm;
        return this;
    }

    public ServerConfig getServer() {
        return server;
    }

    public ApplicationConfig setServer(ServerConfig server) {
        this.server = server;
        return this;
    }

    public SecurityConfig getSecurity() {
        return security;
    }

    public ApplicationConfig setSecurity(SecurityConfig security) {
        this.security = security;
        return this;
    }

    public AppModuleConfig getAppModule() {
        return appModule;
    }

    public ApplicationConfig setAppModule(AppModuleConfig appModule) {
        this.appModule = appModule;
        return this;
    }

    public ServerModuleConfig getServerModule() {
        return serverModule;
    }

    public ApplicationConfig setServerModule(ServerModuleConfig serverModule) {
        this.serverModule = serverModule;
        return this;
    }

    public boolean isEnableSession() {
        return enableSession;
    }

    public ApplicationConfig setEnableSession(boolean enableSession) {
        this.enableSession = enableSession;
        return this;
    }

    public String getEncoding() { return encoding; }

    public ApplicationConfig setEncoding(String encoding) {
        this.encoding = encoding;
        return this;
    }

    public String getCharset() {
        return charset;
    }

    public ApplicationConfig setCharset(String charset) {
        Charset cs = null;
        try {
            cs = Charset.forName(charset);
        } catch (UnsupportedCharsetException e) {
            throw new RuntimeException("'" + charset + "' is an unsupported charset.");
        }
        this.charset = charset;
        this.standardCharset = cs;
        return this;
    }

    public Charset getStandardCharset(){
        return standardCharset;
    }

    public int getSessionMaxInactiveInterval() {
        return sessionMaxInactiveInterval;
    }

    public ApplicationConfig setSessionMaxInactiveInterval(int sessionMaxInactiveInterval) {
        this.sessionMaxInactiveInterval = sessionMaxInactiveInterval;
        return this;
    }

    public CacheConfig getCache() {
        return cache;
    }

    public ApplicationConfig setCache(CacheConfig cache) {
        this.cache = cache;
        return this;
    }

    public DatabaseConfig getDatabase() {
        return database;
    }

    public ApplicationConfig setDatabase(DatabaseConfig database) {
        this.database = database;
        return this;
    }

    public StaticResourceConfig getStaticResource() {
        return staticResource;
    }

    public ApplicationConfig setStaticResource(StaticResourceConfig staticResource) {
        this.staticResource = staticResource;
        return this;
    }

    public List<String> getConfigMergeByReplace() {
        return configMergeByReplace;
    }

    public ApplicationConfig setConfigMergeByReplace(List<String> configMergeByReplace) {
        this.configMergeByReplace = configMergeByReplace;
        return this;
    }

    public ApplicationConfig addConfigMergeByReplace(String configItemName){
        configMergeByReplace.add(configItemName);
        return this;
    }

    public ApplicationConfig addConfigMergeByReplace(Class<?> configItemType){
        configMergeByReplace.add(configItemType.getCanonicalName());
        return this;
    }

    @Override
    public void validate() {
        if(!StrUtil.isValidVersion(version)){
            throw new RuntimeException("invalid version: " + version);
        }
        if(apm != null) apm.validate();
        if(server != null) server.validate();
        if(security != null) security.validate();
        if(staticResource != null) staticResource.validate();
        if(appModule != null) appModule.validate();
        if(serverModule != null) serverModule.validate();
        if(cache != null) cache.validate();
        if(database != null) database.validate();
    }
}
