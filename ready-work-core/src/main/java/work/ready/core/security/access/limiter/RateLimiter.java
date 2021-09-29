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

import io.undertow.conduits.RateLimitingStreamSinkConduit;
import io.undertow.server.ConduitWrapper;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.ConduitFactory;
import org.xnio.conduits.StreamSinkConduit;

import java.util.concurrent.TimeUnit;

public class RateLimiter implements ConduitWrapper<StreamSinkConduit> {

    private volatile int limitRateBytes;
    private volatile int limitRatePeriod;

    public RateLimiter(int limitRateBytes, int limitRatePeriod){
        this.limitRateBytes = limitRateBytes;
        this.limitRatePeriod = limitRatePeriod;
    }

    public int getLimitRateBytes() {
        return limitRateBytes;
    }

    public RateLimiter setLimitRateBytes(int limitRateBytes) {
        this.limitRateBytes = limitRateBytes;
        return this;
    }

    public int getLimitRatePeriod() {
        return limitRatePeriod;
    }

    public RateLimiter setLimitRatePeriod(int limitRatePeriod) {
        this.limitRatePeriod = limitRatePeriod;
        return this;
    }

    @Override
    public StreamSinkConduit wrap(ConduitFactory<StreamSinkConduit> factory, HttpServerExchange exchange) {
        return new RateLimitingStreamSinkConduit(factory.create(), limitRateBytes / 2, limitRatePeriod * 1000 / 2, TimeUnit.MILLISECONDS);
    }

}
