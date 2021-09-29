/**
 *
 * Original work Copyright core-ng
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

package work.ready.core.module.system;

import work.ready.core.handler.Controller;

import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

public class PropertyController extends Controller {

    public void index() {
        renderText(properties());
    }

    String properties() {
        var builder = new StringBuilder(32768);
        builder.append("# system properties\n");
        Properties properties = System.getProperties();
        for (var key : new TreeSet<>(properties.stringPropertyNames())) { 
            String value = properties.getProperty(key);
            String maskedValue = mask(key, value);
            builder.append(key).append('=').append(maskedValue).append('\n');
        }
        builder.append("\n# env variables\n");
        Map<String, String> env = new TreeMap<>(System.getenv());   
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            builder.append(key).append('=').append(mask(key, value)).append('\n');
        }
        return builder.toString();
    }

    private String mask(String key, String value) { 
        String lowerCaseKey = key.toLowerCase();
        if (lowerCaseKey.contains("password") || lowerCaseKey.contains("secret")) return "******";
        return value;
    }
}
