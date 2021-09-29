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

import work.ready.core.config.BaseConfig;
import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.security.access.limiter.limit.Limit;
import work.ready.core.security.access.limiter.limit.LimitBuilder;
import work.ready.core.security.access.limiter.storage.InMemoryStorage;
import work.ready.core.security.access.limiter.storage.LimitUsageStorage;
import work.ready.core.security.access.limiter.storage.RedisStorage;
import work.ready.core.server.Ready;
import work.ready.core.tools.StrUtil;

import java.time.Duration;
import java.util.*;

public class LimiterConfig extends BaseConfig {
    private static final Log logger = LogFactory.getLog(LimiterConfig.class);
    public static final String TYPE_IGNITE = "ignite";
    public static final String TYPE_REDIS = "redis";
    public static final String TYPE_MEMORY = "memory";

    public static final String MODE_SINGLE_NODE = "singleNode";
    public static final String MODE_CLUSTER = "cluster";

    private String mode = MODE_SINGLE_NODE; 
    private String storageType = TYPE_MEMORY; 

    private int version = 0;

    private String allowCIDR; 
    private String denyCIDR;
    private boolean enableSkipLocalIp = true; 
    private Integer maxForwardedIPs = 2;
    private String allowHead; 
    private String denyHead;
    private List<String> skipIpControlUrl;  
    private List<String> needIpControlUrl;

    private boolean enableIpBasedLimiter = true;
    private int limitCapacityForIp = 3; 
    private int durationOfLimitForIp = 2; 
    private List<String> extraLimit; 
    transient List<Limit<String>> extraLimitList; 
    transient Map<String, List<String>> extraLimitParam;

    private boolean enableUserBasedLimiter = false;  
    private int limitCapacityForUser = 2; 
    private int durationOfLimitForUser = 1; 

    private List<String> skipFrequencyLimiterUrl;  
    private List<String> needFrequencyLimiterUrl;

    private boolean enableConcurrentRequestLimiter = false;
    private int maxConcurrentRequests = 1024; 
    private int requestLimitQueueSize = 128; 
    private int maxConcurrentPerIp = 2; 
    private List<String> skipConcurrentLimiterUrl;  
    private List<String> needConcurrentLimiterUrl;

    private boolean enableDownloadRateLimiter = false; 
    private int limitRateBytes = 100*1024; 
    private int limitRatePeriod = 1; 
    private List<String> skipRateLimiterUrl;  
    private List<String> needRateLimiterUrl;  

    final transient Map<String, Class<? extends LimitUsageStorage>> storageProvider = new HashMap<>();
    private transient Timer delayedTask;

    public LimiterConfig(){
        storageProvider.put(TYPE_MEMORY, InMemoryStorage.class);
        storageProvider.put(TYPE_REDIS, RedisStorage.class);
        initExtraLimitCache();
    }

    public int getVersion() {
        return version;
    }

    public Map<String, Class<? extends LimitUsageStorage>> getStorageProvider() {
        return storageProvider;
    }

    public String getAllowCIDR() {
        return allowCIDR;
    }

    public LimiterConfig setAllowCIDR(String allowCIDR) {
        this.allowCIDR = allowCIDR;
        configChanged();
        return this;
    }

    public String getDenyCIDR() {
        return denyCIDR;
    }

    public LimiterConfig setDenyCIDR(String denyCIDR) {
        this.denyCIDR = denyCIDR;
        configChanged();
        return this;
    }

    public boolean isEnableSkipLocalIp() {
        return enableSkipLocalIp;
    }

    public LimiterConfig setEnableSkipLocalIp(boolean enableSkipLocalIp) {
        this.enableSkipLocalIp = enableSkipLocalIp;
        configChanged();
        return this;
    }

    public LimiterConfig setMaxForwardedIPs(Integer maxIPs) {
        if (maxIPs < 1) throw new Error(StrUtil.format("maxIPs must be greater than 1, maxIPs=%s", maxIPs));
        this.maxForwardedIPs = maxIPs;
        configChanged();
        return this;
    }

    public Integer getMaxForwardedIPs(){
        return maxForwardedIPs;
    }

    public String getAllowHead() {
        return allowHead;
    }

