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

public class SwitcherUtil {
    private static SwitcherService switcherService = new LocalSwitcherService();

    public static void initSwitcher(String switcherName, boolean initialValue) {
        switcherService.initSwitcher(switcherName, initialValue);
    }

    public static boolean isOpen(String switcherName) {
        return switcherService.isOpen(switcherName);
    }

    public static boolean switcherIsOpenWithDefault(String switcherName, boolean defaultValue) {
        return switcherService.isOpen(switcherName, defaultValue);
    }

    public static void setValue(String switcherName, boolean value) {
        switcherService.setValue(switcherName, value);
    }

    public static void setGlobalValue(String switcherName, boolean value) {
        switcherService.setGlobalValue(switcherName, value);
    }

    public static SwitcherService getSwitcherService() {
        return switcherService;
    }

    public static void setSwitcherService(SwitcherService switcherService) {
        SwitcherUtil.switcherService = switcherService;
    }

    public static void registerSwitcherListener(String switcherName, SwitcherListener listener) {
        switcherService.registerListener(switcherName, listener);
    }

}
