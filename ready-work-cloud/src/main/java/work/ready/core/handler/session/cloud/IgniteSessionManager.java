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

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.util.AttachmentKey;
import work.ready.core.handler.session.HttpSession;
import work.ready.core.handler.session.SessionManager;
import work.ready.core.handler.session.SessionRepository;

public class IgniteSessionManager implements SessionManager {

    private final AttachmentKey<IgniteSessionRepository.IgniteSession> NEW_SESSION = AttachmentKey.create(IgniteSessionRepository.IgniteSession.class);

    private SessionConfig sessionConfig;
    private SessionRepository sessionRepository;

    public IgniteSessionManager(SessionConfig sessionConfig, SessionRepository sessionRepository) {
        this.sessionConfig = sessionConfig;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public SessionRepository getSessionRepository() {
        return sessionRepository;
    }

    public void setSessionRepository(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    public String getDeploymentName() {
        return "IgniteSessionManager";
    }

    @Override
    public HttpSession createSession(HttpServerExchange serverExchange) {
        final IgniteSessionRepository.IgniteSession session = (IgniteSessionRepository.IgniteSession)sessionRepository.createSession();
        sessionConfig.setSessionId(serverExchange, session.getId());
        serverExchange.putAttachment(NEW_SESSION, session);
        return session;
    }

    @Override
    public HttpSession getSession(HttpServerExchange serverExchange) {
        if (serverExchange != null) {
            IgniteSessionRepository.IgniteSession newSession = serverExchange.getAttachment(NEW_SESSION);
            if(newSession != null) {
                return newSession;
            }
        }
        String sessionId = sessionConfig.findSessionId(serverExchange);
        HttpSession session = getSession(sessionId);
        if (session == null ) {
            sessionConfig.clearSession(serverExchange, sessionId);
        }
        return session;
    }

    @Override
    public HttpSession getSession(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        HttpSession session = sessionRepository.findById(sessionId);
        if (session!=null && !session.isExpired()) {
            session.setLastAccessedTime(System.currentTimeMillis());

            return session;
        }
        return null;
    }

    @Override
    public HttpSession removeSession(HttpServerExchange serverExchange) {
        if (serverExchange != null) {
            String sessionId = sessionConfig.findSessionId(serverExchange);
            HttpSession oldSession =  serverExchange.removeAttachment(NEW_SESSION);
            removeSession(sessionId);
            return oldSession;
        }

        return null;
    }

    @Override
    public void removeSession(String sessionId) {
        sessionRepository.deleteById(sessionId);
    }
}
