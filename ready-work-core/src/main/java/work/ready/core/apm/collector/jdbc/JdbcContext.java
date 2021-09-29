/**
 *
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
package work.ready.core.apm.collector.jdbc;

import work.ready.core.apm.model.Span;

public class JdbcContext {
    private static final ThreadLocal<Boolean> status = ThreadLocal.withInitial(()->true);
    private static final ThreadLocal<Span> localJdbcSpan = new ThreadLocal<>();

    public static void turnOn() {
        status.set(true);
    }

    public static void turnOff() {
        status.set(false);
    }

    public static boolean isOn() {
        return status.get();
    }

    public static void remove(){
        localJdbcSpan.remove();
        status.remove();
    }

    public static Span getJdbcSpan(){
        return localJdbcSpan.get();
    }

    public static void setJdbcSpan(Span span){
        localJdbcSpan.set(span);
    }
}
