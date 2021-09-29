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

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
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

public class JedisCluster extends BaseRedis {
    private static final Log logger = LogFactory.getLog(JedisCluster.class);

    protected redis.clients.jedis.JedisCluster jedisCluster;
    private int timeout = 2000;

    public JedisCluster(RedisConfig config) {

        super(config);

        Integer timeout = config.getTimeout();
        String password = config.getPassword();
        Integer maxAttempts = config.getMaxAttempts();

        if (timeout != null) {
            this.timeout = timeout;
        }

        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();

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
        this.jedisCluster = newJedisCluster(config.getHostAndPorts(), timeout, maxAttempts, password, poolConfig);
    }

    public static redis.clients.jedis.JedisCluster newJedisCluster(Set<HostAndPort> haps, Integer timeout,
                                                                   Integer maxAttempts, String password, GenericObjectPoolConfig poolConfig) {
        redis.clients.jedis.JedisCluster jedisCluster;

        if (timeout != null && maxAttempts != null && password != null && poolConfig != null) {
            jedisCluster = new redis.clients.jedis.JedisCluster(haps, timeout, timeout, maxAttempts, password, poolConfig);
        } else if (timeout != null && maxAttempts != null && poolConfig != null) {
            jedisCluster = new redis.clients.jedis.JedisCluster(haps, timeout, maxAttempts, poolConfig);
        } else if (timeout != null && maxAttempts != null) {
            jedisCluster = new redis.clients.jedis.JedisCluster(haps, timeout, maxAttempts);
        } else if (timeout != null && poolConfig != null) {
            jedisCluster = new redis.clients.jedis.JedisCluster(haps, timeout, poolConfig);
        } else if (timeout != null) {
            jedisCluster = new redis.clients.jedis.JedisCluster(haps, timeout);
        } else {
            jedisCluster = new redis.clients.jedis.JedisCluster(haps);
        }
        return jedisCluster;
    }

    public JedisCluster(redis.clients.jedis.JedisCluster jedisCluster) {
        super(null);
        this.jedisCluster = jedisCluster;
    }

    @Override
    public String set(Object key, Object value) {
        return jedisCluster.set(keyToBytes(key), valueToBytes(value));
    }

    @Override
    public Long setnx(Object key, Object value) {
        return jedisCluster.setnx(keyToBytes(key), valueToBytes(value));
    }

    public String setWithoutSerialize(Object key, Object value) {
        return jedisCluster.set(keyToBytes(key), value.toString().getBytes());
    }

    public String setex(Object key, int seconds, Object value) {

        return jedisCluster.setex(keyToBytes(key), seconds, valueToBytes(value));

    }

    @SuppressWarnings("unchecked")
    public <T> T get(Object key) {

        return (T) valueFromBytes(jedisCluster.get(keyToBytes(key)));

    }

    @Override
    public String getWithoutSerialize(Object key) {
        byte[] bytes = jedisCluster.get(keyToBytes(key));
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return new String(jedisCluster.get(keyToBytes(key)));
    }

    public Long del(Object key) {
        return jedisCluster.del(keyToBytes(key));
    }

    public Long del(Object... keys) {

        return jedisCluster.del(keysToBytesArray(keys));

    }

    public Set<String> keys(String pattern) {
        HashSet<String> keys = new HashSet<>();
        Map<String, JedisPool> clusterNodes = jedisCluster.getClusterNodes();
        for (String k : clusterNodes.keySet()) {
            JedisPool jp = clusterNodes.get(k);
            var jedis = jp.getResource();
            try {
                keys.addAll(jedis.keys(pattern));
            } catch (Exception e) {
                logger.error(e, e.toString());
            } finally {
                jedis.close(); 
            }
        }
        return keys;
    }

    public String mset(Object... keysValues) {
        if (keysValues.length % 2 != 0)
            throw new IllegalArgumentException("wrong number of arguments for met, keysValues length can not be odd");

        byte[][] kv = new byte[keysValues.length][];
        for (int i = 0; i < keysValues.length; i++) {
            if (i % 2 == 0)
                kv[i] = keyToBytes(keysValues[i]);
            else
                kv[i] = valueToBytes(keysValues[i]);
        }
        return jedisCluster.mset(kv);

    }

    @SuppressWarnings("rawtypes")
    public List mget(Object... keys) {

        byte[][] keysBytesArray = keysToBytesArray(keys);
        List<byte[]> data = jedisCluster.mget(keysBytesArray);
        return valueListFromBytesList(data);

    }

    public Long decr(Object key) {

        return jedisCluster.decr(keyToBytes(key));

    }

    public Long decrBy(Object key, long longValue) {

        return jedisCluster.decrBy(keyToBytes(key), longValue);

    }

