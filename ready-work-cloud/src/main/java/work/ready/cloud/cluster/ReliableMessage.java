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

package work.ready.cloud.cluster;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.ignite.IgniteMessaging;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.lang.IgniteBiPredicate;
import work.ready.cloud.ReadyCloud;
import work.ready.cloud.client.ClientConfig;
import work.ready.cloud.cluster.common.MessageBody;
import work.ready.cloud.cluster.common.MessageCmd;
import work.ready.cloud.cluster.common.MessageState;
import work.ready.cloud.cluster.common.MessageException;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.ShutdownHook;
import work.ready.core.server.Ready;
import work.ready.core.tools.define.CheckedSupplier;
import work.ready.core.tools.define.LambdaFinal;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class ReliableMessage {
    private static final Log logger = LogFactory.getLog(ReliableMessage.class);
    public static final String ReliableMessageChannel = "ReliableMessageChannel";
    public static final MessageBody DONE = new MessageBody("", "", "", MessageState.STATE_OK);

    private boolean messageLogger = ReadyCloud.getConfig().isReliableMessageLogger();
    private int cacheLiveSeconds = 60; 
    private int rpcTimeout = ReadyCloud.getConfig().getHttpClient().getTimeout() > 0 ? ReadyCloud.getConfig().getHttpClient().getTimeout() : ClientConfig.DEFAULT_TIMEOUT;
    private Duration defaultTimeout = Duration.ofMillis(ReadyCloud.getConfig().getReliableMessageTimeout()); 
    private final Map<String, List<Listener>> listenerMap = new ConcurrentHashMap<>(); 

    private final Map<String, Cache<String, Map<UUID, MessageBody>>> messageCache = new HashMap<>();
    private static final Map<String, ThreadPoolExecutor> channelPool = new HashMap<>();
    private static final int poolNumber = 5;
    private static final int threadNumber;
    private static final Map<String, Long> poolTaskCounter = new TreeMap<>();
    private static final ScheduledThreadPoolExecutor scheduler = (ScheduledThreadPoolExecutor)Executors.newScheduledThreadPool(100);
    static {
        threadNumber = Math.max(Runtime.getRuntime().availableProcessors() * 2, ReadyCloud.getConfig().getTransaction().getConcurrentLevel());
        scheduler.setRemoveOnCancelPolicy(true);
        
        Ready.shutdownHook.add(ShutdownHook.STAGE_6, (inMs)-> {
            channelPool.forEach((ch,pool)->pool.shutdown());
            scheduler.shutdown();
        });
    }

    public ReliableMessage() {

    }

    public boolean isMessageLogger() {
        return messageLogger;
    }

    public ReliableMessage setMessageLogger(boolean messageLogger) {
        this.messageLogger = messageLogger;
        return this;
    }

    public int getCacheLiveSeconds() {
        return cacheLiveSeconds;
    }

    public ReliableMessage setCacheLiveSeconds(int cacheLiveSeconds) {
        this.cacheLiveSeconds = cacheLiveSeconds;
        return this;
    }

    public Duration getDefaultTimeout() {
        return defaultTimeout;
    }

    public ReliableMessage setDefaultTimeout(Duration defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
        return this;
    }

    private static ExecutorService getPool(String channel) {
        ThreadPoolExecutor pool;
        List<String> possibleBlocked = new ArrayList<>();
        List<String> workWell = new ArrayList<>();
        synchronized (poolTaskCounter) {
            for (int i = 0; i < poolNumber; i++) {
                String poolName = channel + i;
                pool = channelPool.get(poolName);
                if (pool == null) {
                    synchronized (channelPool) {
                        pool = channelPool.get(poolName);
                        if (pool == null) {
                            
                            pool = (ThreadPoolExecutor)Executors.newCachedThreadPool(new CloudThreadFactory("ReliableMessage-" + poolName, 7));

                            channelPool.put(poolName, pool);
                        }
                    }
                }
                long completedCount = pool.getCompletedTaskCount();
                long taskCount = pool.getTaskCount();
                Long lastCount = poolTaskCounter.get(poolName);
                if (lastCount == null || completedCount > lastCount || pool.getActiveCount() == 0) {
                    workWell.add(poolName);
                } else if (taskCount > 0) {
                    possibleBlocked.add(poolName);
                }
                poolTaskCounter.put(poolName, completedCount);
            }
        }
        if(workWell.size() > 0) {
            return channelPool.get(workWell.get(ThreadLocalRandom.current().nextInt(workWell.size())));
        }
        if(possibleBlocked.size() == poolNumber) {
            return channelPool.get(poolTaskCounter.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).findFirst().get().getKey());
        } else {
            return channelPool.entrySet().stream().filter(entry->!possibleBlocked.contains(entry.getKey())).findAny().get().getValue();
        }
    }

    public ReliableMessage addListener(String channel, Listener listener) {
        synchronized (listenerMap) {
            List<Listener> listeners = listenerMap.computeIfAbsent(channel, c -> new ArrayList<>());
            listeners.add(listener);
            return this;
        }
    }

    private void insertListener(String channel, Listener listener) {
        synchronized (listenerMap) {
            List<Listener> listeners = listenerMap.computeIfAbsent(channel, c -> new ArrayList<>());
            listeners.add(0, listener);
        }
    }

    public void removeListener(String channel, Listener listener) {
        synchronized (listenerMap) {
            List<Listener> listeners = listenerMap.get(channel);
            if (listeners != null) {
                listeners.remove(listener);
            }
        }
    }

    private Cache<String, Map<UUID, MessageBody>> getOrCreateCache(String cacheName) {
        Cache<String, Map<UUID, MessageBody>> cache = messageCache.get(cacheName);
        if (cache == null) {
            synchronized (messageCache) {
                cache = messageCache.get(cacheName);
                if (cache == null) {
                    cache = Caffeine.newBuilder()
                            .expireAfterWrite(cacheLiveSeconds, TimeUnit.SECONDS)
                            .build();
                    messageCache.put(cacheName, cache);
                }
            }
        }
        return cache;
    }

    void listen(Cloud cloud) {
        IgniteBiPredicate<UUID, MessageCmd> messageListener = new IgniteBiPredicate<>() {
            @Override
            public boolean apply(UUID nodeId, MessageCmd cmdMessage) {
                if(messageLogger && logger.isTraceEnabled()) {
                    logger.trace("node received reliable message [msg=" + cmdMessage + ", from remote=" + nodeId + "]");
                }
                String sendToChannel = cmdMessage.getSendToChannel();
                String cacheKey = cmdMessage.getMessageId();
                cmdMessage.setNodeId(nodeId);
                var cache = getOrCreateCache(sendToChannel);
                var data = cache.getIfPresent(cacheKey);
                if(data != null) {
                    synchronized (data) {
                        data.put(nodeId, cmdMessage.getMessage());
                        data.notifyAll();
                    }
                }

                var listeners = listenerMap.get(sendToChannel);
                if (listeners != null) {
                    getPool(sendToChannel).execute(() -> {
                        MessageBody[] reply = new MessageBody[1];
                        for (Listener listener : listeners) { 
                            if (!listener.handle(cmdMessage, reply)) {
                                break;
                            }
                        }
                        if (reply[0] != null) {
                            MessageCmd replyMessage = new MessageCmd();
                            replyMessage.setNodeId(nodeId)
                                    .setSendToChannel(cmdMessage.getReceiveChannel()) 
                                    .setReceiveChannel(cmdMessage.getSendToChannel()) 
                                    .setMessageId(cmdMessage.getMessageId())
                                    .setMessage(reply[0]);
                            IgniteMessaging rmtMsg = Cloud.message(Cloud.cluster().forNodeId(nodeId));
                            rmtMsg.send(ReliableMessageChannel, replyMessage);
                        }
                    });
                }
                return true; 
            }
        };
        IgniteMessaging rmtMsg = Cloud.message();
        rmtMsg.localListen(ReliableMessageChannel, messageListener);
    }

    public void finalReply(UUID nodeId, String channel, String messageId) throws MessageException {
        finalReply(Cloud.cluster().forNodeId(nodeId), channel, messageId, DONE);
    }

    public void finalReply(UUID nodeId, String sendToChannel, String receiveChannel, String messageId) throws MessageException {
        finalReply(Cloud.cluster().forNodeId(nodeId), sendToChannel, receiveChannel, messageId, DONE);
    }

    public void finalReply(UUID nodeId, String channel, String messageId, MessageBody message) throws MessageException {
        finalReply(Cloud.cluster().forNodeId(nodeId), channel, messageId, message);
    }

    public void finalReply(UUID nodeId, String sendToChannel, String receiveChannel, String messageId, MessageBody message) throws MessageException {
        finalReply(Cloud.cluster().forNodeId(nodeId), sendToChannel, receiveChannel, messageId, message);
    }

    public void finalReply(ClusterGroup grp, String channel, String messageId) throws MessageException {
        finalReply(grp, channel, messageId, DONE);
    }

    public void finalReply(ClusterGroup grp, String sendToChannel, String receiveChannel, String messageId) throws MessageException {
        finalReply(grp, sendToChannel, receiveChannel, messageId, DONE);
    }

    public void finalReply(ClusterGroup grp, String channel, String messageId, MessageBody message) throws MessageException {
        finalReply(grp, channel, channel, messageId, message);
    }

    public void finalReply(ClusterGroup grp, String sendToChannel, String receiveChannel, String messageId, MessageBody message) throws MessageException {
        try {
            MessageCmd cmdMessage = new MessageCmd();
            cmdMessage.setChannel(sendToChannel).setReceiveChannel(receiveChannel).setMessageId(messageId).setMessage(message);
            IgniteMessaging rmtMsg = Cloud.message(grp);
            rmtMsg.send(ReliableMessageChannel, cmdMessage);
        } catch (Exception e) {
            logger.warn(e, "ReliableMessage exception: ");
            throw new MessageException("ReliableMessage exception: ", e);
        }
    }

    public MessageBody reply(UUID nodeId, String channel, String messageId, MessageBody message) throws MessageException {
        return reply(nodeId, channel, channel, messageId, message, defaultTimeout);
    }

    public MessageBody reply(UUID nodeId, String channel, String messageId, MessageBody message, Duration timeout) throws MessageException {
        return reply(nodeId, channel, channel, messageId, message, timeout);
    }

    public MessageBody reply(UUID nodeId, String sendToChannel, String receiveChannel, String messageId, MessageBody message) throws MessageException {
        return reply(nodeId, sendToChannel, receiveChannel, messageId, message, defaultTimeout);
    }

    public MessageBody reply(UUID nodeId, String sendToChannel, String receiveChannel, String messageId, MessageBody message, Duration timeout) throws MessageException {
        var result = send(Cloud.cluster().forNodeId(nodeId), sendToChannel, receiveChannel, messageId, message, timeout);
        if(result != null) {
            return result.get(nodeId);
        }
        return null;
    }

    public Map<UUID, MessageBody> reply(ClusterGroup grp, String channel, String messageId, MessageBody message) throws MessageException {
        return send(grp, channel, channel, messageId, message, defaultTimeout);
    }

    public Map<UUID, MessageBody> reply(ClusterGroup grp, String channel, String messageId, MessageBody message, Duration timeout) throws MessageException {
        return send(grp, channel, channel, messageId, message, timeout);
    }

    public Map<UUID, MessageBody> reply(ClusterGroup grp, String sendToChannel, String receiveChannel, String messageId, MessageBody message) throws MessageException {
        return send(grp, sendToChannel, receiveChannel, messageId, message, defaultTimeout);
    }

    public Map<UUID, MessageBody> reply(ClusterGroup grp, String sendToChannel, String receiveChannel, String messageId, MessageBody message, Duration timeout) throws MessageException {
        return send(grp, sendToChannel, receiveChannel, messageId, message, timeout);
    }

    public MessageBody send(UUID nodeId, String channel, MessageBody message) throws MessageException {
        return send(nodeId, channel, channel, message, defaultTimeout);
    }

    public MessageBody send(UUID nodeId, String channel, MessageBody message, Duration timeout) throws MessageException {
        return send(nodeId, channel, channel, message, timeout);
    }

    public MessageBody send(UUID nodeId, String sendToChannel, String receiveChannel, MessageBody message) throws MessageException {
        return send(nodeId, sendToChannel, receiveChannel, message, defaultTimeout);
    }

    public MessageBody send(UUID nodeId, String sendToChannel, String receiveChannel, MessageBody message, Duration timeout) throws MessageException {
        var result = send(Cloud.cluster().forNodeId(nodeId), sendToChannel, receiveChannel, String.valueOf(Ready.getId()), message, timeout);
        if(result != null) {
            return result.get(nodeId);
        }
        return null;
    }

    public Map<UUID, MessageBody> send(ClusterGroup grp, String channel, MessageBody message) throws MessageException {
        return send(grp, channel, channel, String.valueOf(Ready.getId()), message, defaultTimeout);
    }

    public Map<UUID, MessageBody> send(ClusterGroup grp, String channel, MessageBody message, Duration timeout) throws MessageException {
        return send(grp, channel, channel, String.valueOf(Ready.getId()), message, timeout);
    }

    public Map<UUID, MessageBody> send(ClusterGroup grp, String sendToChannel, String receiveChannel, MessageBody message) throws MessageException {
        return send(grp, sendToChannel, receiveChannel, String.valueOf(Ready.getId()), message, defaultTimeout);
    }

    public Map<UUID, MessageBody> send(ClusterGroup grp, String sendToChannel, String receiveChannel, MessageBody message, Duration timeout) throws MessageException {
        return send(grp, sendToChannel, receiveChannel, String.valueOf(Ready.getId()), message, timeout);
    }

    public Map<UUID, MessageBody> send(ClusterGroup grp, String sendToChannel, String receiveChannel, String messageId, MessageBody message, Duration timeout) throws MessageException {
        Collection<ClusterNode> nodes = grp.nodes();
        CompletableFuture<Map<UUID, MessageBody>> future = supplyAsync(()->{
            Map<UUID, MessageBody> data = new HashMap<>();
            var cache = getOrCreateCache(receiveChannel);
            synchronized (cache) {
                cache.put(messageId, data);
            }
            while (true) {
                if(nodes.size() == 1) {
                    if(data.containsKey(nodes.iterator().next().id())) {
                        return data;
                    }
                } else {
                    boolean receivedAll = true;
                    for(var node : nodes) {
                        if(!data.containsKey(node.id())) {
                            receivedAll = false;
                        }
                    }
                    if(receivedAll) {
                        return data;
                    }
                }
                synchronized (data) {
                    data.wait();
                }
            }
        } , (cf)->{ getOrCreateCache(receiveChannel).invalidate(messageId); }, timeout == null ? defaultTimeout : timeout, null);
        try {
            MessageCmd cmdMessage = new MessageCmd();
            cmdMessage.setSendToChannel(sendToChannel).setReceiveChannel(receiveChannel).setMessageId(messageId).setMessage(message);
            IgniteMessaging rmtMsg = Cloud.message(grp);
            rmtMsg.send(ReliableMessageChannel, cmdMessage);
            return future.get(); 
        } catch (Exception e) {
            logger.warn(e, "ReliableMessage exception, sendToChannel=%s, receiveChannel=%s, messageId=%s, message=%s", sendToChannel, receiveChannel, messageId, message);
            throw new MessageException("ReliableMessage exception: ", e);
        }
    }

    public static <T, E extends Exception> CompletableFuture<T> supplyAsync(
            final CheckedSupplier<T, E> supplier,
            final Consumer<CompletableFuture<T>> finalAction,
            Duration timeout, T defaultValue) {

        final CompletableFuture<T> cf = new CompletableFuture<T>();
        final LambdaFinal<ScheduledFuture<?>> handler = new LambdaFinal<>();

        Future<?> future = getPool("AsyncCompletableFuture").submit(() -> {
            try {
                cf.complete(supplier.get());
            } catch (Throwable ex) {
                cf.completeExceptionally(ex);
            } finally {
                if(finalAction != null) {
                    finalAction.accept(cf);
                }
                if(handler.get() != null) {
                    handler.get().cancel(true);
                }
            }
        });

        if(timeout != null) {
            handler.set(scheduler.schedule(() -> {
                if (!cf.isDone()) {
                    if (defaultValue != null) {
                        cf.complete(defaultValue);
                    } else {
                        logger.warn("request time out");
                        cf.completeExceptionally(new TimeoutException());
                    }
                    future.cancel(true);
                }
            }, timeout.toMillis(), TimeUnit.MILLISECONDS));
        }

        return cf;
    }

    private static <T> CompletableFuture<T> timeoutFuture(Duration duration) {
        final CompletableFuture<T> timeout = new CompletableFuture<>();
        scheduler.schedule(() -> {
            final TimeoutException ex = new TimeoutException("Timeout after " + duration);
            return timeout.completeExceptionally(ex);
        }, duration.toMillis(), TimeUnit.MILLISECONDS);
        return timeout;
    }

    public static <T> CompletableFuture<T> supplyAsync(CompletableFuture<T> future, Duration duration) {
        return future.applyToEitherAsync(timeoutFuture(duration), Function.identity());
    }

    @FunctionalInterface
    public interface Listener {
        boolean handle(MessageCmd cmdMessage, MessageBody[] result);
    }
}
