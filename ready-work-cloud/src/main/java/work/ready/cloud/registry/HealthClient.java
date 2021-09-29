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
package work.ready.cloud.registry;

import io.undertow.util.Headers;
import work.ready.cloud.ReadyCloud;
import work.ready.cloud.client.CloudClient;
import work.ready.cloud.registry.base.URL;
import work.ready.cloud.registry.base.URLParam;
import work.ready.core.handler.RequestMethod;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Constant;
import work.ready.core.tools.StrUtil;
import work.ready.core.tools.define.SyncWriteMap;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static work.ready.cloud.registry.HeartbeatManager.*;

public class HealthClient {
	private static final Log logger = LogFactory.getLog(HealthClient.class);
	public static final String HEALTH_CLIENT_HEADER = "ReadyHealthClient";
	private static RegistryConfig config;

	private CloudClient client = CloudClient.getInstance(true);

	private HealthConnection healthConnection = new HealthConnection();

	public HealthClient() {
		config = ReadyCloud.getConfig().getRegistry();
	}

	public int haveAlook(URL service, String token) {
		try {
			String healthCheckPath = service.getParameter(URLParam.healthCheckPath.getName());
			if(StrUtil.isBlank(healthCheckPath)) {
				logger.warn("healthCheck is ON, but healthCheckPath is null, service: " + service.getIdentity());
				return STATUS_HEALTH;
			}
			URI uri = new URI(service.getProtocol() + "://" + service.getServerPortStr() + healthCheckPath);
			if(healthConnection.getTestFailureCounter(uri).get() > 0){	
				return takeAtest(service, token);
			}
			HttpResponse<String> response = healthConnection.send(RequestMethod.GET, uri, token, null);
			int statusCode = response.statusCode();
			if(statusCode < 200 || statusCode >= 207 || !Constant.DEFAULT_HEALTH_RESPONSE.equals(response.body())){
				healthConnection.healthFailureCounterIncrease(uri);
				logger.error("Failed to check on server %s : %s : %s", uri, statusCode, response.body());
				if(healthConnection.getHealthFailureCounter(uri).get() >= FAILURE_THRESHOLD){
					return STATUS_BROKE;
				}
				return STATUS_UNHEALTH;
			}
			healthConnection.healthFailureCounterDecrease(uri);
			return STATUS_HEALTH;
		} catch (Exception e) {
			logger.error(e,"Check request exception");
			return STATUS_UNHEALTH;
		}
	}

	public int takeAtest(URL service, String token) {
		try {
			String healthCheckPath = service.getParameter(URLParam.healthCheckPath.getName());
			String crucialCheckPath = service.getParameter(URLParam.crucialCheckPath.getName());
			if(StrUtil.isBlank(crucialCheckPath)){
				crucialCheckPath = healthCheckPath;
			}
			if(StrUtil.isBlank(crucialCheckPath)) {
				logger.warn("healthCheck is ON, but both healthCheckPath and crucialCheckPath are null, service: " + service.getIdentity());
				return STATUS_HEALTH;
			}
			boolean noFunctionalTest = crucialCheckPath.equals(healthCheckPath);
			if(noFunctionalTest) return haveAlook(service, token);  
			URI uri = new URI(service.getProtocol() + "://" + service.getServerPortStr() + crucialCheckPath);
			HttpResponse<String> response = healthConnection.send(RequestMethod.GET, uri, token, null);
			int statusCode = response.statusCode();
			if(statusCode < 200 || statusCode >= 207 || !Constant.DEFAULT_HEALTH_RESPONSE.equals(response.body())){
				healthConnection.testFailureCounterIncrease(uri);
				logger.error("Failed to test on server %s : %s : %s", uri, statusCode, response.body());
				if(healthConnection.getTestFailureCounter(uri).get() >= DISABILITY_THRESHOLD){
					return STATUS_DISABILITY;
				}
				return STATUS_UNHEALTH;
			}
			healthConnection.testFailureCounterDecrease(uri);
			return STATUS_HEALTH;
		} catch (Exception e) {
			logger.error(e,"Test request exception");
			return STATUS_UNHEALTH;
		}
	}

	private class HealthConnection {
		private Map<URI, AtomicInteger> requestCounter;
		private Map<URI, AtomicInteger> healthFailureCounter;
		private Map<URI, AtomicInteger> testFailureCounter;

		public HealthConnection() {
			requestCounter = new SyncWriteMap<>();
			healthFailureCounter = new SyncWriteMap<>();
			testFailureCounter = new SyncWriteMap<>();
		}

		public AtomicInteger getHealthFailureCounter(URI uri) {
			return healthFailureCounter.computeIfAbsent(uri, k->new AtomicInteger(0));
		}

		public void healthFailureCounterIncrease(URI uri){
			getHealthFailureCounter(uri).getAndIncrement();
		}

		public void healthFailureCounterDecrease(URI uri){
			int counter = getHealthFailureCounter(uri).get();
			if(counter > EMERGENCY_COUNTDOWN) {
				getHealthFailureCounter(uri).set(EMERGENCY_COUNTDOWN - 1);
			} else if(counter > 0) {
				getHealthFailureCounter(uri).decrementAndGet();
			}
		}

		public AtomicInteger getTestFailureCounter(URI uri) {
			return testFailureCounter.computeIfAbsent(uri, k->new AtomicInteger(0));
		}

		public void testFailureCounterIncrease(URI uri) {
			getTestFailureCounter(uri).getAndIncrement();
		}

		public void testFailureCounterDecrease(URI uri){
			int counter = getTestFailureCounter(uri).get();
			if(counter > EMERGENCY_COUNTDOWN) {
				getTestFailureCounter(uri).set(EMERGENCY_COUNTDOWN - 1);
			} else if(counter > 0) {
				getTestFailureCounter(uri).decrementAndGet();
			}
		}

		public AtomicInteger getRequestCounter(URI uri) {
			return requestCounter.computeIfAbsent(uri, k->new AtomicInteger(0));
		}

		HttpResponse<String> send(RequestMethod method, URI uri, String token, String json) throws IOException, InterruptedException {
			var builder = HttpRequest.newBuilder(uri).timeout(Duration.ofMillis(config.getConnectTimeout()));
			builder.header(Headers.CONTENT_TYPE_STRING, "application/json");
			builder.header(Headers.USER_AGENT_STRING, HEALTH_CLIENT_HEADER);
			if (token != null) {
				builder.header(Constant.HEALTH_TOKEN_STRING, token);
			}
			if(json != null) {
				builder.header(Headers.TRANSFER_ENCODING_STRING, "chunked");
			}
			
			if(RequestMethod.POST.equals(method)) {
				if(config.isHttpCheckLog() && logger.isTraceEnabled())
				logger.trace("The request sent to application: %s, POST, request token: %s, request body is %s", uri.toString(), token, json);
				builder.method(method.name(), HttpRequest.BodyPublishers.ofString(json == null ? "" : json));
			} else {	
				if(config.isHttpCheckLog() && logger.isTraceEnabled())
				logger.trace("The request sent to application: %s, GET, request token: %s, request body is empty", uri.toString(), token);
				builder.method(method.name(), HttpRequest.BodyPublishers.noBody());
			}
			HttpResponse<String> response = client.send(builder);

			requestCounter.computeIfAbsent(uri, k->new AtomicInteger(0)).getAndIncrement();
			if(config.isHttpCheckLog() && logger.isTraceEnabled())
			logger.trace("The response got from application: %s = %s", uri.toString(), response.body());
			return response;
		}
	}

}
