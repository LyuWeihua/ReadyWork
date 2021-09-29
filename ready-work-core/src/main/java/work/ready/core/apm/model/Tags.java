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

import java.util.HashMap;

public class Tags extends HashMap<String, Object> {

    public static final String PARAMS = "params";
    public static final String URL = "url";
    public static final String REMOTE = "remote";
    public static final String METHOD = "method";
    
    public static final String BODY = "body";
    public static final String HANDLER = "handler";
    public static final String CONTROLLER = "controller";

    public Tags addTag(String key,Object val){
        this.put(key,val);
        return this;
    }
}
