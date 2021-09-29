/**
 *
 * Original work Copyright 2009-2016 Weibo, Inc.
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
package work.ready.core.component.switcher;

import java.util.List;

public interface SwitcherService {
    
    Switcher getSwitcher(String name);

    List<Switcher> getAllSwitchers();

    void initSwitcher(String switcherName, boolean initialValue);

    boolean isOpen(String switcherName);

    boolean isOpen(String switcherName, boolean defaultValue);

    void setValue(String switcherName, boolean value);

    void setGlobalValue(String switcherName, boolean value);

    void registerListener(String switcherName, SwitcherListener listener);

    void unRegisterListener(String switcherName, SwitcherListener listener);

}