    public Long incr(Object key) {

        return jedisCluster.incr(keyToBytes(key));

    }

    public Long incrBy(Object key, long longValue) {
        return jedisCluster.incrBy(keyToBytes(key), longValue);

    }

    public boolean exists(Object key) {

        return jedisCluster.exists(keyToBytes(key));

    }

    public String randomKey() {

        throw new JedisException("randomKey command is not supported in redis cluster.");

    }

    public String rename(Object oldkey, Object newkey) {

        return jedisCluster.rename(keyToBytes(oldkey), keyToBytes(newkey));

    }

    public Long move(Object key, int dbIndex) {

        throw new JedisException("move command is not supported in redis cluster.");

    }

    public String migrate(String host, int port, Object key, int destinationDb, int timeout) {

        throw new JedisException("migrate command is not supported in redis cluster.");

    }

    public String select(int databaseIndex) {
        throw new JedisException("select is deprecated in JedisCluster");
    }

    public Long expire(Object key, int seconds) {

        return jedisCluster.expire(keyToBytes(key), seconds);

    }

    public Long expireAt(Object key, long unixTime) {

        return jedisCluster.expireAt(keyToBytes(key), unixTime);

    }

    public Long pexpire(Object key, long milliseconds) {

        return jedisCluster.pexpire(keyToBytes(key), milliseconds);

    }

    public Long pexpireAt(Object key, long millisecondsTimestamp) {

        return jedisCluster.pexpireAt(keyToBytes(key), millisecondsTimestamp);

    }

    @SuppressWarnings("unchecked")
    public <T> T getSet(Object key, Object value) {

        return (T) valueFromBytes(jedisCluster.getSet(keyToBytes(key), valueToBytes(value)));

    }

    public Long persist(Object key) {

        return jedisCluster.persist(keyToBytes(key));

    }

    public String type(Object key) {

        return jedisCluster.type(keyToBytes(key));

    }

    public Long ttl(Object key) {

        return jedisCluster.ttl(keyToBytes(key));

    }

    public Long pttl(Object key) {

        return jedisCluster.pttl(key.toString());

    }

    public Long objectRefcount(Object key) {

        throw new JedisException("objectRefcount is not supported in redis cluster.");
    }

    public Long objectIdletime(Object key) {

        throw new JedisException("objectIdletime is not supported in redis cluster.");

    }

    public Long hset(Object key, Object field, Object value) {

        return jedisCluster.hset(keyToBytes(key), valueToBytes(field), valueToBytes(value));

    }

    public String hmset(Object key, Map<Object, Object> hash) {

        Map<byte[], byte[]> para = new HashMap<byte[], byte[]>();
        for (Entry<Object, Object> e : hash.entrySet())
            para.put(valueToBytes(e.getKey()), valueToBytes(e.getValue()));
        return jedisCluster.hmset(keyToBytes(key), para);

    }

    @SuppressWarnings("unchecked")
    public <T> T hget(Object key, Object field) {

        return (T) valueFromBytes(jedisCluster.hget(keyToBytes(key), valueToBytes(field)));

    }

    @SuppressWarnings("rawtypes")
    public List hmget(Object key, Object... fields) {

        List<byte[]> data = jedisCluster.hmget(keyToBytes(key), valuesToBytesArray(fields));
        return valueListFromBytesList(data);

    }

    public Long hdel(Object key, Object... fields) {

        return jedisCluster.hdel(keyToBytes(key), valuesToBytesArray(fields));

    }

    public boolean hexists(Object key, Object field) {

        return jedisCluster.hexists(keyToBytes(key), valueToBytes(field));

    }

    @SuppressWarnings("rawtypes")
    public Map hgetAll(Object key) {

        Map<byte[], byte[]> data = jedisCluster.hgetAll(keyToBytes(key));
        Map<Object, Object> result = new HashMap<Object, Object>();
        for (Entry<byte[], byte[]> e : data.entrySet())
            result.put(valueFromBytes(e.getKey()), valueFromBytes(e.getValue()));
        return result;

    }

    @SuppressWarnings("rawtypes")
    public List hvals(Object key) {

        Collection<byte[]> data = jedisCluster.hvals(keyToBytes(key));
        return valueListFromBytesList(data);

    }

    public Set<Object> hkeys(Object key) {

        Set<byte[]> fieldSet = jedisCluster.hkeys(keyToBytes(key));
        Set<Object> result = new HashSet<Object>();
        fieldSetFromBytesSet(fieldSet, result);
        return result;

    }

    public Long hlen(Object key) {

        return jedisCluster.hlen(keyToBytes(key));

    }

    public Long hincrBy(Object key, Object field, long value) {

        return jedisCluster.hincrBy(keyToBytes(key), valueToBytes(field), value);

    }