    public LimiterConfig setAllowHead(String allowHead) {
        this.allowHead = allowHead;
        return this;
    }

    public String getDenyHead() {
        return denyHead;
    }

    public LimiterConfig setDenyHead(String denyHead) {
        this.denyHead = denyHead;
        return this;
    }

    public List<String> getSkipIpControlUrl() {
        return skipIpControlUrl;
    }

    public LimiterConfig setSkipIpControlUrl(List<String> skipIpControlUrl) {
        this.skipIpControlUrl = skipIpControlUrl;
        configChanged();
        return this;
    }

    public LimiterConfig addSkipIpControlUrl(String url) {
        if(skipIpControlUrl == null) skipIpControlUrl = new ArrayList<>();
        this.skipIpControlUrl.add(url);
        configChanged();
        return this;
    }

    public List<String> getNeedIpControlUrl() {
        return needIpControlUrl;
    }

    public LimiterConfig setNeedIpControlUrl(List<String> needIpControlUrl) {
        this.needIpControlUrl = needIpControlUrl;
        configChanged();
        return this;
    }

    public LimiterConfig addNeedIpControlUrl(String url) {
        if(needIpControlUrl == null) needIpControlUrl = new ArrayList<>();
        this.needIpControlUrl.add(url);
        configChanged();
        return this;
    }

    public String getMode() {
        return mode;
    }

    public LimiterConfig setMode(String mode) {
        this.mode = mode;
        return this;
    }

    public String getStorageType() {
        return storageType;
    }

    public LimiterConfig setStorageType(String storageType) {
        this.storageType = storageType;
        return this;
    }

    public List<String> getSkipFrequencyLimiterUrl() {
        return skipFrequencyLimiterUrl;
    }

    public LimiterConfig setSkipFrequencyLimiterUrl(List<String> skipFrequencyLimiterUrl) {
        this.skipFrequencyLimiterUrl = skipFrequencyLimiterUrl;
        configChanged();
        return this;
    }

    public LimiterConfig addSkipFrequencyLimiterUrl(String url) {
        if(skipFrequencyLimiterUrl == null) skipFrequencyLimiterUrl = new ArrayList<>();
        this.skipFrequencyLimiterUrl.add(url);
        configChanged();
        return this;
    }

    public List<String> getNeedFrequencyLimiterUrl() {
        return needFrequencyLimiterUrl;
    }

    public LimiterConfig setNeedFrequencyLimiterUrl(List<String> needFrequencyLimiterUrl) {
        this.needFrequencyLimiterUrl = needFrequencyLimiterUrl;
        configChanged();
        return this;
    }

    public LimiterConfig addNeedFrequencyLimiterUrl(String url) {
        if(needFrequencyLimiterUrl == null) needFrequencyLimiterUrl = new ArrayList<>();
        this.needFrequencyLimiterUrl.add(url);
        configChanged();
        return this;
    }

    public List<String> getSkipConcurrentLimiterUrl() {
        return skipConcurrentLimiterUrl;
    }

    public LimiterConfig setSkipConcurrentLimiterUrl(List<String> skipConcurrentLimiterUrl) {
        this.skipConcurrentLimiterUrl = skipConcurrentLimiterUrl;
        configChanged();
        return this;
    }

    public LimiterConfig addSkipConcurrentLimiterUrl(String url) {
        if(skipConcurrentLimiterUrl == null) skipConcurrentLimiterUrl = new ArrayList<>();
        this.skipConcurrentLimiterUrl.add(url);
        configChanged();
        return this;
    }

    public List<String> getNeedConcurrentLimiterUrl() {
        return needConcurrentLimiterUrl;
    }

    public LimiterConfig setNeedConcurrentLimiterUrl(List<String> needConcurrentLimiterUrl) {
        this.needConcurrentLimiterUrl = needConcurrentLimiterUrl;
        configChanged();
        return this;
    }

    public LimiterConfig addNeedConcurrentLimiterUrl(String url) {
        if(needConcurrentLimiterUrl == null) needConcurrentLimiterUrl = new ArrayList<>();
        this.needConcurrentLimiterUrl.add(url);
        configChanged();
        return this;
    }

    public List<String> getSkipRateLimiterUrl() {
        return skipRateLimiterUrl;
    }

