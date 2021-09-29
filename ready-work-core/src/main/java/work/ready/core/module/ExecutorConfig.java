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

public class ExecutorConfig {

    private ApplicationContext context;

    public ExecutorConfig(ApplicationContext context){
        this.context = context;
    }

    public Executor add() {
        return add(null, Runtime.getRuntime().availableProcessors() * 2);
    }

    public Executor add(String name, int poolSize) {
        Executor executor = createExecutor(name, poolSize);
        context.coreContext.beanManager.addSingletonObject(Executor.class, executor);
        return executor;
    }

    Executor createExecutor(String name, int poolSize) {
        var executor = new MainExecutor(poolSize, name);
        context.shutdownHook.add(ShutdownHook.STAGE_2, timeoutInMs -> executor.shutdown());
        context.shutdownHook.add(ShutdownHook.STAGE_3, executor::awaitTermination);
        return executor;
    }
}
