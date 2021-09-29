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

package work.ready.core.security.access.limiter;

import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.SameThreadExecutor;
import io.undertow.util.StatusCodes;
import work.ready.core.handler.BaseHandler;
import work.ready.core.handler.HandlerManager;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.service.status.Status;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class RequestLimiter {
    private static final Log logger = LogFactory.getLog(RequestLimiter.class);

    private String TOO_MANY_REQUESTS = "ERROR10999";

    private volatile int requests;
    private volatile ConcurrentMap<String, Integer> ipRequests = new ConcurrentHashMap<>();
    private volatile int maxConcurrentPerIp; 
    private volatile int maxConcurrentRequests; 

    private static final AtomicIntegerFieldUpdater<RequestLimiter> requestsUpdater = AtomicIntegerFieldUpdater.newUpdater(RequestLimiter.class, "requests");

    private final HandlerManager manager;
    private final Queue<SuspendedRequest> queue;
    private final ExchangeCompletionListener COMPLETION_LISTENER = new ExchangeCompletionListener() {

        @Override
        public void exchangeEvent(final HttpServerExchange exchange, final NextListener nextListener) {
            SuspendedRequest task = null;
            boolean found = false;
            while ((task = queue.poll()) != null) {
                try {
                    String ip = task.exchange.getSourceAddress().getAddress().getHostAddress();
                    if(ipRequests.get(ip) >= maxConcurrentPerIp) {
                        tooManyRequests(task.exchange);
                        logger.error("Suspended request was skipped due to exceeded the max(%s) concurrent requests for %s", maxConcurrentPerIp, ip);
                        continue;
                    }
                    ipRequests.computeIfPresent(ip, (key,val)-> val + 1);

                    task.exchange.addExchangeCompleteListener(COMPLETION_LISTENER);
                    manager.next(task.exchange, task.next, true);  
                    found = true;
                    break;
                } catch (Throwable e) {
                    logger.error(e,"Suspended request was skipped");
                }
            }

            String ip = exchange.getSourceAddress().getAddress().getHostAddress();
            ipRequests.computeIfPresent(ip, (key,val)-> val > 0 ? val - 1 : val);
            if(ipRequests.get(ip) <= 0) ipRequests.remove(ip);

            if (!found) {
                decrementRequests();
            }

            nextListener.proceed();
        }
    };

    private void tooManyRequests(HttpServerExchange exchange) {
        Status status = new Status(TOO_MANY_REQUESTS);
        logger.debug(status.toString());
        exchange.setStatusCode(StatusCodes.TOO_MANY_REQUESTS);
        exchange.getResponseHeaders().add(Headers.RETRY_AFTER, 30);
        exchange.getResponseSender().send(status.toString());
        exchange.endExchange();
    }

    public RequestLimiter(HandlerManager manager, int maximumConcurrentRequests, int maximumConcurrentPerIp, int queueSize) {
        this.manager = manager;
        if (maximumConcurrentRequests < 1) {
            throw new IllegalArgumentException("Maximum concurrent requests must be at least 1");
        }
        maxConcurrentRequests = maximumConcurrentRequests;
        maxConcurrentPerIp = maximumConcurrentPerIp;

        this.queue = new LinkedBlockingQueue<>(queueSize <= 0 ? Integer.MAX_VALUE : queueSize);
    }

    public void handleRequest(final HttpServerExchange exchange, final BaseHandler next) throws Exception {
        int oldVal, newVal;
        String ip = exchange.getSourceAddress().getAddress().getHostAddress();
        ipRequests.putIfAbsent(ip, 0);
        if(ipRequests.get(ip) >= maxConcurrentPerIp){
            tooManyRequests(exchange);
            return;
        }
        do {
            oldVal = requests;
            if (oldVal >= maxConcurrentRequests) {
                exchange.dispatch(SameThreadExecutor.INSTANCE, new Runnable() {
                    @Override
                    public void run() {

                        synchronized (RequestLimiter.this) {
                            int oldVal, newVal;
                            do {
                                oldVal = requests;
                                if (oldVal >= maxConcurrentRequests) {
                                    if (!queue.offer(new SuspendedRequest(exchange, next))) {
                                        
                                        tooManyRequests(exchange);
                                    }
                                    return;
                                }
                                newVal = oldVal + 1;
                            } while (!requestsUpdater.compareAndSet(RequestLimiter.this, oldVal, newVal));

                            ipRequests.computeIfPresent(ip, (key,val)-> val + 1);

                            exchange.addExchangeCompleteListener(COMPLETION_LISTENER);
                            try{
                                manager.next(exchange, next, true); 
                            } catch (Exception e){
                                logger.error(e, "Exception: ");
                            }
                        }
                    }
                });
                return;
            }
            newVal = oldVal + 1;
        } while (!requestsUpdater.compareAndSet(this, oldVal, newVal));

        ipRequests.computeIfPresent(ip, (key,val)-> val + 1);

        exchange.addExchangeCompleteListener(COMPLETION_LISTENER);
        manager.next(exchange, next); 
    }

    public int getMaximumConcurrentRequests() {
        return maxConcurrentRequests;
    }

    public void setMaxConcurrentPerIp(int maxConcurrentPerIp) {
        this.maxConcurrentPerIp = maxConcurrentPerIp;
    }

    public int setMaximumConcurrentRequests(int newMax) {
        if (newMax < 1) {
            throw new IllegalArgumentException("Maximum concurrent requests must be at least 1");
        }
        int oldMax = this.maxConcurrentRequests;
        this.maxConcurrentRequests = newMax;
        if(newMax > oldMax) {
            synchronized (this) {
                while (!queue.isEmpty()) {
                    int oldVal, newVal;
                    do {
                        oldVal = requests;
                        if (oldVal >= maxConcurrentRequests) {
                            return oldMax;
                        }
                        newVal = oldVal + 1;
                    } while (!requestsUpdater.compareAndSet(this, oldVal, newVal));
                    SuspendedRequest res = queue.poll();

                    String ip = res.exchange.getSourceAddress().getAddress().getHostAddress();
                    if(ipRequests.get(ip) >= maxConcurrentPerIp) {
                        tooManyRequests(res.exchange);
                        logger.error("Suspended request was skipped due to exceeded the max(%s) concurrent requests for %s", maxConcurrentPerIp, ip);
                        continue;
                    }
                    ipRequests.computeIfPresent(ip, (key,val)-> val + 1);
                    res.exchange.addExchangeCompleteListener(COMPLETION_LISTENER);
                    try {
                        manager.next(res.exchange, res.next, true); 
                    } catch (Exception e){
                        logger.error(e, "Exception: ");
                    }
                }
            }
        }
        return oldMax;
    }

    private void decrementRequests() {
        requestsUpdater.decrementAndGet(this);
    }

    private static final class SuspendedRequest {
        final HttpServerExchange exchange;
        final BaseHandler next;

        private SuspendedRequest(HttpServerExchange exchange, BaseHandler next) {
            this.exchange = exchange;
            this.next = next;
        }
    }
}