    public LimiterConfig setSkipRateLimiterUrl(List<String> skipRateLimiterUrl) {
        this.skipRateLimiterUrl = skipRateLimiterUrl;
        configChanged();
        return this;
    }

    public LimiterConfig addSkipRateLimiterUrl(String url) {
        if(skipRateLimiterUrl == null) skipRateLimiterUrl = new ArrayList<>();
        this.skipRateLimiterUrl.add(url);
        configChanged();
        return this;
    }

    public List<String> getNeedRateLimiterUrl() {
        return needRateLimiterUrl;
    }

    public LimiterConfig setNeedRateLimiterUrl(List<String> needRateLimiterUrl) {
        this.needRateLimiterUrl = needRateLimiterUrl;
        configChanged();
        return this;
    }

    public LimiterConfig addNeedRateLimiterUrl(String url) {
        if(needRateLimiterUrl == null) needRateLimiterUrl = new ArrayList<>();
        this.needRateLimiterUrl.add(url);
        configChanged();
        return this;
    }

    public boolean isEnableIpBasedLimiter() {
        return enableIpBasedLimiter;
    }

    public LimiterConfig setEnableIpBasedLimiter(boolean enableIpBasedLimiter) {
        this.enableIpBasedLimiter = enableIpBasedLimiter;
        configChanged();
        return this;
    }

    public int getLimitCapacityForIp() {
        return limitCapacityForIp;
    }

    public LimiterConfig setLimitCapacityForIp(int limitCapacityForIp) {
        this.limitCapacityForIp = limitCapacityForIp;
        configChanged();
        return this;
    }

    public int getDurationOfLimitForIp() {
        return durationOfLimitForIp;
    }

    public LimiterConfig setDurationOfLimitForIp(int durationOfLimitForIp) {
        this.durationOfLimitForIp = durationOfLimitForIp;
        configChanged();
        return this;
    }

    public List<String> getExtraLimit() {
        return extraLimit;
    }

    public LimiterConfig setExtraLimit(List<String> extraLimit) {
        this.extraLimit = extraLimit;
        initExtraLimitCache();
        configChanged();
        return this;
    }

    public LimiterConfig addExtraLimit(String line) {
        if(extraLimit == null) extraLimit = new ArrayList<>();
        this.extraLimit.add(line);
        initExtraLimitCache();
        configChanged();
        return this;
    }

    private synchronized void initExtraLimitCache() {
        if(this.extraLimit != null && this.extraLimit.size() > 0) {
            List<Limit<String>> limitList = new ArrayList<>();
            Map<String, List<String>> limitParam = new HashMap<>();
            for(String line : extraLimit){
                String[] param = StrUtil.split(line, ",");
                String[] head = StrUtil.split(param[0],":");
                if(param.length != 3 || head.length < 2 || !StrUtil.isNumbers(param[1]+param[2])){
                    logger.warn("Skip wrong ExtraLimit config string: " + line);
                    continue;
                }
                limitParam.put(head[0], new ArrayList<>(Arrays.asList(head).subList(1, head.length)));
                limitList.add(LimitBuilder.of(head[0]).to(Integer.parseInt(param[1])).per(Duration.ofSeconds(Integer.parseInt(param[2]))).build());
            }
            extraLimitParam = limitParam;
            extraLimitList = limitList;
        } else { 
            extraLimitParam = null;
            extraLimitList = null;
        }
    }

    public boolean isEnableUserBasedLimiter() {
        return enableUserBasedLimiter;
    }

    public LimiterConfig setEnableUserBasedLimiter(boolean enableUserBasedLimiter) {
        this.enableUserBasedLimiter = enableUserBasedLimiter;
        configChanged();
        return this;
    }

    public int getLimitCapacityForUser() {
        return limitCapacityForUser;
    }

    public LimiterConfig setLimitCapacityForUser(int limitCapacityForUser) {
        this.limitCapacityForUser = limitCapacityForUser;
        configChanged();
        return this;
    }

    public int getDurationOfLimitForUser() {
        return durationOfLimitForUser;
    }

    public LimiterConfig setDurationOfLimitForUser(int durationOfLimitForUser) {
        this.durationOfLimitForUser = durationOfLimitForUser;
        configChanged();
        return this;
    }

