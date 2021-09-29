/**
 *
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http:www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package work.ready.cloud.registry;

public class RegistryConfig {
    private int retryPeriod;
    private int registrySessionTimeout;

    private int connectTimeout = 3000;
    private String token = "simpleToken";
    private int failureThreshold = 3;
    private int checkInterval = 10; 
    private boolean tcpCheck = false;
    private boolean tcpCheckLog = false;
    private boolean httpCheck = true;
    private boolean httpCheckLog = true;

    public int getRetryPeriod() {
        return retryPeriod;
    }

    public void setRetryPeriod(int retryPeriod) {
        this.retryPeriod = retryPeriod;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getRegistrySessionTimeout() {
        return registrySessionTimeout;
    }

    public void setRegistrySessionTimeout(int registrySessionTimeout) {
        this.registrySessionTimeout = registrySessionTimeout;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public int getCheckInterval() {
        return checkInterval;
    }

    public void setCheckInterval(int checkInterval) {
        this.checkInterval = checkInterval;
    }

    public boolean isTcpCheck() {
        return tcpCheck;
    }

    public void setTcpCheck(boolean tcpCheck) {
        this.tcpCheck = tcpCheck;
    }

    public boolean isHttpCheck() {
        return httpCheck;
    }

    public void setHttpCheck(boolean httpCheck) {
        this.httpCheck = httpCheck;
    }

    public boolean isHttpCheckLog() {
        return httpCheckLog;
    }

    public void setHttpCheckLog(boolean httpCheckLog) {
        this.httpCheckLog = httpCheckLog;
    }
}
