/**
 *
 * Original work Copyright (c) 2016 Network New Technologies Inc.
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

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import work.ready.core.handler.BaseHandler;
import work.ready.core.json.Json;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.HashUtil;
import work.ready.core.tools.NetUtil;
import work.ready.core.tools.StrUtil;

import java.io.InputStream;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.*;

public class SystemInfoServerModule extends BaseHandler {

    private static final String STATUS_SERVER_INFO_DISABLED = "ERROR10013";

    private static final Log logger = LogFactory.getLog(SystemInfoServerModule.class);

    public SystemInfoServerModule(){
        setOrder(6);
        logger.info("SystemInfoServerModule is loaded");
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if(isEnabled()) {
            Map<String, Object> infoMap = new LinkedHashMap<>();
            infoMap.put("deployment", getDeployment());
            infoMap.put("environment", getEnvironment(exchange));
            infoMap.put("security", getSecurity());
            infoMap.put("component", manager.getRegistry());
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(Json.getJson().toJson(infoMap));
        } else {
            exception(exchange, STATUS_SERVER_INFO_DISABLED);
        }
    }

    public Map<String, Object> getDeployment() {
        Map<String, Object> deploymentMap = new LinkedHashMap<>();
        deploymentMap.put("apiVersion", StrUtil.getJarVersion());
        deploymentMap.put("frameworkVersion", getFrameworkVersion());
        return deploymentMap;
    }

    public Map<String, Object> getEnvironment(HttpServerExchange exchange) {
        Map<String, Object> envMap = new LinkedHashMap<>();
        envMap.put("host", getHost(exchange));
        envMap.put("runtime", getRuntime());
        envMap.put("system", getSystem());
        return envMap;
    }

    public Map<String, Object> getSecurity() {
        Map<String, Object> secMap = new LinkedHashMap<>();
        Map<String, Object> moduleMap = manager.getRegistry();
        Set<String> fingerprints = new HashSet<>();
        for(Object module: moduleMap.entrySet()) {
            if(module instanceof Map) {
                Object fingerPrint = ((Map<?, ?>) module).get("FingerPrint");
                if(fingerPrint instanceof Collection) {
                    fingerprints.addAll((Collection<String>)fingerPrint);
                } else {
                    fingerprints.add(fingerPrint.toString());
                }
            }
        }

        if(fingerprints.size() > 0) {
            secMap.put("oauth2FingerPrints", new ArrayList<String>(fingerprints));
        }
        secMap.put("serverFingerPrint", getServerTlsFingerPrint());
        return secMap;
    }

    public Map<String, Object> getHost(HttpServerExchange exchange) {
        Map<String, Object> hostMap = new LinkedHashMap<>();
        String ip = "unknown";
        String hostname = "unknown";
        InetAddress inetAddress = NetUtil.getLocalAddress();
        ip = inetAddress.getHostAddress();
        hostname = inetAddress.getHostName();
        hostMap.put("ip", ip);
        hostMap.put("hostname", hostname);
        hostMap.put("dns", exchange.getSourceAddress().getHostName());
        return hostMap;
    }

    public Map<String, Object> getRuntime() {
        Map<String, Object> runtimeMap = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();
        runtimeMap.put("availableProcessors", runtime.availableProcessors());
        runtimeMap.put("freeMemory", runtime.freeMemory());
        runtimeMap.put("totalMemory", runtime.totalMemory());
        runtimeMap.put("maxMemory", runtime.maxMemory());
        return runtimeMap;
    }

    public Map<String, Object> getSystem() {
        Map<String, Object> systemMap = new LinkedHashMap<>();
        Properties properties = System.getProperties();
        systemMap.put("javaVendor", properties.getProperty("java.vendor"));
        systemMap.put("javaVersion", properties.getProperty("java.version"));
        systemMap.put("osName", properties.getProperty("os.name"));
        systemMap.put("osVersion", properties.getProperty("os.version"));
        systemMap.put("userTimezone", properties.getProperty("user.timezone"));
        return systemMap;
    }

    public String getFrameworkVersion() {
        String version = null;
        String path = "META-INF/maven/work.ready/ready-work-core/pom.properties";
        InputStream in = ClassLoader.getSystemResourceAsStream(path);
        try {
            Properties prop = new Properties();
            prop.load(in);
            version = prop.getProperty("version");
        } catch (Exception e) {
            
        } finally {
            try { in.close(); }
            catch (Exception ignored){}
        }
        return version;
    }

    private String getServerTlsFingerPrint() {
        String fingerPrint = null;
        String keystoreName = Ready.getMainApplicationConfig().getSecurity().getServerKeystoreName();
        String serverKeystorePass = Ready.getMainApplicationConfig().getSecurity().getServerKeystorePass();
        if(keystoreName != null) {
            try (InputStream stream = Ready.config().getInputStreamFromFile(keystoreName)) {
                KeyStore loadedKeystore = KeyStore.getInstance("PKCS12");
                loadedKeystore.load(stream, serverKeystorePass.toCharArray());
                X509Certificate cert = (X509Certificate)loadedKeystore.getCertificate("server");
                if(cert != null) {
                    fingerPrint = HashUtil.getCertFingerPrint(cert);
                } else {
                    logger.error("Unable to find the certificate with alias name as server in the keystore");
                }
            } catch (Exception e) {
                logger.error(e,"Unable to load server keystore ");
            }
        }
        return fingerPrint;
    }
}
