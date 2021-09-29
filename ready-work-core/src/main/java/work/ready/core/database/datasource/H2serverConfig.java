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

package work.ready.core.database.datasource;

import work.ready.core.config.BaseConfig;

public class H2serverConfig extends BaseConfig {

    private boolean enabled = false;
    private Integer tcpPort;
    private Integer webPort;
    private boolean tcpAllowOthers;
    private boolean webAllowOthers;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(Integer tcpPort) {
        this.tcpPort = tcpPort;
    }

    public Integer getWebPort() {
        return webPort;
    }

    public void setWebPort(Integer webPort) {
        this.webPort = webPort;
    }

    public boolean isTcpAllowOthers() {
        return tcpAllowOthers;
    }

    public void setTcpAllowOthers(boolean tcpAllowOthers) {
        this.tcpAllowOthers = tcpAllowOthers;
    }

    public boolean isWebAllowOthers() {
        return webAllowOthers;
    }

    public void setWebAllowOthers(boolean webAllowOthers) {
        this.webAllowOthers = webAllowOthers;
    }

    @Override
    public void validate() { 

    }
}
