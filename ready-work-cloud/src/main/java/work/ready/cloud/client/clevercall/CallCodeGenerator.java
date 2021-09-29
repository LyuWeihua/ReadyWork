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

package work.ready.cloud.client.clevercall;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.undertow.util.Headers;
import work.ready.cloud.ReadyCloud;
import work.ready.cloud.client.ClientConfig;
import work.ready.cloud.client.CloudClient;
import work.ready.cloud.client.annotation.Call;
import work.ready.cloud.registry.base.URLParam;
import work.ready.core.component.proxy.CodeGenerator;
import work.ready.core.database.DatabaseManager;
import work.ready.core.handler.ContentType;
import work.ready.core.handler.RequestMethod;
import work.ready.core.log.Log;
import work.ready.core.server.Ready;
import work.ready.core.tools.ClassUtil;
import work.ready.core.tools.HttpClient;
import work.ready.core.tools.StrUtil;

import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CallCodeGenerator implements CodeGenerator {
    protected final DatabaseManager dbManager;
    protected Method method;
    protected Call annotation;
    protected Map<String, Object> clazzData;
    protected Map<String, Object> methodData;

    protected Class<?> returnType;
    protected Type genericReturnType;

    protected Parameter[] parameters;
    protected Map<String, Integer> pathParam = new HashMap<>();

    protected String project;
    protected String serviceId;
    protected String url;
    protected String url_protocol;
    protected String url_host;
    protected int url_port;
    protected String url_path;
    protected Call.Protocol protocol;
    protected RequestMethod requestMethod;
    protected Call.RequestType type;
    protected String authorization;
    protected String projectVersion;
    protected String serviceVersion;
    protected String group;
    protected String profile;
    protected int timeout;
    protected int retry;
    protected Call.Balance loadBalance;
    protected Class<? extends Callback> callback;
    protected Class<? extends CallHandler> callHandler;
    protected Class<? extends Fallback> fallback;

    protected String loggerVar;
    public static final String DEFAULT_USER_AGENT = "ReadyCleverCallClient";
    protected String userAgent = DEFAULT_USER_AGENT;

    private static final Pattern pattern = Pattern.compile("\\{(.+?)\\}");

    protected String replace; 
    protected String insert; 
    protected String append; 

    public CallCodeGenerator(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public CodeGenerator generatorCode(Class<?> target, Method method, Map<String, Object> clazzData, Map<String, Object> methodData) {
        CallCodeGenerator thisGenerator = new CallCodeGenerator(dbManager);
        thisGenerator.method = method;

        thisGenerator.clazzData = clazzData;
        thisGenerator.methodData = methodData;

        thisGenerator.parameters = thisGenerator.method.getParameters();
        thisGenerator.returnType = thisGenerator.method.getReturnType();
        thisGenerator.genericReturnType = thisGenerator.method.getGenericReturnType();

        thisGenerator.annotation = thisGenerator.method.getAnnotation(Call.class);
        thisGenerator.project = thisGenerator.annotation.project();
        thisGenerator.serviceId = thisGenerator.annotation.serviceId();
        thisGenerator.url = thisGenerator.annotation.url();
        thisGenerator.protocol = thisGenerator.annotation.protocol();
        thisGenerator.requestMethod = thisGenerator.annotation.method();
        thisGenerator.type = thisGenerator.annotation.type();
        thisGenerator.authorization = thisGenerator.annotation.authorization();
        thisGenerator.projectVersion = thisGenerator.annotation.projectVersion();
        thisGenerator.serviceVersion = thisGenerator.annotation.serviceVersion();
        thisGenerator.group = thisGenerator.annotation.group();
        thisGenerator.profile = thisGenerator.annotation.profile();
        thisGenerator.timeout = thisGenerator.annotation.timeout();
        thisGenerator.retry = thisGenerator.annotation.retry();
        thisGenerator.loadBalance = thisGenerator.annotation.loadBalance();
        thisGenerator.callback = thisGenerator.annotation.callback();
        thisGenerator.callHandler = thisGenerator.annotation.handler();
        thisGenerator.fallback = thisGenerator.annotation.fallback();

        for(Field field : target.getDeclaredFields()) {
            if(Log.class.equals(field.getType()) && !Modifier.isPrivate(field.getModifiers())){
                thisGenerator.loggerVar = field.getName();
                break;
            }
        }
        thisGenerator.validate();
        thisGenerator.start();

        return thisGenerator;
    }

    private void validate(){
        if(StrUtil.isBlank(url)) {
            throw new RuntimeException("url cannot be empty.");
        }

        userAgent = StrUtil.isBlank(serviceId) ? ReadyCloud.getConfig().getHttpClient().getUserAgent() : DEFAULT_USER_AGENT;

        if(url.startsWith("http:") || url.startsWith("https:") || url.startsWith("ws:") || url.startsWith("wss:")){
            try {
                URL urlObj = new URL(url);
                url_protocol = urlObj.getProtocol();
                url_host = urlObj.getHost();
                url_port = urlObj.getPort(); 
                url_path = urlObj.getPath();
            } catch (MalformedURLException e){
                throw new RuntimeException("url '" + url + "' is invalid.");
            }
        } else if(url.startsWith("/")){
            url_path = url;
        } else {
            url_path = "/" + url;
        }

        if(url_protocol != null && !url_protocol.equals(protocol.name())){
            url_protocol = Call.Protocol.http.equals(protocol) ? url_protocol : protocol.name();
        } else {
            url_protocol = url_protocol != null ? url_protocol : protocol.name();
        }
        url_protocol = url_protocol.toLowerCase();
        url_host = url_host != null ? url_host : "localhost";
        if(("http".equals(url_protocol) || "ws".equals(url_protocol)) && url_port <= 0) {
            url_port = 80;
        }
        if(("https".equals(url_protocol) || "wss".equals(url_protocol)) && url_port <= 0) {
            url_port = 443;
        }

        int cfgRetry = ReadyCloud.getConfig().getHttpClient().getRetry();
        retry = retry < 0 ? cfgRetry : retry;

        int cfgTimeout = ReadyCloud.getConfig().getHttpClient().getTimeout() > 0 ? ReadyCloud.getConfig().getHttpClient().getTimeout() : ClientConfig.DEFAULT_TIMEOUT;
        timeout = timeout > 0 ? timeout : cfgTimeout;

        profile = StrUtil.notBlank(profile) ? profile : Ready.getBootstrapConfig().getActiveProfile();

        if(url_path.indexOf("}") > url_path.indexOf("{")){
            Matcher m = pattern.matcher(url_path);
            while (m.find()) {
                boolean found = false;
                for(int i = 0; i < parameters.length; i++){
                    if(parameters[i].getName().equals(m.group(1))){
                        found = true;
                        pathParam.put(m.group(1), i);
                    }
                }
                if(!found) throw new RuntimeException("Url '" + url + "' contains path variable {" + m.group(1) + "}, but couldn't find parameter named '" + m.group(1) + "' on " + method.getDeclaringClass().getCanonicalName() + "." + method.getName() + "().");
            }
        }

        if(callback == null) callback = DefaultCallback.class;
        if(callHandler == null) callHandler = DefaultCallHandler.class;
        if(fallback == null) fallback = DefaultFallback.class;
    }

    private void start(){

        addClassImport(HashMap.class);
        addClassImport(Headers.class);
        addClassImport(Map.class);
        addClassImport(URI.class);
        addClassImport(Ready.class);
        addClassImport(ContentType.class);
        addClassImport(HttpClient.class);
        addClassImport(HttpRequest.class);
        addClassImport(CallHandler.class);
        addClassImport(RuntimeException.class);
        addClassImport(JsonProcessingException.class);

        String requestBody = "\n" +
                "       String requestBody = \"\";\n" +
                "       String paramString = \"\";\n" +
                "       Map<String, Object> params = new HashMap<>();\n";
        int requestParamCount = 0;
        String authorizationVar = null;
        if(parameters.length > 0) {
            for (int i = 0; i < parameters.length; i++) {
                if (!ClassUtil.isSimpleType(parameters[i].getType())) {
                    if (!RequestMethod.hasBody(requestMethod) || !Call.RequestType.json.equals(type)) {
                        throw new RuntimeException("invalid type of parameter " + parameters[i].getName() + " for " + requestMethod + " type of request on " + method.getDeclaringClass().getCanonicalName() + "." + method.getName() + "(). Only simple types can be used as form or url parameters.");
                    }
                }
                if (parameters[i].getName().equals(authorization)) {
                    authorizationVar = "p" + i;
                    continue;
                }
                if (pathParam.containsValue(i)) continue;
                requestBody += "       params.put(\"" + parameters[i].getName() + "\", p" + i + ");\n";
                requestParamCount++;
            }
        }

        requestBody +=
                "       final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();\n" +
                "       HttpRequest.BodyPublisher publisher;\n";
        if (RequestMethod.hasBody(requestMethod)) {
            if(Call.RequestType.json.equals(type)) {
                if(requestParamCount > 0) {
                    requestBody += "       try {\n";
                    requestBody += "       requestBody = Ready.getConfig().getJsonMapper().writeValueAsString(params);\n" + logger("\"request body json: \" + requestBody", null);
                    requestBody += "       } catch (JsonProcessingException e) {\n";
                    requestBody += "       " + logger("\"parameters of " + method.getDeclaringClass().getCanonicalName() + "." + method.getName() + "()" + " can not convert to json: \"", "e");
                    requestBody += "       throw new RuntimeException(e);\n";
                    requestBody += "       }\n";
                }
                requestBody += "       requestBuilder.header(Headers.CONTENT_TYPE_STRING, ContentType.APPLICATION_JSON.toString());\n";
            } else {
                if(requestParamCount > 0)
                requestBody += "       requestBody = HttpClient.getFormDataString(params);\n";
                requestBody += "       requestBuilder.header(Headers.CONTENT_TYPE_STRING, ContentType.APPLICATION_FORM_URLENCODED.toString());\n";
            }
            
            requestBody += "       publisher = HttpClient.getStringBodyPublisher(requestBody);\n";
        } else {
            if(requestParamCount > 0)
            requestBody += "       paramString = \"?\" + HttpClient.getFormDataString(params);\n";
            requestBody += "       publisher = HttpClient.getNoBodyPublisher();\n";
        }
        if(authorizationVar != null) {
            requestBody += "       requestBuilder.header(Headers.AUTHORIZATION_STRING, " + authorizationVar + ".toString());\n";
        }
        requestBody += "       requestBuilder.header(Headers.USER_AGENT_STRING, \"" + userAgent + "\");\n";
        requestBody += "       requestBuilder.method(\"" + requestMethod.name() + "\", publisher);\n";
        requestBody += "       String path = \"" + url_path + "\";\n";
        for(String var : pathParam.keySet()){
            requestBody += "       path = path.replace(\"{" + var + "}\", p" + pathParam.get(var) + ".toString());\n";
        }
        if(StrUtil.isBlank(serviceId)) { 
            requestBody += "       requestBuilder.uri(URI.create(\"" + url_protocol + "://" + url_host + ":" + url_port + "\" + path + paramString));\n";
        }
        replace = requestBody + "       CallHandler handler = Ready.beanManager().get(" + callHandler.getCanonicalName() + ".class, true, null, null);\n";
        replace += "       returnObject = handler.handle(this, \"" + method.getName() + "\", \"" + url_protocol + "\", \"" + project + "\", \"" + projectVersion + "\", \"" + serviceId + "\", \"" + serviceVersion + "\", \"" + profile + "\", path + paramString, " + timeout + ", " + retry + ", " + returnType.getCanonicalName() + ".class, " + callback.getCanonicalName() + ".class, " + fallback.getCanonicalName() + ".class, requestBuilder);\n";
    }

    private String logger(String content, String exception) {
        if(this.loggerVar != null) {
            if(exception == null) {
                return "if (" + this.loggerVar + ".isDebugEnabled()) " + this.loggerVar + ".debug(" + content + ");\n";
            } else {
                return "if (" + this.loggerVar + ".isErrorEnabled()) " + this.loggerVar + ".error(" + exception + ", " + content + ");\n";
            }
        } else {
            String ret = "System.out.println(" + content + ");\n";
            if(exception != null) {
                ret = "System.err.println(" + content + ");\n";
                ret = ret + exception + ".printStackTrace();\n";
            }
            return ret;
        }
    }

    @Override
    public boolean isReplace(){
        return replace != null;
    }

    @Override
    public String getInsertCode(){
        return insert;
    }

    @Override
    public String getReplaceCode(){
        return replace;
    }

    @Override
    public String getAppendCode(){
        return append;
    }

    protected void addClassProperty(String line){
        if(StrUtil.isBlank(line)) return;
        if(line.endsWith(";")) line = line.substring(0, line.length()-1);
        addClassData("classProperties", line);
    }

    protected void addClassImport(Class<?> clazz){
        addClassImport(clazz.getCanonicalName());
    }

    protected void addClassImport(String line){
        if(StrUtil.isBlank(line)) return;
        if(line.endsWith(";")) line = line.substring(0, line.length()-1);
        addClassData("classImports", line);
    }

    protected void addMethodException(Class<?> clazz){
        String throwsString = (String)methodData.get("throws");
        if(StrUtil.notBlank(throwsString)){
            throwsString += ", " + clazz.getCanonicalName();
        } else {
            throwsString = " throws " + clazz.getCanonicalName();
        }
        methodData.put("throws", throwsString);
    }

    private void addClassData(String name, String value){
        LinkedList<String> properties = null;
        properties = (LinkedList<String>)clazzData.get(name);
        if(properties == null) properties = new LinkedList<>();
        if(!properties.contains(value)) properties.add(value);
        clazzData.put(name, properties);
    }

}
