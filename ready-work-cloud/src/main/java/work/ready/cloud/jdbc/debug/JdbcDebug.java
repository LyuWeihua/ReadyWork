/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package work.ready.cloud.jdbc.debug;

import work.ready.core.tools.define.SuppressForbidden;

import javax.sql.DataSource;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public final class JdbcDebug {

    private static final Map<String, DebugLog> OUTPUT_CACHE = new HashMap<>();
    
    private static final Map<String, Integer> OUTPUT_REFS = new HashMap<>();
    
    private static final Map<PrintWriter, DebugLog> OUTPUT_MANAGED = new HashMap<>();

    private static volatile DebugLog ERR = null, OUT = null;
    private static volatile PrintStream SYS_ERR = null, SYS_OUT = null;

    public static Connection proxy(JdbcDebugConfig info, Connection connection, PrintWriter managedPrinter) {
        return createProxy(Connection.class, new ConnectionProxy(logger(info, managedPrinter), connection));
    }

    static DatabaseMetaData proxy(DatabaseMetaDataProxy handler) {
        return createProxy(DatabaseMetaData.class, handler);
    }

    static ParameterMetaData proxy(ParameterMetaDataProxy handler) {
        return createProxy(ParameterMetaData.class, handler);
    }

    static ResultSet proxy(ResultSetProxy handler) {
        return createProxy(ResultSet.class, handler);
    }

    static ResultSetMetaData proxy(ResultSetMetaDataProxy handler) {
        return createProxy(ResultSetMetaData.class, handler);
    }

    static Statement proxy(Object statement, StatementProxy handler) {
        Class<? extends Statement> i = Statement.class;

        if (statement instanceof CallableStatement) {
            i = CallableStatement.class;
        }
        else if (statement instanceof PreparedStatement) {
            i = PreparedStatement.class;
        }

        return createProxy(i, handler);
    }

    @SuppressWarnings("unchecked")
    private static <P> P createProxy(Class<P> proxy, InvocationHandler handler) {
        return (P) Proxy.newProxyInstance(JdbcDebug.class.getClassLoader(), new Class<?>[] { DebugProxy.class, proxy }, handler);
    }

    private static DebugLog logger(JdbcDebugConfig info, PrintWriter managedPrinter) {
        DebugLog log = null;

        if (managedPrinter != null) {
            synchronized (JdbcDebug.class) {
                log = OUTPUT_MANAGED.get(managedPrinter);
                if (log == null) {
                    log = createLog(managedPrinter, info.flushAlways());
                    OUTPUT_MANAGED.put(managedPrinter, log);
                }
            }
        }

        String out = info.debugOut();

        if ("err".equals(out)) {
            PrintStream sys = stderr();

            if (SYS_ERR == null) {
                SYS_ERR = sys;
            }
            if (SYS_ERR != sys) {
                SYS_ERR.flush();
                SYS_ERR = sys;
                ERR = null;
            }
            if (ERR == null) {
                ERR = createLog(new PrintWriter(new OutputStreamWriter(sys, StandardCharsets.UTF_8)), info.flushAlways());
            }
            return ERR;
        }

        if ("out".equals(out)) {
            PrintStream sys = stdout();

            if (SYS_OUT == null) {
                SYS_OUT = sys;
            }

            if (SYS_OUT != sys) {
                SYS_OUT.flush();
                SYS_OUT = sys;
                OUT = null;
            }

            if (OUT == null) {
                OUT = createLog(new PrintWriter(new OutputStreamWriter(sys, StandardCharsets.UTF_8)), info.flushAlways());
            }
            return OUT;
        }

        synchronized (JdbcDebug.class) {
            log = OUTPUT_CACHE.get(out);
            if (log == null) {
                
                try {
                    PrintWriter print = new PrintWriter(Files.newBufferedWriter(Paths.get("").resolve(out), StandardCharsets.UTF_8));
                    log = createLog(print, info.flushAlways());
                    OUTPUT_CACHE.put(out, log);
                    OUTPUT_REFS.put(out, Integer.valueOf(0));
                } catch (Exception ex) {
                    throw new JdbcException(ex, "Cannot open debug output [" + out + "]");
                }
            }
            OUTPUT_REFS.put(out, Integer.valueOf(OUTPUT_REFS.get(out).intValue() + 1));
        }

        return log;
    }

    private static DebugLog createLog(PrintWriter print, boolean flushAlways) {
        DebugLog log = new DebugLog(print, flushAlways);
        log.logSystemInfo();
        return log;
    }

    public static void release(JdbcDebugConfig info) {
        if (!info.debug()) {
            return;
        }

        String out = info.debugOut();
        synchronized (JdbcDebug.class) {
            Integer ref = OUTPUT_REFS.get(out);
            if (ref != null) {
                int r = ref.intValue();
                if (r < 2) {
                    OUTPUT_REFS.remove(out);
                    DebugLog d = OUTPUT_CACHE.remove(out);
                    if (d != null) {
                        if (d.print != null) {
                            d.print.close();
                        }
                    }
                }
                else {
                    OUTPUT_REFS.put(out, Integer.valueOf(r - 1));
                }
            }
        }
    }

    public static synchronized void close() {
        
        OUTPUT_REFS.clear();

        for (DebugLog d : OUTPUT_CACHE.values()) {
            if (d.print != null) {
                d.print.close();
            }
        }
        OUTPUT_CACHE.clear();

        for (DebugLog d : OUTPUT_MANAGED.values()) {
            d.print.flush();
        }

        OUTPUT_MANAGED.clear();
    }

    @SuppressForbidden(reason = "JDBC drivers allows logging to Sys.out")
    private static PrintStream stdout() {
        return System.out;
    }

    @SuppressForbidden(reason = "JDBC drivers allows logging to Sys.err")
    private static PrintStream stderr() {
        return System.err;
    }
}
