/**
 *
 * Original work Copyright core-ng
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
package work.ready.core.module;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.log.LogManager;
import work.ready.core.server.Ready;
import work.ready.core.tools.NetUtil;
import work.ready.core.tools.StopWatch;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ShutdownHook implements Runnable {
    private static final Log logger = LogFactory.getLog(ShutdownHook.class);
    private static final String SHUTDOWN_TIMEOUT_PROPERTY = "ready.shutdown_timeout";

    public static final int STAGE_0 = 0;    
    public static final int STAGE_1 = 1;    
    public static final int STAGE_2 = 2;    
    public static final int STAGE_3 = 3;    
    public static final int STAGE_4 = 4;    
    public static final int STAGE_5 = 5;    
    public static final int STAGE_6 = 6;    
    public static final int STAGE_7 = 7;    
    public static final int STAGE_8 = 8;    
    public static final int STAGE_9 = 9;    

    final Thread thread = new Thread(this, "shutdown");
    private final Application application;
    private final long shutdownTimeoutInMs;
    private final ShutdownStage[] stages = new ShutdownStage[STAGE_9 + 1];

    public ShutdownHook() {
        this.application = null;
        this.shutdownTimeoutInMs = shutdownTimeoutInSeconds();
        Runtime.getRuntime().addShutdownHook(thread);
    }

    ShutdownHook(ShutdownHook parent, Application application) {
        this.application = application;
        this.shutdownTimeoutInMs = shutdownTimeoutInSeconds();
        parent.insert(STAGE_1, timeoutInMs -> this.run());
    }

    long shutdownTimeoutInSeconds() {
        String shutdownTimeout = Ready.getProperty(SHUTDOWN_TIMEOUT_PROPERTY);
        if (shutdownTimeout != null) {
            Duration timeout = Duration.ofSeconds(Long.parseLong(shutdownTimeout));
            if (timeout.isZero() || timeout.isNegative()) throw new Error("shutdown timeout must be greater than 0, timeout=" + shutdownTimeout);
            return timeout.toMillis();
        }
        return Duration.ofSeconds(20).toMillis(); 
    }

    private void insert(int stage, Shutdown shutdown) {
        if (stages[stage] == null) stages[stage] = new ShutdownStage();
        stages[stage].shutdowns.add(0, shutdown);
    }

    public void add(int stage, Shutdown shutdown) {
        if (stages[stage] == null) stages[stage] = new ShutdownStage();
        stages[stage].shutdowns.add(shutdown);
    }

    @Override
    public void run() {
        var watch = new StopWatch();
        logger.info("=== " + (application == null ? "server " : application.getName() + " ") + "shutdown begin ===");
        currentHost();

        long endTime = Ready.currentTimeMillis() + shutdownTimeoutInMs;

        shutdown(endTime, STAGE_0, STAGE_5);
        logger.info("=== " + (application == null ? "all applications " : application.getName() + " main processes ") + "shutdown end ==="); 

        shutdown(endTime, STAGE_6, STAGE_9);
        logger.info("=== " + (application == null ? "server " : application.getName() + " ") + "shutdown completed, elapsed=%ss ===", watch.elapsedSeconds());

        if(application == null) LogManager.resetFinally();
    }

    void currentHost() {
        logger.info((application == null ? "server" : application.getName()) + " is going to stop");
        if(application == null) {
            InetAddress inetAddress = NetUtil.getLocalAddress();
            logger.info("host %s", inetAddress.getHostAddress());
            logger.info("start time %s", Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime()));
            logger.info("uptime in ms %s", ManagementFactory.getRuntimeMXBean().getUptime());
        }
    }

    private void shutdown(long endTime, int fromStage, int toStage) {
        for (int i = fromStage; i <= toStage; i++) {
            ShutdownStage stage = stages[i];
            if (stage == null) continue;
            logger.info((application == null ? "server" : application.getName()) + " shutdown stage: %s", i);
            
            if(Ready.shutdownHook.equals(this) && ShutdownHook.STAGE_7 == i) {
                Collections.reverse(stage.shutdowns);
            }
            for (Shutdown shutdown : stage.shutdowns) {
                try {
                    shutdown.execute(endTime - Ready.currentTimeMillis());
                } catch (Throwable e) {
                    logger.warn(e,"Failed to stop: failed to shutdown, method=%s", shutdown);
                }
            }
        }
    }

    public interface Shutdown {
        void execute(long timeoutInMs) throws Exception;
    }

    static class ShutdownStage {
        final List<Shutdown> shutdowns = new ArrayList<>();
    }
}
