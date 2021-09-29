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

package work.ready.cloud.transaction.coordination.core.storage;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheRebalanceMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.transactions.Transaction;
import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.TransactionConfig;
import work.ready.cloud.transaction.common.exception.FastStorageException;
import work.ready.cloud.transaction.common.lock.DtxLocks;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.util.*;
import java.util.stream.Collectors;

public class IgniteStorage implements FastStorage {
    private static final Log logger = LogFactory.getLog(IgniteStorage.class);
    public static final String transactionCacheName = "ready.work:transaction";
    public static final String transactionStateCacheName = "ready.work:transaction:state";
    public static final String txcLockCacheName = "ready.work:transaction:txcLock";
    private IgniteCache<String, Map<String, TransactionUnit>> TRANSACTION_CACHE;
    private IgniteCache<String, Map<String, LockValue>> TXC_LOCK_CACHE;
    private IgniteCache<String, Integer> TRANSACTION_STATE_CACHE;
    private TransactionConfig config;

    public IgniteStorage() {
        config = Cloud.getTransactionManager().getConfig();

        CacheConfiguration<String, Map<String, TransactionUnit>> txCacheConfig = new CacheConfiguration<>();
        txCacheConfig.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        txCacheConfig.setCacheMode(CacheMode.REPLICATED);
        txCacheConfig.setRebalanceMode(CacheRebalanceMode.SYNC);
        txCacheConfig.setDataRegionName(Cloud.WITHOUT_PERSISTENCE);
        txCacheConfig.setEagerTtl(true);
        if(Cloud.cluster().nodes().size() > 3) {
            txCacheConfig.setBackups(3);
        } else {
            txCacheConfig.setBackups(Cloud.cluster().nodes().size());
        }
        txCacheConfig.setName(transactionCacheName);
        TRANSACTION_CACHE = Cloud.getOrCreateCache(txCacheConfig);

        CacheConfiguration<String, Map<String, LockValue>> lockCacheConfig = new CacheConfiguration<>();
        lockCacheConfig.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        lockCacheConfig.setCacheMode(CacheMode.REPLICATED);
        lockCacheConfig.setRebalanceMode(CacheRebalanceMode.SYNC);
        lockCacheConfig.setDataRegionName(Cloud.WITHOUT_PERSISTENCE);
        lockCacheConfig.setEagerTtl(true);
        if(Cloud.cluster().nodes().size() > 3) {
            lockCacheConfig.setBackups(3);
        } else {
            lockCacheConfig.setBackups(Cloud.cluster().nodes().size());
        }
        lockCacheConfig.setName(txcLockCacheName);
        TXC_LOCK_CACHE = Cloud.getOrCreateCache(lockCacheConfig);

        CacheConfiguration<String, Integer> stateCacheConfig = new CacheConfiguration<>();
        stateCacheConfig.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        stateCacheConfig.setCacheMode(CacheMode.REPLICATED);
        stateCacheConfig.setRebalanceMode(CacheRebalanceMode.SYNC);
        stateCacheConfig.setDataRegionName(Cloud.WITHOUT_PERSISTENCE);
        stateCacheConfig.setEagerTtl(true);
        if(Cloud.cluster().nodes().size() > 3) {
            stateCacheConfig.setBackups(3);
        } else {
            stateCacheConfig.setBackups(Cloud.cluster().nodes().size());
        }
        stateCacheConfig.setName(transactionStateCacheName);
        TRANSACTION_STATE_CACHE = Cloud.getOrCreateCache(stateCacheConfig);
    }

    @Override
    public void initGroup(String groupId) throws FastStorageException {
        Cloud.putWithExpiration(TRANSACTION_CACHE, groupId, new HashMap<>(Map.of("root", new TransactionUnit())), config.getTxTimeout() + 10 * 1000);
    }

    @Override
    public boolean containsGroup(String groupId) {
        return TRANSACTION_CACHE.containsKey(groupId);
    }

