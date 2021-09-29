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
package work.ready.core.security.access.limiter.limit.override;

import work.ready.core.security.access.limiter.trigger.LimitTrigger;
import work.ready.core.security.access.limiter.trigger.LimitTriggerCallback;
import work.ready.core.security.access.limiter.trigger.ValueThresholdTrigger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class LimitOverrideBuilder {

  private String limitProperty;
  private Duration limitExpiration;
  private int limitCapacity;

  private List<LimitTrigger> triggers = new ArrayList<>();

  private LimitOverrideBuilder() {}

  public LimitOverrideBuilder to(int capacity) {
    this.limitCapacity = capacity;
    return this;
  }

  public LimitOverrideBuilder per(Duration expiration) {
    this.limitExpiration = expiration;
    return this;
  }

  public LimitOverrideBuilder withLimitTrigger(LimitTrigger limitTrigger) {
    this.triggers.add(limitTrigger);
    return this;
  }

  public LimitOverrideBuilder withExceededCallback(LimitTriggerCallback limitTriggerCallback) {
    triggers.add(new ValueThresholdTrigger(limitCapacity, limitTriggerCallback));
    return this;
  }

  public LimitOverride build() {
    return new LimitOverride(
        new LimitOverrideDefinition(limitProperty, limitCapacity, limitExpiration), triggers);
  }

  public static LimitOverrideBuilder of(String limitProperty) {
    LimitOverrideBuilder limitBuilder = new LimitOverrideBuilder();
    limitBuilder.limitProperty = limitProperty;
    return limitBuilder;
  }
}
