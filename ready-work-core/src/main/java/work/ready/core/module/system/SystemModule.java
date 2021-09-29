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

package work.ready.core.module.system;

import work.ready.core.handler.RequestMethod;
import work.ready.core.handler.resource.PathResourceHandler;
import work.ready.core.handler.resource.StaticResourceHandler;
import work.ready.core.handler.resource.VirtualHostHandler;
import work.ready.core.handler.route.RouteConfig;
import work.ready.core.module.AppModule;
import work.ready.core.server.Constant;

public class SystemModule extends AppModule {

    public static final String name = "SystemModule";
    public static final String version = "1.0.1.200501";
    public static final String apiVersion = Constant.READY_API_VERSION;

    public SystemModule(){

    }

    @Override
    protected void initialize() {
        systemInformationSupport();
    }

    @Override
    protected void destroy(){

    }

    protected void systemInformationSupport(){
        handler().addHandler( "/_sys/" + apiVersion + "/info", RequestMethod.GET, new SystemInfoServerModule());
        route().add(new RouteConfig().addRoute("/_sys/" + apiVersion + "/properties", PropertyController.class, "index", new RequestMethod[]{RequestMethod.GET})
                .addRoute("/_sys/" + apiVersion + "/vm", DiagnosticController.class, "vm", new RequestMethod[]{RequestMethod.GET})
                .addRoute("/_sys/" + apiVersion + "/thread", DiagnosticController.class, "thread", new RequestMethod[]{RequestMethod.GET})
                .addRoute("/_sys/" + apiVersion + "/heap", DiagnosticController.class, "heap", new RequestMethod[]{RequestMethod.GET})
                .addRoute("/_sys/" + apiVersion + "/memory", DiagnosticController.class, "memory", new RequestMethod[]{RequestMethod.GET})
                .addRoute("/_sys/" + apiVersion + "/threadUsage", DiagnosticController.class, "threadUsage", new RequestMethod[]{RequestMethod.GET})
                .addRoute("/_sys/" + apiVersion + "/threadDump", DiagnosticController.class, "threadDump", new RequestMethod[]{RequestMethod.GET})
                .addRoute("/_sys/" + apiVersion + "/jobs", SchedulerController.class, "jobs", new RequestMethod[]{RequestMethod.GET})
                .addRoute("/_sys/" + apiVersion + "/triggerJob/{job}", SchedulerController.class, "triggerJob", new RequestMethod[]{RequestMethod.POST})
                .addRoute("/_sys/" + apiVersion + "/cache/get/{name}/{key}", CacheController.class, "get", new RequestMethod[]{RequestMethod.GET})
                .addRoute("/_sys/" + apiVersion + "/cache/delete/{name}/{key}", CacheController.class, "delete", new RequestMethod[]{RequestMethod.DELETE})
                .addRoute("/_sys/" + apiVersion + "/cache/list/{name}", CacheController.class, "list", new RequestMethod[]{RequestMethod.GET})
        );
    }

}
