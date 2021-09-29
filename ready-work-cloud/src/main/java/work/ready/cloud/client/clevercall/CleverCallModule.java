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

package work.ready.cloud.client.clevercall;

import work.ready.cloud.ReadyCloud;
import work.ready.cloud.client.annotation.Call;
import work.ready.core.component.proxy.JavaCoder;
import work.ready.core.module.AppModule;
import work.ready.core.service.BusinessService;

public class CleverCallModule extends AppModule {

    @Override
    protected void initialize() {
        ensureDependsAvailable();
        cleverCallFeatureSupport();
    }

    private void ensureDependsAvailable(){
        if(!ReadyCloud.isReady()){
            throw new RuntimeException("CleverCallModule depends on Registry, please start server with cloud mode.");
        }
    }

    private void cleverCallFeatureSupport(){
        CallCodeGenerator generator = new CallCodeGenerator(dbManager());
        context.coreContext.getProxyManager().addAutoCoder(new JavaCoder().setAnnotation(Call.class)
                .setAssignableFrom(BusinessService.class)
                .setGenerator(generator));
    }
}
