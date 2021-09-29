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

import work.ready.core.config.BaseConfig;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.zip.Deflater;

public class ServerConfig extends BaseConfig {

    private String ip = "0.0.0.0";
    private Integer httpPort = Constant.DEFAULT_HTTP_PORT;
    private boolean enableHttp = true;
    private Integer httpsPort = Constant.DEFAULT_HTTPS_PORT;
    private boolean dynamicPort = false;
    private boolean enableGzip = true;
    private Integer gzipLevel = Deflater.DEFAULT_COMPRESSION;
    private boolean enableHttps = false;
    private boolean httpToHttps = false;
    private Integer minPort = 1024; 
    private Integer maxPort = 65534;
    private Integer bufferSize = 1024 * 16;
    private Integer ioThreads = Runtime.getRuntime().availableProcessors() * 2;
    private Integer workerThreads = 200;
    private Integer backlog = 10000;
    private Boolean alwaysSetDate = false; 
    private Boolean allowUnescapedCharactersInUrl = false;
    private Long maxEntitySize = Constant.DEFAULT_MAX_ENTITY_SIZE;
    private boolean singletonController = true;
    private String serverString = "ReadyWork";
    private boolean healthCheck = true;
    private String healthCheckPath = "/health-check";
    private String crucialCheckPath = null;

	public ServerConfig() {
    }

    public String getIp() {
        return ip;
    }

    public ServerConfig setIp(String ip) {
        this.ip = ip;
        return this;
    }

    public Integer getHttpPort() {
    	String port = Ready.getProperty("ready.http_port");
    	if (port != null) {
    		try {
                httpPort = Integer.parseInt(port);
    		}
    		catch (NumberFormatException ex) {
    			ex.printStackTrace(System.err);
    		}
    	}
        return httpPort;
    }

    public boolean isEnableGzip() {
        return enableGzip;
    }

    public ServerConfig setEnableGzip(boolean enableGzip) {
        this.enableGzip = enableGzip;
		return this;
	}

    public int getGzipLevel() {
        return gzipLevel;
    }

    public ServerConfig setGzipLevel(int gzipLevel) {
        this.gzipLevel = gzipLevel;
		return this;
	}

    public ServerConfig setHttpPort(int httpPort) {
        this.httpPort = httpPort;
        setEnableHttp(true);
		return this;
	}

    public boolean isEnableHttp() {
        return enableHttp;
    }

    public ServerConfig setEnableHttp(boolean enableHttp) {
        this.enableHttp = enableHttp;
		return this;
	}

    public Integer getHttpsPort() {
    	String port = Ready.getProperty("ready.https_port");
    	if (port != null) {
    		try {
                httpsPort = Integer.parseInt(port);
    		}
    		catch (NumberFormatException ex) {
    			ex.printStackTrace(System.err);
    		}
    	}
        return httpsPort;
    }

    public ServerConfig setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
        setEnableHttps(true);
		return this;
	}

    public boolean isEnableHttps() {
        return enableHttps;
    }

    public ServerConfig setEnableHttps(boolean enableHttps) {
        this.enableHttps = enableHttps;
		return this;
	}

    public boolean isHttpToHttps() {
        return httpToHttps;
    }

    public ServerConfig setHttpToHttps(boolean httpToHttps) {
        this.httpToHttps = httpToHttps;
		return this;
	}

    public boolean isDynamicPort() {
        return dynamicPort;
    }

    public ServerConfig setDynamicPort(boolean dynamicPort) {
        this.dynamicPort = dynamicPort;
		return this;
	}

    public int getMinPort() {
        return minPort;
    }

    public ServerConfig setMinPort(int minPort) {
        this.minPort = minPort;
		return this;
	}

    public int getMaxPort() {
        return maxPort;
    }

    public ServerConfig setMaxPort(int maxPort) {
        this.maxPort = maxPort;
		return this;
	}

    public Integer getBufferSize() {
        return bufferSize;
    }

    public ServerConfig setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
		return this;
	}

    public Integer getIoThreads() {
        return ioThreads;
    }

    public ServerConfig setIoThreads(int ioThreads) {
        this.ioThreads = ioThreads;
		return this;
	}

    public Integer getWorkerThreads() {
        return workerThreads;
    }

    public ServerConfig setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
		return this;
	}

    public Integer getBacklog() {
        return backlog;
    }

    public ServerConfig setBacklog(int backlog) {
        this.backlog = backlog;
		return this;
	}

    public Boolean isAlwaysSetDate() {
        return alwaysSetDate;
    }

    public ServerConfig setAlwaysSetDate(boolean alwaysSetDate) {
        this.alwaysSetDate = alwaysSetDate;
		return this;
	}

    public String getServerString() {
        return serverString;
    }

    public ServerConfig setServerString(String serverString) {
        this.serverString = serverString;
		return this;
	}

    public Boolean isAllowUnescapedCharactersInUrl() {
        return allowUnescapedCharactersInUrl;
    }

    public ServerConfig setAllowUnescapedCharactersInUrl(boolean allowUnescapedCharactersInUrl) {
        this.allowUnescapedCharactersInUrl = allowUnescapedCharactersInUrl;
		return this;
	}

    public Long getMaxEntitySize() {
        return maxEntitySize;
    }

    public ServerConfig setMaxEntitySize(long maxEntitySize) {
        this.maxEntitySize = maxEntitySize;
		return this;
	}

    public boolean isSingletonController() {
        return singletonController;
    }

    public ServerConfig setSingletonController(boolean singletonController) {
        this.singletonController = singletonController;
        return this;
    }

    public boolean isHealthCheck() {
        return healthCheck;
    }

    public ServerConfig setHealthCheck(boolean healthCheck) {
        this.healthCheck = healthCheck;
        return this;
    }

    public String getHealthCheckPath() {
        return healthCheckPath;
    }

    public ServerConfig setHealthCheckPath(String healthCheckPath) {
        this.healthCheckPath = healthCheckPath;
        return this;
    }

    public String getCrucialCheckPath() {
        return crucialCheckPath;
    }

    public ServerConfig setCrucialCheckPath(String crucialCheckPath) {
        this.crucialCheckPath = crucialCheckPath;
        return this;
    }

    @Override
    public void validate() {
        try {
            new URI(healthCheckPath);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid URI " + healthCheckPath, e);
        }
        if(crucialCheckPath != null) {
            try {
                new URI(crucialCheckPath);
            } catch (URISyntaxException e) {
                throw new RuntimeException("Invalid URI " + crucialCheckPath, e);
            }
        }
    }
}
