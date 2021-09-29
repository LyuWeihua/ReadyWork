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
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import work.ready.core.handler.BaseHandler;
import work.ready.core.handler.RequestMethod;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.nio.file.Paths;

public class PathResourceHandler extends BaseHandler {

    private static final Log logger = LogFactory.getLog(PathResourceHandler.class);
    PathHandler pathHandler;
    PathResourceConfig config;

    public PathResourceHandler() {
        setOrder(4);
        logger.info("PathResourceHandler is loaded");
    }

    @Override
    public void initialize(){
        if(applicationConfig.getStaticResource() == null) return;
        config = applicationConfig.getStaticResource().getPathResource();
        if(config == null || config.getPaths() == null || config.getPaths().size() == 0) return;
        String rootPath;
        if(Ready.rootExist()){
            rootPath = Ready.root().toAbsolutePath().toString();
        } else {
            rootPath = System.getProperty("user.dir");
            if(logger.isWarnEnabled())
                logger.warn("Couldn't find root path, use current location %s as root path", System.getProperty("user.dir"));
        }
        for(PathResource pathResource : config.getPaths()){
            if(pathResource.getPath() == null || pathResource.getBasePath() == null || !pathResource.getPath().startsWith("/") || !pathResource.getBasePath().startsWith("/")){
                throw new RuntimeException("PathResource: path resource url path and basePath in application config should start with '/'");
            }
            if(pathHandler == null) pathHandler = new PathHandler(20);
            if( pathResource.isPrefix()) {
                pathHandler.addPrefixPath(pathResource.getPath(), new ResourceHandler(new PathResourceManager(Paths.get(rootPath + pathResource.getBasePath()), pathResource.getTransferMinSize()))
                                .setDirectoryListingEnabled(pathResource.isDirectoryListingEnabled()));
            } else {
                pathHandler.addExactPath(pathResource.getPath(), new ResourceHandler(new PathResourceManager(Paths.get(rootPath + pathResource.getBasePath()), pathResource.getTransferMinSize()))
                                .setDirectoryListingEnabled(pathResource.isDirectoryListingEnabled()));
            }
            manager.addHandler(pathResource.getPath() + (pathResource.isPrefix() ? "*" : ""), RequestMethod.GET, this);
            if(logger.isDebugEnabled())
                logger.debug("Mapping static resource path %s to url %s", rootPath + pathResource.getBasePath() , pathResource.getPath());
        }
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        pathHandler.handleRequest(httpServerExchange);
    }
}
