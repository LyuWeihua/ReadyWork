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
package work.ready.cloud.jdbc.oltp;

import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;
import javax.sql.DataSource;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.configuration.ClientConnectorConfiguration;
import org.apache.ignite.internal.jdbc.thin.ConnectionPropertiesImpl;
import org.apache.ignite.internal.processors.odbc.SqlStateCode;
import org.apache.ignite.internal.util.HostAndPortRange;
import org.apache.ignite.internal.util.typedef.F;
import work.ready.cloud.jdbc.ReadyJdbcDriver;

public class OltpDataSource implements DataSource, Serializable {
    
    private static final long serialVersionUID = 0L;

    private ConnectionPropertiesImpl props = new ConnectionPropertiesImpl();

    private int loginTimeout;

    @Override public Connection getConnection() throws SQLException {
        return getConnection(null, null);
    }

    @Override public Connection getConnection(String username, String pwd) throws SQLException {
        Properties props = this.props.storeToProperties();

        if (!F.isEmpty(username))
            props.put("user", username);

        if (!F.isEmpty(pwd))
            props.put("password", pwd);

        return ReadyJdbcDriver.register().connect(getUrl(), props);
    }

    @Override public <T> T unwrap(Class<T> iface) throws SQLException {
        if (!isWrapperFor(iface))
            throw new SQLException("DataSource is not a wrapper for " + iface.getName());

        return (T)this;
    }

