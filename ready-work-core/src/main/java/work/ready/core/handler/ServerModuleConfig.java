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

package work.ready.core.handler;

import work.ready.core.config.BaseConfig;
import work.ready.core.tools.ClassUtil;
import work.ready.core.tools.StrUtil;

import java.util.*;
import java.util.stream.Collectors;

public class ServerModuleConfig extends BaseConfig {
    private Map<String, Map<String, Object>> moduleConfig = new HashMap<>();
    private Map<String, PathChain> pathChain = new HashMap<>();
    private List<String> host = new ArrayList<>();
    private List<String> subHost = new ArrayList<>();

    public Map<String, PathChain> getPathChain() {
        return pathChain;
    }

    public Map<String, Map<String, Object>> getModuleConfig() {
        return moduleConfig;
    }

    public Map<String, Object> getModuleConfig(String module){
        return moduleConfig.get(module);
    }

    public ServerModuleConfig setPathChain(Map<String, PathChain> pathChain) {
        this.pathChain = pathChain;
        return this;
    }

    public ServerModuleConfig addPathChain(String chianName, PathChain chain){
        pathChain.put(chianName, chain);
        return this;
    }

    public ServerModuleConfig addPathChain(String path, RequestMethod method, BaseHandler... handler){
        return addPathChain("*", "*", path, method, handler);
    }

    public ServerModuleConfig addPathChain(String path, RequestMethod[] method, BaseHandler... handler){
        return addPathChain(new String[]{"*"}, new String[]{"*"}, new String[]{path}, method, handler);
    }

    public ServerModuleConfig addPathChain(String[] path, RequestMethod[] method, BaseHandler... handler){
        return addPathChain(new String[]{"*"}, new String[]{"*"}, path, method, handler);
    }

    public ServerModuleConfig addPathChain(String host, String subHost, String path, RequestMethod method, BaseHandler... handler){
        return addPathChain(new String[]{host}, new String[]{subHost}, new String[]{path}, new RequestMethod[]{method}, handler );
    }

    ServerModuleConfig addPathChain(String[] host, String[] subHost, String[] path, RequestMethod[] method, BaseHandler... handler){
        PathChain chain  = new PathChain();
        chain.setHost(Arrays.asList(host)).setSubHost(Arrays.asList(subHost)).setPath(Arrays.asList(path))
                .setMethod(Arrays.stream(method).map(Enum::name).collect(Collectors.toList()))
                .setHandlers(Arrays.stream(handler).map((o)->o.getClass().getCanonicalName()).collect(Collectors.toList())).validate("ServerModuleConfig ");
        Integer newPathId = new Random().nextInt();
        while (pathChain.containsKey(newPathId.toString())) {
            newPathId = new Random().nextInt();
        }
        pathChain.put(newPathId.toString(), chain);
        return this;
    }

    public ServerModuleConfig setHost(String... host){
        if(host == null) { this.host.clear(); return this;}
        for(int i = 0; i < host.length; i ++){
            PathChain.verifyHost(host[i]);
            this.host.add(host[i]);
        }
        return this;
    }

    public List<String> getHost(){
        return this.host;
    }

    public ServerModuleConfig setSubHost(String... subHost){
        if(subHost == null) { this.subHost.clear(); return this;}
        for(int i = 0; i < subHost.length; i ++){
            PathChain.verifySubHost(subHost[i]);
            this.subHost.add(subHost[i]);
        }
        return this;
    }

    public List<String> getSubHost(){
        return this.subHost;
    }

    @Override
    public void validate(){
        host.forEach(PathChain::verifyHost);
        host.forEach(PathChain::verifySubHost);
        pathChain.forEach(
                (name, chain)->{
                    chain.validate("ServerModule Config ");
                }
        );
    }

    public static class PathChain {
        private List<String> host = new ArrayList<>();
        private List<String> subHost = new ArrayList<>();
        private List<String> path = new ArrayList<>();
        private List<String> method = new ArrayList<>();
        private List<String> handlers = new ArrayList<>();

        public List<String> getHost() { return host; }

        public PathChain setHost(List<String> host){
            this.host = host;
            return this;
        }

        public List<String> getSubHost() { return subHost; }

        public PathChain setSubHost(List<String> subHost){
            this.subHost = subHost;
            return this;
        }

        public List<String> getPath() {
            return path;
        }

        public PathChain setPath(List<String> path){
            this.path = path;
            return this;
        }

        public List<String> getHandlers() {
            return handlers;
        }

        public PathChain setHandlers(List<String> handlers){
            this.handlers = handlers;
            return this;
        }

        public List<String> getMethod() {
            return method;
        }

        public PathChain setMethod(List<String> method){
            this.method = method;
            return this;
        }

        @Override
        public String toString() {
            String str = path + "@" + method + " → " + handlers;
            if(host != null && host.size() > 0){
                if(subHost != null && subHost.size() > 0){
                    str = subHost + "." + host + ": " + str;
                }else {
                    str = host + ": " + str;
                }
            }
            return str;
        }

        public static void verifyHost(String host){
            if(StrUtil.isBlank(host)) throw new RuntimeException("host name is empty");
            if(!StrUtil.isHostName(host, true) && !host.equals("*")){
                throw new RuntimeException("host name '"+ host +"' is invalid");
            }
        }

        public static void verifySubHost(String subHost){
            if(StrUtil.isBlank(subHost)) throw new RuntimeException("subHost name is empty");
            String forCheck = subHost;
            if(forCheck.startsWith("*")) forCheck = forCheck.substring(1);
            if(forCheck.endsWith("*")) forCheck = forCheck.substring(0,forCheck.length() - 1);
            if(!StrUtil.isHostName(forCheck, true) && !subHost.equals("*")){
                throw new RuntimeException("subHost name '"+ subHost +"' is invalid");
            }
        }

        public PathChain validate(String origin) {
            List<String> problems = new ArrayList<>();
            if(method == null || method.size()==0) {
                problems.add("You must specify method along with path: " + path);
            } else if(handlers == null || handlers.size()==0){
                problems.add("You must specify handlers");
            }
            
            if(getHost()!=null) getHost().forEach(PathChain::verifyHost);
            if(getSubHost()!=null) getSubHost().forEach(PathChain::verifySubHost);
            if(path != null) {
                path.forEach(
                        (p)->{
                            if(!p.startsWith("/")) problems.add("Invalid path: " + p + ", path must starts with /");
                        }
                );
            }
            if(method != null) {
                method.forEach(
                        (m)->{
                            if(!RequestMethod.contains(m.toUpperCase()))
                                problems.add("Invalid HTTP method: " + m);
                        }
                );
            }
            if(handlers != null){
                handlers.forEach(
                        (h)->{
                            if(!StrUtil.isClassName(h)) {
                                problems.add("Invalid handler class name: " + h);
                            }else {
                                Class<?> handlerClass = ClassUtil.forName(h, true);
                                if (handlerClass == null) problems.add("Couldn't load handler class: " + h);
                                else if(!BaseHandler.class.isAssignableFrom(handlerClass)) problems.add("Unsupported type of handler class: " + h);
                            }
                        }
                );
            }

            if(!problems.isEmpty()) {
                throw new RuntimeException("Bad element in " + origin + " [ " + String.join(" | ", problems) + " ]");
            }
            return this;
        }
    }
}

