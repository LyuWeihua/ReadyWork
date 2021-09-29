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

package work.ready.core.server;

import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class GracefulShutdownHandler implements ExchangeCompletionListener {
    final AtomicLong activeRequests = new AtomicLong(0);

    private static final Log logger = LogFactory.getLog(GracefulShutdownHandler.class);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final Object lock = new Object();

    public boolean handle(HttpServerExchange exchange) {
        activeRequests.getAndIncrement();
        exchange.addExchangeCompleteListener(this);

        if (shutdown.get()) {
            logger.warn("reject request due to server is shutting down, requestURL=%s", exchange.getRequestURL());

            exchange.setPersistent(false);
            exchange.setStatusCode(StatusCodes.SERVICE_UNAVAILABLE);
            exchange.endExchange();
            return true;
        }

        return false;
    }

    void shutdown() {
        shutdown.set(true);
    }

    boolean awaitTermination(long timeoutInMs) throws InterruptedException {
        long end = Ready.currentTimeMillis() + timeoutInMs;
        synchronized (lock) {
            while (activeRequests.get() > 0) {
                long left = end - Ready.currentTimeMillis();
                if (left <= 0) {
                    return false;
                }
                lock.wait(left);
            }
            return true;
        }
    }

    @Override
    public void exchangeEvent(HttpServerExchange exchange, NextListener next) {
        try {
            long count = activeRequests.decrementAndGet();
            if (count <= 0 && shutdown.get()) {
                synchronized (lock) {
                    lock.notifyAll();
                }
            }
        } finally {
            next.proceed();
        }
    }
}
