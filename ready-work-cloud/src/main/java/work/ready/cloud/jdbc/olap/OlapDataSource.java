/**
 *
 * Original work copyright Elasticsearch.
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
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
package work.ready.cloud.jdbc.olap;

import work.ready.cloud.jdbc.debug.JdbcDebug;
import work.ready.cloud.jdbc.olap.client.ClientVersion;
import work.ready.cloud.jdbc.olap.client.ConnectionConfiguration;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Wrapper;
import java.util.Properties;
import java.util.logging.Logger;

public class OlapDataSource implements DataSource, Wrapper {

    static {
        
        ClientVersion.CURRENT.toString();
    }

    private String url;
    private PrintWriter writer;
    private int loginTimeout;
    private Properties props;

    public OlapDataSource() {}

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return writer;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.writer = out;
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        if (seconds < 0) {
            throw new SQLException("Negative timeout specified " + seconds);
        }
        loginTimeout = seconds;
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return loginTimeout;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Properties getProperties() {
        Properties copy = new Properties();
        if (props != null) {
            copy.putAll(props);
        }
        return copy;
    }

    public void setProperties(Properties props) {
        this.props = new Properties();
        this.props.putAll(props);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return doGetConnection(getProperties());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Properties p = getProperties();
        p.setProperty(ConnectionConfiguration.AUTH_USER, username);
        p.setProperty(ConnectionConfiguration.AUTH_PASS, password);
        return doGetConnection(p);
    }

    private Connection doGetConnection(Properties p) throws SQLException {
        JdbcConfiguration cfg = JdbcConfiguration.create(url, p, loginTimeout);
        JdbcConnection con = new JdbcConnection(cfg);
        
        return cfg.debug() ? JdbcDebug.proxy(cfg, con, writer) : con;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface != null && iface.isAssignableFrom(getClass());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (isWrapperFor(iface)) {
            return (T) this;
        }
        throw new SQLException();
    }
}
