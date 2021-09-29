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

package work.ready.core.handler.request;

import work.ready.core.event.BaseEvent;

public class RequestAttributeEvent extends BaseEvent<HttpRequest> {

    private static final long serialVersionUID = -1L;
    private String name;
    private Object value;
    private Action type;

    public RequestAttributeEvent(HttpRequest source, Action type, String name, Object value) {
        super(source);
        this.name = name;
        this.value = value;
        this.type = type;
    }

    public String getName() {
        return this.name;
    }

    public Object getValue() {
        return this.value;
    }

    public Action getType() { return this.type; }

    public enum Action {
        Replaced, Added, Removed, Cleared
    }
}