    public Double hincrByFloat(Object key, Object field, double value) {

        return jedisCluster.hincrByFloat(keyToBytes(key), valueToBytes(field), value);

    }

    @SuppressWarnings("unchecked")

    public <T> T lindex(Object key, long index) {

        return (T) valueFromBytes(jedisCluster.lindex(keyToBytes(key), index));

    }

    public Long llen(Object key) {

        return jedisCluster.llen(keyToBytes(key));

    }

    @SuppressWarnings("unchecked")
    public <T> T lpop(Object key) {

        return (T) valueFromBytes(jedisCluster.lpop(keyToBytes(key)));

    }

    public Long lpush(Object key, Object... values) {

        return jedisCluster.lpush(keyToBytes(key), valuesToBytesArray(values));

    }

    public String lset(Object key, long index, Object value) {

        return jedisCluster.lset(keyToBytes(key), index, valueToBytes(value));

    }

    public Long lrem(Object key, long count, Object value) {

        return jedisCluster.lrem(keyToBytes(key), count, valueToBytes(value));

    }

    @SuppressWarnings("rawtypes")
    public List lrange(Object key, long start, long end) {

        List<byte[]> data = jedisCluster.lrange(keyToBytes(key), start, end);
        if (data != null) {
            return valueListFromBytesList(data);
        } else {
            return new ArrayList<byte[]>(0);
        }

    }

    public String ltrim(Object key, long start, long end) {

        return jedisCluster.ltrim(keyToBytes(key), start, end);

    }

    @SuppressWarnings("unchecked")
    public <T> T rpop(Object key) {

        return (T) valueFromBytes(jedisCluster.rpop(keyToBytes(key)));

    }

    @SuppressWarnings("unchecked")
    public <T> T rpoplpush(Object srcKey, Object dstKey) {

        return (T) valueFromBytes(jedisCluster.rpoplpush(keyToBytes(srcKey), keyToBytes(dstKey)));

    }

    public Long rpush(Object key, Object... values) {

        return jedisCluster.rpush(keyToBytes(key), valuesToBytesArray(values));

    }

    @SuppressWarnings("rawtypes")
    public List blpop(Object... keys) {

        List<byte[]> data = jedisCluster.blpop(timeout, keysToBytesArray(keys));

        if (data != null && data.size() == 2) {
            List<Object> objects = new ArrayList<>();
            objects.add(new String(data.get(0)));
            objects.add(valueFromBytes(data.get(1)));
            return objects;
        }

        return valueListFromBytesList(data);

    }

    @SuppressWarnings("rawtypes")
    public List blpop(Integer timeout, Object... keys) {

        List<byte[]> data = jedisCluster.blpop(timeout, keysToBytesArray(keys));
        return valueListFromBytesList(data);

    }

    @SuppressWarnings("rawtypes")
    public List brpop(Object... keys) {

        List<byte[]> data = jedisCluster.brpop(timeout, keysToBytesArray(keys));
        return valueListFromBytesList(data);

    }

    @SuppressWarnings("rawtypes")
    public List brpop(Integer timeout, Object... keys) {

        List<byte[]> data = jedisCluster.brpop(timeout, keysToBytesArray(keys));
        return valueListFromBytesList(data);

    }

    public String ping() {
        throw new JedisException("ping is deprecated in JedisCluster");
    }

    public Long sadd(Object key, Object... members) {

        return jedisCluster.sadd(keyToBytes(key), valuesToBytesArray(members));

    }

    public Long scard(Object key) {

        return jedisCluster.scard(keyToBytes(key));

    }

    @SuppressWarnings("unchecked")
    public <T> T spop(Object key) {

        return (T) valueFromBytes(jedisCluster.spop(keyToBytes(key)));

    }

    @SuppressWarnings("rawtypes")
    public Set smembers(Object key) {

        Set<byte[]> data = jedisCluster.smembers(keyToBytes(key));
        Set<Object> result = new HashSet<Object>();
        valueSetFromBytesSet(data, result);
        return result;

    }

    public boolean sismember(Object key, Object member) {

        return jedisCluster.sismember(keyToBytes(key), valueToBytes(member));

    }

    @SuppressWarnings("rawtypes")
    public Set sinter(Object... keys) {

        Set<byte[]> data = jedisCluster.sinter(keysToBytesArray(keys));
        Set<Object> result = new HashSet<Object>();
        valueSetFromBytesSet(data, result);
        return result;

    }

    @SuppressWarnings("unchecked")
    public <T> T srandmember(Object key) {

        return (T) valueFromBytes(jedisCluster.srandmember(keyToBytes(key)));

    }

    @SuppressWarnings("rawtypes")
    public List srandmember(Object key, int count) {

        List<byte[]> data = jedisCluster.srandmember(keyToBytes(key), count);
        return valueListFromBytesList(data);

    }

