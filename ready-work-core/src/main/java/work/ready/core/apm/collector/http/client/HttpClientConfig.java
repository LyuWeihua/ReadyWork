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

package work.ready.core.apm.collector.http.client;

import work.ready.core.apm.collector.http.client.interceptor.HttpClientInterceptor;
import work.ready.core.apm.model.CollectorConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpClientConfig extends CollectorConfig {

    public static final String name = "httpclient";

    private boolean enabled = true;
    private long spend = -1;
    private List<String> urlPrefixInclude;
    private List<String> urlPrefixExclude;
    private Map<String, String> headerExclude;

    @Override
    public String getCollectorName() {
        return name;
    }

    @Override
    public List<Class<?>> getCollectorClasses() {
        List<Class<?>> interceptor = new ArrayList<>();
        interceptor.add(HttpClientInterceptor.class);
        return interceptor;
    }

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getSpend() {
        return spend;
    }

    public void setSpend(long spend) {
        this.spend = spend;
    }

    public List<String> getUrlPrefixInclude() {
        return urlPrefixInclude;
    }

    public void setUrlPrefixInclude(List<String> urlPrefixInclude) {
        this.urlPrefixInclude = urlPrefixInclude;
    }

    public void setUrlPrefixInclude(String urlPrefixInclude) {
        if(this.urlPrefixInclude == null) {
            this.urlPrefixInclude = new ArrayList<>();
        }
        this.urlPrefixInclude.add(urlPrefixInclude);
    }

    public List<String> getUrlPrefixExclude() {
        return urlPrefixExclude;
    }

    public void setUrlPrefixExclude(List<String> urlPrefixExclude) {
        this.urlPrefixExclude = urlPrefixExclude;
    }

    public void setUrlPrefixExclude(String urlPrefixExclude) {
        if(this.urlPrefixExclude == null) {
            this.urlPrefixExclude = new ArrayList<>();
        }
        this.urlPrefixExclude.add(urlPrefixExclude);
    }

    public Map<String, String> getHeaderExclude() {
        return headerExclude;
    }

    public void setHeaderExclude(Map<String, String> headerExclude) {
        this.headerExclude = headerExclude;
    }

    public void setHeaderExclude(String header, String value) {
        if(this.headerExclude == null) {
            this.headerExclude = new HashMap<>();
        }
        this.headerExclude.put(header, value);
    }

    public boolean isUrlIncluded(String url) {
        boolean result = true;
        if(urlPrefixInclude != null && !urlPrefixInclude.isEmpty()) {
            result = false;
            int size = urlPrefixInclude.size();
            for (int i = 0; i < size; i++) {
                if (url.startsWith(urlPrefixInclude.get(i))) {
                    result = true;
                    break;
                }
            }
        }
        if(result && urlPrefixExclude != null && !urlPrefixExclude.isEmpty()) {
            int size = urlPrefixExclude.size();
            for (int i = 0; i < size; i++) {
                if (url.startsWith(urlPrefixExclude.get(i))) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }
}
