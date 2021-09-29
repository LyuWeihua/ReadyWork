/**
 * The MIT License
 * Copyright (c) 2016 Coveo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package work.ready.core.security.access.limiter.limit;

import work.ready.core.security.access.limiter.limit.override.LimitOverride;
import work.ready.core.security.access.limiter.trigger.LimitTrigger;
import work.ready.core.security.access.limiter.trigger.LimitTriggerCallback;
import work.ready.core.security.access.limiter.trigger.ValueThresholdTrigger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class LimitBuilder<T> {

  private String limitName;
  private Duration limitExpiration;
  private int limitCapacity;
  private boolean distributed = true;

  private Function<T, String> propertyExtractor;
  private List<LimitTrigger> triggers = new ArrayList<>();
  private Set<LimitOverride> overrides = new HashSet<>();

  private LimitBuilder() {}

  public LimitBuilder<T> to(int capacity) {
    this.limitCapacity = capacity;
    return this;
  }

  public LimitBuilder<T> per(Duration expiration) {
    this.limitExpiration = expiration;
    return this;
  }

  public LimitBuilder<T> withDistributed(boolean distributed) {
    this.distributed = distributed;
    return this;
  }

  public LimitBuilder<T> withLimitTrigger(LimitTrigger limitTrigger) {
    this.triggers.add(limitTrigger);
    return this;
  }

  public LimitBuilder<T> withExceededCallback(LimitTriggerCallback limitTriggerCallback) {
    triggers.add(new ValueThresholdTrigger(limitCapacity, limitTriggerCallback));
    return this;
  }

  public LimitBuilder<T> withLimitOverride(LimitOverride limitOverride) {
    this.overrides.remove(limitOverride);
    this.overrides.add(limitOverride);
    return this;
  }

  public Limit<T> build() {
    return new Limit<>(
        new LimitDefinition(limitName, limitCapacity, limitExpiration),
        distributed,
        propertyExtractor,
        overrides,
        triggers);
  }

  public static LimitBuilder<String> of(String limitName) {
    return of(limitName, Function.identity());
  }

  public static <T> LimitBuilder<T> of(String limitName, Function<T, String> propertyExtractor) {
    LimitBuilder<T> limitBuilder = new LimitBuilder<>();
    limitBuilder.limitName = limitName;
    limitBuilder.propertyExtractor = propertyExtractor;
    return limitBuilder;
  }
}
