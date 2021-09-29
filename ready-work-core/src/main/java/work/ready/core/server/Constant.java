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
package work.ready.core.server;

import io.undertow.util.HttpString;

import java.nio.charset.Charset;
import java.util.StringTokenizer;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class Constant {

    public static final String JVM_VENDOR = System.getProperty("java.vm.vendor");
    public static final String JVM_VERSION = System.getProperty("java.vm.version");
    public static final String JVM_NAME = System.getProperty("java.vm.name");
    public static final String JVM_SPEC_VERSION = System.getProperty("java.specification.version");
    public static final String JAVA_VERSION = System.getProperty("java.version");
    public static final String OS_NAME = System.getProperty("os.name");
    public static final boolean LINUX;
    public static final boolean WINDOWS;
    public static final boolean SUN_OS;
    public static final boolean MAC_OS_X;
    public static final boolean FREE_BSD;
    public static final String OS_ARCH;
    public static final String OS_VERSION;
    public static final String JAVA_VENDOR;
    private static final int JVM_MAJOR_VERSION;
    private static final int JVM_MINOR_VERSION;
    public static final boolean JRE_IS_64BIT;
    public static final boolean JRE_IS_MINIMUM_JAVA8;
    public static final boolean JRE_IS_MINIMUM_JAVA11;

    private Constant() {
    }

    static {
        LINUX = OS_NAME.startsWith("Linux");
        WINDOWS = OS_NAME.startsWith("Windows");
        SUN_OS = OS_NAME.startsWith("SunOS");
        MAC_OS_X = OS_NAME.startsWith("Mac OS X");
        FREE_BSD = OS_NAME.startsWith("FreeBSD");
        OS_ARCH = System.getProperty("os.arch");
        OS_VERSION = System.getProperty("os.version");
        JAVA_VENDOR = System.getProperty("java.vendor");
        StringTokenizer st = new StringTokenizer(JVM_SPEC_VERSION, ".");
        JVM_MAJOR_VERSION = Integer.parseInt(st.nextToken());
        if (st.hasMoreTokens()) {
            JVM_MINOR_VERSION = Integer.parseInt(st.nextToken());
        } else {
            JVM_MINOR_VERSION = 0;
        }

        boolean is64Bit = false;
        String datamodel = null;

        try {
            datamodel = System.getProperty("sun.arch.data.model");
            if (datamodel != null) {
                is64Bit = datamodel.contains("64");
            }
        } catch (SecurityException var4) {
        }

        if (datamodel == null) {
            if (OS_ARCH != null && OS_ARCH.contains("64")) {
                is64Bit = true;
            } else {
                is64Bit = false;
            }
        }

        JRE_IS_64BIT = is64Bit;
        JRE_IS_MINIMUM_JAVA8 = JVM_MAJOR_VERSION > 1 || JVM_MAJOR_VERSION == 1 && JVM_MINOR_VERSION >= 8;
        JRE_IS_MINIMUM_JAVA11 = JVM_MAJOR_VERSION > 1 || JVM_MAJOR_VERSION == 1 && JVM_MINOR_VERSION >= 11;
    }

    public static String DEFAULT_ENCODING = "UTF-8";

    public static Charset DEFAULT_CHARSET = UTF_8;

    public static String DEV_PROFILE = "dev";

    public static String DEFAULT_BASE_WORKING_DIR = "ready.work";

    public static String READY_HOST_IP_PROPERTY = "ready.host_ip";

    public static String READY_WORK_ROOT_PATH_PROPERTY = "ready.root_path";
    
    public static String READY_CONFIG_LOCAL_REPOSITORY_PROPERTY = "ready.config_repository";
    
    public static String DEFAULT_CONFIG_REPOSITORY_DIR = "config-repository";
    
    public static String READY_WORK_CONFIG_SERVER_PROPERTY = "ready.config_server";
    
    public static String READY_WORK_CONFIG_DIR_PROPERTY = "ready.config_path";
    
    public static String DEFAULT_CONFIG_FILE_DIR = "config";

    public static String DEFAULT_LIB_FILE_DIR = "lib";

    public static String DEFAULT_PLUGIN_FILE_DIR = "plugin";

    public static String DEFAULT_BASE_UPLOAD_PATH = "upload";

    public static String DEFAULT_BASE_DOWNLOAD_PATH = "download";

    public static String BOOTSTRAP_CONFIG_NAME = "bootstrap";

    public static String APPLICATION_CONFIG_NAME = "application";

    public static String STATUS_CONFIG_NAME = "status";

    public static String VALUES_CONFIG_NAME = "values";

    public static String DEFAULT_VIEW_FILE_EXT = ".html";

    public static long DEFAULT_MAX_ENTITY_SIZE = 1024 * 1024 * 20; 

    public static int DEFAULT_MAX_UPLOAD_FILE_SIZE = 1024 * 1024 * 10; 

    public static int DEFAULT_FILE_SIZE_THRESHOLD = 1024 * 100; 

    public static int DEFAULT_I18N_MAX_AGE_OF_COOKIE = 999999999;

    public static int DEFAULT_AOP_CACHE_TIME = 1000;

    public static int DEFAULT_EHCACHE_MAX_ENTRIES_LOCAL_HEAP = 1024*5;

    public static String CORRELATION_ID_STRING = "X-Correlation-Id";
    public static String TRACEABILITY_ID_STRING = "X-Traceability-Id";
    public static String USER_ID_STRING = "user_id";
    public static String CLIENT_ID_STRING = "client_id";
    public static String SCOPE_CLIENT_ID_STRING = "scope_client_id";
    public static String SCOPE_STRING = "scope";
    public static String ENDPOINT_STRING = "endpoint";
    public static String CSRF_STRING = "csrf";

    public static String SCOPE_TOKEN_STRING = "X-Scope-Token";
    public static String HEALTH_TOKEN_STRING = "X-Health-Token";
    public static String CSRF_TOKEN_STRING = "X-CSRF-TOKEN";

    public static final HttpString CORRELATION_ID = new HttpString(CORRELATION_ID_STRING);
    public static final HttpString TRACEABILITY_ID = new HttpString(TRACEABILITY_ID_STRING);
    public static final HttpString SCOPE_TOKEN = new HttpString(SCOPE_TOKEN_STRING);

    public static String FRAMEWORK_NAME = "Ready.Work";
    public static String METHOD_CONFIG_PREFIX = "methodConfig.";

    public static String APPLICATION_READY_SWITCHER = "application_ready_switcher";

    public static int MILLS = 1;
    public static int SECOND_MILLS = 1000;
    public static int MINUTE_MILLS = 60 * SECOND_MILLS;

    public static String READY_VERSION = "1.0.0.000000"; 
    public static String READY_API_VERSION = "v1.0"; 

    public static String DEFAULT_PROJECT = "project";

    public static String DEFAULT_VERSION = "1.0.0.000000";
    public static String DEFAULT_API_VERSION = "v1.0";
    public static String DEFAULT_VALUE = "default";
    public static int DEFAULT_INT_VALUE = 0;
    public static String DEFAULT_HEALTH_RESPONSE = "READY";

    public static String NODE_TYPE_APPLICATION = "application";
    public static String NODE_TYPE_APPLICATION_WITH_OLTP = "application_with_oltp";
    public static String NODE_TYPE_APPLICATION_WITH_OLAP = "application_with_olap";
    public static String NODE_TYPE_COMPUTING_CPU = "computing_cpu";
    public static String NODE_TYPE_COMPUTING_GPU = "computing_gpu";
    public static String NODE_TYPE_COMPUTING_CPU_GPU = "computing_cpu_gpu";
    public static String NODE_TYPE_MEMORY_POOL = "memory_pool";
    public static String NODE_TYPE_CONFIG = "config";
    public static String NODE_TYPE_OAUTH2 = "oauth2";

    public static String PROTOCOL_SEPARATOR = "://";
    public static String PROTOCOL_DEFAULT = "ready";
    public static String PROTOCOL_HTTPS = "https";
    public static String PROTOCOL_WSS = "wss";
    public static int DEFAULT_HTTPS_PORT = 8443;
    public static String PROTOCOL_HTTP = "http";
    public static String PROTOCOL_WS = "ws";
    public static int DEFAULT_HTTP_PORT = 8080;
    public static String TAG_ENVIRONMENT = "profile";

}
