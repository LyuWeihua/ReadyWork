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

package work.ready.cloud.log;

import org.apache.ignite.IgniteLogger;
import org.apache.ignite.logger.java.JavaLogger;
import org.apache.ignite.logger.java.JavaLoggerFileHandler;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IgniteJavaLogger extends JavaLogger {

    private Level igniteInternalLevel = Level.INFO; 

    public IgniteJavaLogger(){
        super(LogFactory.getLog("").getLogger()); 
        Logger logger = LogFactory.getLog("").getLogger();
        for(var handler : logger.getHandlers()) {
            if(handler.getClass().equals(ConsoleHandler.class) || handler.getClass().equals(JavaLoggerFileHandler.class)) {
                logger.removeHandler(handler);
            }
        }
    }

    public IgniteJavaLogger(final Logger impl) {
        super(impl);
        String igniteLevel = Optional.ofNullable(Ready.getBootstrapConfig().getLog().getIgniteLogLevel()).orElse("INFO").toUpperCase();
        if(LogFactory.slf4jToLevel(igniteLevel) != null){
            igniteInternalLevel = LogFactory.slf4jToLevel(igniteLevel);
        } else {
            getLogger(IgniteJavaLogger.class).error("Unknown ignite log level: " + igniteLevel);
        }
    }

    @Override
    public IgniteLogger getLogger(Object ctgr) {
        Logger logger = ctgr == null ? Logger.getLogger("") : Logger.getLogger(
                ctgr instanceof Class ? ((Class)ctgr).getName() : String.valueOf(ctgr));
        logger.setLevel(igniteInternalLevel);
        return new JavaLogger(logger);
    }

}
