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
package work.ready.core.handler.route;

import work.ready.core.handler.RequestMethod;

import java.lang.annotation.*;

@Inherited
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping {

    String[] value() default {}; 

    RequestMethod[] method() default {RequestMethod.GET, RequestMethod.POST}; 

    String[] host() default {}; 

    String[] subHost() default {}; 

    String[] params() default {}; 

    String[] headers() default {}; 

    String[] consumes() default {}; 

    Produces produces() default Produces.Json;

    String viewPath() default ""; 

    public enum Produces {
        Json, Xml, General
    }
}