    public Long srem(Object key, Object... members) {

        return jedisCluster.srem(keyToBytes(key), valuesToBytesArray(members));

    }

    @SuppressWarnings("rawtypes")
    public Set sunion(Object... keys) {

        Set<byte[]> data = jedisCluster.sunion(keysToBytesArray(keys));
        Set<Object> result = new HashSet<Object>();
        valueSetFromBytesSet(data, result);
        return result;

    }

    @SuppressWarnings("rawtypes")
    public Set sdiff(Object... keys) {

        Set<byte[]> data = jedisCluster.sdiff(keysToBytesArray(keys));
        Set<Object> result = new HashSet<Object>();
        valueSetFromBytesSet(data, result);
        return result;

    }

    public Long zadd(Object key, double score, Object member) {

        return jedisCluster.zadd(keyToBytes(key), score, valueToBytes(member));

    }

    public Long zadd(Object key, Map<Object, Double> scoreMembers) {

        Map<byte[], Double> para = new HashMap<byte[], Double>();
        for (Entry<Object, Double> e : scoreMembers.entrySet())
            para.put(valueToBytes(e.getKey()), e.getValue());    
        return jedisCluster.zadd(keyToBytes(key), para);

    }

    public Long zcard(Object key) {

        return jedisCluster.zcard(keyToBytes(key));

    }

    public Long zcount(Object key, double min, double max) {

        return jedisCluster.zcount(keyToBytes(key), min, max);

    }

    public Double zincrby(Object key, double score, Object member) {

        return jedisCluster.zincrby(keyToBytes(key), score, valueToBytes(member));

    }

    @SuppressWarnings("rawtypes")
    public Set zrange(Object key, long start, long end) {

        Set<byte[]> data = jedisCluster.zrange(keyToBytes(key), start, end);
        Set<Object> result = new LinkedHashSet<Object>();    
        valueSetFromBytesSet(data, result);
        return result;

    }

    @SuppressWarnings("rawtypes")
    public Set zrevrange(Object key, long start, long end) {

        Set<byte[]> data = jedisCluster.zrevrange(keyToBytes(key), start, end);
        Set<Object> result = new LinkedHashSet<Object>();    
        valueSetFromBytesSet(data, result);
        return result;

    }

    @SuppressWarnings("rawtypes")
    public Set zrangeByScore(Object key, double min, double max) {

        Set<byte[]> data = jedisCluster.zrangeByScore(keyToBytes(key), min, max);
        Set<Object> result = new LinkedHashSet<Object>();    
        valueSetFromBytesSet(data, result);
        return result;

    }

    public Long zrank(Object key, Object member) {

        return jedisCluster.zrank(keyToBytes(key), valueToBytes(member));

    }

    public Long zrevrank(Object key, Object member) {

        return jedisCluster.zrevrank(keyToBytes(key), valueToBytes(member));

    }

    public Long zrem(Object key, Object... members) {

        return jedisCluster.zrem(keyToBytes(key), valuesToBytesArray(members));

    }

    public Double zscore(Object key, Object member) {

        return jedisCluster.zscore(keyToBytes(key), valueToBytes(member));

    }

    public void publish(String channel, String message) {

        jedisCluster.publish(channel, message);

    }

    public void publish(byte[] channel, byte[] message) {

        jedisCluster.publish(channel, message);

    }

    public void subscribe(JedisPubSub listener, final String... channels) {
        
        new Thread("redisCluster-subscribe-JedisPubSub") {
            @Override
            public void run() {
                while (true) {
                    
                    try {
                        jedisCluster.subscribe(listener, channels);
                        logger.warn("Disconnect to redis channel in subscribe JedisPubSub!");
                        break;
                    } catch (JedisConnectionException e) {
                        logger.error(e, "failed connect to redis, reconnect it.");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    }
                }
            }
        }.start();
    }

    public void subscribe(BinaryJedisPubSub binaryListener, final byte[]... channels) {
        
        new Thread("jboot-redisCluster-subscribe-BinaryJedisPubSub") {
            @Override
            public void run() {
                while (!isClose()) {
                    
                    try {
                        jedisCluster.subscribe(binaryListener, channels);
                        logger.warn("Disconnect to redis channel in subscribe BinaryJedisPubSub!");
                        break;
                    } catch (Throwable e) {
                        logger.error(e, "failed connect to redis, reconnect it.");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            break;
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
        ScanResult<String> scanResult = jedisCluster.scan(cursor, params);
        return scanResult == null ? null : new RedisScanResult(scanResult.getCursor(),scanResult.getResult());
    }

    public redis.clients.jedis.JedisCluster getJedisCluster() {
        return jedisCluster;
    }

}