    @Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface != null && iface.isAssignableFrom(OltpDataSource.class);
    }

    @Override public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    @Override public void setLogWriter(PrintWriter out) throws SQLException {
        
    }

    @Override public void setLoginTimeout(int seconds) throws SQLException {
        loginTimeout = seconds;
    }

    @Override public int getLoginTimeout() throws SQLException {
        return loginTimeout;
    }

    @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return Logger.getLogger("org.apache.ignite");
    }

    public String getURL() {
        return getUrl();
    }

    public void setURL(String url) throws SQLException {
        setUrl(url);
    }

    public String[] getAddresses() {
        HostAndPortRange[] addrs = props.getAddresses();

        if (addrs == null)
            return null;

        String[] addrsStr = new String[addrs.length];

        for (int i = 0; i < addrs.length; ++i)
            addrsStr[i] = addrs[i].toString();

        return addrsStr;
    }

    public void setAddresses(String... addrsStr) throws SQLException {
        HostAndPortRange[] addrs = new HostAndPortRange[addrsStr.length];

        for (int i = 0; i < addrs.length; ++i) {
            try {
                addrs[i] = HostAndPortRange.parse(addrsStr[i],
                    ClientConnectorConfiguration.DFLT_PORT, ClientConnectorConfiguration.DFLT_PORT,
                    "Invalid endpoint format (should be \"host[:portRangeFrom[..portRangeTo]]\")");
            }
            catch (IgniteCheckedException e) {
                throw new SQLException(e.getMessage(), SqlStateCode.CLIENT_CONNECTION_FAILED, e);
            }
        }

        props.setAddresses(addrs);
    }

    public String getSchema() {
        return props.getSchema();
    }

    public void setSchema(String schema) {
        props.setSchema(schema);
    }

    public String getUrl() {
        return props.getUrl();
    }

    public void setUrl(String url) throws SQLException {
        props = new ConnectionPropertiesImpl();

        props.setUrl(url);
    }

    public boolean isDistributedJoins() {
        return props.isDistributedJoins();
    }

    public void setDistributedJoins(boolean distributedJoins) {
        props.setDistributedJoins(distributedJoins);
    }

    public boolean isEnforceJoinOrder() {
        return props.isEnforceJoinOrder();
    }

    public void setEnforceJoinOrder(boolean enforceJoinOrder) {
        props.setEnforceJoinOrder(enforceJoinOrder);
    }

    public boolean isCollocated() {
        return props.isCollocated();
    }

    public void setCollocated(boolean collocated) {
        props.setCollocated(collocated);
    }

    public boolean isReplicatedOnly() {
        return props.isReplicatedOnly();
    }

    public void setReplicatedOnly(boolean replicatedOnly) {
        props.setReplicatedOnly(replicatedOnly);
    }

    public boolean isAutoCloseServerCursor() {
        return props.isAutoCloseServerCursor();
    }

    public void setAutoCloseServerCursor(boolean autoCloseServerCursor) {
        props.setAutoCloseServerCursor(autoCloseServerCursor);
    }

    public int getSocketSendBuffer() {
        return props.getSocketSendBuffer();
    }

    public void setSocketSendBuffer(int size) throws SQLException {
        props.setSocketSendBuffer(size);
    }

    public int getSocketReceiveBuffer() {
        return props.getSocketReceiveBuffer();
    }

    public void setSocketReceiveBuffer(int size) throws SQLException {
        props.setSocketReceiveBuffer(size);
    }

    public boolean isTcpNoDelay() {
        return props.isTcpNoDelay();
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        props.setTcpNoDelay(tcpNoDelay);
    }

    public boolean isLazy() {
        return props.isLazy();
    }

    public void setLazy(boolean lazy) {
        props.setLazy(lazy);
    }

    public boolean isSkipReducerOnUpdate() {
        return props.isSkipReducerOnUpdate();
    }

    public void setSkipReducerOnUpdate(boolean skipReducerOnUpdate) {
        props.setSkipReducerOnUpdate(skipReducerOnUpdate);
    }

    public String getSslMode() {
        return props.getSslMode();
    }

    public void setSslMode(String mode) {
        props.setSslMode(mode);
    }

    public String getSslProtocol() {
        return props.getSslProtocol();
    }

    public void setSslProtocol(String sslProtocol) {
        props.setSslProtocol(sslProtocol);
    }

    public String getCipherSuites() {
        return props.getSslCipherSuites();
    }

    public void setCipherSuites(String cipherSuites) {
        props.setSslCipherSuites(cipherSuites);
    }

    public String getSslKeyAlgorithm() {
        return props.getSslKeyAlgorithm();
    }

    public void setSslKeyAlgorithm(String keyAlgorithm) {
        props.setSslKeyAlgorithm(keyAlgorithm);
    }

    public String getSslClientCertificateKeyStoreUrl() {
        return props.getSslClientCertificateKeyStoreUrl();
    }

    public void setSslClientCertificateKeyStoreUrl(String url) {
        props.setSslClientCertificateKeyStoreUrl(url);
    }

    public String getSslClientCertificateKeyStorePassword() {
        return props.getSslClientCertificateKeyStorePassword();
    }

    public void setSslClientCertificateKeyStorePassword(String passwd) {
        props.setSslClientCertificateKeyStorePassword(passwd);
    }

    public String getSslClientCertificateKeyStoreType() {
        return props.getSslClientCertificateKeyStoreType();
    }

    public void setSslClientCertificateKeyStoreType(String ksType) {
        props.setSslClientCertificateKeyStoreType(ksType);
    }

    public String getSslTrustCertificateKeyStoreUrl() {
        return props.getSslTrustCertificateKeyStoreUrl();
    }

    public void setSslTrustCertificateKeyStoreUrl(String url) {
        props.setSslTrustCertificateKeyStoreUrl(url);
    }

    public String getSslTrustCertificateKeyStorePassword() {
        return props.getSslTrustCertificateKeyStorePassword();
    }

    public void setSslTrustCertificateKeyStorePassword(String passwd) {
        props.setSslTrustCertificateKeyStorePassword(passwd);
    }

    public String getSslTrustCertificateKeyStoreType() {
        return props.getSslTrustCertificateKeyStoreType();
    }

    public void setSslTrustCertificateKeyStoreType(String ksType) {
        props.setSslTrustCertificateKeyStoreType(ksType);
    }

    public boolean isSslTrustAll() {
        return props.isSslTrustAll();
    }

    public void setSslTrustAll(boolean trustAll) {
        props.setSslTrustAll(trustAll);
    }

    public String getSslFactory() {
        return props.getSslFactory();
    }

    public void setSslFactory(String sslFactory) {
        props.setSslFactory(sslFactory);
    }

    public void setUsername(String name) {
        props.setUsername(name);
    }

    public String getUsername() {
        return props.getUsername();
    }

    public void setPassword(String passwd) {
        props.setPassword(passwd);
    }

    public String getPassword() {
        return props.getPassword();
    }
}
