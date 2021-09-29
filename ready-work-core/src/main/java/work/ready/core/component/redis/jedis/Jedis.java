/**
 *
 * Original work Copyright (c) 2015-2020, Michael Yang 杨福海 (fuhai999@gmail.com).
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
package work.ready.core.component.redis.jedis;

import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import work.ready.core.component.redis.BaseRedis;
import work.ready.core.component.redis.RedisConfig;
import work.ready.core.component.redis.RedisScanResult;
import work.ready.core.exception.JedisException;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.util.*;
import java.util.Map.Entry;

public class Jedis extends BaseRedis {
    private static final Log logger = LogFactory.getLog(Jedis.class);

    protected JedisPool jedisPool;
    protected RedisConfig config;

    public Jedis(RedisConfig config) {
        super(config);

        this.config = config;

        String host = config.getHost();
        Integer port = config.getPort();
        Integer timeout = config.getTimeout();
        String password = config.getPassword();
        Integer database = config.getDatabase();
        String clientName = config.getClientName();

        if (host.contains(":")) {
            port = Integer.valueOf(host.split(":")[1]);
        }

        JedisPoolConfig poolConfig = new JedisPoolConfig();

        if (config.getTestWhileIdle() != null) {
            poolConfig.setTestWhileIdle(config.getTestWhileIdle());
        }

        if (config.getTestOnBorrow() != null) {
            poolConfig.setTestOnBorrow(config.getTestOnBorrow());
        }

        if (config.getTestOnCreate() != null) {
            poolConfig.setTestOnCreate(config.getTestOnCreate());
        }

        if (config.getTestOnReturn() != null) {
            poolConfig.setTestOnReturn(config.getTestOnReturn());
        }

        if (config.getMinEvictableIdleTimeMillis() != null) {
            poolConfig.setMinEvictableIdleTimeMillis(config.getMinEvictableIdleTimeMillis());
        }

        if (config.getTimeBetweenEvictionRunsMillis() != null) {
            poolConfig.setTimeBetweenEvictionRunsMillis(config.getTimeBetweenEvictionRunsMillis());
        }

        if (config.getNumTestsPerEvictionRun() != null) {
            poolConfig.setNumTestsPerEvictionRun(config.getNumTestsPerEvictionRun());
        }

        if (config.getMaxTotal() != null) {
            poolConfig.setMaxTotal(config.getMaxTotal());
        }

        if (config.getMaxIdle() != null) {
            poolConfig.setMaxIdle(config.getMaxIdle());
        }

        if (config.getMinIdle() != null) {
            poolConfig.setMinIdle(config.getMinIdle());
        }

        if (config.getMaxWaitMillis() != null) {
            poolConfig.setMaxWaitMillis(config.getMaxWaitMillis());
        }

        this.jedisPool = newJedisPool(poolConfig, host, port, timeout, password, database, clientName);
    }

    public static JedisPool newJedisPool(JedisPoolConfig jedisPoolConfig, String host, Integer port, Integer timeout, String password, Integer database, String clientName) {
        JedisPool jedisPool;
        if (port != null && timeout != null && password != null && database != null && clientName != null)
            jedisPool = new JedisPool(jedisPoolConfig, host, port, timeout, password, database, clientName);
        else if (port != null && timeout != null && password != null && database != null)
            jedisPool = new JedisPool(jedisPoolConfig, host, port, timeout, password, database);
        else if (port != null && timeout != null && password != null)
            jedisPool = new JedisPool(jedisPoolConfig, host, port, timeout, password);
        else if (port != null && timeout != null)
            jedisPool = new JedisPool(jedisPoolConfig, host, port, timeout);
        else if (port != null)
            jedisPool = new JedisPool(jedisPoolConfig, host, port);
        else
            jedisPool = new JedisPool(jedisPoolConfig, host);

        return jedisPool;
    }

    public Jedis(JedisPool jedisPool) {
        super(null);
        this.jedisPool = jedisPool;
    }

    @Override
    public String set(Object key, Object value) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.set(keyToBytes(key), valueToBytes(value));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long setnx(Object key, Object value) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.setnx(keyToBytes(key), valueToBytes(value));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public String setWithoutSerialize(Object key, Object value) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.set(keyToBytes(key), value.toString().getBytes());
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public String setex(Object key, int seconds, Object value) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.setex(keyToBytes(key), seconds, valueToBytes(value));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return (T) valueFromBytes(jedis.get(keyToBytes(key)));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public String getWithoutSerialize(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            byte[] bytes = jedis.get(keyToBytes(key));
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            return new String(bytes);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long del(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.del(keyToBytes(key));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long del(Object... keys) {
        if (keys == null || keys.length == 0) {
            return 0L;
        }
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.del(keysToBytesArray(keys));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Set<String> keys(String pattern) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.keys(pattern);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public String mset(Object... keysValues) {
        if (keysValues.length % 2 != 0) {
            throw new IllegalArgumentException("wrong number of arguments for met, keysValues length can not be odd");
        }
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            byte[][] kv = new byte[keysValues.length][];
            for (int i = 0; i < keysValues.length; i++) {
                if (i % 2 == 0) {
                    kv[i] = keyToBytes(keysValues[i]);
                } else {
                    kv[i] = valueToBytes(keysValues[i]);
                }
            }
            return jedis.mset(kv);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List mget(Object... keys) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            byte[][] keysBytesArray = keysToBytesArray(keys);
            List<byte[]> data = jedis.mget(keysBytesArray);
            return valueListFromBytesList(data);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long decr(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.decr(keyToBytes(key));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long decrBy(Object key, long longValue) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.decrBy(keyToBytes(key), longValue);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long incr(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.incr(keyToBytes(key));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long incrBy(Object key, long longValue) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.incrBy(keyToBytes(key), longValue);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public boolean exists(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.exists(keyToBytes(key));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public String randomKey() {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.randomKey();
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public String rename(Object oldkey, Object newkey) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.rename(keyToBytes(oldkey), keyToBytes(newkey));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long move(Object key, int dbIndex) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.move(keyToBytes(key), dbIndex);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public String migrate(String host, int port, Object key, int destinationDb, int timeout) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.migrate(host, port, keyToBytes(key), destinationDb, timeout);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public String select(int databaseIndex) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.select(databaseIndex);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long expire(Object key, int seconds) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.expire(keyToBytes(key), seconds);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long expireAt(Object key, long unixTime) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.expireAt(keyToBytes(key), unixTime);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long pexpire(Object key, long milliseconds) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.pexpire(keyToBytes(key), milliseconds);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long pexpireAt(Object key, long millisecondsTimestamp) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.pexpireAt(keyToBytes(key), millisecondsTimestamp);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getSet(Object key, Object value) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return (T) valueFromBytes(jedis.getSet(keyToBytes(key), valueToBytes(value)));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long persist(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.persist(keyToBytes(key));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public String type(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.type(keyToBytes(key));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long ttl(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.ttl(keyToBytes(key));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long pttl(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.pttl(keyToBytes(key));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long objectRefcount(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.objectRefcount(keyToBytes(key));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long objectIdletime(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.objectIdletime(keyToBytes(key));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long hset(Object key, Object field, Object value) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.hset(keyToBytes(key), valueToBytes(field), valueToBytes(value));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public String hmset(Object key, Map<Object, Object> hash) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            Map<byte[], byte[]> para = new HashMap<byte[], byte[]>();
            for (Entry<Object, Object> e : hash.entrySet())
                para.put(valueToBytes(e.getKey()), valueToBytes(e.getValue()));
            return jedis.hmset(keyToBytes(key), para);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T hget(Object key, Object field) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return (T) valueFromBytes(jedis.hget(keyToBytes(key), valueToBytes(field)));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List hmget(Object key, Object... fields) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            List<byte[]> data = jedis.hmget(keyToBytes(key), valuesToBytesArray(fields));
            return valueListFromBytesList(data);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long hdel(Object key, Object... fields) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.hdel(keyToBytes(key), valuesToBytesArray(fields));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public boolean hexists(Object key, Object field) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.hexists(keyToBytes(key), valueToBytes(field));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Map hgetAll(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            Map<byte[], byte[]> data = jedis.hgetAll(keyToBytes(key));
            Map<Object, Object> result = new HashMap<Object, Object>();
            for (Entry<byte[], byte[]> e : data.entrySet())
                result.put(valueFromBytes(e.getKey()), valueFromBytes(e.getValue()));
            return result;
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List hvals(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            List<byte[]> data = jedis.hvals(keyToBytes(key));
            return valueListFromBytesList(data);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Set<Object> hkeys(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            Set<byte[]> fieldSet = jedis.hkeys(keyToBytes(key));
            Set<Object> result = new HashSet<Object>();
            fieldSetFromBytesSet(fieldSet, result);
            return result;
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long hlen(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.hlen(keyToBytes(key));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long hincrBy(Object key, Object field, long value) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.hincrBy(keyToBytes(key), valueToBytes(field), value);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Double hincrByFloat(Object key, Object field, double value) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.hincrByFloat(keyToBytes(key), valueToBytes(field), value);
        } finally {
            returnResource(jedis);
        }
    }

    @SuppressWarnings("unchecked")

    @Override
    public <T> T lindex(Object key, long index) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return (T) valueFromBytes(jedis.lindex(keyToBytes(key), index));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long llen(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.llen(keyToBytes(key));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T lpop(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return (T) valueFromBytes(jedis.lpop(keyToBytes(key)));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long lpush(Object key, Object... values) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.lpush(keyToBytes(key), valuesToBytesArray(values));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public String lset(Object key, long index, Object value) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.lset(keyToBytes(key), index, valueToBytes(value));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long lrem(Object key, long count, Object value) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.lrem(keyToBytes(key), count, valueToBytes(value));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List lrange(Object key, long start, long end) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            List<byte[]> data = jedis.lrange(keyToBytes(key), start, end);
            if (data != null) {
                return valueListFromBytesList(data);
            } else {
                return new ArrayList<byte[]>(0);
            }
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public String ltrim(Object key, long start, long end) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.ltrim(keyToBytes(key), start, end);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T rpop(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return (T) valueFromBytes(jedis.rpop(keyToBytes(key)));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T rpoplpush(Object srcKey, Object dstKey) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return (T) valueFromBytes(jedis.rpoplpush(keyToBytes(srcKey), keyToBytes(dstKey)));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long rpush(Object key, Object... values) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.rpush(keyToBytes(key), valuesToBytesArray(values));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List blpop(Object... keys) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            List<byte[]> data = jedis.blpop(keysToBytesArray(keys));
            return valueListFromBytesList(data);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List blpop(Integer timeout, Object... keys) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {

            List<byte[]> data = jedis.blpop(timeout, keysToBytesArray(keys));

            if (data != null && data.size() == 2) {
                List<Object> objects = new ArrayList<>();
                objects.add(new String(data.get(0)));
                objects.add(valueFromBytes(data.get(1)));
                return objects;
            }

            return null;
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List brpop(Object... keys) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            List<byte[]> data = jedis.brpop(keysToBytesArray(keys));
            return valueListFromBytesList(data);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List brpop(Integer timeout, Object... keys) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            List<byte[]> data = jedis.brpop(timeout, keysToBytesArray(keys));
            return valueListFromBytesList(data);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public String ping() {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.ping();
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long sadd(Object key, Object... members) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.sadd(keyToBytes(key), valuesToBytesArray(members));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long scard(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.scard(keyToBytes(key));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T spop(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return (T) valueFromBytes(jedis.spop(keyToBytes(key)));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Set smembers(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            Set<byte[]> data = jedis.smembers(keyToBytes(key));
            Set<Object> result = new HashSet<Object>();
            valueSetFromBytesSet(data, result);
            return result;
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public boolean sismember(Object key, Object member) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.sismember(keyToBytes(key), valueToBytes(member));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Set sinter(Object... keys) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            Set<byte[]> data = jedis.sinter(keysToBytesArray(keys));
            Set<Object> result = new HashSet<Object>();
            valueSetFromBytesSet(data, result);
            return result;
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T srandmember(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return (T) valueFromBytes(jedis.srandmember(keyToBytes(key)));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List srandmember(Object key, int count) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            List<byte[]> data = jedis.srandmember(keyToBytes(key), count);
            return valueListFromBytesList(data);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long srem(Object key, Object... members) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.srem(keyToBytes(key), valuesToBytesArray(members));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Set sunion(Object... keys) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            Set<byte[]> data = jedis.sunion(keysToBytesArray(keys));
            Set<Object> result = new HashSet<Object>();
            valueSetFromBytesSet(data, result);
            return result;
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Set sdiff(Object... keys) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            Set<byte[]> data = jedis.sdiff(keysToBytesArray(keys));
            Set<Object> result = new HashSet<Object>();
            valueSetFromBytesSet(data, result);
            return result;
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long zadd(Object key, double score, Object member) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.zadd(keyToBytes(key), score, valueToBytes(member));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long zadd(Object key, Map<Object, Double> scoreMembers) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            Map<byte[], Double> para = new HashMap<>();
            for (Entry<Object, Double> e : scoreMembers.entrySet()) {
                para.put(valueToBytes(e.getKey()), e.getValue());    
            }
            return jedis.zadd(keyToBytes(key), para);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long zcard(Object key) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.zcard(keyToBytes(key));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long zcount(Object key, double min, double max) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.zcount(keyToBytes(key), min, max);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Double zincrby(Object key, double score, Object member) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.zincrby(keyToBytes(key), score, valueToBytes(member));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Set zrange(Object key, long start, long end) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            Set<byte[]> data = jedis.zrange(keyToBytes(key), start, end);
            Set<Object> result = new LinkedHashSet<Object>();    
            valueSetFromBytesSet(data, result);
            return result;
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Set zrevrange(Object key, long start, long end) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            Set<byte[]> data = jedis.zrevrange(keyToBytes(key), start, end);
            Set<Object> result = new LinkedHashSet<Object>();    
            valueSetFromBytesSet(data, result);
            return result;
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Set zrangeByScore(Object key, double min, double max) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            Set<byte[]> data = jedis.zrangeByScore(keyToBytes(key), min, max);
            Set<Object> result = new LinkedHashSet<Object>();    
            valueSetFromBytesSet(data, result);
            return result;
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long zrank(Object key, Object member) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.zrank(keyToBytes(key), valueToBytes(member));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long zrevrank(Object key, Object member) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.zrevrank(keyToBytes(key), valueToBytes(member));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Long zrem(Object key, Object... members) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.zrem(keyToBytes(key), valuesToBytesArray(members));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public Double zscore(Object key, Object member) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            return jedis.zscore(keyToBytes(key), valueToBytes(member));
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public void publish(String channel, String message) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            jedis.publish(channel, message);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public void publish(byte[] channel, byte[] message) {
        redis.clients.jedis.Jedis jedis = getJedis();
        try {
            jedis.publish(channel, message);
        } finally {
            returnResource(jedis);
        }
    }

    @Override
    public void subscribe(JedisPubSub listener, final String... channels) {
        
        new Thread("jboot-redis-subscribe-JedisPubSub") {
            @Override
            public void run() {
                while (true) {
                    redis.clients.jedis.Jedis jedis = getJedis();
                    try {
                        
                        jedis.subscribe(listener, channels);
                        logger.warn("Disconnect to redis channels : " + Arrays.toString(channels));
                        break;
                    } catch (JedisConnectionException e) {
                        logger.error(e, "Failed connect to redis, reconnect it.");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    } finally {
                        returnResource(jedis);
                    }
                }

            }
        }.start();
    }

    @Override
    public void subscribe(BinaryJedisPubSub binaryListener, final byte[]... channels) {
        
        new Thread("jboot-redis-subscribe-BinaryJedisPubSub") {
            @Override
            public void run() {
                
                while (!isClose()) {
                    redis.clients.jedis.Jedis jedis = null;
                    try {
                        jedis = jedisPool.getResource();
                        
                        jedis.subscribe(binaryListener, channels);
                        logger.warn("Disconnect to redis channel in subscribe binaryListener!");
                        break;
                    } catch (Throwable e) {
                        logger.error(e,"Failed connect to redis, reconnect it.");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    } finally {
                        if (jedis != null) {
                            returnResource(jedis);
                        }
                    }
                }
            }
        }.start();
    }

    @Override
    public RedisScanResult scan(String pattern, String cursor, int scanCount) {
        ScanParams params = new ScanParams();
        params.match(pattern).count(scanCount);
        try (redis.clients.jedis.Jedis jedis = getJedis()) {
            ScanResult<String> scanResult = jedis.scan(cursor, params);
            return scanResult == null ? null : new RedisScanResult(scanResult.getCursor(), scanResult.getResult());
        }
    }

    public redis.clients.jedis.Jedis getJedis() {
        try {
            return jedisPool.getResource();
        } catch (JedisConnectionException e) {
            throw new JedisException("can not connect to redis host  " + config.getHost() + ":" + config.getPort() + " ," +
                    " cause : " + e.toString(), e);
        }
    }

    public void returnResource(redis.clients.jedis.Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }

}

