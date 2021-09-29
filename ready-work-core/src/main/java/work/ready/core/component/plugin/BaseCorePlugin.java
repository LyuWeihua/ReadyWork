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

package work.ready.core.component.plugin;

import work.ready.core.module.Application;
import work.ready.core.module.CoreContext;

public abstract class BaseCorePlugin implements CorePlugin {

    @Override
    public void bootstrap() {

    }

    @Override
    public void initBegin(CoreContext context) {

    }

    @Override
    public void initEnd(CoreContext context) {

    }

    @Override
    public void appInitBegin(Application app) {

    }

    @Override
    public void appInitEnd(Application app) {

    }

}