    @Override
    public List<TransactionUnit> findTransactionUnitsFromGroup(String groupId) throws FastStorageException {
        if (containsGroup(groupId)) {
            return TRANSACTION_CACHE.get(groupId).entrySet().stream()
                    .filter(entry -> !entry.getKey().equals("root")).map(Map.Entry::getValue).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void saveTransactionUnitToGroup(String groupId, TransactionUnit transactionUnit) throws FastStorageException {
        if (containsGroup(groupId)) {
            try (Transaction tx = Cloud.transactions().txStart()) {
                var map = TRANSACTION_CACHE.get(groupId);
                map.put(transactionUnit.getUnitId(), transactionUnit);
                Cloud.putWithExpiration(TRANSACTION_CACHE, groupId, map, config.getTxTimeout() + 10 * 1000);
                tx.commit();
            }
            return;
        }
        throw new FastStorageException("attempts to the non-existent transaction group " + groupId,
                FastStorageException.EX_CODE_NO_GROUP);
    }

    @Override
    public void clearGroup(String groupId) throws FastStorageException {
        logger.debug("remove group:%s from ignite.", groupId);
        TRANSACTION_CACHE.remove(groupId);
        TRANSACTION_STATE_CACHE.remove(groupId);
    }

    @Override
    public void saveTransactionState(String groupId, int state) throws FastStorageException {
        Cloud.putWithExpiration(TRANSACTION_STATE_CACHE, groupId, state, config.getTxTimeout() + 10 * 1000);
    }

    @Override
    public int getTransactionState(String groupId) throws FastStorageException {
        Integer state = TRANSACTION_STATE_CACHE.get(groupId);
        return state == null ? -1 : state;
    }

    @Override
    public void acquireLocks(String contextId, Map<String, Set<String>> lockMap, LockValue lockValue) throws FastStorageException {
        if (lockMap == null || lockMap.isEmpty()) {
            return;
        }

        try (Transaction tx = Cloud.transactions().txStart()) {
            for(var entry : lockMap.entrySet()) {
                if(TXC_LOCK_CACHE.containsKey(entry.getKey())) {
                    Map<String, LockValue> locks = TXC_LOCK_CACHE.get(entry.getKey());
                    
                    boolean lockConflict = locks.entrySet().stream()
                            .filter(each -> entry.getValue().contains(each.getKey()))
                            .filter(each -> !each.getValue().getGroupId().equals(lockValue.getGroupId()))
                            .anyMatch(each -> each.getValue().getLockType() == DtxLocks.X_LOCK || lockValue.getLockType() != DtxLocks.S_LOCK);
                    if (lockConflict) {
                        throw new FastStorageException("acquire locks fail.", FastStorageException.EX_CODE_REPEAT_LOCK);
                    }

                    entry.getValue().forEach(k -> {

                        if(locks.containsKey(k) && locks.get(k).getLockType() == DtxLocks.X_LOCK && lockValue.getLockType() == DtxLocks.S_LOCK) {
                            LockValue mergedLock = new LockValue(lockValue.getGroupId(), DtxLocks.X_LOCK);
                            locks.put(k, mergedLock);
                        } else {
                            locks.put(k, lockValue);
                        }
                    });
                    
                    Cloud.putWithExpiration(TXC_LOCK_CACHE, entry.getKey(), locks, config.getTxTimeout());
                } else {
                    Map<String, LockValue> locks = entry.getValue().stream().collect(Collectors.toMap(lock -> lock, lock -> lockValue));
                    Cloud.putWithExpiration(TXC_LOCK_CACHE, entry.getKey(), locks, config.getTxTimeout());
                }
            }
            tx.commit();
        }
    }

    @Override
    public void releaseLocks(String contextId, Map<String, Set<String>> lockMap) throws FastStorageException {
        try (Transaction tx = Cloud.transactions().txStart()) {
            for(var entry : lockMap.entrySet()) {
                Map<String, LockValue> locks = TXC_LOCK_CACHE.get(entry.getKey());
                if(locks != null && !locks.isEmpty()) {
                    entry.getValue().forEach(locks::remove);
                }
                if(locks == null || locks.isEmpty()) {
                    TXC_LOCK_CACHE.remove(entry.getKey());
                } else {
                    
                    Cloud.putWithExpiration(TXC_LOCK_CACHE, entry.getKey(), locks, config.getTxTimeout());
                }
            }
            tx.commit();
        }
    }
}
