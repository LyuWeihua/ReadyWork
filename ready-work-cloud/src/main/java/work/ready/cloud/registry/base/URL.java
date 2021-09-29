/**
 *
 * Original work copyright (c) 2016 Network New Technologies Inc.
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
package work.ready.cloud.registry.base;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Constant;
import work.ready.core.tools.StrUtil;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class URL implements Serializable {
    private static final Log logger = LogFactory.getLog(URL.class);

    private String protocol;

    private String host;

    private int port;

    private String path;

    private Map<String, String> parameters;

    private volatile transient Map<String, Number> numbers;

    public URL(String protocol, String host, int port, String path) {
        this(protocol, host, port, path, new HashMap<>());
    }
    public URL() {

    }

    public URL(String protocol, String host, int port, String path, Map<String, String> parameters) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.path = path;
        this.parameters = parameters;
    }

    public static URL valueOf(String url) {
        String protocol = null;
        String host = null;
        int port = -1;
        String path = null;
        Map<String, String> parameters = new HashMap<>();
        int i = url.indexOf("?"); 
        if (i >= 0) {
            String[] parts = url.substring(i + 1).split("&");

            for (String part : parts) {
                part = part.trim();
                if (part.length() > 0) {
                    int j = part.indexOf('=');
                    if (j >= 0) {
                        parameters.put(part.substring(0, j), part.substring(j + 1));
                    } else {
                        parameters.put(part, part);
                    }
                }
            }
            url = url.substring(0, i);
        }
        i = url.indexOf("://");
        if (i >= 0) {
            if (i == 0) throw new IllegalStateException("url missing protocol: \"" + url + "\"");
            protocol = url.substring(0, i);
            url = url.substring(i + 3);
        } else {
            i = url.indexOf(":/");
            if (i >= 0) {
                if (i == 0) throw new IllegalStateException("url missing protocol: \"" + url + "\"");
                protocol = url.substring(0, i);
                url = url.substring(i + 1);
            }
        }

        i = url.indexOf("/");
        if (i >= 0) {
            path = url.substring(i + 1);
            url = url.substring(0, i);
        }

        i = url.indexOf(":");
        if (i >= 0 && i < url.length() - 1) {
            port = Integer.parseInt(url.substring(i + 1));
            url = url.substring(0, i);
        }
        if (url.length() > 0) host = url;
        if(port == -1) {
            
            if("http".equalsIgnoreCase(protocol)) {
                port = 80;
            } else if("https".equalsIgnoreCase(protocol)) {
                port = 443;
            } else if("ready".equalsIgnoreCase(protocol)) {
                port = 0;
            } else {
                logger.error("Unknown protocol " + protocol);
            }
        }
        return new URL(protocol, host, port, path, parameters);
    }

    private static String buildHostPortStr(String host, int defaultPort) {
        if (defaultPort <= 0) {
            return host;
        }

        int idx = host.indexOf(":");
        if (idx < 0) {
            return host + ":" + defaultPort;
        }
        if(idx > 1) { 
            return "[" + host + "]:" + defaultPort;
        }

        int port = Integer.parseInt(host.substring(idx + 1));
        if (port <= 0) {
            return host.substring(0, idx + 1) + defaultPort;
        }
        return host;
    }

    public URL createCopy() {
        Map<String, String> params = new HashMap<>();
        if (this.parameters != null) {
            params.putAll(this.parameters);
        }

        return new URL(protocol, host, port, path, params);
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getProjectVersion() {
        return getParameter(URLParam.projectVersion.getName(), URLParam.projectVersion.getValue());
    }

    public String getServiceVersion() {
        return getParameter(URLParam.serviceVersion.getName(), URLParam.serviceVersion.getValue());
    }

    public String getGroup() {
        return getParameter(URLParam.group.getName(), URLParam.group.getValue());
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }

    public String getParameter(String name, String defaultValue) {
        String value = getParameter(name);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public String getMethodParameter(String methodName, String paramDesc, String name) {
        String value = getParameter(Constant.METHOD_CONFIG_PREFIX + methodName + "(" + paramDesc + ")." + name);
        if (value == null || value.length() == 0) {
            return getParameter(name);
        }
        return value;
    }

    public String getMethodParameter(String methodName, String paramDesc, String name, String defaultValue) {
        String value = getMethodParameter(methodName, paramDesc, name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return value;
    }

    public void addParameter(String name, String value) {
        if (name == null || value == null) {
            return;
        }
        parameters.put(name, value);
    }

    public void removeParameter(String name) {
        if (name != null) {
            parameters.remove(name);
        }
    }

    public void addParameters(Map<String, String> params) {
        parameters.putAll(params);
    }

    public void addParameterIfAbsent(String name, String value) {
        if (hasParameter(name)) {
            return;
        }
        parameters.put(name, value);
    }

    public Boolean getBooleanParameter(String name, boolean defaultValue) {
        String value = getParameter(name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value);
    }

    public Boolean getMethodParameter(String methodName, String paramDesc, String name, boolean defaultValue) {
        String value = getMethodParameter(methodName, paramDesc, name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public Integer getIntParameter(String name, int defaultValue) {
        Number n = getNumbers().get(name);
        if (n != null) {
            return n.intValue();
        }
        String value = parameters.get(name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        getNumbers().put(name, i);
        return i;
    }

    public Integer getMethodParameter(String methodName, String paramDesc, String name, int defaultValue) {
        String key = methodName + "(" + paramDesc + ")." + name;
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.intValue();
        }
        String value = getMethodParameter(methodName, paramDesc, name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        getNumbers().put(key, i);
        return i;
    }

    public String getRequestUri(){
        return protocol + Constant.PROTOCOL_SEPARATOR + host + ":" + port;
    }

    public String getUri() {
        return protocol + Constant.PROTOCOL_SEPARATOR + host + ":" + port +
                "/" + path;
    }

    public String getIdentity() {
        return protocol + Constant.PROTOCOL_SEPARATOR + host + ":" + port +
                "/" + getParameter(URLParam.nodeType.getName(), "*") +
                "/" + getParameter(URLParam.group.getName(), "*") +
                "/" + getParameter(URLParam.project.getName(), "*") +
                "[" + getParameter(URLParam.projectVersion.getName(), "*") + "]" +
                "/" + getPath() + "[" + getParameter(URLParam.serviceVersion.getName(), "*") + "]";
    }

    public boolean canServe(URL refUrl) {
        if (refUrl == null || !this.getPath().equals(refUrl.getPath())) {
            return false;
        }

        if(!protocol.equals(refUrl.getProtocol())) {
            return false;
        }

        if (!this.getParameter(URLParam.project.getName(), URLParam.project.getValue()).equals(refUrl.getParameter(URLParam.project.getName(), URLParam.project.getValue()))) {
            return false;
        }

        if (!this.getParameter(URLParam.nodeType.getName(), URLParam.nodeType.getValue()).equals(refUrl.getParameter(URLParam.nodeType.getName(), URLParam.nodeType.getValue()))) {
            return false;
        }

        String version = getParameter(URLParam.projectVersion.getName(), URLParam.projectVersion.getValue());
        String refVersion = refUrl.getParameter(URLParam.projectVersion.getName(), URLParam.projectVersion.getValue());
        if (!version.equals(refVersion)) {
            return false;
        }

        version = getParameter(URLParam.serviceVersion.getName(), URLParam.serviceVersion.getValue());
        refVersion = refUrl.getParameter(URLParam.serviceVersion.getName(), URLParam.serviceVersion.getValue());
        if (!version.equals(refVersion)) {
            return false;
        }

        String serialize = getParameter(URLParam.serialize.getName(), URLParam.serialize.getValue());
        String refSerialize = refUrl.getParameter(URLParam.serialize.getName(), URLParam.serialize.getValue());
        if (!serialize.equals(refSerialize)) {
            return false;
        }
        
        return true;
    }

    public String toFullStr() {
        StringBuilder builder = new StringBuilder();
        builder.append(getUri()).append("?");

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();

            builder.append(name).append("=").append(value).append('&');
        }
        if(builder.charAt(builder.length()-1) == '&') builder.deleteCharAt(builder.length()-1);
        return builder.toString();
    }

    @Override
    public String toString() {
        return toSimpleString();
    }

    public String toSimpleString() {
        String nodeName = getParameter(URLParam.nodeConsistentId.getName());
        if(nodeName != null && !nodeName.isEmpty()) {
            return nodeName + "@" + getUri() + "?group=" + getGroup();
        }
        return getUri() + "?group=" + getGroup();
    }

    public boolean hasParameter(String key) {
        String p = getParameter(key);
        return p != null && p.trim().length() > 0;
    }

    public String getServerPortStr() {
        return buildHostPortStr(host, port);

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        URL url = (URL) o;

        if (port != url.getPort()) return false;
        if (protocol != null ? !protocol.equals(url.getProtocol()) : url.getProtocol() != null) return false;
        if (host != null ? !host.equals(url.getHost()) : url.getHost() != null) return false;
        if (path != null ? !path.equals(url.getPath()) : url.getPath() != null) return false;
        if (parameters != null ? !parameters.equals(url.getParameters()) : url.getParameters() != null) return false;
        return parameters != null ? parameters.equals(url.getParameters()) : url.getParameters() == null;
    }

    @Override
    public int hashCode() {
        int result = protocol != null ? protocol.hashCode() : 0;
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        return result;
    }

    private Map<String, Number> getNumbers() {
        if (numbers == null) {
            
            numbers = new ConcurrentHashMap<>();
        }
        return numbers;
    }

}
