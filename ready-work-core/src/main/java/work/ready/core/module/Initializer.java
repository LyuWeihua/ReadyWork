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

import work.ready.core.ioc.annotation.DisposableBean;

public interface Initializer<T> extends DisposableBean {

    void startInit(T target) throws Exception;

    default void endInit(T target) throws Exception {

    }

    @Override
    default void destroy() throws Exception {
    }

    default int order() {
        return 0;
    }
}
