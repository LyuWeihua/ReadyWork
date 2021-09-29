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
import work.ready.core.tools.define.BiTuple;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public interface LimitUsageStorage {

  default BiTuple<LimitKey, Integer> incrementAndGet(
          String resource,
          String limitName,
          String property,
          boolean distributed,
          Duration expiration,
          Instant eventTimestamp) {
    return addAndGet(resource, limitName, property, distributed, expiration, eventTimestamp, 1);
  }

  default BiTuple<LimitKey, Integer> addAndGet(
          String resource,
          String limitName,
          String property,
          boolean distributed,
          Duration expiration,
          Instant eventTimestamp,
          int cost) {
    return addAndGet(
        new AddAndGetRequest.Builder()
            .withResource(resource)
            .withLimitName(limitName)
            .withProperty(property)
            .withDistributed(distributed)
            .withExpiration(expiration)
            .withEventTimestamp(eventTimestamp)
            .withCost(cost)
            .build());
  }

  default BiTuple<LimitKey, Integer> addAndGet(AddAndGetRequest request) {
    Map.Entry<LimitKey, Integer> value =
        addAndGet(Arrays.asList(request)).entrySet().iterator().next();
    return new BiTuple<>(value.getKey(), value.getValue());
  }

  Map<LimitKey, Integer> addAndGet(Collection<AddAndGetRequest> requests);

  Map<LimitKey, Integer> addAndGetWithLimit(Collection<AddAndGetRequest> requests);

  Map<LimitKey, Integer> getCurrentLimitCounters();

  Map<LimitKey, Integer> getCurrentLimitCounters(String resource);

  Map<LimitKey, Integer> getCurrentLimitCounters(String resource, String limitName);

  Map<LimitKey, Integer> getCurrentLimitCounters(
          String resource, String limitName, String property);

  void close() throws Exception;
}
