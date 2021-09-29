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

package work.ready.cloud.registry.support;

import work.ready.cloud.ReadyCloud;
import work.ready.core.handler.RequestMethod;
import work.ready.core.handler.route.RouteConfig;
import work.ready.core.module.AppModule;
import work.ready.core.server.Constant;
import work.ready.core.server.Ready;

public class RegistrySupportModule extends AppModule {

    public static final String name = "RegistrySupportModule";
    public static final String version = "1.0.1.210501";
    public static final String apiVersion = Constant.READY_API_VERSION;;

    @Override
    protected void initialize() {
        ensureDependsAvailable();
        route().add(new RouteConfig().addRoute( "/_sys/" + apiVersion + "/registry", SupportController.class, "list", new RequestMethod[]{RequestMethod.GET}));
    }

    private void ensureDependsAvailable(){
        if(!ReadyCloud.isReady()){
            throw new RuntimeException("RegistrySupportModule depends on Registry, please start server with cloud mode.");
        }
    }
}
