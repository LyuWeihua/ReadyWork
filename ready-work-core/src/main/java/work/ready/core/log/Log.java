/**
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

package work.ready.core.log;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static work.ready.core.aop.transformer.TransformerManager.METHOD_PREFIX;

public final class Log {
    public static final String LoggerName = Log.class.getName();

    public static Log get(Class<?> cls) {
        return LogFactory.getLog(cls);
    }

    public static Log get(String name) {
        return LogFactory.getLog(name);
    }

    private final Logger logger;
    private int forward = 0;

    Log(String name) {
        logger = Logger.getLogger(name);
    }

    public void setForward(int forward) {
        this.forward = forward;
    }

    public void setLevel(Level newLevel) {
        logger.setLevel(newLevel);
    }

    public Logger getLogger() {
        return logger;
    }

    public String getName() {
        return logger.getName();
    }

    public boolean isTraceEnabled() {
        return logger.isLoggable(Level.FINEST);
    }

    public void trace(String format, Object... args) {
        if (logger.isLoggable(Level.FINEST)) {
            log(Level.FINEST, format, args, null);
        }
    }

    public void trace(Callback callback) {
        if (logger.isLoggable(Level.FINEST)) {
            try {
                log(Level.FINEST, callback.get(), null);
            } catch (RuntimeException ex) {
                logUncaughtExceptionInCb(ex, null);
            }
        }
    }

    public void trace(Throwable t, String format, Object... args) {
        if (logger.isLoggable(Level.FINEST)) {
            log(Level.FINEST, format, args, t);
        }
    }

    public void trace(Throwable t, Callback callback) {
        if (logger.isLoggable(Level.FINEST)) {
            try {
                log(Level.FINEST, callback.get(), null);
            } catch (RuntimeException ex) {
                logUncaughtExceptionInCb(ex, t);
            }
        }
    }

    public boolean isDebugEnabled() {
        return logger.isLoggable(Level.FINE);
    }

    public void debug(String format, Object... args) {
        if (logger.isLoggable(Level.FINE)) {
            log(Level.FINE, format, args, null);
        }
    }

    public void debug(Callback callback) {
        if (logger.isLoggable(Level.FINE)) {
            try {
                log(Level.FINE, callback.get(), null);
            } catch (RuntimeException ex) {
                logUncaughtExceptionInCb(ex, null);
            }
        }
    }

    public void debug(Throwable t, String format, Object... args) {
        if (logger.isLoggable(Level.FINE)) {
            log(Level.FINE, format, args, t);
        }
    }

    public void debug(Throwable t, Callback callback) {
        if (logger.isLoggable(Level.FINE)) {
            try {
                log(Level.FINE, callback.get(), null);
            } catch (RuntimeException ex) {
                logUncaughtExceptionInCb(ex, t);
            }
        }
    }

    public boolean isInfoEnabled() {
        return logger.isLoggable(Level.INFO);
    }

    public void info(String format, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            log(Level.INFO, format, args, null);
        }
    }

    public void info(Callback callback) {
        if (logger.isLoggable(Level.INFO)) {
            try {
                log(Level.INFO, callback.get(), null);
            } catch (RuntimeException ex) {
                logUncaughtExceptionInCb(ex, null);
            }
        }
    }

    public void info(Throwable t, String format, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            log(Level.INFO, format, args, t);
        }
    }

    public void info(Throwable t, Callback callback) {
        if (logger.isLoggable(Level.INFO)) {
            try {
                log(Level.INFO, callback.get(), null);
            } catch (RuntimeException ex) {
                logUncaughtExceptionInCb(ex, t);
            }
        }
    }

    public boolean isWarnEnabled() {
        return logger.isLoggable(Level.WARNING);
    }

    public void warn(String format, Object... args) {
        if (logger.isLoggable(Level.WARNING)) {
            log(Level.WARNING, format, args, null);
        }
    }

    public void warn(Callback callback) {
        if (logger.isLoggable(Level.WARNING)) {
            try {
                log(Level.WARNING, callback.get(), null);
            } catch (RuntimeException ex) {
                logUncaughtExceptionInCb(ex, null);
            }
        }
    }

    public void warn(Throwable t, String format, Object... args) {
        if (logger.isLoggable(Level.WARNING)) {
            log(Level.WARNING, format, args, t);
        }
    }

    public void warn(Throwable t, Callback callback) {
        if (logger.isLoggable(Level.WARNING)) {
            try {
                log(Level.WARNING, callback.get(), null);
            } catch (RuntimeException ex) {
                logUncaughtExceptionInCb(ex, t);
            }
        }
    }

    public boolean isErrorEnabled() {
        return logger.isLoggable(Level.SEVERE);
    }

    public void error(String format, Object... args) {
        if (logger.isLoggable(Level.SEVERE)) {
            log(Level.SEVERE, format, args, null);
        }
    }

    public void error(Callback callback) {
        if (logger.isLoggable(Level.SEVERE)) {
            try {
                log(Level.SEVERE, callback.get(), null);
            } catch (RuntimeException ex) {
                logUncaughtExceptionInCb(ex, null);
            }
        }
    }

    public void error(Throwable t, String format, Object... args) {
        if (logger.isLoggable(Level.SEVERE)) {
            log(Level.SEVERE, format, args, t);
        }
    }

    public void error(Throwable t, Callback callback) {
        if (logger.isLoggable(Level.SEVERE)) {
            try {
                log(Level.SEVERE, callback.get(), null);
            } catch (RuntimeException ex) {
                logUncaughtExceptionInCb(ex, t);
            }
        }
    }

    private void log(Level level, String format, Object[] args, Throwable t) {
        try {
            log(level, String.format(format, args), t);
        } catch (RuntimeException ex) {
            log(Level.WARNING, String.format("Formatting error: `%s' [%s]", format, ex), t);
        }
    }

    private void logUncaughtExceptionInCb(RuntimeException ex, Throwable t) {
        log(Level.WARNING, String.format("Callback error: [%s]", ex), t);
    }

    private void log(Level level, String msg, Throwable t) {
        LogRecord record = new LogRecord(level, msg);
        record.setThrown(t);
        record.setLoggerName(LoggerName);
        fillCallerData(LoggerName, record);
        logger.log(record);
    }

    private void fillCallerData(String caller, LogRecord record) {
        StackTraceElement[] steArray = new Throwable().getStackTrace();

        int selfIndex = -1;
        for (int i = 0; i < steArray.length; i++) {
            if (steArray[i].getClassName().equals(caller)) {
                selfIndex = i;
                break;
            }
        }

        int found = -1;
        String originalClass = null;
        String originalMethod = null;
        for (int i = selfIndex + 1; i < steArray.length; i++) {
            if (!(steArray[i].getClassName().equals(caller))) {
                found = i;
                if(steArray[i].getMethodName().startsWith(METHOD_PREFIX)) {
                    originalClass = steArray[i].getClassName();
                    originalMethod = steArray[i].getMethodName().substring(METHOD_PREFIX.length());
                }
                break;
            }
        }

        if(originalClass != null) {
            for (int i = found + 1; i < steArray.length; i++) {
                if(originalClass.equals(steArray[i].getClassName()) && originalMethod.equals(steArray[i].getMethodName())) {
                    found = i;
                    break;
                }
            }
        }

        if (found != -1) {
            if(forward > 0) {
                found += forward;
            }
            StackTraceElement ste = steArray[found];

            record.setSourceClassName(ste.getClassName());
            record.setSourceMethodName(ste.getMethodName());
        }
    }

    @FunctionalInterface
    public interface Callback extends Supplier<String> {
        @Override
        String get();
    }
}
