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

package work.ready.core.database.annotation;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Auto {
    String db() default "";
    String value() default ""; 
    String sortColumn() default "";
    Order sortBy() default Order.ASC;
    Order orderBy() default Order.ASC; 
    String groupColumn() default "";
    int status() default Status.VALID; 
    interface Status {
        int ALL = Integer.MAX_VALUE;
        int VALID = Integer.MAX_VALUE - 1;
        int INVALID = Integer.MIN_VALUE;
    }
    enum Order {
        ASC, DESC
    }
}
