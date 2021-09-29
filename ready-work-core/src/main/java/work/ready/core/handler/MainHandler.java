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

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormEncodedDataDefinition;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.xnio.channels.StreamSourceChannel;
import work.ready.core.handler.request.DefaultUploadPathProvider;
import work.ready.core.handler.request.MultiPartParserDefinition;
import work.ready.core.handler.request.RequestBodyReader;
import work.ready.core.handler.request.UploadPathProvider;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.ApplicationContext;
import work.ready.core.server.*;
import work.ready.core.tools.StrUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MainHandler extends BaseHandler {
    private static final Log logger = LogFactory.getLog(MainHandler.class);
    protected final ApplicationContext context;
    private final FormParserFactory formParserFactory;
    private final RequestHandler handler;
    private final GracefulShutdownHandler shutdownHandler;
    private final boolean isHealthCheck;
    private final String healthCheckPath;

    public MainHandler(ApplicationContext context) {
        this.context = context;
        this.handler = context.webServer.getRequestHandler();
        this.shutdownHandler = context.webServer.getShutdownHandler();
        setManager(context.handlerManager);
        manager.mainHandler = this;
        setApplicationConfig(Ready.getApplicationConfig(context.application.getName()));

        if(applicationConfig.getServer().isHealthCheck() && StrUtil.notBlank(applicationConfig.getServer().getHealthCheckPath())) {
            isHealthCheck = true;
            healthCheckPath = applicationConfig.getServer().getHealthCheckPath();
        } else {
            isHealthCheck = false;
            healthCheckPath = null;
        }

        var builder = FormParserFactory.builder(false);
        var multiPartParser = new MultiPartParserDefinition();
        multiPartParser.setFileSizeThreshold(Constant.DEFAULT_FILE_SIZE_THRESHOLD);
        multiPartParser.setMaxIndividualFileSize(applicationConfig.getMaxUploadFileSize());
        if(applicationConfig.isDirectlySaveUploadFile()){
            Path uploadPath = Ready.root().resolve(applicationConfig.getUploadPath()).toAbsolutePath();
            if(Ready.rootExist() && !Files.exists(uploadPath)) {
                try { Files.createDirectories(uploadPath); } catch (IOException e) {}
            }
            if(Files.exists(uploadPath) && Files.isDirectory(uploadPath) && Files.isWritable(uploadPath)) {
                multiPartParser.setDirectlySaveUploadFile(true);
                multiPartParser.setFileSizeThreshold(0); 
                multiPartParser.setUploadPath(uploadPath);
                UploadPathProvider provider = Ready.beanManager().get(UploadPathProvider.class);
                if(provider == null) provider = new DefaultUploadPathProvider();
                multiPartParser.setPathProvider(provider);
            } else {
               throw new RuntimeException("DirectlySaveUploadFile has been set to True in application config, but uploadPath cannot be used, path: " + uploadPath.toString());
            }
        }
        builder.addParsers(new FormEncodedDataDefinition(), multiPartParser);
        builder.setDefaultCharset(Constant.DEFAULT_ENCODING);
        formParserFactory = builder.build();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (isHealthCheck && healthCheckPath.equals(exchange.getRequestPath())) {      
            exchange.getResponseSender().send(Constant.DEFAULT_HEALTH_RESPONSE);
            exchange.endExchange(); 
            return;
        }

        if (shutdownHandler.handle(exchange)) {
            return;
        }

        manager.start(exchange);
    }

    public void processRequest(HttpServerExchange exchange) throws Exception {
        if (hasBody(exchange)) {    
            FormDataParser parser = formParserFactory.createParser(exchange);
            if (parser != null) {
                parser.parse(handler);
                return;
            }

            var reader = new RequestBodyReader(exchange, handler);
            StreamSourceChannel channel = exchange.getRequestChannel();
            reader.read(channel);  
            if (!reader.complete()) {
                channel.getReadSetter().set(reader);
                channel.resumeReads();
                return;
            }
        }
        handler.handleRequest(exchange);
    }

    private boolean hasBody(HttpServerExchange exchange) {
        int length = (int) exchange.getRequestContentLength();
        if (length == 0) return false;  

        HttpString method = exchange.getRequestMethod();
        return Methods.POST.equals(method) || Methods.PUT.equals(method) || Methods.PATCH.equals(method);
    }

}
