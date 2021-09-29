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

package work.ready.cloud.transaction;

import work.ready.core.database.DatabaseManager;
import work.ready.core.module.Initializer;
import work.ready.core.server.Ready;

public class TransactionManagerInitializer implements Initializer<DatabaseManager> {

    private DistributedTransactionManager transactionManager;

    @Override
    public void startInit(DatabaseManager target) throws Exception {
        transactionManager = Ready.beanManager().get(DistributedTransactionManager.class);
        target.setTransactionManager(transactionManager);
        transactionManager.distributedTransactionIntegration();
    }

    @Override
    public void endInit(DatabaseManager target) throws Exception {
        transactionManager.distributedTransactionSupport();
    }
}
