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

package work.ready.core.log;

import java.lang.reflect.Field;
import java.util.Properties;

public class LogManager extends java.util.logging.LogManager {
    static LogManager proxyLogManager;

    public LogManager() {
        proxyLogManager = this;
    }

    @Override
    public void reset() {  }

    private void resetLater() {
        super.reset();
    }

    public static void setProperty(String name, String value) {
        try {
            Field field = java.util.logging.LogManager.class.getDeclaredField("props");
            field.setAccessible(true);
            Properties props = (Properties)field.get(getLogManager());
            props.setProperty(name, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
        }
    }

    public static void resetFinally() { if(proxyLogManager != null) proxyLogManager.resetLater(); }
}
