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

package work.ready.core.component.time;

import work.ready.core.component.cache.FileCache.FileCache;
import work.ready.core.component.serializer.KryoSerializer;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TimeWorker {

    private static final Log logger = LogFactory.getLog(TimeWorker.class);

    private static final TimeWorker instance = new TimeWorker();
    private static final SntpClient sntpClient = new SntpClient();
    private static FileCache fileCache;

    private static float _rootDelayMax = 100;
    private static float _rootDispersionMax = 100;
    private static int _serverResponseDelayMax = 750;
    private static int _udpSocketTimeoutInMillis = 15_000;

    private String defaultNtpHost = "cn.ntp.org.cn";
    private List<String> fallbackNtpHost;
    private TimeInfo lastCachedTimeInfo;

    private Boolean syncState;
    private boolean computeTimeFailure = false;
    private AtomicLong now;

    public TimeWorker(){
        defaultNtpHost = Ready.getBootstrapConfig().getMainNtpHost();
        fallbackNtpHost = Ready.getBootstrapConfig().getFallbackNtpHost();
        ((KryoSerializer)KryoSerializer.instance).register(TimeInfo.class);
        scheduleClockUpdating();
    }

    private void scheduleClockUpdating() {
        now = new AtomicLong(System.currentTimeMillis());
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "System Clock");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(() -> {
            if (syncState != null && !computeTimeFailure) {
                try {
                    now.set(TimeWorker.this.computeTimeMillis());
                } catch (Exception e) {
                    now.set(System.currentTimeMillis());
                    computeTimeFailure = true;
                    logger.error(e, "Compute time failure, use System.currentTimeMillis() instead: ");
                }
            } else {
                now.set(System.currentTimeMillis());
            }
        }, 1, 1, TimeUnit.MILLISECONDS);
    }

    public long currentTimeMillis(){
        return now.get();
    }

    public static boolean isInitialized() {
        return sntpClient.wasInitialized();
    }

    public static TimeWorker getInstance() {
        return instance;
    }

    public boolean syncTime() {
        syncState = syncTime(defaultNtpHost);
        computeTimeFailure = false;
        return syncState;
    }

    public synchronized TimeWorker withConnectionTimeout(int timeoutInMillis) {
        _udpSocketTimeoutInMillis = timeoutInMillis;
        return instance;
    }

    public synchronized TimeWorker withRootDelayMax(float rootDelayMax) {
        if (rootDelayMax > _rootDelayMax) {
            String log = String.format(Locale.getDefault(),
                    "The recommended max rootDelay value is %f. You are setting it at %f",
                    _rootDelayMax, rootDelayMax);
            logger.info(log);
        }

        _rootDelayMax = rootDelayMax;
        return instance;
    }

    public synchronized TimeWorker withRootDispersionMax(float rootDispersionMax) {
        if (rootDispersionMax > _rootDispersionMax) {
            String log = String.format(Locale.getDefault(),
                    "The recommended max rootDispersion value is %f. You are setting it at %f",
                    _rootDispersionMax, rootDispersionMax);
            logger.info(log);
        }

        _rootDispersionMax = rootDispersionMax;
        return instance;
    }

    public synchronized TimeWorker withServerResponseDelayMax(int serverResponseDelayInMillis) {
        _serverResponseDelayMax = serverResponseDelayInMillis;
        return instance;
    }

    public synchronized TimeWorker withNtpHost(String ntpHost) {
        defaultNtpHost = ntpHost;
        return instance;
    }

    protected synchronized boolean syncTime(String ntpHost) {
        try {
            requestTime(ntpHost);
        } catch (Exception e){
            if(fallbackNtpHost == null || fallbackNtpHost.size() == 0){
                logger.warn("sync time from ntp host " + ntpHost + " failed and there is no fallback ntp host available, try next time.");
                return false;
            }
            int n = new Random().nextInt(fallbackNtpHost.size());
            logger.warn("sync time from ntp host " + ntpHost + " failed, try to sync from fallback ntp host " + fallbackNtpHost.get(n));
            try {
                requestTime(fallbackNtpHost.get(n));
            } catch (Exception e2){
                logger.warn("sync time from fallback ntp host " + fallbackNtpHost.get(n) + " failed, give up, try next time.");
                return false;
            }
        }

        cacheTimeInfoToDisk();
        return true;
    }

    long[] requestTime(String ntpHost) throws Exception {
        return sntpClient.requestTime(ntpHost,
                _rootDelayMax,
                _rootDispersionMax,
                _serverResponseDelayMax,
                _udpSocketTimeoutInMillis);
    }

    private void cacheTimeInfoToDisk() {
        if (!sntpClient.wasInitialized()) {
            logger.warn("SNTP client is not available, cannot cache time info to disk.");
            return;
        }
        TimeInfo timeInfo = new TimeInfo();
        timeInfo.sntpTime = sntpClient.getCachedSntpTime();
        timeInfo.lastTicks = sntpClient.getCachedLastTicks();
        timeInfo.lastSyncTime = sntpClient.getCachedLastSyncTime();
        fileCache = FileCache.getInstance();
        fileCache.add("sntpCahce", "sntpCahce", timeInfo);
    }

    private long computeTimeMillis() {
        if(sntpClient.wasInitialized()) {  
            return sntpClient.getCachedSntpTime() + (TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) - sntpClient.getCachedLastTicks());
        } else {
            if(lastCachedTimeInfo == null) {
                fileCache = FileCache.getInstance();
                TimeInfo timeInfo = (TimeInfo) fileCache.get("sntpCahce", "sntpCahce");
                if(timeInfo != null && timeInfo.sntpTime > 0 && timeInfo.lastSyncTime > 0) {
                    logger.warn("SNTP client is not available, load last time info from disk.");

                    if(System.currentTimeMillis() - timeInfo.lastSyncTime > 86400000) {
                        logger.warn("The cached time info is old, it's better to call Ready.syncTime() once a day.");
                    }
                    timeInfo.sntpTime = timeInfo.sntpTime + (System.currentTimeMillis() - timeInfo.lastSyncTime);
                    timeInfo.lastTicks = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()); 
                    lastCachedTimeInfo = timeInfo;
                } else {
                    throw new RuntimeException("expected SNTP time from sync process to be cached, but couldn't find it. \nYou need to call Ready.syncTime() successfully at least once, it's better to call Ready.syncTime() once a day.");
                }
            }
            return lastCachedTimeInfo.sntpTime + (TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) - lastCachedTimeInfo.lastTicks);
        }
    }

    static class TimeInfo implements Serializable {
        long sntpTime;
        long lastTicks;
        long lastSyncTime;
    }

}
