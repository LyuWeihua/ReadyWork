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
package work.ready.core.handler.resource;

import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.cache.LRUCache;
import io.undertow.util.PathMatcher;
import work.ready.core.handler.BaseHandler;

public class PathHandler extends BaseHandler {

    private final PathMatcher<HttpHandler> pathMatcher = new PathMatcher<>();

    private final LRUCache<String, PathMatcher.PathMatch<HttpHandler>> cache;

    public PathHandler(final HttpHandler defaultHandler) {
        this(0);
        pathMatcher.addPrefixPath("/", defaultHandler);
    }

    public PathHandler(final HttpHandler defaultHandler, int cacheSize) {
        this(cacheSize);
        pathMatcher.addPrefixPath("/", defaultHandler);
    }

    public PathHandler() {
        this(0);
    }

    public PathHandler(int cacheSize) {
        if(cacheSize > 0) {
            cache = new LRUCache<>(cacheSize, -1, true);
        } else {
            cache = null;
        }
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        PathMatcher.PathMatch<HttpHandler> match = null;
        boolean hit = false;
        if(cache != null) {
            match = cache.get(exchange.getRelativePath());
            hit = true;
        }
        if(match == null) {
            match = pathMatcher.match(exchange.getRelativePath());
        }
        if (match.getValue() == null) {
            ResponseCodeHandler.HANDLE_404.handleRequest(exchange);
            return;
        }
        if(hit) {
            cache.add(exchange.getRelativePath(), match);
        }
        exchange.setRelativePath(match.getRemaining());
        if(exchange.getResolvedPath().isEmpty()) {
            
            exchange.setResolvedPath(match.getMatched());
        } else {
            
            exchange.setResolvedPath(exchange.getResolvedPath() + match.getMatched());
        }
        match.getValue().handleRequest(exchange);
    }

    @Deprecated
    public synchronized PathHandler addPath(final String path, final HttpHandler handler) {
        return addPrefixPath(path, handler);
    }

    public synchronized PathHandler addPrefixPath(final String path, final HttpHandler handler) {
        Handlers.handlerNotNull(handler);
        pathMatcher.addPrefixPath(path, handler);
        return this;
    }

    public synchronized PathHandler addExactPath(final String path, final HttpHandler handler) {
        Handlers.handlerNotNull(handler);
        pathMatcher.addExactPath(path, handler);
        return this;
    }

    @Deprecated
    public synchronized PathHandler removePath(final String path) {
        return removePrefixPath(path);
    }

    public synchronized PathHandler removePrefixPath(final String path) {
        pathMatcher.removePrefixPath(path);
        return this;
    }

    public synchronized PathHandler removeExactPath(final String path) {
        pathMatcher.removeExactPath(path);
        return this;
    }

    public synchronized PathHandler clearPaths() {
        pathMatcher.clearPaths();
        return this;
    }
}

