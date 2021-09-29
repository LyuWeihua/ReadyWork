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

package work.ready.core.handler;

import io.undertow.predicate.Predicate;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

import java.util.Set;

public class GZipPredicate implements Predicate {
    private static final int MIN_GZIP_LENGTH = 20;  
    private final Set<String> gzipContentTypes = Set.of(ContentType.TEXT_PLAIN.toString(),
            ContentType.TEXT_HTML.toString(),
            ContentType.TEXT_CSS.toString(),
            ContentType.TEXT_XML.toString(),
            ContentType.APPLICATION_JSON.toString(),
            ContentType.APPLICATION_JAVASCRIPT.toString());

    @Override
    public boolean resolve(HttpServerExchange exchange) {
        HeaderMap headers = exchange.getResponseHeaders();
        return resolve(headers);
    }

    boolean resolve(HeaderMap headers) {
        String contentType = headers.getFirst(Headers.CONTENT_TYPE);
        if (contentType == null || !gzipContentTypes.contains(contentType)) return false;
        String length = headers.getFirst(Headers.CONTENT_LENGTH);
        return length == null || Long.parseLong(length) > MIN_GZIP_LENGTH;
    }
}
