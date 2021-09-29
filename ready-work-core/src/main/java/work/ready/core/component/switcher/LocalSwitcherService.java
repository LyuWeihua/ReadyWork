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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LocalSwitcherService implements SwitcherService {

    private static ConcurrentMap<String, Switcher> switchers = new ConcurrentHashMap<>();

    final private Map<String, List<SwitcherListener>> listenerMap = new ConcurrentHashMap<>();

    @Override
    public Switcher getSwitcher(String name) {
        return switchers.get(name);
    }

    @Override
    public List<Switcher> getAllSwitchers() {
        return new ArrayList<>(switchers.values());
    }

    private void putSwitcher(Switcher switcher) {
        if (switcher == null) {
            throw new IllegalArgumentException("LocalSwitcherService addSwitcher Error: switcher is null");
        }
        switchers.put(switcher.getName(), switcher);
    }

    @Override
    public void initSwitcher(String switcherName, boolean initialValue) {
        setValue(switcherName, initialValue);
    }

    @Override
    public boolean isOpen(String switcherName) {
        Switcher switcher = switchers.get(switcherName);
        return switcher != null && switcher.isOn();
    }

    @Override
    public boolean isOpen(String switcherName, boolean defaultValue) {
        Switcher switcher = switchers.get(switcherName);
        if (switcher == null) {
            switchers.putIfAbsent(switcherName, new Switcher(switcherName, defaultValue));
            switcher = switchers.get(switcherName);
        }
        return switcher.isOn();
    }

    @Override
    public void setValue(String switcherName, boolean value) {
        putSwitcher(new Switcher(switcherName, value));

        List<SwitcherListener> listeners = listenerMap.get(switcherName);
        if(listeners != null) {
            for (SwitcherListener listener : listeners) {
                listener.onValueChanged(switcherName, value);
            }
        }
    }

    @Override
    public void setGlobalValue(String switcherName, boolean value) {
        setValue(switcherName, value);
    }

    @Override
    public void registerListener(String switcherName, SwitcherListener listener) {
        synchronized (listenerMap) {
            if (listenerMap.get(switcherName) == null) {
                List<SwitcherListener> listeners = Collections.synchronizedList(new ArrayList<>());
                listenerMap.put(switcherName, listeners);
                listeners.add(listener);
            } else {
                List<SwitcherListener> listeners = listenerMap.get(switcherName);
                if (!listeners.contains(listener)) {
                    listeners.add(listener);
                }
            }
        }
    }

    @Override
    public void unRegisterListener(String switcherName, SwitcherListener listener) {
        synchronized (listenerMap) {
            if (listener == null) {
                listenerMap.remove(switcherName);
            } else {
                List<SwitcherListener> listeners = listenerMap.get(switcherName);
                listeners.remove(listener);
            }
        }
    }

}
