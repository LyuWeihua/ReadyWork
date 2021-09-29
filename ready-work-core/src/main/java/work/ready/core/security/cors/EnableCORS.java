/**
 *
 * Original work Copyright (c) 2015-2020, Michael Yang 杨福海 (fuhai999@gmail.com).
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
package work.ready.core.security.cors;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface EnableCORS {

    String allowOrigin() default "*";

    String allowCredentials() default "true";

    String allowHeaders() default "Origin,X-Requested-With,Content-Type,Accept,Authorization,Jwt";

    String allowMethods() default "GET,PUT,POST,DELETE,PATCH,OPTIONS";

    String exposeHeaders() default "";

    String requestHeaders() default "";

    String requestMethod() default "";

    String origin() default "";

    String maxAge() default "3600";
}
