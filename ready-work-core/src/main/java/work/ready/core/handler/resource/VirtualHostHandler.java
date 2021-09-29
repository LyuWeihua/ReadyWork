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
package work.ready.core.handler.resource;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.NameVirtualHostHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import work.ready.core.handler.BaseHandler;
import work.ready.core.handler.RequestMethod;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.nio.file.Paths;

public class VirtualHostHandler extends BaseHandler {

    private static final Log logger = LogFactory.getLog(VirtualHostHandler.class);
    VirtualHostConfig config;
    NameVirtualHostHandler virtualHostHandler;

    public VirtualHostHandler() {
        setOrder(4);
        logger.info("StaticResourceHandler is loaded");
    }

    @Override
    public void initialize(){
        if(applicationConfig.getStaticResource() == null) return;
        config = applicationConfig.getStaticResource().getVirtualHost();
        if(config == null || config.getHosts() == null || config.getHosts().size() == 0) return;
        String rootPath;
        if(Ready.rootExist()){
            rootPath = Ready.root().toAbsolutePath().toString();
        } else {
            rootPath = System.getProperty("user.dir");
            if(logger.isWarnEnabled())
                logger.warn("Couldn't find root path, use current location %s as root path", System.getProperty("user.dir"));
        }
        for(VirtualHost host: config.getHosts()) {
            if(host.getPath() == null || host.getBasePath() == null || !host.getPath().startsWith("/") || !host.getBasePath().startsWith("/")){
                throw new RuntimeException("VirtualHost: static resource url path and basePath in application config should start with '/'");
            }
            if(virtualHostHandler == null) virtualHostHandler = new NameVirtualHostHandler();
            if(host.getDomain() == null || host.getDomain().isBlank()) throw new RuntimeException("VirtualHost: host domain is invalid");
            virtualHostHandler.addHost(host.getDomain(), new PathHandler(20).addPrefixPath(host.getPath(), new ResourceHandler((new PathResourceManager(Paths.get(rootPath + host.getBasePath()), host.getTransferMinSize()))).setDirectoryListingEnabled(host.isDirectoryListingEnabled())));
            if(logger.isDebugEnabled())
                logger.debug("Mapping static resource path %s to url %s, bind on %s domain", rootPath + host.getBasePath() , host.getPath(), host.getDomain());
            manager.addHandler(new String[]{host.getDomain()}, new String[]{"*"}, new String[]{host.getPath() + "*"}, new RequestMethod[]{RequestMethod.GET}, this);
        }
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        virtualHostHandler.handleRequest(httpServerExchange);
    }
}
