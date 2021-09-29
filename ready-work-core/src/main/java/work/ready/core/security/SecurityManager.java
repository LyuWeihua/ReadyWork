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

package work.ready.core.security;

import work.ready.core.aop.AopComponent;
import work.ready.core.component.cache.annotation.CacheEvict;
import work.ready.core.component.cache.interceptor.CacheEvictInterceptor;
import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.handler.RequestMethod;
import work.ready.core.handler.request.RequestParser;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.ApplicationContext;
import work.ready.core.module.Initializer;
import work.ready.core.security.access.limiter.Limiter;
import work.ready.core.security.access.limiter.RateLimiter;
import work.ready.core.security.access.limiter.RequestLimiter;
import work.ready.core.security.access.limiter.exception.LimiterWithSameNameException;
import work.ready.core.security.access.limiter.limit.Limit;
import work.ready.core.security.access.limiter.storage.LimitUsageStorage;
import work.ready.core.security.cors.CorsInterceptor;
import work.ready.core.security.cors.EnableCORS;
import work.ready.core.server.ApplicationConfig;
import work.ready.core.server.Ready;
import work.ready.core.tools.ClassUtil;
import work.ready.core.tools.StrUtil;

import java.time.Clock;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SecurityManager {

    private static final Log logger = LogFactory.getLog(SecurityManager.class);

    private final ApplicationContext context;
    protected List<Initializer<SecurityManager>> initializers = new ArrayList<>();
    protected final SecurityConfig config;
    protected Consumer<LimiterConfig> configSyncer;
    protected AccessControl accessControl;
    private RateLimiter rateLimiter;
    private RequestLimiter requestLimiter;
    private Set<String> allowIpSet;
    private Set<String> denyIpSet;
    private Set<String> allowHeadSet;
    private Set<String> denyHeadSet;
    private LimitUsageStorage limitStorage;
    private LimiterServerModule limiterServerModule;
    private Clock clock;

    public SecurityManager(ApplicationContext context) {
        this.context = context;
        config = Ready.getApplicationConfig(context.application.getName()).getSecurity();
        if(config != null) config.validate();
        Ready.post(new GeneralEvent(Event.SECURITY_MANAGER_CREATE, this));
    }

    public void addInitializer(Initializer<SecurityManager> initializer) {
        this.initializers.add(initializer);
        initializers.sort(Comparator.comparing(Initializer::order));
    }

    public void startInit() {
        try {
            for (Initializer<SecurityManager> i : initializers) {
                i.startInit(this);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        context.coreContext.getInterceptorManager().addAopComponent(new AopComponent().setAnnotation(EnableCORS.class)
                .setInterceptorClass(CorsInterceptor.class));

        Ready.eventManager().addListener(this, "limiterConfigChangeListener",
                (setter -> setter.addName(Event.LIMITER_CONFIG_CHANGED)
                        .setContextReference(config.getLimiter()))); 

        if (config.getLimiter().getMaxForwardedIPs() != null)
            RequestParser.clientIPParser.maxForwardedIPs = config.getLimiter().getMaxForwardedIPs();

        clock = Clock.systemDefaultZone();
        if(config.getLimiter().getStorageProvider().containsKey(config.getLimiter().getStorageType())){
            limitStorage = (LimitUsageStorage)ClassUtil.newInstance(config.getLimiter().getStorageProvider().get(config.getLimiter().getStorageType()));
        } else {
            throw new RuntimeException("Unknown type of LimitUsageStorage: " + config.getLimiter().getStorageType());
        }
        limiterServerModule = new LimiterServerModule(this);
        context.handlerManager.addHandler(new String[]{"/**"}, new RequestMethod[]{RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE}, limiterServerModule);
        if(accessControl == null) applyRulesFromConfig(); // make sure the accessControl initialized, it is possible that rules have been applied already in cloud mode
    }

    public void endInit() {
        try {
            for (Initializer<SecurityManager> i : initializers) {
                i.endInit(this);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void limiterConfigChangeListener(GeneralEvent event) {
        //if (event.get(LimiterConfig.externalType) == null) return;  // make sure from external, LimiterConfig changed from outside
        //LimiterConfig config = event.getObject();
        applyRulesFromConfig(); // we have to apply changes from external and local, because changes could be made locally at runtime
    }

    private void applyRulesFromConfig() {
        if(logger.isTraceEnabled()) logger.trace("applying rules from LimiterConfig");
        String cidr = config.getLimiter().getAllowCIDR();
        allowIp(cidr != null ? Arrays.asList(cidr.replace(" ","").split("[,;]")) : Collections.emptyList(), true);
        cidr = config.getLimiter().getDenyCIDR();
        denyIp(cidr != null ? Arrays.asList(cidr.replace(" ","").split("[,;]")) : Collections.emptyList(), true);

        String head = config.getLimiter().getAllowHead();
        allowHead(true, head != null ? head.split("[|`]") : new String[]{});
        head = config.getLimiter().getDenyHead();
        denyHead(true, head != null ? head.split("[|`]") : new String[]{});

        if(config.getLimiter().isEnableDownloadRateLimiter()){
            rateLimiter(true);
        } else {
            setRateLimiter(null);
        }
        if(config.getLimiter().isEnableConcurrentRequestLimiter()){
            requestLimiter(true);
        } else {
            setRequestLimiter(null);
        }
    }

    public void addStorageType(String storageType, Class<? extends LimitUsageStorage> storageClass) {
        config.getLimiter().getStorageProvider().put(storageType, storageClass);
    }

    public String getStorageType() {
        return config.getLimiter().getStorageType();
    }

    public SecurityManager setStorageType(String storageType) {
        config.getLimiter().setStorageType(storageType);
        return this;
    }

    public SecurityConfig getConfig() {
        return config;
    }

    public SecurityManager setConfigSyncer(Consumer<LimiterConfig> configSyncer){
        this.configSyncer = configSyncer;
        return this;
    }

    public SecurityManager configSync(){
        if(configSyncer != null) configSyncer.accept(config.getLimiter());
        return this;
    }

    RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    SecurityManager setRateLimiter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
        return this;
    }

    RateLimiter rateLimiter(){ return rateLimiter(false); }
    RateLimiter rateLimiter(boolean reNew){
        if (getRequestLimiter() == null || reNew) {
            if(config.getLimiter().isEnableDownloadRateLimiter()){
                rateLimiter = new RateLimiter(config.getLimiter().getLimitRateBytes(), config.getLimiter().getLimitRatePeriod());
            } else {
                throw new RuntimeException("enableDownloadRateLimiter config is not enabled, please enabled it first.");
            }
        }
        return getRateLimiter();
    }

    public boolean isEnableDownloadRateLimiter() {
        return config.getLimiter().isEnableDownloadRateLimiter();
    }

    public SecurityManager setEnableDownloadRateLimiter(boolean enableDownloadRateLimiter) {
        config.getLimiter().setEnableDownloadRateLimiter(enableDownloadRateLimiter);
        return this;
    }

    public int getLimitRateBytes() {
        return config.getLimiter().getLimitRateBytes();
    }

    public SecurityManager setLimitRateBytes(int limitRateBytes) {
        rateLimiter().setLimitRateBytes(limitRateBytes);
        config.getLimiter().setLimitRateBytes(limitRateBytes);
        return this;
    }

    public int getLimitRatePeriod() {
        return config.getLimiter().getLimitRatePeriod();
    }

    public SecurityManager setLimitRatePeriod(int limitRatePeriod) {
        rateLimiter().setLimitRatePeriod(limitRatePeriod);
        config.getLimiter().setLimitRatePeriod(limitRatePeriod);
        return this;
    }

    RequestLimiter getRequestLimiter() {
        return requestLimiter;
    }

    SecurityManager setRequestLimiter(RequestLimiter requestLimiter) {
        this.requestLimiter = requestLimiter;
        return this;
    }

    RequestLimiter requestLimiter(){ return requestLimiter(false); }
    RequestLimiter requestLimiter(boolean reNew){
        if (getRequestLimiter() == null || reNew) {
            if(config.getLimiter().isEnableConcurrentRequestLimiter()){
                requestLimiter = new RequestLimiter(context.handlerManager,
                        config.getLimiter().getMaxConcurrentRequests(),
                        config.getLimiter().getMaxConcurrentPerIp(),
                        config.getLimiter().getRequestLimitQueueSize());
            } else {
                throw new RuntimeException("enableConcurrentRequestLimiter config is not enabled, please enabled it first.");
            }
        }
        return getRequestLimiter();
    }

    public boolean isEnableConcurrentRequestLimiter() {
        return config.getLimiter().isEnableConcurrentRequestLimiter();
    }

    public SecurityManager setEnableConcurrentRequestLimiter(boolean enableConcurrentRequestLimiter) {
        config.getLimiter().setEnableConcurrentRequestLimiter(enableConcurrentRequestLimiter);
        return this;
    }

    public int getMaxConcurrentRequests() {
        return config.getLimiter().getMaxConcurrentRequests();
    }

    public SecurityManager setMaxConcurrentRequests(int maxConcurrentRequests) {
        requestLimiter().setMaximumConcurrentRequests(maxConcurrentRequests);
        config.getLimiter().setMaxConcurrentRequests(maxConcurrentRequests);
        return this;
    }

    public int getRequestLimitQueueSize() {
        return config.getLimiter().getRequestLimitQueueSize();
    }

    public SecurityManager setRequestLimitQueueSize(int requestLimitQueueSize) {
        config.getLimiter().setRequestLimitQueueSize(requestLimitQueueSize);
        return this;
    }

    public int getMaxConcurrentPerIp() {
        return config.getLimiter().getMaxConcurrentPerIp();
    }

    public SecurityManager setMaxConcurrentPerIp(int maxConcurrentPerIp) {
        requestLimiter().setMaxConcurrentPerIp(maxConcurrentPerIp);
        config.getLimiter().setMaxConcurrentPerIp(maxConcurrentPerIp);
        return this;
    }

    AccessControl getAccessControl(){
        return accessControl;
    }

    SecurityManager setAccessControl(AccessControl accessControl){
        this.accessControl = accessControl;
        return this;
    }

    public LimiterServerModule getLimiterServerModule() {
        return limiterServerModule;
    }

    private AccessControl accessControl() {
        if (getAccessControl() == null) {
            setAccessControl(new AccessControl(this));
            allowIpSet = new HashSet<>();
            denyIpSet = new HashSet<>();
            allowHeadSet = new HashSet<>();
            denyHeadSet = new HashSet<>();
        }
        return getAccessControl();
    }

    public boolean isEnableSkipLocalIp() {
        return config.getLimiter().isEnableSkipLocalIp();
    }

    public SecurityManager setEnableSkipLocalIp(boolean skipLocalIp) {
        config.getLimiter().setEnableSkipLocalIp(skipLocalIp);
        return this;
    }

    public List<String> getSkipIpControlUrl() {
        return config.getLimiter().getSkipIpControlUrl();
    }

    public SecurityManager addSkipIpControlUrl(String url) {
        config.getLimiter().addSkipIpControlUrl(url);
        return this;
    }

    public List<String> getNeedIpControlUrl() {
        return config.getLimiter().getNeedIpControlUrl();
    }

    public SecurityManager addNeedIpControlUrl(String url) {
        config.getLimiter().addNeedIpControlUrl(url);
        return this;
    }

    public List<String> getSkipFrequencyLimiterUrl() {
        return config.getLimiter().getSkipFrequencyLimiterUrl();
    }

    public SecurityManager addSkipFrequencyLimiterUrl(String url) {
        config.getLimiter().addSkipFrequencyLimiterUrl(url);
        return this;
    }

    public List<String> getNeedFrequencyLimiterUrl() {
        return config.getLimiter().getNeedFrequencyLimiterUrl();
    }

    public SecurityManager addNeedFrequencyLimiterUrl(String url) {
        config.getLimiter().addNeedFrequencyLimiterUrl(url);
        return this;
    }

    public List<String> getSkipConcurrentLimiterUrl() {
        return config.getLimiter().getSkipConcurrentLimiterUrl();
    }

    public SecurityManager addSkipConcurrentLimiterUrl(String url) {
        config.getLimiter().addSkipConcurrentLimiterUrl(url);
        return this;
    }

    public List<String> getNeedConcurrentLimiterUrl() {
        return config.getLimiter().getNeedConcurrentLimiterUrl();
    }

    public SecurityManager addNeedConcurrentLimiterUrl(String url) {
        config.getLimiter().addNeedConcurrentLimiterUrl(url);
        return this;
    }

    public List<String> getSkipRateLimiterUrl() {
        return config.getLimiter().getSkipRateLimiterUrl();
    }

    public SecurityManager addSkipRateLimiterUrl(String url) {
        config.getLimiter().addSkipRateLimiterUrl(url);
        return this;
    }

    public List<String> getNeedRateLimiterUrl() {
        return config.getLimiter().getNeedRateLimiterUrl();
    }

    public SecurityManager addNeedRateLimiterUrl(String url) {
        config.getLimiter().addNeedRateLimiterUrl(url);
        return this;
    }

    public boolean isEnableIpBasedLimiter() {
        return config.getLimiter().isEnableIpBasedLimiter();
    }

    public SecurityManager setEnableIpBasedLimiter(boolean enableIpBasedLimiter) {
        config.getLimiter().setEnableIpBasedLimiter(enableIpBasedLimiter);
        return this;
    }

    public int getLimitCapacityForIp() {
        return config.getLimiter().getLimitCapacityForIp();
    }

    public SecurityManager setLimitCapacityForIp(int limitCapacityForIp) {
        config.getLimiter().setLimitCapacityForIp(limitCapacityForIp);
        return this;
    }

    public int getDurationOfLimitForIp() {
        return config.getLimiter().getDurationOfLimitForIp();
    }

    public SecurityManager setDurationOfLimitForIp(int durationOfLimitForIp) {
        config.getLimiter().setDurationOfLimitForIp(durationOfLimitForIp);
        return this;
    }

    public List<String> getExtraLimit() {
        return config.getLimiter().getExtraLimit();
    }

    public SecurityManager addExtraLimit(String line) {
        config.getLimiter().addExtraLimit(line);
        return this;
    }

    public boolean isEnableUserBasedLimiter() {
        return config.getLimiter().isEnableUserBasedLimiter();
    }

    public SecurityManager setEnableUserBasedLimiter(boolean enableUserBasedLimiter) {
        config.getLimiter().setEnableUserBasedLimiter(enableUserBasedLimiter);
        return this;
    }

    public int getLimitCapacityForUser() {
        return config.getLimiter().getLimitCapacityForUser();
    }

    public SecurityManager setLimitCapacityForUser(int limitCapacityForUser) {
        config.getLimiter().setLimitCapacityForUser(limitCapacityForUser);
        return this;
    }

    public int getDurationOfLimitForUser() {
        return config.getLimiter().getDurationOfLimitForUser();
    }

    public SecurityManager setDurationOfLimitForUser(int durationOfLimitForUser) {
        config.getLimiter().setDurationOfLimitForUser(durationOfLimitForUser);
        return this;
    }

    public SecurityManager allowIp(String... cidr) {
        return allowIp(Arrays.asList(cidr), false);
    }

    /**
     * Set allowed cidr blocks, e.g. 192.168.0.1/24 or 192.168.1.1/32 for single ip
     *
     * <p>
     * cidr can take several forms:
     * <p>
     * a.b.c.d = Literal IPv4 Address
     * a:b:c:d:e:f:g:h = Literal IPv6 Address
     * a.b.* = Wildcard IPv4 Address
     * a:b:* = Wildcard IPv6 Address
     * a.b.c.0/24 = Classless wildcard IPv4 address
     * a:b:c:d:e:f:g:0/120 = Classless wildcard IPv4 address
     *
     * @param cidrs cidr blocks
     */
    public SecurityManager allowIp(List<String> cidrs){
        return allowIp(cidrs, false);
    }

    private SecurityManager allowIp(List<String> cidrs, boolean fromConfig) {
        AccessControl accessControl = accessControl();
        if(fromConfig) {
            allowIpSet.clear();
            accessControl.clearIpAllowRules();
        }
        cidrs.forEach(cidr->{
            if(StrUtil.notBlank(cidr) && allowIpSet.add(cidr)) {
                accessControl.addAllowIp(cidr);
                if(!fromConfig) config.getLimiter().setAllowCIDR(StrUtil.join(allowIpSet, ","));
            }
        });
        logger.info("allow http access, cidrs=%s", cidrsLogParam(cidrs, 5));
        return this;
    }

    public SecurityManager removeAllowIp(String ip){
        accessControl();
        if(allowIpSet.remove(ip)) {
            accessControl().removeAllowIp(ip);
            config.getLimiter().setAllowCIDR(StrUtil.join(allowIpSet, ","));
        }
        return this;
    }

    public SecurityManager denyIp(String... cidr) {
        return denyIp(Arrays.asList(cidr), false);
    }

    public SecurityManager denyIp(List<String> cidrs){
        return denyIp(cidrs, false);
    }

    private SecurityManager denyIp(List<String> cidrs, boolean fromConfig) {
        AccessControl accessControl = accessControl();
        if(fromConfig) {
            denyIpSet.clear();
            accessControl.clearIpDenyRules();
        }
        cidrs.forEach(cidr->{
            if(StrUtil.notBlank(cidr) && denyIpSet.add(cidr)) {
                accessControl.addDenyIp(cidr);
                if(!fromConfig) config.getLimiter().setDenyCIDR(StrUtil.join(denyIpSet, ","));
            }
        });
        logger.info("deny http access, cidrs=%s", cidrsLogParam(cidrs, 5));
        return this;
    }

    public SecurityManager removeDenyIp(String ip) {
        accessControl();
        if(denyIpSet.remove(ip)) {
            accessControl().removeDenyIp(ip);
            config.getLimiter().setDenyCIDR(StrUtil.join(denyIpSet, ","));
        }
        return this;
    }

    public SecurityManager clearIpRules() {
        return clearIpAllowRules().clearIpDenyRules();
    }

    public SecurityManager clearIpAllowRules() {
        accessControl().clearIpAllowRules();
        allowIpSet.clear();
        config.getLimiter().setAllowCIDR(null);
        return this;
    }

    public SecurityManager clearIpDenyRules() {
        accessControl().clearIpDenyRules();
        denyIpSet.clear();
        config.getLimiter().setDenyCIDR(null);
        return this;
    }

    String cidrsLogParam(List<String> cidrs, int maxSize) {
        if (cidrs.size() <= maxSize) return String.valueOf(cidrs);
        return "[" + String.join(", ", cidrs.subList(0, maxSize)) + ", ...]";
    }

    private SecurityManager allowHead(boolean fromConfig, String... heads) {
        if(fromConfig) {
            accessControl().clearHeadAllowRules();
            allowHeadSet.clear();
        }
        for(String head : heads){
            String[] cfg = StrUtil.split(head, ":");
            if(cfg.length == 2){
                allowHead(cfg[0].trim(), cfg[1].trim(), fromConfig);
            } else {
                logger.warn("Skip wrong AllowHead config string: " + head);
                continue;
            }
        }
        return this;
    }

    public SecurityManager allowHead(final String headName, final String match) {
        return allowHead(headName, match, false);
    }

    private SecurityManager allowHead(final String headName, final String match, boolean fromConfig) {
        accessControl();
        if(allowHeadSet.add(headName + ":" + match)) {
            accessControl().addAllowHead(headName, match);
            if(!fromConfig) config.getLimiter().setAllowHead(StrUtil.join(allowHeadSet, "|"));
            logger.info("allow http access, head %s matches %s", headName, match);
        }
        return this;
    }

    public SecurityManager removeAllowHead(final String headName) {
        accessControl();
        if(allowHeadSet.remove(headName)) {
            accessControl().removeAllowHead(headName);
            config.getLimiter().setAllowHead(StrUtil.join(allowHeadSet, "|"));
        }
        return this;
    }

    private SecurityManager denyHead(boolean fromConfig, String... heads) {
        if(fromConfig) {
            accessControl().clearHeadDenyRules();
            denyHeadSet.clear();
        }
        for(String head : heads){
            String[] cfg = StrUtil.split(head, ":");
            if(cfg.length == 2){
                denyHead(cfg[0].trim(), cfg[1].trim(), fromConfig);
            } else {
                logger.warn("Skip wrong DenyHead config string: " + head);
                continue;
            }
        }
        return this;
    }

    public SecurityManager denyHead(final String headName, final String match) {
        return denyHead(headName, match, false);
    }

    private SecurityManager denyHead(final String headName, final String match, boolean fromConfig) {
        accessControl();
        if(denyHeadSet.add(headName + ":" + match)) {
            accessControl().addDenyHead(headName, match);
            if(!fromConfig) config.getLimiter().setDenyHead(StrUtil.join(denyHeadSet, "|"));
            logger.info("deny http access, head %s matches %s", headName, match);
        }
        return this;
    }

    public SecurityManager removeDenyHead(final String headName) {
        accessControl();
        if(denyHeadSet.remove(headName)) {
            accessControl().removeDenyHead(headName);
            config.getLimiter().setDenyHead(StrUtil.join(denyHeadSet, "|"));
        }
        return this;
    }

    public SecurityManager clearHeadRules() {
        return clearHeadAllowRules().clearHeadDenyRules();
    }

    public SecurityManager clearHeadAllowRules() {
        accessControl().clearHeadAllowRules();
        allowHeadSet.clear();
        config.getLimiter().setAllowHead(null);
        return this;
    }

    public SecurityManager clearHeadDenyRules() {
        accessControl().clearHeadDenyRules();
        denyHeadSet.clear();
        config.getLimiter().setDenyHead(null);
        return this;
    }

    public LimitUsageStorage getLimitStorage() {
        return limitStorage;
    }

    public SecurityManager setLimitStorage(LimitUsageStorage limitStorage) {
        this.limitStorage = limitStorage;
        return this;
    }

    public Clock getClock() {
        return clock;
    }

    public SecurityManager setClock(Clock clock) {
        this.clock = clock;
        return this;
    }

    public final <T> Limiter<T> getLimiter(String resource, Limit<T>... limits)
            throws LimiterWithSameNameException {
        if(limitStorage == null) throw new RuntimeException("limitStorage is null, please provide a limitStorage.");
        return getLimiter(resource, clock, limitStorage, limits);
    }

    public final <T> Limiter<T> getLimiter(String resource, LimitUsageStorage limitStorage, Limit<T>... limits)
            throws LimiterWithSameNameException {
        if(clock == null) throw new RuntimeException("clock is null, please provide a clock.");
        return getLimiter(resource, clock, limitStorage, limits);
    }

    public final <T> Limiter<T> getLimiter(String resource, Clock clock, LimitUsageStorage limitStorage, Limit<T>... limits)
            throws LimiterWithSameNameException {
        Map<String, Long> countOfDistinctLimits =
                Arrays.stream(limits).collect(Collectors.groupingBy(Limit::getName, Collectors.counting()));

        List<String> duplicateNames =
                countOfDistinctLimits
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getValue() > 1)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());

        if (!duplicateNames.isEmpty()) {
            throw new LimiterWithSameNameException(
                    String.join(" ", duplicateNames));
        }

        return new Limiter<>(clock, limitStorage, resource, limits);
    }

    public static final String IpControl = "IpControl";
    public static final String FrequencyLimiter = "FrequencyLimiter";
    public static final String ConcurrentLimiter = "ConcurrentLimiter";
    public static final String RateLimiter = "RateLimiter";
    public boolean pathShouldHandle(String path, String type){
        if(IpControl.equals(type)){
            return pathShouldHandle(path,
                    getConfig().getLimiter().getSkipIpControlUrl(),
                    getConfig().getLimiter().getNeedIpControlUrl());
        }
        if(FrequencyLimiter.equals(type)){
            return pathShouldHandle(path,
                    getConfig().getLimiter().getSkipFrequencyLimiterUrl(),
                    getConfig().getLimiter().getNeedFrequencyLimiterUrl());
        }
        if(ConcurrentLimiter.equals(type)){
            return pathShouldHandle(path,
                    getConfig().getLimiter().getSkipConcurrentLimiterUrl(),
                    getConfig().getLimiter().getNeedConcurrentLimiterUrl());
        }
        if(RateLimiter.equals(type)){
            return pathShouldHandle(path,
                    getConfig().getLimiter().getSkipRateLimiterUrl(),
                    getConfig().getLimiter().getNeedRateLimiterUrl());
        }
        return true;
    }

    private boolean pathShouldHandle(String path, List<String> skipUrl, List<String> needUrl){
        if (skipUrl != null) {
            for(String url : skipUrl){
                if(path.startsWith(url)){
                    return false;
                }
            }
        }

        if (needUrl != null) {
            for(String url : needUrl){
                if(path.startsWith(url)){
                    return true;
                }
            }
        } else {
            return true;
        }

        return false;
    }

}
