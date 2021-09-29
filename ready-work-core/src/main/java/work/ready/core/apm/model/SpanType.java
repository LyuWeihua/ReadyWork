/**
 *
 * Original work copyright bee-apm
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
package work.ready.core.apm.model;

public class SpanType {
    public final static String PROCESS = "proc";
    public final static String SQL = "sql";
    public final static String SQL_PARAM = "sqlp";
    public final static String PARAM = "param";
    public final static String REQUEST = "req";
    public final static String LOGGER = "log";
    public final static String SPRING_TX = "tx";
    public final static String ERROR = "err";
    public final static String REQUEST_PARAM = "rp";
    public final static String REQUEST_BODY = "reqb";
    public final static String REQUEST_HEADERS ="reqh";
    public final static String RESPONSE_HEADERS ="resh";
    public final static String RESPONSE_BODY = "resb";
    public final static String HEARTBEAT = "hb";
    public final static String TOPOLOGY = "topo";
    public final static String JVM = "jvm";
}
