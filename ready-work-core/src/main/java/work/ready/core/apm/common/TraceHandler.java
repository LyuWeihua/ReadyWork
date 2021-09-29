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

package work.ready.core.apm.common;

import work.ready.core.apm.model.Span;
import work.ready.core.server.Ready;

public interface TraceHandler {

    Span before(String className, String methodName, Object[] arguments, Object[] extraParam);

    Object after(String className,String methodName, Object[] arguments, Object result, Throwable t, Object[] extraParam);

    default void calculateSpend(Span span) {
        if (span != null) {
            span.setSpend(Ready.currentTimeMillis() - span.getTime().getTime());
        }
    }

}
