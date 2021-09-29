/**
 *
 * Original work copyright (c) 2016 Network New Technologies Inc.
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
package work.ready.cloud.registry;

import work.ready.cloud.ReadyCloud;
import work.ready.cloud.client.clevercall.CircuitBreaker;
import work.ready.cloud.registry.base.URL;
import work.ready.cloud.registry.base.URLParam;
import work.ready.core.event.cloud.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.define.ConcurrentHashSet;

import java.util.concurrent.*;

public class HeartbeatManager {
	private static final Log logger = LogFactory.getLog(HeartbeatManager.class);
	private static RegistryConfig config;
	private HealthClient client;
	private String token;
	private HeartbeatManager instance;

	private static int INTERVAL = 10;
	
	static int FAILURE_THRESHOLD = 3;
	
	static int DISABILITY_THRESHOLD = 3;
	
	static int EMERGENCY_COUNTDOWN = 10;
	
	private static final int TEST_CHECK_RATE = 10;

	public static final int STATUS_HEALTH = 1;
	public static final int STATUS_UNHEALTH = 0;
	public static final int STATUS_DISABILITY = -1;
	public static final int STATUS_BROKE = -2;

	public static final String UNSTABLE_FAILURE = "FAILURE";
	public static final String UNSTABLE_UNHEALTH = "UNHEALTH";
	public static final String UNSTABLE_RECOVERY = "RECOVERY";

	private Registry registry;
	private ThreadPoolExecutor jobExecutor;
	private ScheduledExecutorService heartbeatExecutor;
	private volatile boolean heartBeatSwitcherStatus = false;
	private int healthCheckTimes = 0;

	public HeartbeatManager(Registry registry, HealthClient client, String token) {
		this.registry = registry;
		this.client = client;
		this.token = token;
		this.instance = this;
		config = ReadyCloud.getConfig().getRegistry();
		if(config.getCheckInterval() * 1000 > config.getConnectTimeout()) {
			INTERVAL = config.getCheckInterval();
		} else {
			INTERVAL = config.getConnectTimeout() / 1000 + 5;
		}
		FAILURE_THRESHOLD = config.getFailureThreshold();
		heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
		ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(
				1000);
		jobExecutor = new ThreadPoolExecutor(5, 30, 30 * 1000,
				TimeUnit.MILLISECONDS, workQueue);
	}

	public void start() {
		heartbeatExecutor.scheduleAtFixedRate(
				new Runnable() {
					@Override
					public void run() {
						try {
							boolean switcherStatus = isHeartbeatOpen();
							if (switcherStatus) {
								if(healthCheckTimes >= TEST_CHECK_RATE) {
									processHeartbeat(false);
									healthCheckTimes = 0;
								} else {
									processHeartbeat(true);
									healthCheckTimes++;
								}
							}
						} catch (Exception e) {
							logger.error(e,"heartbeat executor err:");
						}
					}
				}, INTERVAL,
				INTERVAL, TimeUnit.SECONDS);
	}

	protected void processHeartbeat(boolean isHealthCheck) {
		for (URL service : registry.getStabilityUrls()) {
			boolean healthCheck = service.getBooleanParameter(URLParam.healthCheck.getName(), URLParam.healthCheck.getBooleanValue());
			if(!healthCheck) continue;
			try {
				jobExecutor.execute(new HeartbeatJob(service, isHealthCheck));
			} catch (RejectedExecutionException ree) {
				logger.error(ree,"execute heartbeat job fail! job: "
						+ service + " is rejected");
			}
		}
	}

	public void close() {
		heartbeatExecutor.shutdown();
		jobExecutor.shutdown();
		logger.info("heartbeatManager closed.");
	}

	private boolean isHeartbeatOpen() {
		return heartBeatSwitcherStatus;
	}

	public void setHeartbeatOpen(boolean open) {
		heartBeatSwitcherStatus = open;
	}

	class HeartbeatJob implements Runnable {
		private URL service;
		private boolean isHealthCheck;

		public HeartbeatJob(URL service, boolean isHealthCheck) {
			super();
			this.service = service;
			this.isHealthCheck = isHealthCheck;
		}

		@Override
		public void run() {
			try {
				int result = STATUS_HEALTH;
				if (isHealthCheck) {
					result = client.haveAlook(service, token);
				} else {
					result = client.takeAtest(service, token);
				}
				if(result == STATUS_BROKE) {
					if (registry.getStabilityLevel(service) < CircuitBreaker.MAX_UNSTABLE_LEVEL) {
						logger.error("heartbeat check failed on service " + service + " for " + FAILURE_THRESHOLD + " times, notice Registry to temporarily remove this service from discovery list.");
						Ready.post(new GeneralEvent(Event.SERVICE_UNSTABLE, instance, service).put("STATUS", UNSTABLE_FAILURE));
					}
				} else if(result == STATUS_DISABILITY) {
					if(registry.getStabilityLevel(service) < CircuitBreaker.MAX_UNSTABLE_LEVEL) {
						logger.error("functional check failed on service " + service + " for " + DISABILITY_THRESHOLD + " times, notice Registry to temporarily remove this service from discovery list.");
						Ready.post(new GeneralEvent(Event.SERVICE_UNSTABLE, instance, service).put("STATUS", UNSTABLE_FAILURE));
					}
				} else if(result == STATUS_UNHEALTH) {
					if(registry.getStabilityLevel(service) < CircuitBreaker.MAX_UNSTABLE_LEVEL) {
						logger.error("heartbeat check failed on service " + service + ", notice Registry to degrade this service.");
						Ready.post(new GeneralEvent(Event.SERVICE_UNSTABLE, instance, service).put("STATUS", UNSTABLE_UNHEALTH));
					}
				} else {
					if(registry.getStabilityLevel(service) > CircuitBreaker.MIN_UNSTABLE_LEVEL) {
						logger.debug("heartbeat check succeed on degraded service " + service + ", notice Registry to upgrade this service.");
						Ready.post(new GeneralEvent(Event.SERVICE_UNSTABLE, instance, service).put("STATUS", UNSTABLE_RECOVERY));
					}
				}
			} catch (Exception e) {
				logger.error(e,"heartbeat check error!");
			}
		}
	}

	public void setClient(HealthClient client) {
		this.client = client;
	}

}
