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

package work.ready.core.handler.session.cloud;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.configuration.CacheConfiguration;
import work.ready.cloud.ReadyCloud;
import work.ready.cloud.cluster.Cloud;
import work.ready.core.handler.session.HttpSession;
import work.ready.core.handler.session.MapSession;
import work.ready.core.handler.session.SessionFlushMode;
import work.ready.core.handler.session.SessionRepository;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import javax.cache.Cache;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class IgniteSessionRepository implements SessionRepository<IgniteSessionRepository.IgniteSession> {

    private static final Log logger = LogFactory.getLog(IgniteSessionRepository.class);

    private final IgniteCache<String, MapSession> sessions;

    private SessionFlushMode sessionFlushMode = SessionFlushMode.IMMEDIATE;

    private int defaultMaxInactiveInterval;

    public IgniteSessionRepository() {
        CacheConfiguration<String, MapSession> sessionCfg = ReadyCloud.getInstance().newCacheConfig("ready.work:session", false, true, 0, false);
        this.sessions = Cloud.getOrCreateCache(sessionCfg);
    }

    public void setDefaultMaxInactiveInterval(int defaultMaxInactiveInterval) {
        this.defaultMaxInactiveInterval = defaultMaxInactiveInterval;
    }

    @Override
    public void setSessionFlushMode(SessionFlushMode flushMode) {
        this.sessionFlushMode = flushMode;
    }

    @Override
    public SessionFlushMode getSessionFlushMode() {
        return sessionFlushMode;
    }

    @Override
    public IgniteSession createSession() {
        IgniteSession result = new IgniteSession();
        if (this.defaultMaxInactiveInterval != 0) {
            result.setMaxInactiveInterval(this.defaultMaxInactiveInterval);
        }
        return result;
    }

    @Override
    public void save(IgniteSession session) {
        if (!session.getId().equals(session.originalId)) {
            this.sessions.remove(session.originalId);
            session.originalId = session.getId();
        }
        if (session.isChanged()) {
            ExpiryPolicy plc = new CreatedExpiryPolicy(new Duration(TimeUnit.SECONDS, session.getMaxInactiveInterval()));
            this.sessions.withExpiryPolicy(plc).put(session.getId(), session.getDelegate());
            session.markUnchanged();
        }
    }

    @Override
    public IgniteSession findById(String id) {
        MapSession saved = this.sessions.get(id);
        if (saved == null) {
            return null;
        }
        if (saved.isExpired()) {
            deleteById(saved.getId());
            return null;
        }
        return new IgniteSession(saved);
    }

    @Override
    public void deleteById(String id) {
        this.sessions.remove(id);
    }

    public Map<String, MapSession> getSessions() {
        Map<String, MapSession> sessionMap = new HashMap<>();
        Iterator<Cache.Entry<String, MapSession>> itr = this.sessions.iterator();
        while(itr.hasNext()) {
            var entry = itr.next();
            sessionMap.put(entry.getKey(), entry.getValue());
        }
        return sessionMap;
    }

    final class IgniteSession implements HttpSession {

        private final MapSession delegate;
        private boolean changed;
        private String originalId;

        IgniteSession() {
            this(new MapSession());
            this.changed = true;
            flushImmediateIfNecessary();
        }

        IgniteSession(MapSession cached) {
            this.delegate = cached;
            this.originalId = cached.getId();
        }

        @Override
        public void setLastAccessedTime(long lastAccessedTime) {
            this.delegate.setLastAccessedTime(lastAccessedTime);
            this.changed = true;
            flushImmediateIfNecessary();
        }

        @Override
        public boolean isExpired() {
            return this.delegate.isExpired();
        }

        @Override
        public long getCreationTime() {
            return this.delegate.getCreationTime();
        }

        @Override
        public String getId() {
            return this.delegate.getId();
        }

        @Override
        public String changeSessionId() {
            this.changed = true;
            String result = this.delegate.changeSessionId();
            return result;
        }

        @Override
        public long getLastAccessedTime() {
            return this.delegate.getLastAccessedTime();
        }

        @Override
        public void setMaxInactiveInterval(int interval) {
            this.delegate.setMaxInactiveInterval(interval);
            this.changed = true;
            flushImmediateIfNecessary();
        }

        @Override
        public int getMaxInactiveInterval() {
            return this.delegate.getMaxInactiveInterval();
        }

        @Override
        public Object getAttribute(String attributeName) {
            return this.delegate.getAttribute(attributeName);
        }

        @Override
        public Set<String> getAttributeNames() {
            return this.delegate.getAttributeNames();
        }

        @Override
        public Object setAttribute(String attributeName, Object attributeValue) {
            Object object = this.delegate.setAttribute(attributeName, attributeValue);
            this.changed = true;
            flushImmediateIfNecessary();
            return object;
        }

        @Override
        public Object removeAttribute(String attributeName) {
            Object object = this.delegate.removeAttribute(attributeName);
            this.changed = true;
            flushImmediateIfNecessary();
            return object;
        }

        boolean isChanged() {
            return this.changed;
        }

        void markUnchanged() {
            this.changed = false;
        }

        MapSession getDelegate() {
            return this.delegate;
        }

        private void flushImmediateIfNecessary() {
            if (IgniteSessionRepository.this.sessionFlushMode == SessionFlushMode.IMMEDIATE) {
                IgniteSessionRepository.this.save(this);
            }
        }

    }

}
