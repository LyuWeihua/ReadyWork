/**
 *
 * Original work Copyright (c) 2016 Coveo, under the MIT License
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
package work.ready.core.security.access.limiter;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.security.SecurityManager;
import work.ready.core.security.access.limiter.exception.LimiterExceededException;
import work.ready.core.security.access.limiter.limit.Limit;
import work.ready.core.security.access.limiter.limit.LimitDefinition;
import work.ready.core.security.access.limiter.limit.LimitKey;
import work.ready.core.security.access.limiter.storage.LimitUsageStorage;
import work.ready.core.security.access.limiter.storage.utils.AddAndGetRequest;
import work.ready.core.security.access.limiter.trigger.LimitTrigger;
import work.ready.core.security.access.limiter.limit.LimitBuilder;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class Limiter<T> {

  private static final Log logger = LogFactory.getLog(Limiter.class);

  private final Clock clock;

  private final LimitUsageStorage storage;
  private final String resource;
  private final List<Limit<T>> limits;

  @SafeVarargs
  public Limiter(Clock clock, LimitUsageStorage storage, String resourceName, Limit<T>... limits) {
    this.clock = clock;
    this.storage = storage;
    this.resource = resourceName;
    this.limits = Collections.unmodifiableList(Arrays.asList(limits));
  }

  public void call(T context) throws LimiterExceededException {
    call(context, 1);
  }

  public void updateAndVerifyLimit(T context) throws LimiterExceededException {
    updateAndVerifyLimit(context, 1);
  }

  public void call(T context, int cost) throws LimiterExceededException {
    List<LimitDefinition> exceededLimits = getExceededLimits(context, cost, true);
    if (!exceededLimits.isEmpty()) {
      throw new LimiterExceededException(exceededLimits, context, cost);
    }
  }

  public void updateAndVerifyLimit(T context, int cost) throws LimiterExceededException {
    List<LimitDefinition> exceededLimits = updateAndVerifyExceededLimits(context, cost);
    if (!exceededLimits.isEmpty()) {
      throw new LimiterExceededException(exceededLimits, context, cost);
    }
  }

  public boolean tryCall(T context) {
    return tryCall(context, 1);
  }

  public boolean tryCall(T context, int cost) {
    return getExceededLimits(context, cost, true).isEmpty();
  }

  public boolean tryUpdateAndVerifyLimit(T context) {
    return tryUpdateAndVerifyLimit(context, 1);
  }

  public boolean tryUpdateAndVerifyLimit(T context, int cost) {
    return updateAndVerifyExceededLimits(context, cost).isEmpty();
  }

  public boolean checkLimit(T context) {
    return checkLimit(context, 1);
  }

  public boolean checkLimit(T context, int cost) {
    return getExceededLimits(context, cost, false).isEmpty();
  }

  private List<LimitDefinition> getExceededLimits(T context, int cost, boolean shouldUpdateLimit) {
    if (cost < 1) {
      throw new IllegalArgumentException("'cost' must be greater than zero");
    }

    Instant now = Instant.now(clock);
    List<AddAndGetRequest> requests = buildRequestsFromLimits(context, 0, now);

    Map<LimitKey, Integer> results = storage.addAndGet(requests);

    List<LimitDefinition> exceededLimits = new ArrayList<>();
    if (results.size() == limits.size()) {
      for (Entry<LimitKey, Integer> result : results.entrySet()) {
        Limit<T> limit =
            limits
                .stream()
                .filter(entry -> entry.getName().equals(result.getKey().getLimitName()))
                .findFirst()
                .get();

        if (shouldUpdateLimit) {
          handleTriggers(context, cost, now, result.getValue() + cost, limit);
        }

        if (result.getValue() + cost > limit.getCapacity(context)) {
          exceededLimits.add(limit.getDefinition());
        }
      }
    } else {
      logger.error(
          "Something went very wrong. We sent %s limits to the backend but received %s responses. Assuming that no limits were exceeded. Limits: %s. Results: %s.",
          limits.size(),
          results.size(),
          limits,
          results);
    }

    if (shouldUpdateLimit && exceededLimits.isEmpty()) {
      requests = buildRequestsFromLimits(context, cost, now);
      storage.addAndGet(requests);
    }

    return exceededLimits;
  }

  private List<LimitDefinition> updateAndVerifyExceededLimits(T context, int cost) {
    if (cost < 1) {
      throw new IllegalArgumentException("'cost' must be greater than zero");
    }

    Instant now = Instant.now(clock);
    List<AddAndGetRequest> requests = buildRequestsFromLimits(context, cost, now);

    Map<LimitKey, Integer> results = storage.addAndGetWithLimit(requests);

    List<LimitDefinition> exceededLimits = new ArrayList<>();
    if (results.size() == limits.size()) {
      for (Entry<LimitKey, Integer> result : results.entrySet()) {
        Limit<T> limit =
            limits
                .stream()
                .filter(entry -> entry.getName().equals(result.getKey().getLimitName()))
                .findFirst()
                .get();

        handleTriggers(context, cost, now, result.getValue(), limit);
        if (result.getValue() > limit.getCapacity(context)) {
          exceededLimits.add(limit.getDefinition());
        }
      }
    } else {
      logger.error(
          "Something went very wrong. We sent %s limits to the backend but received %s responses. Assuming that no limits were exceeded. Limits: %s. Results: %s.",
          limits.size(),
          results.size(),
          limits,
          results);
    }
    return exceededLimits;
  }

  private List<AddAndGetRequest> buildRequestsFromLimits(T context, int cost, Instant now) {

    int minLimit =
        limits.stream().map(limit -> limit.getCapacity(context)).min(Integer::compareTo).orElse(0);
    return limits
        .stream()
        .map(
            limit
                -> new AddAndGetRequest.Builder()
                    .withResource(resource)
                    .withLimitName(limit.getName())
                    .withLimit(minLimit)
                    .withProperty(limit.getProperty(context))
                    .withDistributed(limit.isDistributed())
                    .withExpiration(limit.getExpiration(context))
                    .withEventTimestamp(now)
                    .withCost(cost)
                    .build())
        .collect(Collectors.toList());
  }

  private void handleTriggers(
      T context, int cost, Instant timestamp, int currentValue, Limit<T> limit) {
    for (LimitTrigger trigger : limit.getLimitTriggers(context)) {
      try {
        trigger.callbackIfRequired(
            context, cost, timestamp, currentValue, limit.getDefinition(context));
      } catch (RuntimeException ex) {
        logger.warn(ex,
            "Trigger callback %s for limit %s threw an exception. Ignoring.", trigger, limit);
      }
    }
  }

  public Map<LimitKey, Integer> debugCurrentLimitCounters() {
    return storage.getCurrentLimitCounters();
  }
}
