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
import java.util.Map;

public class StaticResourceHandler extends BaseHandler {

    private static final Log logger = LogFactory.getLog(StaticResourceHandler.class);
    PathHandler pathHandler;
    Map<String, String> config;

    public StaticResourceHandler() {
        setOrder(4);
        logger.info("StaticResourceHandler is loaded");
    }

    @Override
    public void initialize(){
        if(applicationConfig.getStaticResource() == null) return;
        config = applicationConfig.getStaticResource().getMapping();
        if(config == null || config.size() == 0) return;
        String rootPath;
        if(Ready.rootExist()){
            rootPath = Ready.root().toAbsolutePath().toString();
        }else{
            rootPath = System.getProperty("user.dir");
            if(logger.isWarnEnabled())
                logger.warn("Couldn't find root path, use current location %s as root path", System.getProperty("user.dir"));
        }
        config.forEach((url, path)->{
                if(!url.startsWith("/") || !path.startsWith("/")){
                    throw new RuntimeException("StaticResource: static resource url and path mappings in application config should start with '/'");
                }

                PathResource resConfig = new PathResource();
                resConfig.setDirectoryListingEnabled(false);
                resConfig.setTransferMinSize(1024);

                path = rootPath + path;

                if(url.endsWith("/")) url += "*";
                if(url.endsWith("*")){
                    resConfig.setPrefix(true);
                    resConfig.setPath(url.substring(0,url.length()-1));
                }else{
                    resConfig.setPrefix(false);
                    resConfig.setPath(url);
                }
                resConfig.setBasePath(path);

                if(pathHandler == null) pathHandler = new PathHandler(20);
                if( resConfig.isPrefix()) {
                    pathHandler.addPrefixPath(resConfig.getPath(), new ResourceHandler(new PathResourceManager(Paths.get(resConfig.getBasePath()), resConfig.getTransferMinSize()))
                                    .setDirectoryListingEnabled(resConfig.isDirectoryListingEnabled()));
                } else {
                    pathHandler.addExactPath(resConfig.getPath(), new ResourceHandler(new PathResourceManager(Paths.get(resConfig.getBasePath()), resConfig.getTransferMinSize()))
                                    .setDirectoryListingEnabled(resConfig.isDirectoryListingEnabled()));
                }
                manager.addHandler(url, RequestMethod.GET, this);
                if(logger.isDebugEnabled())
                logger.debug("Mapping static resource %s to url %s", path, resConfig.getPath());
            }
        );

    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        pathHandler.handleRequest(httpServerExchange);
    }
}
