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

package work.ready.core.config;

import work.ready.core.ioc.annotation.Scope;
import work.ready.core.ioc.annotation.ScopeType;
import work.ready.core.module.ApplicationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@Scope(ScopeType.prototype)
public abstract class BaseConfig {  

    boolean sealed = false;

    public void validate() {
    }

    protected void checkIfSealed()
    {
        if (sealed) throw new IllegalStateException("The configuration is sealed, couldn't be modified.");
    }

    public <T extends BaseConfig> void copyStateTo(T other)
    {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (!Modifier.isFinal(field.getModifiers())) {
                field.setAccessible(true);
                try {
                    field.set(other, field.get(this));
                }
                catch (Exception e) {
                    throw new RuntimeException("Failed to copy state: " + e.getMessage(), e);
                }
            }
        }

        other.sealed = false;
    }
}
