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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public enum RequestMethod {
    HEAD, GET, POST, PUT, DELETE, OPTIONS, TRACE, CONNECT, PATCH;

    static final List<String> methods = new ArrayList<>();
    static {
        for(RequestMethod method : values()){
            methods.add(method.name());
        }
    }
    public static boolean contains(String method){
        return methods.contains(method);
    }

    public static final EnumSet<RequestMethod> MethodHasBody = EnumSet.of(POST, PUT, PATCH);
    public static boolean hasBody(RequestMethod method) { return MethodHasBody.contains(method); }
}
