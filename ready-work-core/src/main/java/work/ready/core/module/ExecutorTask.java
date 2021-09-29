/**
 *
 * Original work Copyright core-ng
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
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

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.util.concurrent.Callable;

public class ExecutorTask<T> implements Callable<T> {
    private static final Log logger = LogFactory.getLog(ExecutorTask.class);
    final String action;
    private final Callable<T> task;

    ExecutorTask(String action, Callable<T> task) {
        this.task = task;
        this.action = action;
    }

    @Override
    public T call() throws Exception {
        try {
            logger.info("=== " + action + " task execution start ===");
            return task.call();
        } catch (Throwable e) {
            logger.error(e, action + " task execution exception");
            throw e;
        } finally {
            logger.info("=== " + action + " task execution end ===");
        }
    }
}
