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

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.security.access.limiter.limit.LimitKey;
import work.ready.core.security.access.limiter.storage.utils.AddAndGetRequest;
import work.ready.core.security.access.limiter.storage.utils.OverrideKeyRequest;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AsyncLimitUsageStorage implements LimitUsageStorage {

  private static final Log logger = LogFactory.getLog(AsyncLimitUsageStorage.class);

  private final LimitUsageStorage wrappedLimitUsageStorage;
  private final ExecutorService executorService;
  private InMemoryStorage cache;

  public AsyncLimitUsageStorage(LimitUsageStorage wrappedLimitUsageStorage) {
    this.wrappedLimitUsageStorage = wrappedLimitUsageStorage;
    this.executorService = Executors.newSingleThreadExecutor();
    this.cache = new InMemoryStorage();
  }

  @Override
  public Map<LimitKey, Integer> addAndGet(Collection<AddAndGetRequest> requests) {
    Map<LimitKey, Integer> cachedEntries = cache.addAndGet(requests);
    executorService.submit(() -> sendAndCacheRequests(requests));

    return cachedEntries;
  }

  @Override
  public Map<LimitKey, Integer> addAndGetWithLimit(Collection<AddAndGetRequest> requests) {
    Map<LimitKey, Integer> cachedEntries = cache.addAndGetWithLimit(requests);
    executorService.submit(() -> sendAndCacheRequests(requests));

    return cachedEntries;
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

  public void shutdownStorage() {
    executorService.shutdown();
  }

  public void awaitTermination(Duration timeOut) throws InterruptedException {
    executorService.awaitTermination(timeOut.toMillis(), TimeUnit.MILLISECONDS);
  }

  public boolean isTerminated() {
    return executorService.isTerminated();
  }

  public void sendAndCacheRequests(Collection<AddAndGetRequest> requests) {
    try {
      requests =
          requests.stream().filter(AddAndGetRequest::isDistributed).collect(Collectors.toList());
      Map<LimitKey, Integer> responses = wrappedLimitUsageStorage.addAndGet(requests);

      Map<LimitKey, Integer> rawOverrides = new HashMap<>();
      for (AddAndGetRequest request : requests) {
        LimitKey limitEntry = LimitKey.fromRequest(request);

        rawOverrides.merge(limitEntry, responses.get(limitEntry), Integer::sum);
      }
      List<OverrideKeyRequest> overrides =
          rawOverrides
              .entrySet()
              .stream()
              .map(entry -> new OverrideKeyRequest(entry.getKey(), entry.getValue()))
              .collect(Collectors.toList());
      cache.overrideKeys(overrides);
    } catch (RuntimeException ex) {
      logger.warn("Failed to send and cache requests.", ex);
    }
  }

  @Override
  public void close() throws Exception {
    wrappedLimitUsageStorage.close();
  }
}
