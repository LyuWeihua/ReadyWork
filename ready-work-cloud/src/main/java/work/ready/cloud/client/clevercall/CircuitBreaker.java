/**
 *
 * Original work copyright (c) 2016 Network New Technologies Inc.
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
package work.ready.cloud.client.clevercall;

import work.ready.cloud.ReadyCloud;
import work.ready.cloud.client.ClientConfig;
import work.ready.cloud.registry.base.URL;
import work.ready.core.server.Ready;
import work.ready.core.tools.define.BiTuple;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class CircuitBreaker {
    private static final Map<URL, BiTuple<AtomicInteger, AtomicLong>> stateHolder = new HashMap<>();
    private static final ClientConfig clientConfig = ReadyCloud.getConfig().getHttpClient();
    private final Supplier<CompletableFuture<HttpResponse<String>>> supplier;
    private final URL url;
    private int timeout = 0;

    public static final int MIN_UNSTABLE_LEVEL = 0; 
    public static final int MAX_UNSTABLE_LEVEL = 100;   

    public CircuitBreaker(URL url, Supplier<CompletableFuture<HttpResponse<String>>> supplier) {
        this.url = url;
        this.supplier = supplier;
        stateHolder.computeIfAbsent(url, k->new BiTuple<>(new AtomicInteger(0), new AtomicLong(0)));
    }

    public static void close(URL url){
        var state = getState(url);
        state.get1().set(0);
    }

    public static void open(URL url){
        var state = getState(url);
        state.get1().set(clientConfig.getErrorThreshold() + 1);
        state.get2().set(Ready.currentTimeMillis());
    }

    public static void tryHalfOpen(URL url){
        var state = getState(url);
        if(state.get1().get() > clientConfig.getErrorThreshold()) {
            state.get1().set(clientConfig.getErrorThreshold());
        }
    }

    public static void halfOpen(URL url){
        getState(url).get1().set(clientConfig.getErrorThreshold());
    }

    private static BiTuple<AtomicInteger, AtomicLong> getState(URL url) {
        synchronized (stateHolder) {
            return stateHolder.computeIfAbsent(url, k -> new BiTuple<>(new AtomicInteger(0), new AtomicLong(0)));
        }
    }

    public URL getUrl() { return url; }

    public boolean isOpen() {
        return stateHolder.get(url).get1().get() >= clientConfig.getErrorThreshold();
    }

    public int getTimeoutCount() { return stateHolder.get(url).get1().get(); }

    public long getLastErrorTime() {
        return stateHolder.get(url).get2().get();
    }

    public CircuitBreaker setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public HttpResponse<String> call() throws TimeoutException, ExecutionException, InterruptedException {
        State state = checkState();

        try {
            if (State.OPEN == state) {
                throw new CircuitBreakerException("circuit is opened.");
            }

            timeout = timeout > 0 ? timeout : clientConfig.getTimeout();
            HttpResponse<String> httpResponse = supplier.get().get(timeout, TimeUnit.MILLISECONDS);
            if(stateHolder.get(url).get1().get() > 0) {
                stateHolder.get(url).get1().decrementAndGet();
            }
            return httpResponse;
        } catch (InterruptedException | ExecutionException e) {
            throw e;
        } catch (TimeoutException e) {
            recordTimeout();
            throw e;
        }
    }

    private State checkState() {
        if (stateHolder.get(url).get2().get() > 0 && clientConfig.getResetTimeout() > 0) {
            if(Ready.currentTimeMillis() - stateHolder.get(url).get2().get() > clientConfig.getResetTimeout()) {
                
                if(stateHolder.get(url).get1().get() > clientConfig.getErrorThreshold()) {
                    stateHolder.get(url).get1().set(clientConfig.getErrorThreshold());
                } else { 
                    stateHolder.get(url).get1().set(0);
                }
                stateHolder.get(url).get2().set(0);
            }
        }
        if (stateHolder.get(url).get1().get() == clientConfig.getErrorThreshold()) {   
            return State.HALF_OPEN;
        }
        if (stateHolder.get(url).get1().get() >= clientConfig.getErrorThreshold()) {
            return State.OPEN;
        }
        return State.CLOSE;
    }

    private void recordTimeout() {
        stateHolder.get(url).get1().incrementAndGet();
        stateHolder.get(url).get2().set(Ready.currentTimeMillis());
    }

    enum State {
        CLOSE,
        HALF_OPEN,
        OPEN
    }
}
