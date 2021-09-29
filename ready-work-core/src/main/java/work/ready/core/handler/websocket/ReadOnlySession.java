/**
 *
 * Original work Copyright core-ng
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
package work.ready.core.handler.websocket;

import work.ready.core.handler.session.HttpSession;

import java.util.Set;

class ReadOnlySession implements HttpSession {

    private final HttpSession session;
    private final String readOnly = "session is readonly for websocket";

    ReadOnlySession(HttpSession session) {
        this.session = session;
    }

    @Override
    public String getId() {
        return session.getId();
    }

    @Override
    public long getCreationTime() {
        return session.getCreationTime();
    }

    @Override
    public long getLastAccessedTime() {
        return session.getLastAccessedTime();
    }

    @Override
    public void setLastAccessedTime(long lastAccessedTime) {
        throw new Error(readOnly);
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        throw new Error(readOnly);
    }

    @Override
    public int getMaxInactiveInterval() {
        return session.getMaxInactiveInterval();
    }

    @Override
    public boolean isExpired() {
        return session.isExpired();
    }

    @Override
    public Set<String> getAttributeNames() {
        return session.getAttributeNames();
    }

    @Override
    public String getAttribute(String key) {
        return (String)session.getAttribute(key);
    }

    @Override
    public Object removeAttribute(String attributeName) {
        throw new Error(readOnly);
    }

    @Override
    public Object setAttribute(String key, Object value) {
        throw new Error(readOnly);
    }

    @Override
    public String changeSessionId() {
        throw new Error(readOnly);
    }

}
