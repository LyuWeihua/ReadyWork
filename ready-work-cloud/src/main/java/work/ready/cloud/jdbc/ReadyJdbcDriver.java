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

package work.ready.cloud.jdbc;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;
import work.ready.cloud.jdbc.debug.JdbcDebug;
import work.ready.cloud.jdbc.olap.JdbcConfiguration;
import work.ready.cloud.jdbc.olap.JdbcSQLException;
import work.ready.cloud.jdbc.oltp.ConnectionProperties;

public class ReadyJdbcDriver implements Driver {
    
    public static final String OLTP_URL_PREFIX = "jdbc:ready:oltp://";
    public static final String OLAP_URL_PREFIX = "jdbc:ready:olap://";

    private static final ReadyJdbcDriver INSTANCE = new ReadyJdbcDriver();

    private static volatile boolean registered;

    static {
        register();
    }

    private static final int MAJOR_VER = 0;

    private static final int MINOR_VER = 6;

    @Override public Connection connect(String url, Properties props) throws SQLException {
        if (url == null) {
            throw new JdbcSQLException("Non-null url required");
        }
        if (!acceptsURL(url)) {
            return null;
        }

        if(url.startsWith(OLTP_URL_PREFIX)) {   
            ConnectionProperties cfg = new ConnectionProperties();
            cfg.init(url, props);
            var con = new work.ready.cloud.jdbc.oltp.JdbcConnection(cfg);
            return cfg.debug() ? JdbcDebug.proxy(cfg, con, DriverManager.getLogWriter()) : con;
        } else {    
            JdbcConfiguration cfg = initCfg(url, props);
            var con = new work.ready.cloud.jdbc.olap.JdbcConnection(cfg);
            return cfg.debug() ? JdbcDebug.proxy(cfg, con, DriverManager.getLogWriter()) : con;
        }
    }

    private static JdbcConfiguration initCfg(String url, Properties props) throws JdbcSQLException {
        return JdbcConfiguration.create(url, props, DriverManager.getLoginTimeout());
    }

    @Override public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith(OLTP_URL_PREFIX) || url.startsWith(OLAP_URL_PREFIX);
    }

    @Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return new DriverPropertyInfo[0];
        }
        if(url.startsWith(OLTP_URL_PREFIX)) {   
            ConnectionProperties connProps = new ConnectionProperties();

            connProps.init(url, info);

            return connProps.getDriverPropertyInfo();
        } else {    
            return JdbcConfiguration.create(url, info, DriverManager.getLoginTimeout()).driverPropertyInfo();
        }
    }

    @Override public int getMajorVersion() {
        return MAJOR_VER;
    }

    @Override public int getMinorVersion() {
        return MINOR_VER;
    }

    @Override public boolean jdbcCompliant() {
        return false;
    }

    @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("java.util.logging is not used.");
    }

    public static synchronized Driver register() {
        try {
            if (!registered) {
                DriverManager.registerDriver(INSTANCE, INSTANCE::close);

                registered = true;
            }
        }
        catch (SQLException e) {

            PrintWriter writer = DriverManager.getLogWriter();
            if (writer != null) {
                e.printStackTrace(writer);
                writer.flush();
            }
            throw new ExceptionInInitializerError(new RuntimeException("Failed to register Ready JDBC driver.", e));
        }

        return INSTANCE;
    }

    public static void deregister() throws SQLException {
        try {
            DriverManager.deregisterDriver(INSTANCE);
        } catch (SQLException e) {

            PrintWriter writer = DriverManager.getLogWriter();
            if (writer != null) {
                e.printStackTrace(writer);
                writer.flush();
            }
            throw new RuntimeException("Failed to deregister Ready JDBC driver.", e);
        }
    }

    private void close() {
        JdbcDebug.close();
    }
}
