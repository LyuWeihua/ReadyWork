/**
 *
 * Original work Copyright (c) 2016 Network New Technologies Inc.
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
package work.ready.core.handler.session;

import io.undertow.server.session.SecureRandomSessionIdGenerator;
import work.ready.core.server.Ready;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class MapSession implements HttpSession, Serializable {

    private static final long serialVersionUID = 1L;
    
    public static final int DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS = 1800;

    private String id;
    private String originalId;
    private Map<String, Object> sessionAttrs = new HashMap<>();
    private long creationTime  = Ready.currentTimeMillis();
    private long lastAccessedTime = creationTime;

    private int maxInactiveInterval = DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS;

    public MapSession() {
        this(generateId());
    }

    public MapSession(String id) {
        this.id = id;
        this.originalId = id;
    }

    public MapSession(HttpSession session) {
        if (session == null) {
            throw new IllegalArgumentException("session cannot be null");
        }
        this.id = session.getId();
        this.originalId = this.id;
        this.sessionAttrs = new HashMap<>(
                session.getAttributeNames().size());
        for (String attrName : session.getAttributeNames()) {
            Object attrValue = session.getAttribute(attrName);
            if (attrValue != null) {
                this.sessionAttrs.put(attrName, attrValue);
            }
        }
        this.lastAccessedTime = session.getLastAccessedTime();
        this.creationTime = session.getCreationTime();
        this.maxInactiveInterval = session.getMaxInactiveInterval();
    }

    @Override
    public void setLastAccessedTime(long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    @Override
    public long getCreationTime() {
        return this.creationTime;
    }

    @Override
    public String getId() {
        return this.id;
    }

    String getOriginalId() {
        return this.originalId;
    }

    void setOriginalId(String originalId) {
        this.originalId = originalId;
    }

    @Override
    public String changeSessionId() {
        String changedId = generateId();
        setId(changedId);
        return changedId;
    }

    @Override
    public long getLastAccessedTime() {
        return this.lastAccessedTime;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }

    @Override
    public int getMaxInactiveInterval() {
        return this.maxInactiveInterval;
    }

    @Override
    public boolean isExpired() {
        return isExpired(Ready.currentTimeMillis());
    }

    boolean isExpired(long now) {
        if (this.maxInactiveInterval<0) {
            return false;
        }
        return now - this.lastAccessedTime >= this.maxInactiveInterval * 1000L;
    }

    @Override
    public Object getAttribute(String attributeName) {
        return  this.sessionAttrs.get(attributeName);
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.sessionAttrs.keySet();
    }

    @Override
    public Object setAttribute(String attributeName, Object attributeValue) {
        if (attributeValue == null) {
            return removeAttribute(attributeName);
        } else {
            return this.sessionAttrs.put(attributeName, attributeValue);
        }
    }

    @Override
    public Object removeAttribute(String attributeName) {
        return this.sessionAttrs.remove(attributeName);
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof HttpSession && this.id.equals(((HttpSession) obj).getId());
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    private static String generateId() {
        return new SecureRandomSessionIdGenerator().createSessionId();
    }

}