    public boolean isEnableConcurrentRequestLimiter() {
        return enableConcurrentRequestLimiter;
    }

    public LimiterConfig setEnableConcurrentRequestLimiter(boolean enableConcurrentRequestLimiter) {
        this.enableConcurrentRequestLimiter = enableConcurrentRequestLimiter;
        configChanged();
        return this;
    }

    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    public LimiterConfig setMaxConcurrentRequests(int maxConcurrentRequests) {
        this.maxConcurrentRequests = maxConcurrentRequests;
        configChanged();
        return this;
    }

    public int getRequestLimitQueueSize() {
        return requestLimitQueueSize;
    }

    public LimiterConfig setRequestLimitQueueSize(int requestLimitQueueSize) {
        this.requestLimitQueueSize = requestLimitQueueSize;
        configChanged();
        return this;
    }

    public int getMaxConcurrentPerIp() {
        return maxConcurrentPerIp;
    }

    public LimiterConfig setMaxConcurrentPerIp(int maxConcurrentPerIp) {
        this.maxConcurrentPerIp = maxConcurrentPerIp;
        configChanged();
        return this;
    }

    public boolean isEnableDownloadRateLimiter() {
        return enableDownloadRateLimiter;
    }

    public LimiterConfig setEnableDownloadRateLimiter(boolean enableDownloadRateLimiter) {
        this.enableDownloadRateLimiter = enableDownloadRateLimiter;
        configChanged();
        return this;
    }

    public int getLimitRateBytes() {
        return limitRateBytes;
    }

    public LimiterConfig setLimitRateBytes(int limitRateBytes) {
        this.limitRateBytes = limitRateBytes;
        configChanged();
        return this;
    }

    public int getLimitRatePeriod() {
        return limitRatePeriod;
    }

    public LimiterConfig setLimitRatePeriod(int limitRatePeriod) {
        this.limitRatePeriod = limitRatePeriod;
        configChanged();
        return this;
    }

    @Override
    public void validate() {

    }

    private void configChanged(){
        configChanged(false);
    }

    public static final String externalType = "external";
    private void configChanged(boolean external) {
        if(!external) version = version + 1;
        if(delayedTask != null) {
            delayedTask.cancel();
            delayedTask.purge();
        }
        delayedTask = new Timer();
        delayedTask.schedule(new TimerTask() {
            @Override
            public void run() {
                
                if(external) {
                    Ready.post(new GeneralEvent(Event.LIMITER_CONFIG_CHANGED, LimiterConfig.this, LimiterConfig.this)
                            .setContextReference(LimiterConfig.this).put(externalType, true));
                } else {
                    Ready.post(new GeneralEvent(Event.LIMITER_CONFIG_CHANGED, LimiterConfig.this, LimiterConfig.this)
                            .setContextReference(LimiterConfig.this));
                }
            }
        }, 100);
    }

    public String toMessage(){
        return toMessage(false);
    }
    public String toMessage(boolean force){
        StringBuilder sb = new StringBuilder();
        sb.append(force ? Integer.MAX_VALUE : version);  
        sb.append("`");
        sb.append(allowCIDR != null ? allowCIDR : "");
        sb.append("`");
        sb.append(denyCIDR != null ? denyCIDR : "");
        sb.append("`");
        sb.append(enableSkipLocalIp);
        sb.append("`");
        sb.append(maxForwardedIPs != null ? maxForwardedIPs : "");
        sb.append("`");
        sb.append(allowHead != null ? allowHead : "");
        sb.append("`");
        sb.append(denyHead != null ? denyHead : "");
        sb.append("`");
        sb.append(StrUtil.join(skipIpControlUrl, "|"));
        sb.append("`");
        sb.append(StrUtil.join(needIpControlUrl, "|"));
        sb.append("`");
        sb.append(enableIpBasedLimiter);
        sb.append("`");
        sb.append(limitCapacityForIp);
        sb.append("`");
        sb.append(durationOfLimitForIp);
        sb.append("`");
        sb.append(StrUtil.join(extraLimit, "|!"));
        sb.append("`");
        sb.append(enableUserBasedLimiter);
        sb.append("`");
        sb.append(limitCapacityForUser);
        sb.append("`");
        sb.append(durationOfLimitForUser);
        sb.append("`");
        sb.append(StrUtil.join(skipFrequencyLimiterUrl, "|"));
        sb.append("`");
        sb.append(StrUtil.join(needFrequencyLimiterUrl, "|"));
        sb.append("`");
        sb.append(enableConcurrentRequestLimiter);
        sb.append("`");
        sb.append(maxConcurrentRequests);
        sb.append("`");
        sb.append(requestLimitQueueSize);
        sb.append("`");
        sb.append(maxConcurrentPerIp);
        sb.append("`");
        sb.append(enableDownloadRateLimiter);
        sb.append("`");
        sb.append(limitRateBytes);
        sb.append("`");
        sb.append(limitRatePeriod);
        return sb.toString();
    }

