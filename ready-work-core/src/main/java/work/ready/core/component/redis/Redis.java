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
package work.ready.core.component.redis;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.JedisPubSub;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Redis {

    public String set(Object key, Object value);

    public Long setnx(Object key, Object value);

    public String setWithoutSerialize(Object key, Object value);

    public String setex(Object key, int seconds, Object value);

    @SuppressWarnings("unchecked")
    public <T> T get(Object key);

    public String getWithoutSerialize(Object key);

    public Long del(Object key);

    public Long del(Object... keys);

    public Set<String> keys(String pattern);

    public String mset(Object... keysValues);

    @SuppressWarnings("rawtypes")
    public List mget(Object... keys);

    public Long decr(Object key);

    public Long decrBy(Object key, long value);

    public Long incr(Object key);

    public Long incrBy(Object key, long value);

    public boolean exists(Object key);

    public String randomKey();

    public String rename(Object oldkey, Object newkey);

    public Long move(Object key, int dbIndex);

    public String migrate(String host, int port, Object key, int destinationDb, int timeout);

    public String select(int databaseIndex);

    public Long expire(Object key, int seconds);

    public Long expireAt(Object key, long unixTime);

    public Long pexpire(Object key, long milliseconds);

    public Long pexpireAt(Object key, long millisecondsTimestamp);

    public <T> T getSet(Object key, Object value);

    public Long persist(Object key);

    public String type(Object key);

    public Long ttl(Object key);

    public Long pttl(Object key);

    public Long objectRefcount(Object key);

    public Long objectIdletime(Object key);

    public Long hset(Object key, Object field, Object value);

    public String hmset(Object key, Map<Object, Object> hash);

    public <T> T hget(Object key, Object field);

    public List hmget(Object key, Object... fields);

    public Long hdel(Object key, Object... fields);

    public boolean hexists(Object key, Object field);

    public Map hgetAll(Object key);

    @SuppressWarnings("rawtypes")
    public List hvals(Object key);

    public Set<Object> hkeys(Object key);

    public Long hlen(Object key);

    public Long hincrBy(Object key, Object field, long value);

    public Double hincrByFloat(Object key, Object field, double value);

    public <T> T lindex(Object key, long index);

    public Long llen(Object key);

    @SuppressWarnings("unchecked")
    public <T> T lpop(Object key);

    public Long lpush(Object key, Object... values);

    public String lset(Object key, long index, Object value);

    public Long lrem(Object key, long count, Object value);

    public List lrange(Object key, long start, long end);

    public String ltrim(Object key, long start, long end);

    public <T> T rpop(Object key);

    @SuppressWarnings("unchecked")
    public <T> T rpoplpush(Object srcKey, Object dstKey);

    public Long rpush(Object key, Object... values);

    public List blpop(Object... keys);

    public List blpop(Integer timeout, Object... keys);

    public List brpop(Object... keys);

    public List brpop(Integer timeout, Object... keys);

    public String ping();

    public Long sadd(Object key, Object... members);

    public Long scard(Object key);

    public <T> T spop(Object key);

    public Set smembers(Object key);

    public boolean sismember(Object key, Object member);

    public Set sinter(Object... keys);

    public <T> T srandmember(Object key);

    public List srandmember(Object key, int count);

    public Long srem(Object key, Object... members);

    public Set sunion(Object... keys);

    public Set sdiff(Object... keys);

    public Long zadd(Object key, double score, Object member);

    public Long zadd(Object key, Map<Object, Double> scoreMembers);

    public Long zcard(Object key);

    public Long zcount(Object key, double min, double max);

    public Double zincrby(Object key, double score, Object member);

    public Set zrange(Object key, long start, long end);

    public Set zrevrange(Object key, long start, long end);

    public Set zrangeByScore(Object key, double min, double max);

    public Long zrank(Object key, Object member);

    public Long zrevrank(Object key, Object member);

    public Long zrem(Object key, Object... members);

    public Double zscore(Object key, Object member);

    public void publish(String channel, String message);

    public void publish(byte[] channel, byte[] message);

    public void subscribe(JedisPubSub listener, final String... channels);

    public void subscribe(BinaryJedisPubSub binaryListener, final byte[]... channels);

    public RedisScanResult scan(String pattern, String cursor, int scanCount);

    public byte[] keyToBytes(Object key);

    public String bytesToKey(byte[] bytes);

    public byte[][] keysToBytesArray(Object... keys);

    public void fieldSetFromBytesSet(Set<byte[]> data, Set<Object> result);

    public byte[] valueToBytes(Object value);

    public Object valueFromBytes(byte[] bytes);

    public byte[][] valuesToBytesArray(Object... valuesArray);

    public void valueSetFromBytesSet(Set<byte[]> data, Set<Object> result);

    public List valueListFromBytesList(Collection<byte[]> data);

}

