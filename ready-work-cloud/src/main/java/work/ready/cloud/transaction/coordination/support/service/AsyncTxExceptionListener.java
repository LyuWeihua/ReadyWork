/**
 *
 * Original work Copyright 2017-2019 CodingApi
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
package work.ready.cloud.transaction.coordination.support.service;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.TransactionConfig;
import work.ready.cloud.transaction.coordination.support.dto.TxException;
import work.ready.core.tools.HttpClient;
import work.ready.core.tools.ReadyThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncTxExceptionListener implements TxExceptionListener {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor(new ReadyThreadFactory("AsyncTxException"));
    private final TransactionConfig config;

    public AsyncTxExceptionListener() {
        config = Cloud.getTransactionManager().getConfig();
    }

    @Override
    public void onException(TxException txException) {
        executorService.submit(() -> HttpClient.getInstance().postAsync(config.getExceptionListenerUrl(), txException.toMap(), (res)->{
            if(res.statusCode() != 200) {
                System.err.println(res.body());
            }
        }));
    }
}
