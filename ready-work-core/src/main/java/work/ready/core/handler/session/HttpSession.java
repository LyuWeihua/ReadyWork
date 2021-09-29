/**
 *
 * Original work Copyright 2014 Red Hat, Inc.
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

import java.util.Set;

public interface HttpSession {
    
    String getId();

    long getCreationTime();

    long getLastAccessedTime();

    void setLastAccessedTime(long lastAccessedTime);
    
    void setMaxInactiveInterval(int interval);

    int getMaxInactiveInterval();

    boolean isExpired();

    Object getAttribute(String name);

    Set<String> getAttributeNames();

    Object setAttribute(final String name, Object value);

    Object removeAttribute(final String name);

    String changeSessionId();

}
