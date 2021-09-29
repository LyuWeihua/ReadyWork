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
package work.ready.core.security.access.limiter.storage;

import work.ready.core.security.access.limiter.limit.LimitKey;
import work.ready.core.security.access.limiter.storage.utils.AddAndGetRequest;
import work.ready.core.security.access.limiter.storage.utils.CacheSynchronization;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Timer;

public class AsyncBatchLimitUsageStorage implements LimitUsageStorage {
  private final LimitUsageStorage wrappedLimitUsageStorage;
  private InMemoryStorage cache;
  private Timer timer;

  public AsyncBatchLimitUsageStorage(
      LimitUsageStorage wrappedLimitUsageStorage, Duration timeBetweenSynchronizations) {
    this(
        wrappedLimitUsageStorage,
        new InMemoryStorage(),
        timeBetweenSynchronizations,
        Duration.ofMillis(0),
        false);
  }

  public AsyncBatchLimitUsageStorage(
      LimitUsageStorage wrappedLimitUsageStorage,
      Duration timeBetweenSynchronizations,
      boolean forceCacheInit) {
    this(
        wrappedLimitUsageStorage,
        new InMemoryStorage(),
        timeBetweenSynchronizations,
        Duration.ofMillis(0),
        forceCacheInit);
  }

   AsyncBatchLimitUsageStorage(
      LimitUsageStorage wrappedLimitUsageStorage,
      InMemoryStorage cache,
      Duration timeBetweenSynchronisations,
      Duration delayBeforeFirstSync,
      boolean forceCacheInit) {
    this(
        wrappedLimitUsageStorage,
        cache,
        new CacheSynchronization(cache, wrappedLimitUsageStorage),
        timeBetweenSynchronisations,
        delayBeforeFirstSync,
        forceCacheInit);
  }

   AsyncBatchLimitUsageStorage(
      LimitUsageStorage wrappedLimitUsageStorage,
      InMemoryStorage cache,
      CacheSynchronization cacheSynchronization,
      Duration timeBetweenSynchronisations,
      Duration delayBeforeFirstSync,
      boolean forceCacheInit) {
    this.wrappedLimitUsageStorage = wrappedLimitUsageStorage;
    this.cache = cache;

    if (forceCacheInit) {
      cacheSynchronization.init();
    }

    timer = new Timer();
    timer.schedule(
        cacheSynchronization,
        delayBeforeFirstSync.toMillis(),
        timeBetweenSynchronisations.toMillis());
  }

  @Override
  public Map<LimitKey, Integer> addAndGet(Collection<AddAndGetRequest> requests) {
    return cache.addAndGet(requests);
  }

  @Override
  public Map<LimitKey, Integer> addAndGetWithLimit(Collection<AddAndGetRequest> requests) {
    return cache.addAndGetWithLimit(requests);
  }

  public Map<LimitKey, Integer> debugCacheLimitCounters() {
    return cache.getCurrentLimitCounters();
  }

  @Override
  public Map<LimitKey, Integer> getCurrentLimitCounters() {
    return wrappedLimitUsageStorage.getCurrentLimitCounters();
  }

  @Override
  public Map<LimitKey, Integer> getCurrentLimitCounters(String resource) {
    return wrappedLimitUsageStorage.getCurrentLimitCounters(resource);
  }

  @Override
  public Map<LimitKey, Integer> getCurrentLimitCounters(String resource, String limitName) {
    return wrappedLimitUsageStorage.getCurrentLimitCounters(resource, limitName);
  }

  @Override
  public Map<LimitKey, Integer> getCurrentLimitCounters(
      String resource, String limitName, String property) {
    return wrappedLimitUsageStorage.getCurrentLimitCounters(resource, limitName, property);
  }

  @Override
  public void close() throws Exception {
    timer.cancel();
    wrappedLimitUsageStorage.close();
    cache.close();
  }
}
