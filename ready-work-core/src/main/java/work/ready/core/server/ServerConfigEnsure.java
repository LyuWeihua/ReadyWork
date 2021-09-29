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

import static work.ready.core.server.Ready.logger;

public class ServerConfigEnsure {

    protected static void confirm(ServerConfig serverConfig) {
        String warnMessage = "Server config: '%s' set in bootstrap.yml is invalid, has been reset to default value.";
        if (serverConfig.getBacklog() == null || serverConfig.getBacklog() <= 0) {
            serverConfig.setBacklog(10000);
            logger.warn(warnMessage, "backlog");
        }
        if (serverConfig.getIoThreads() == null || serverConfig.getIoThreads() <= 0) {
            serverConfig.setIoThreads(Runtime.getRuntime().availableProcessors() * 2);
            logger.warn(warnMessage, "ioThreads");
        }
        if (serverConfig.getWorkerThreads() == null || serverConfig.getWorkerThreads() <= 0) {
            serverConfig.setWorkerThreads(200);
            logger.warn(warnMessage, "workerThreads");
        }
        if (serverConfig.getBufferSize() == null || serverConfig.getBufferSize() <= 0) {
            serverConfig.setBufferSize(1024 * 16);
            logger.warn(warnMessage, "bufferSize");
        }
        if (serverConfig.getServerString() == null || serverConfig.getServerString().isEmpty()) {
            serverConfig.setServerString("ReadyWork");
            logger.warn(warnMessage, "serverString");
        }
        if (serverConfig.isAlwaysSetDate() == null) {
            serverConfig.setAlwaysSetDate(true);
            logger.warn(warnMessage, "alwaysSetDate");
        }
        if (serverConfig.getMaxEntitySize() == null || serverConfig.getMaxEntitySize() <= 0) {
            serverConfig.setMaxEntitySize(Constant.DEFAULT_MAX_ENTITY_SIZE);
            logger.warn(warnMessage, "maxEntitySize");
        }
        if (serverConfig.isAllowUnescapedCharactersInUrl() == null) {
            serverConfig.setAllowUnescapedCharactersInUrl(false);
            logger.warn(warnMessage, "allowUnescapedCharactersInUrl");
        }
    }

}
