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

import io.undertow.server.HttpServerExchange;

public interface SessionManager {
    
    String getDeploymentName();

    SessionRepository getSessionRepository();

    HttpSession createSession(final HttpServerExchange serverExchange);

    HttpSession getSession(final HttpServerExchange serverExchange);

    HttpSession getSession(final String sessionId);

    HttpSession removeSession(final HttpServerExchange serverExchange);

    void removeSession(final String sessionId);

}
