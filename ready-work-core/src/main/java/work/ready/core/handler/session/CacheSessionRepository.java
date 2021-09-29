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

package work.ready.core.handler.session;

import work.ready.core.component.cache.Cache;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CacheSessionRepository implements SessionRepository<CacheSessionRepository.CacheSession> {

    private SessionFlushMode sessionFlushMode = SessionFlushMode.IMMEDIATE;

    private int defaultMaxInactiveInterval;

    private Cache cache;

    public CacheSessionRepository(Cache cache) {
        this.cache = cache;
    }

    public void setDefaultMaxInactiveInterval(int defaultMaxInactiveInterval) {
        this.defaultMaxInactiveInterval = defaultMaxInactiveInterval;
    }

    @Override
    public void setSessionFlushMode(SessionFlushMode sessionFlushMode) {
        this.sessionFlushMode = sessionFlushMode;
    }

    @Override
    public SessionFlushMode getSessionFlushMode(){ return sessionFlushMode; }

    @Override
    public CacheSession createSession() {
        CacheSession result = new CacheSession();
        if (this.defaultMaxInactiveInterval != 0) {
            result.setMaxInactiveInterval(this.defaultMaxInactiveInterval);
        }
        return result;
    }

    @Override
    public void save(CacheSession session) {
        if (!session.getId().equals(session.originalId)) {
            this.cache.remove(CacheSessionManager.DEPLOY_NAME, session.originalId);
            session.originalId = session.getId();
        }
        if (session.isChanged()) {
            
            this.cache.put(CacheSessionManager.DEPLOY_NAME, session.getId(), session.getDelegate(), session.getMaxInactiveInterval());
            session.markUnchanged();
        }
    }

    @Override
    public CacheSession findById(String id) {
        MapSession saved = this.cache.get(CacheSessionManager.DEPLOY_NAME, id);
        if (saved == null) {
            return null;
        }
        if (saved.isExpired()) {
            deleteById(saved.getId());
            return null;
        }
        return new CacheSession(saved);
    }

    @Override
    public void deleteById(String id) {
        this.cache.remove(CacheSessionManager.DEPLOY_NAME, id);
    }

    public List<MapSession> getSessions() {
        return this.cache.getKeys(CacheSessionManager.DEPLOY_NAME);
    }

    final class CacheSession implements HttpSession {

        private final MapSession delegate;
        private boolean changed;
        private String originalId;
        private boolean isNew;

        CacheSession() {
            this(new MapSession());
            this.isNew = true;
            flushImmediateIfNecessary();
        }

        CacheSession(MapSession cached) {
            Objects.requireNonNull(cached);
            this.delegate = cached;
            this.originalId = cached.getId();
        }

        public boolean isNew() {
            return isNew;
        }

        public void setNew(boolean aNew) {
            isNew = aNew;
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
            if (CacheSessionRepository.this.sessionFlushMode == SessionFlushMode.IMMEDIATE) {
                CacheSessionRepository.this.save(this);
            }
        }

    }

}
