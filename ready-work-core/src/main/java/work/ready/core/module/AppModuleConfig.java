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

import work.ready.core.config.BaseConfig;

import java.util.HashMap;
import java.util.Map;

public class AppModuleConfig extends BaseConfig {

    private boolean enableSystemModule = false;

    private Map<String, Map<String, Object>> moduleConfig = new HashMap<>();

    public boolean isEnableSystemModule() {
        return enableSystemModule;
    }

    public void setEnableSystemModule(boolean enableSystemModule) {
        this.enableSystemModule = enableSystemModule;
    }

    public Map<String, Map<String, Object>> getModuleConfig(){
        return moduleConfig;
    }

    public Map<String, Object> getModuleConfig(String module){
        return moduleConfig.get(module);
    }

    @Override
    public void validate() {

    }
}
