/**
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package work.ready.core.module;

import work.ready.core.component.plugin.BaseCorePlugin;
import work.ready.core.server.Ready;

import java.util.Map;

public class Configurer {

    protected Map<String, BaseCorePlugin> pluginMap;

    public Configurer(Map<String, BaseCorePlugin> pluginMap) {
        this.pluginMap = pluginMap;
    }

    public void config() {
        
        Ready.getMainApp().globalConfig(Ready.getBootstrapConfig());
        Ready.config().resetApplicationConfig(); 
        
        Ready.getApp(appList->appList.forEach(app->app.appConfig(Ready.getApplicationConfig(app.getName()))));
        
        pluginMap.forEach((name,plugin) -> plugin.config());
    }

}