    public static LimiterConfig fromMessage(String str, LimiterConfig config){
        if(config == null) throw new RuntimeException("config instance is null");
        String[] fields = StrUtil.split(str,'`');
        if(fields.length != 25) throw new RuntimeException("Bad LimiterConfig String: " + str);
        int ver = Integer.parseInt(fields[0]);
        config.version = ver == Integer.MAX_VALUE ? 0 : ver;  
        config.allowCIDR = StrUtil.isBlank(fields[1]) ? null : fields[1];
        config.denyCIDR = StrUtil.isBlank(fields[2]) ? null : fields[2];
        config.enableSkipLocalIp = Boolean.parseBoolean(fields[3]);
        config.maxForwardedIPs = StrUtil.isBlank(fields[4]) ? null : Integer.parseInt(fields[4]);
        config.allowHead = StrUtil.isBlank(fields[5]) ? null : fields[5];
        config.denyHead = StrUtil.isBlank(fields[6]) ? null : fields[6];
        if(StrUtil.isBlank(fields[7])) {
            config.skipIpControlUrl = null;
        } else {
            String[] url = StrUtil.split(fields[7],'|');
            config.skipIpControlUrl = new ArrayList<>(Arrays.asList(url));
        }
        if(StrUtil.isBlank(fields[8])) {
            config.needIpControlUrl = null;
        } else {
            String[] url = StrUtil.split(fields[8],'|');
            config.needIpControlUrl = new ArrayList<>(Arrays.asList(url));
        }
        config.enableIpBasedLimiter = Boolean.parseBoolean(fields[9]);
        config.limitCapacityForIp = Integer.parseInt(fields[10]);
        config.durationOfLimitForIp = Integer.parseInt(fields[11]);
        if(StrUtil.isBlank(fields[12])){
            config.extraLimit = null;
        } else {
            String[] limit = StrUtil.split(fields[12],"|!");
            config.extraLimit = new ArrayList<>(Arrays.asList(limit));
        }
        config.initExtraLimitCache(); 
        config.enableUserBasedLimiter = Boolean.parseBoolean(fields[13]);
        config.limitCapacityForUser = Integer.parseInt(fields[14]);
        config.durationOfLimitForUser = Integer.parseInt(fields[15]);
        if(StrUtil.isBlank(fields[16])){
            config.skipFrequencyLimiterUrl = null;
        } else {
            String[] url = StrUtil.split(fields[16],'|');
            config.skipFrequencyLimiterUrl = new ArrayList<>(Arrays.asList(url));
        }
        if(StrUtil.isBlank(fields[17])){
            config.needFrequencyLimiterUrl = null;
        } else {
            String[] url = StrUtil.split(fields[17],'|');
            config.needFrequencyLimiterUrl = new ArrayList<>(Arrays.asList(url));
        }
        config.enableConcurrentRequestLimiter = Boolean.parseBoolean(fields[18]);
        config.maxConcurrentRequests = Integer.parseInt(fields[19]);
        config.requestLimitQueueSize = Integer.parseInt(fields[20]);
        config.maxConcurrentPerIp = Integer.parseInt(fields[21]);
        config.enableDownloadRateLimiter = Boolean.parseBoolean(fields[22]);
        config.limitRateBytes = Integer.parseInt(fields[23]);
        config.limitRatePeriod = Integer.parseInt(fields[24]);
        config.configChanged(true);
        return config;
    }
}
